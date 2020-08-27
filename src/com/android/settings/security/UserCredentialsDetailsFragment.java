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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.widget.LayoutPreference;

import java.util.EnumSet;

/**
 * Fragment containing details for a single User Credential.
 * Triggered when user credential is clicked in CertificatesPreferenceController.
 */
public class UserCredentialsDetailsFragment extends DashboardFragment {

    private static final String TAG = "UserCredentialsDetailsFragment";

    public static final String ARG_ALIAS = "alias";
    public static final String ARG_UID = "uid";
    public static final String ARG_USER_ID = "user_id";

    private SettingsActivity mActivity;
    private PreferenceScreen mScreen;
    private LayoutPreference mHeader;
    private PreferenceCategory mCertificateCategory;
    private RestrictedPreference mRemovePref;
    private Credential mCredential;
    private int mUid;
    private String mAlias;
    private int mUserId;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_USER_CREDENTIAL;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mActivity = (SettingsActivity) getActivity();

        Bundle args = getArguments();
        mAlias = (String) args.get(ARG_ALIAS);
        mUid = (int) args.get(ARG_UID);
        mUserId = (int) args.get(ARG_USER_ID);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        mScreen = getPreferenceScreen();
        mHeader = (LayoutPreference) mScreen.findPreference("header_view");

        Preference userCertificate = mScreen.findPreference("user_certificate");
        userCertificate.setOnPreferenceClickListener(preference -> {
            // TODO: Open details fragment for user certificate
            return true;
        });
        mCertificateCategory = mScreen.findPreference("ca_certificates");
        mRemovePref = mScreen.findPreference("user_credential_remove");
        mRemovePref.setOnPreferenceClickListener(preference -> {
            remove();
            return true;
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mRemovePref.checkRestrictionAndSetDisabled(UserManager.DISALLOW_CONFIG_CREDENTIALS,
                mUserId);
        new AliasLoader().execute();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.user_credential_details;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    private void addCaPreference(Credential cred) {
        final Preference pref = new Preference(getActivity());

        // Launch details page or captive portal on click.
        pref.setOnPreferenceClickListener(
                preference -> {
                    // showCaCert(cred);
                    return true;
                });
        pref.setTitle("CA Certificate");
        mCertificateCategory.addPreference(pref);
    }

    private void showCaCert(final Credential cred) {
        Bundle args = new Bundle();
        args.putString(TrustedCredentialsDetailsPreference.ARG_ALIAS, cred.getAlias());
        args.putInt(TrustedCredentialsDetailsPreference.ARG_PROFILE_ID, mUserId);
        args.putBoolean(TrustedCredentialsDetailsPreference.ARG_IS_SYSTEM, false);

        new SubSettingLauncher(getActivity())
                .setDestination(TrustedCredentialsDetailsPreference.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .setArguments(args)
                .launch();
    }

    private void remove() {
        new RemoveCredentialsTask(getContext()).execute(mCredential);
    }

    private class AliasLoader extends AsyncTask<Void, Void, Credential> {
        private Context mContext;

        AliasLoader() {
            mContext = getActivity();
        }

        @Override protected void onPreExecute() {
            mScreen.setVisible(false);
        }
        @Override protected Credential doInBackground(Void... params) {
            Credential cred = new Credential(mAlias, mUid, false);
            if (getArguments().getBoolean(Credential.Type.USER_KEY.toString())) {
                cred.addType(Credential.Type.USER_KEY);
            }
            if (getArguments().getBoolean(Credential.Type.USER_CERTIFICATE.toString())) {
                cred.addType(Credential.Type.USER_CERTIFICATE);
            }
            if (getArguments().getBoolean(Credential.Type.CA_CERTIFICATE.toString())) {
                cred.addType(Credential.Type.CA_CERTIFICATE);
            }
            //TODO actually get user and CA certificate using KeyChain
            return cred;
        }

        @Override protected void onPostExecute(Credential cred) {
            if (cred == null) {
                Toast.makeText(mContext, "Empty Certificate", Toast.LENGTH_SHORT).show();
                return;
            }
            mCredential = cred;
            mActivity.setTitle(mAlias);

            ((ImageView) mHeader.findViewById(R.id.entity_header_icon))
                    .setImageResource(R.drawable.ic_friction_lock_closed);
            ((TextView) mHeader.findViewById(R.id.entity_header_title)).setText(mAlias);
            ((TextView) mHeader.findViewById(R.id.entity_header_summary))
                    .setText(mCredential.isSystem()
                            ? R.string.user_certificate
                            : R.string.wifi_certificate);

            if (cred.getStoredTypes().contains(Credential.Type.CA_CERTIFICATE)) {
                addCaPreference(cred);
            }

            mScreen.setVisible(true);
        }

    }

    /**
     * Deletes all certificates and keys under a given alias.
     *
     * If the {@link Credential} is for a system alias, all active grants to the alias will be
     * removed using {@link KeyChain}. If the {@link Credential} is for Wi-Fi alias, all
     * credentials and keys will be removed using {@link KeyStore}.
     */
    private class RemoveCredentialsTask extends AsyncTask<Credential, Void, Credential[]> {
        private Context mContext;

        RemoveCredentialsTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected Credential[] doInBackground(Credential... credentials) {
            final KeyChain.KeyChainConnection conn;
            try {
                conn = KeyChain.bindAsUser(mContext, UserHandle.of(mUserId));
            } catch (InterruptedException e) {
                Log.w(TAG, "Connecting to KeyChain", e);
                return null;
            }
            try {
                IKeyChainService service = conn.getService();
                for (final Credential credential : credentials) {
                    if (credential.isSystem()) {
                        service.removeKeyPair(credential.getAlias());
                    } else {
                        deleteWifiCredential(service, credential);
                    }
                }
                return credentials;
            } catch (RemoteException e) {
                Log.w(TAG, "Removing credentials", e);
                return null;
            } finally {
                conn.close();
            }

        }

        private void deleteWifiCredential(IKeyChainService service, final Credential credential)
                throws RemoteException {
            final EnumSet<Credential.Type> storedTypes = credential.getStoredTypes();

            // Remove all Wi-Fi credentials
            if (storedTypes.contains(Credential.Type.USER_KEY)) {
                service.deleteWifiCertificate(Credentials.USER_PRIVATE_KEY
                        + credential.getAlias(), Process.WIFI_UID);
            }
            if (storedTypes.contains(Credential.Type.USER_CERTIFICATE)) {
                service.deleteWifiCertificate(Credentials.USER_CERTIFICATE
                        + credential.getAlias(), Process.WIFI_UID);
            }
            if (storedTypes.contains(Credential.Type.CA_CERTIFICATE)) {
                service.deleteWifiCertificate(Credentials.CA_CERTIFICATE
                        + credential.getAlias(), Process.WIFI_UID);
            }
        }

        @Override
        protected void onPostExecute(Credential... credentials) {
            UserCredentialsDetailsFragment.this.finish();
        }
    }

}
