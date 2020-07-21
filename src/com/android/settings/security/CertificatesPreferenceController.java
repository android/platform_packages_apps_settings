/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.security;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterDefs;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CertificatesPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnPreferenceClickListener,
        LifecycleObserver, OnPause, OnResume {

    private static final String TAG = "CertificatesPrefController";

    //TODO check if all member variables used
    private UserManager mUm;
    private String[] mAuthorities;
    private SettingsPreferenceFragment mParent;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private @ProfileSelectFragment.ProfileType int mType;
    private PreferenceCategory mUserCredentialsPreferenceCategory;

    public CertificatesPreferenceController(Context context, SettingsPreferenceFragment parent,
            String[] authorities, @ProfileSelectFragment.ProfileType int type) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mAuthorities = authorities;
        mParent = parent;
        final FeatureFactory featureFactory = FeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
        mType = type;
        Log.d(TAG, "Created CertPrefController with type " + mType);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUserCredentialsPreferenceCategory =
                (PreferenceCategory) mParent.findPreference("user_credentials");
    }

    //TODO check if actually used
    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {
        updateUi();
    }

    /**
     * Creates a Preference for the given {@link Credential} and adds it to the
     * {@link #mUserCredentialsPreferenceCategory}.
     */
    private void addUserCredentialPreference(Credential cred) {
        final Preference pref = new Preference(mContext);

        // Launch details page or captive portal on click.
        pref.setOnPreferenceClickListener(
                preference -> {
                    //TODO: launch details preference and remove log
                    Log.d(TAG, "Real preference pressed!");
                    UserCredentialsSettings.CredentialDialogFragment.show(mParent,cred);
                    return true;
                });
        pref.setTitle(cred.getAlias());
        pref.setSummary(cred.isSystem()
                ? R.string.credential_for_vpn_and_apps
                : R.string.credential_for_wifi);
        pref.setIcon(R.drawable.ic_friction_lock_closed);
        mUserCredentialsPreferenceCategory.addPreference(pref);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        //TODO
        return false;
    }

    private void updateUi() {
        if (!isAvailable()) {
            // This should not happen
            Log.e(TAG, "We should not be showing settings for a managed profile");
            return;
        }
        new AliasLoader().execute();
    }

    /**
     * Opens a background connection to KeyStore to list user credentials.
     * A preference is then created for each credentials in the fragment.
     */
    private class AliasLoader extends AsyncTask<Void, Void, List<Credential>> {
        /**
         * @return a list of credentials ordered:
         * <ol>
         *   <li>first by purpose;</li>
         *   <li>then by alias.</li>
         * </ol>
         */
        @Override
        protected List<Credential> doInBackground(Void... params) {

            // Certificates can be installed into SYSTEM_UID or WIFI_UID through CertInstaller.
            final int myUserId = UserHandle.myUserId();

            Log.d(TAG, "myUserId is: " + myUserId + "mType is: " + mType);

            int profileId = 0;
            int i=0;
            for(UserInfo user : mUm.getProfiles(myUserId)) {
                i++;
                Log.d(TAG, "User No " + i + " is " + user + " and has ID " + user.id);

                if(user.isManagedProfile() && (mType == ProfileSelectFragment.WORK ||
                        mType == ProfileSelectFragment.ALL)) {
                    Log.d(TAG, user + "is Managed Tab and Managed Profile");

                    profileId = user.id;
                    break;
                }
                profileId = user.id;

            }
            final int systemUid = UserHandle.getUid(profileId, Process.SYSTEM_UID);
            final int wifiUid = UserHandle.getUid(profileId, Process.WIFI_UID);

            Log.d(TAG, "ProfileId is: " + profileId);
            List<Credential> credentials = new ArrayList<>();
            credentials.addAll(getCredentialsForUid(systemUid, profileId).values());
            if(profileId == UserHandle.USER_SYSTEM) credentials.addAll(getCredentialsForUid(wifiUid,
                    profileId).values());
            return credentials;
        }

        private SortedMap<String, Credential> getCredentialsForUid(int uid, int userId) {
            try {
                KeyChainConnection connection = KeyChain.bindAsUser(mContext,
                        UserHandle.of(userId));
                IKeyChainService keyChainService = connection.getService();

                final SortedMap<String, Credential> aliasMap = new TreeMap<>();
                for (final Credential.Type type : Credential.Type.values()) {
                    for (final String prefix : type.prefix) {
                        for (final String alias :
                                keyChainService.getUserCertificateAliases(prefix, uid)) {
                            if (UserHandle.getAppId(uid) == Process.SYSTEM_UID) {
                                // Do not show work profile keys in user credentials
                                if (alias.startsWith(LockPatternUtils.PROFILE_KEY_NAME_ENCRYPT) ||
                                        alias.startsWith(LockPatternUtils.PROFILE_KEY_NAME_DECRYPT))
                                {
                                    continue;
                                }
                                // Do not show synthetic password keys in user credential
                                if (alias.startsWith(LockPatternUtils.SYNTHETIC_PASSWORD_KEY_PREFIX)
                                ) {
                                    continue;
                                }
                            }
                            try {
                                if (type == Credential.Type.USER_KEY &&
                                        !isAsymmetric(keyChainService, null,
                                                prefix + alias, uid)) {
                                    continue;
                                }
                            } catch (UnrecoverableKeyException e) {
                                Log.e(TAG, "Unable to determine algorithm of key: " +
                                        prefix + alias, e);
                                continue;
                            }
                            Credential c = aliasMap.get(alias);
                            if (c == null) {
                                c = new Credential(alias, uid);
                                aliasMap.put(alias, c);
                            }
                            c.storedTypes.add(type);
                        }
                    }
                }
                connection.close(); //necessary? put in a finally clause?
                return aliasMap;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while getting certificate aliases from KeyChain");
                return null;
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while binding KeyChain profile.");
                return null;
            }
        }

        private SortedMap<String, Credential> getCredentialsForWifi(int uid) {
            final KeyStore keyStore = KeyStore.getInstance();
            final SortedMap<String, Credential> aliasMap = new TreeMap<>();
            for (final Credential.Type type : Credential.Type.values()) {
                for (final String prefix : type.prefix) {
                    for (final String alias : keyStore.list(prefix, uid)) {
                        try {
                            if (type == Credential.Type.USER_KEY &&
                                    !isAsymmetric(null, keyStore,
                                            prefix + alias, uid)) {
                                continue;
                            }
                        } catch (UnrecoverableKeyException e) {
                            Log.e(TAG, "Unable to determine algorithm of key: " + prefix + alias, e);
                            continue;
                        } catch (RemoteException e) {
                            Log.e(TAG, "Unable to get KeyCharacteristics of key: " +
                                    prefix + alias);
                            continue;
                        }
                        Credential c = aliasMap.get(alias);
                        if (c == null) {
                            c = new Credential(alias, uid);
                            aliasMap.put(alias, c);
                        }
                        c.storedTypes.add(type);
                    }
                }
            }
            return aliasMap;
        }

        private boolean isAsymmetric(@Nullable IKeyChainService keyChainService,
                @Nullable KeyStore keyStore, String alias, int uid)
            throws UnrecoverableKeyException, RemoteException {
            KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
            int errorCode;
            // Retrieve error code through KeyChain only for System certificates
            if(keyStore == null) {
                errorCode = keyChainService.getKeyCharacteristics(alias, uid,
                        keyCharacteristics);
            }else {
                errorCode = keyStore.getKeyCharacteristics(alias, null, null, uid,
                        keyCharacteristics);
            }
            if (errorCode != KeyStore.NO_ERROR) {
                throw (UnrecoverableKeyException)
                        new UnrecoverableKeyException("Failed to obtain information about key")
                                .initCause(KeyStore.getKeyStoreException(errorCode));
            }
            Integer keymasterAlgorithm = keyCharacteristics.getEnum(
                    KeymasterDefs.KM_TAG_ALGORITHM);
            if (keymasterAlgorithm == null) {
                throw new UnrecoverableKeyException("Key algorithm unknown");
            }
            return keymasterAlgorithm == KeymasterDefs.KM_ALGORITHM_RSA ||
                    keymasterAlgorithm == KeymasterDefs.KM_ALGORITHM_EC;
        }

        @Override
        protected void onPostExecute(List<Credential> credentials) {
            mUserCredentialsPreferenceCategory.removeAll();
            for (Credential item : credentials) {
                addUserCredentialPreference(item);
            }
        }
    }
}
