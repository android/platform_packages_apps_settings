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
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
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
import java.util.EnumSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CertificatesPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnPreferenceClickListener,
        LifecycleObserver, OnPause, OnResume {

    private static final String TAG = "CertificatesPrefController";

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
    }

    @Override
    public boolean isAvailable() {
        return mUserCredentialsPreferenceCategory.isVisible();
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

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {
        updateUi();
    }

    private void addDummies() {
        for(int i=1; i<4; i++) {
            final Preference pref = new Preference(mContext);
            pref.setOnPreferenceClickListener(
                    preference -> {
                        Log.d(TAG, "Dummy preference pressed!");
                        return true;
                    });
            pref.setTitle("Certificate Example " + i);
            pref.setSummary((i % 2) == 0
                    ? R.string.credential_for_vpn_and_apps
                    : R.string.credential_for_wifi);
            pref.setIcon(R.drawable.ic_friction_lock_closed);
            mUserCredentialsPreferenceCategory.addPreference(pref);
            mUserCredentialsPreferenceCategory.setVisible(true);
        }

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
                    //TODO: launch details preference
                    Log.d(TAG, "Real preference pressed!");
                    return true;
                });
        pref.setTitle(cred.getAlias());
        pref.setSummary(cred.isSystem()
                ? R.string.credential_for_vpn_and_apps
                : R.string.credential_for_wifi);
        pref.setIcon(R.drawable.ic_friction_lock_closed);
        mUserCredentialsPreferenceCategory.addPreference(pref);
//        mUserCredentialsPreferenceCategory.setVisible(true);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
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
            final KeyStore keyStore = KeyStore.getInstance();

            // Certificates can be installed into SYSTEM_UID or WIFI_UID through CertInstaller.
            final int myUserId = UserHandle.myUserId();
            final int systemUid = UserHandle.getUid(myUserId, Process.SYSTEM_UID);
            final int wifiUid = UserHandle.getUid(myUserId, Process.WIFI_UID);

            List<Credential> credentials = new ArrayList<>();
            credentials.addAll(getCredentialsForUid(keyStore, systemUid).values());
            credentials.addAll(getCredentialsForUid(keyStore, wifiUid).values());
            return credentials;
        }

        private boolean isAsymmetric(KeyStore keyStore, String alias, int uid)
                throws UnrecoverableKeyException {
            KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
            int errorCode = keyStore.getKeyCharacteristics(alias, null, null, uid,
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

        private SortedMap<String, Credential> getCredentialsForUid(KeyStore keyStore, int uid) {
            final SortedMap<String, Credential> aliasMap = new TreeMap<>();
            for (final Credential.Type type : Credential.Type.values()) {
                for (final String prefix : type.prefix) {
                    for (final String alias : keyStore.list(prefix, uid)) {
                        if (UserHandle.getAppId(uid) == Process.SYSTEM_UID) {
                            // Do not show work profile keys in user credentials
                            if (alias.startsWith(LockPatternUtils.PROFILE_KEY_NAME_ENCRYPT) ||
                                    alias.startsWith(LockPatternUtils.PROFILE_KEY_NAME_DECRYPT)) {
                                continue;
                            }
                            // Do not show synthetic password keys in user credential
                            if (alias.startsWith(LockPatternUtils.SYNTHETIC_PASSWORD_KEY_PREFIX)) {
                                continue;
                            }
                        }
                        try {
                            if (type == Credential.Type.USER_KEY &&
                                    !isAsymmetric(keyStore, prefix + alias, uid)) {
                                continue;
                            }
                        } catch (UnrecoverableKeyException e) {
                            Log.e(TAG, "Unable to determine algorithm of key: " + prefix + alias, e);
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

        @Override
        protected void onPostExecute(List<Credential> credentials) {
            mUserCredentialsPreferenceCategory.removeAll();
            for (Credential item : credentials) {
                addUserCredentialPreference(item);
            }
        }
    }

    static class Credential implements Parcelable {
        public static enum Type {
            CA_CERTIFICATE (Credentials.CA_CERTIFICATE),
            USER_CERTIFICATE (Credentials.USER_CERTIFICATE),
            USER_KEY(Credentials.USER_PRIVATE_KEY, Credentials.USER_SECRET_KEY);

            final String[] prefix;

            Type(String... prefix) {
                this.prefix = prefix;
            }
        }

        /**
         * Main part of the credential's alias. To fetch an item from KeyStore, prepend one of the
         * prefixes from { CredentialItem.storedTypes}.
         */
        public final String alias;

        /**
         * UID under which this credential is stored. Typically {@link Process#SYSTEM_UID} but can
         * also be {@link Process#WIFI_UID} for credentials installed as wifi certificates.
         */
        public final int uid;

        /**
         * Should contain some non-empty subset of:
         * <ul>
         *   <li>{ Credentials.CA_CERTIFICATE}</li>
         *   <li>{ Credentials.USER_CERTIFICATE}</li>
         *   <li>{ Credentials.USER_KEY}</li>
         * </ul>
         */
        public final EnumSet<Credential.Type> storedTypes = EnumSet.noneOf(
                Credential.Type.class);

        Credential(final String alias, final int uid) {
            this.alias = alias;
            this.uid = uid;
        }

        Credential(Parcel in) {
            this(in.readString(), in.readInt());

            long typeBits = in.readLong();
            for (Credential.Type i : Credential.Type.values()) {
                if ((typeBits & (1L << i.ordinal())) != 0L) {
                    storedTypes.add(i);
                }
            }
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(alias);
            out.writeInt(uid);

            long typeBits = 0;
            for (Credential.Type i : storedTypes) {
                typeBits |= 1L << i.ordinal();
            }
            out.writeLong(typeBits);
        }

        public int describeContents() {
            return 0;
        }

        public static final Parcelable.Creator<Credential> CREATOR
                = new Parcelable.Creator<Credential>() {
            public Credential createFromParcel(Parcel in) {
                return new Credential(in);
            }

            public Credential[] newArray(int size) {
                return new Credential[size];
            }
        };

        public boolean isSystem() {
            return UserHandle.getAppId(uid) == Process.SYSTEM_UID;
        }

        public String getAlias() { return alias; }

        public EnumSet<Credential.Type> getStoredTypes() {
            return storedTypes;
        }
    }

}
