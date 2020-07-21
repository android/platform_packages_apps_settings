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

import android.content.Context;
import android.content.Intent;
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
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CertificatesPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume {

    private static final String TAG = "CertificatesPrefController";
    private static final String KEY_USER_CREDENTIALS = "user_credentials";

    private UserManager mUm;
    private SettingsPreferenceFragment mParent;
    private @ProfileSelectFragment.ProfileType int mType;
    private PreferenceCategory mUserCredentialsPreferenceCategory;
    private RestrictedPreference mInstallPreference;
    private RestrictedPreference mDeletePreference;
    private int mUserId;

    public CertificatesPreferenceController(Context context, SettingsPreferenceFragment parent,
            @ProfileSelectFragment.ProfileType int type, Lifecycle lifecycle) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mParent = parent;
        mType = type;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mUserId = UserHandle.myUserId();

        if(mType == ProfileSelectFragment.WORK) {
            for(UserInfo user : mUm.getProfiles(UserHandle.myUserId())) {
                if(user.isManagedProfile()) mUserId = user.id;
            }
        }
        Log.d(TAG, "Created CertPrefController with type " + mType + " & mUserId " + mUserId);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_USER_CREDENTIALS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUserCredentialsPreferenceCategory = mParent.findPreference(KEY_USER_CREDENTIALS);

//        mInstallPreference = mParent.findPreference("install_certificate");
//        mInstallPreference.setOnPreferenceClickListener(
//                preference -> {
//                    Intent installIntent = new Intent(CredentialStorage.ACTION_INSTALL);
//                    installIntent.setPackage("com.android.certinstaller");
//                    installIntent.setClass(mContext, com.android.certinstaller.CertInstallerMain);
//                    // Same value as CERTIFICATE_USAGE_CA in keystore/java/android/security/Credentials.java
//                    <extra android:name="certificate_install_usage" android:value="ca"/>
//                    mContext.startActivityAsUser(installIntent, UserHandle.of(mUserId));
//                    return true;
//                });
        mDeletePreference = (mType == ProfileSelectFragment.WORK ?
                mParent.findPreference("credentials_reset_work")
                : mParent.findPreference("credentials_reset_personal"));
        mDeletePreference.setOnPreferenceClickListener(
            preference -> {
                Intent deleteIntent = new Intent(CredentialStorage.ACTION_RESET);
                deleteIntent.setPackage("com.android.settings");
                mContext.startActivityAsUser(deleteIntent, UserHandle.of(mUserId));
                return true;
            });
    }

    @Override
    public void onResume() {
        mParent.onResume();
        updateUi();

    }

//    @Override
//    public void updateState(Preference preference) {
//        updateUi();
//    }

    /**
     * Creates a Preference for the given {@link Credential} and adds it to the
     * {@link #mUserCredentialsPreferenceCategory}.
     */
    private void addUserCredentialPreference(Credential cred) {
        final Preference pref = new Preference(mContext);

        // Launch details page or captive portal on click.
        pref.setOnPreferenceClickListener(
                preference -> {
                    UserCredentialsSettings.CredentialDialogFragment.show(mParent, cred, this);
                    return true;
                });
        pref.setTitle(cred.getAlias());
        pref.setSummary(cred.isSystem()
                ? R.string.credential_for_vpn_and_apps
                : R.string.credential_for_wifi);
        pref.setIcon(R.drawable.ic_friction_lock_closed);
        mUserCredentialsPreferenceCategory.addPreference(pref);
    }



    public void updateUi() {
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
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                Log.e(TAG, "Thread attempted to sleep 5000 " + e);
//            }
            // Certificates can be installed into SYSTEM_UID or WIFI_UID through CertInstaller.
            final int systemUid = UserHandle.getUid(mUserId, Process.SYSTEM_UID);
            final int wifiUid = UserHandle.getUid(mUserId, Process.WIFI_UID);

            List<Credential> credentials = new ArrayList<>();
            credentials.addAll(getCredentialsForUid(systemUid).values());
            if(mUserId == UserHandle.USER_SYSTEM) credentials.addAll(
                    getCredentialsForUid(wifiUid).values());
            return credentials;
        }

        private SortedMap<String, Credential> getCredentialsForUid(int uid) {
            try {
                KeyChainConnection connection = KeyChain.bindAsUser(mContext,
                        UserHandle.of(mUserId));
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
                                        !isAsymmetric(keyChainService,prefix + alias, uid)) {
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

        private boolean isAsymmetric(IKeyChainService keyChainService, String alias, int uid)
            throws UnrecoverableKeyException, RemoteException {
            KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
            int errorCode = keyChainService.getKeyCharacteristics(alias, uid,
                        keyCharacteristics);
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
