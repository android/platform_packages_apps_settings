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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
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
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Controller managing User Credentials for both personal and work profiles
 */
public class CertificatesPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume {

    private static final String TAG = "CertificatesPrefController";
    private static final String KEY_USER_CREDENTIALS = "user_credentials";

    private SettingsPreferenceFragment mParent;
    private PreferenceCategory mUserCredentialsPreferenceCategory;
    private int mUserId;

    public CertificatesPreferenceController(Context context, SettingsPreferenceFragment parent,
            int userId, Lifecycle lifecycle) {
        super(context);
        mParent = parent;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mUserId = userId;
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
    }

    @Override
    public void onResume() {
        mParent.onResume();
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
                    showUserCert(cred);
                    return true;
                });
        pref.setTitle(cred.getAlias());
        pref.setSummary(cred.isSystem()
                ? R.string.user_certificate
                : R.string.wifi_certificate);
        pref.setIcon(R.drawable.ic_friction_lock_closed);
        mUserCredentialsPreferenceCategory.addPreference(pref);
    }

    /**
     * Creates a Preference for the given {@link Credential} and adds it to the
     * {@link #mUserCredentialsPreferenceCategory}.
     */
    private void addCaCertificatePreference(Credential cred, String title) {
        final Preference pref = new Preference(mContext);

        // Launch details page or captive portal on click.
        pref.setOnPreferenceClickListener(
                preference -> {
                    showCaCert(cred);
                    return true;
                });
        pref.setTitle(title);
        pref.setSummary(R.string.ca_certificate);
        pref.setIcon(R.drawable.ic_friction_lock_closed);
        mUserCredentialsPreferenceCategory.addPreference(pref);
    }

    private void showCaCert(final Credential cred) {
        Bundle args = new Bundle();
        args.putString(TrustedCredentialsDetailsPreference.ARG_ALIAS, cred.getAlias());
        args.putInt(TrustedCredentialsDetailsPreference.ARG_PROFILE_ID, mUserId);
        args.putBoolean(TrustedCredentialsDetailsPreference.ARG_IS_SYSTEM, false);

        new SubSettingLauncher(mContext)
                .setDestination(TrustedCredentialsDetailsPreference.class.getName())
                .setSourceMetricsCategory(SettingsEnums.MANAGE_CERTIFICATES)
                .setArguments(args)
                .launch();
    }

    private void showUserCert(final Credential cred) {
        Bundle args = new Bundle();
        args.putString(UserCredentialsDetailsFragment.ARG_ALIAS, cred.getAlias());
        args.putInt(UserCredentialsDetailsFragment.ARG_UID, cred.getId());
        args.putInt(UserCredentialsDetailsFragment.ARG_USER_ID, mUserId);
        args.putBoolean(Credential.Type.USER_KEY.toString(),
                cred.getStoredTypes().contains(Credential.Type.USER_KEY));
        args.putBoolean(Credential.Type.USER_CERTIFICATE.toString(),
                cred.getStoredTypes().contains(Credential.Type.USER_CERTIFICATE));
        args.putBoolean(Credential.Type.CA_CERTIFICATE.toString(),
                cred.getStoredTypes().contains(Credential.Type.CA_CERTIFICATE));

        new SubSettingLauncher(mContext)
                .setDestination(UserCredentialsDetailsFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.MANAGE_CERTIFICATES)
                .setArguments(args)
                .launch();
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
        Map<Credential, SslCertificate> mCaCerts = new HashMap<>();

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
            final int systemUid = UserHandle.getUid(mUserId, Process.SYSTEM_UID);
            final int wifiUid = UserHandle.getUid(mUserId, Process.WIFI_UID);

            List<Credential> credentials = new ArrayList<>();

            final KeyChainConnection connection;
            try {
                connection = KeyChain.bindAsUser(mContext, UserHandle.of(mUserId));
                IKeyChainService service = connection.getService();
                credentials.addAll(getCredentialsForUid(systemUid, service).values());
                if (mUserId == UserHandle.USER_SYSTEM) {
                    credentials.addAll(getCredentialsForUid(wifiUid, service).values());
                }
                credentials.addAll(getCaCredentials(service).values());
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while binding KeyChain profile.");
                return null;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while getting certificate aliases from KeyChain");
                return null;
            }
            connection.close();
            return credentials;
        }

        private SortedMap<String, Credential> getCaCredentials(IKeyChainService keyChainService)
                throws RemoteException {
            final SortedMap<String, Credential> aliasMap = new TreeMap<>();
            for (final String alias : keyChainService.getUserCaAliases().getList()) {
                Credential c = aliasMap.get(alias);
                if (c == null) {
                    c = Credential.buildCaCertificate(alias, mUserId);
                    aliasMap.put(alias, c);
                    c.addType(Credential.Type.CA_CERTIFICATE);

                    byte[] encodedCertificate = new byte[0];
                    encodedCertificate = keyChainService.getEncodedCaCertificate(alias,
                            true);
                    X509Certificate cert = KeyChain.toCertificate(encodedCertificate);
                    SslCertificate sslCert = new SslCertificate(cert);
                    mCaCerts.put(c, sslCert);
                }
            }
            return aliasMap;
        }

        private SortedMap<String, Credential> getCredentialsForUid(int uid,
                IKeyChainService keyChainService) throws RemoteException {
            final SortedMap<String, Credential> aliasMap = new TreeMap<>();
            for (final Credential.Type type : Credential.Type.values()) {
                for (final String prefix : type.prefix) {
                    for (final String alias :
                            keyChainService.getUserCertificateAliases(prefix, uid)) {
                        if (UserHandle.getAppId(uid) == Process.SYSTEM_UID) {
                            // Do not show work profile keys in user credentials
                            if (alias.startsWith(LockPatternUtils.PROFILE_KEY_NAME_ENCRYPT)
                                    || alias.startsWith(
                                            LockPatternUtils.PROFILE_KEY_NAME_DECRYPT)) {
                                continue;
                            }
                            // Do not show synthetic password keys in user credential
                            if (alias.startsWith(
                                    LockPatternUtils.SYNTHETIC_PASSWORD_KEY_PREFIX)) {
                                continue;
                            }
                        }
                        try {
                            if (type == Credential.Type.USER_KEY && !isAsymmetric(keyChainService,
                                    prefix + alias, uid)) {
                                continue;
                            }
                        } catch (UnrecoverableKeyException e) {
                            Log.e(TAG, "Unable to determine algorithm of key: "
                                    + prefix + alias, e);
                            continue;
                        } catch (RemoteException e) {
                            Log.e(TAG, "Unable to get KeyCharacteristics of key: "
                                    + prefix + alias, e);
                            continue;
                        }
                        Credential c = aliasMap.get(alias);
                        if (c == null) {
                            c = Credential.buildUserCredential(alias, uid);
                            aliasMap.put(alias, c);
                        }
                        c.addType(type);
                    }
                }
            }
            return aliasMap;
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
            return keymasterAlgorithm == KeymasterDefs.KM_ALGORITHM_RSA
                    || keymasterAlgorithm == KeymasterDefs.KM_ALGORITHM_EC;
        }

        @Override
        protected void onPostExecute(List<Credential> credentials) {
            mUserCredentialsPreferenceCategory.removeAll();
            for (Credential item : credentials) {
                if (item.isCA()) {
                    SslCertificate ssl = mCaCerts.get(item);
                    addCaCertificatePreference(item, getTitle(ssl));
                } else {
                    addUserCredentialPreference(item);
                }
            }
        }

        private String getTitle(SslCertificate cert) {
            String OName = cert.getIssuedTo().getOName();
            String CName = cert.getIssuedTo().getCName();
            String DName = cert.getIssuedTo().getDName();
            if (!OName.isEmpty()) {
                return OName;
            } else if (!CName.isEmpty()) {
                return CName;
            } else {
                return DName;
            }
        }
    }
}
