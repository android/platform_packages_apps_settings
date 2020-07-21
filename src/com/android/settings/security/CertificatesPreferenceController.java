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

import android.annotation.LayoutRes;
import android.annotation.Nullable;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterDefs;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
                    CredentialDialogFragment.show(mParent, cred, this);
                    return true;
                });
        pref.setTitle(cred.getAlias());
        pref.setSummary(cred.isSystem()
                ? R.string.credential_for_vpn_and_apps
                : R.string.credential_for_wifi);
        pref.setIcon(R.drawable.ic_friction_lock_closed);
        mUserCredentialsPreferenceCategory.addPreference(pref);
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
            final int systemUid = UserHandle.getUid(mUserId, Process.SYSTEM_UID);
            final int wifiUid = UserHandle.getUid(mUserId, Process.WIFI_UID);

            List<Credential> credentials = new ArrayList<>();
            credentials.addAll(getCredentialsForUid(systemUid).values());
            if (mUserId == UserHandle.USER_SYSTEM) {
                credentials.addAll(getCredentialsForUid(wifiUid).values());
            }
            return credentials;
        }

        private SortedMap<String, Credential> getCredentialsForUid(int uid) {
            final KeyChainConnection connection;
            try {
                connection = KeyChain.bindAsUser(mContext, UserHandle.of(mUserId));
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while binding KeyChain profile.");
                return null;
            }
            try {
                IKeyChainService keyChainService = connection.getService();
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
                                if (type == Credential.Type.USER_KEY && !isAsymmetric(
                                        keyChainService, prefix + alias, uid)) {
                                    continue;
                                }
                            } catch (UnrecoverableKeyException e) {
                                Log.e(TAG, "Unable to determine algorithm of key: "
                                        + prefix + alias, e);
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
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while getting certificate aliases from KeyChain");
                return null;
            }  finally {
                connection.close();
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
            return keymasterAlgorithm == KeymasterDefs.KM_ALGORITHM_RSA
                    || keymasterAlgorithm == KeymasterDefs.KM_ALGORITHM_EC;
        }

        @Override
        protected void onPostExecute(List<Credential> credentials) {
            mUserCredentialsPreferenceCategory.removeAll();
            for (Credential item : credentials) {
                addUserCredentialPreference(item);
            }
        }
    }

    /**
     * Dialog to handle click on individual user credential. Can remove credential.
     */
    public static class CredentialDialogFragment extends InstrumentedDialogFragment {
        private static final String TAG = "CredentialDialogFragment";
        private static final String ARG_CREDENTIAL = "credential";
        private static int sMyUserId;
        private static CertificatesPreferenceController sController;

        private static void show(Fragment target, Credential item,
                CertificatesPreferenceController controller) {
            final Bundle args = new Bundle();
            args.putParcelable(ARG_CREDENTIAL, item);
            sMyUserId = UserHandle.getUserId(item.uid);
            sController = controller;

            if (target.getFragmentManager().findFragmentByTag(TAG) == null) {
                final DialogFragment frag = new CredentialDialogFragment();
                frag.setTargetFragment(target, /* requestCode */ -1);
                frag.setArguments(args);
                frag.show(target.getFragmentManager(), TAG);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Credential item = (Credential) getArguments().getParcelable(ARG_CREDENTIAL);
            View root = getActivity().getLayoutInflater()
                    .inflate(R.layout.user_credential_dialog, null);
            ViewGroup infoContainer = (ViewGroup) root.findViewById(R.id.credential_container);
            View contentView = getCredentialView(item, R.layout.user_credential, null,
                    infoContainer, /* expanded */ true);
            infoContainer.addView(contentView);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setView(root)
                    .setTitle(R.string.user_credential_title)
                    .setPositiveButton(R.string.done, null);

            final String restriction = UserManager.DISALLOW_CONFIG_CREDENTIALS;
            if (!RestrictedLockUtilsInternal.hasBaseUserRestriction(getContext(), restriction,
                    sMyUserId)) {
                DialogInterface.OnClickListener listener = (dialog, id) -> {
                    final RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtilsInternal
                            .checkIfRestrictionEnforced(getContext(), restriction, sMyUserId);
                    if (admin != null) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                                admin);
                    } else {
                        new CredentialDialogFragment.RemoveCredentialsTask(getContext())
                                .execute(item);
                    }
                    dialog.dismiss();
                };
                // TODO: b/127865361
                //       a safe means of clearing wifi certificates. Configs refer to aliases
                //       directly so deleting certs will break dependent access points.
                //       However, Wi-Fi used to remove this certificate from storage if the network
                //       was removed, regardless if it is used in more than one network.
                //       It has been decided to allow removing certificates from this menu, as we
                //       assume that the user who manually adds certificates must have a way to
                //       manually remove them.
                builder.setNegativeButton(R.string.trusted_credentials_remove_label, listener);
            }
            return builder.create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_USER_CREDENTIAL;
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
                final KeyChainConnection conn;
                try {
                    conn = KeyChain.bindAsUser(mContext, UserHandle.of(sMyUserId));
                } catch (InterruptedException e) {
                    Log.w(TAG, "Connecting to KeyChain", e);
                    return null;
                }
                try {
                    IKeyChainService service = conn.getService();
                    for (final Credential credential : credentials) {
                        if (credential.isSystem()) {
                            service.removeKeyPair(credential.alias);
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
                sController.onResume();
            }
        }
    }

    /**
     * Mapping from View IDs in {@link R} to the types of credentials they describe.
     */
    private static final SparseArray<Credential.Type> sCredentialViewTypes = new SparseArray<>();
    static {
        sCredentialViewTypes.put(R.id.contents_userkey, Credential.Type.USER_KEY);
        sCredentialViewTypes.put(R.id.contents_usercrt, Credential.Type.USER_CERTIFICATE);
        sCredentialViewTypes.put(R.id.contents_cacrt, Credential.Type.CA_CERTIFICATE);
    }

    protected static View getCredentialView(Credential item, @LayoutRes int layoutResource,
            @Nullable View view, ViewGroup parent, boolean expanded) {
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
        }

        ((TextView) view.findViewById(R.id.alias)).setText(item.alias);
        ((TextView) view.findViewById(R.id.purpose)).setText(item.isSystem()
                ? R.string.credential_for_vpn_and_apps
                : R.string.credential_for_wifi);

        view.findViewById(R.id.contents).setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (expanded) {
            for (int i = 0; i < sCredentialViewTypes.size(); i++) {
                final View detail = view.findViewById(sCredentialViewTypes.keyAt(i));
                detail.setVisibility(item.storedTypes.contains(sCredentialViewTypes.valueAt(i))
                        ? View.VISIBLE : View.GONE);
            }
        }
        return view;
    }
}
