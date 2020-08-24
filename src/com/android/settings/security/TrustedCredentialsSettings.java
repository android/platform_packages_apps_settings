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

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.annotations.GuardedBy;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Implements Trusted Credentials screen containing list of system certificates
 */
public class TrustedCredentialsSettings extends SettingsPreferenceFragment {

    public static final String ARG_SHOW_NEW_FOR_USER = "ARG_SHOW_NEW_FOR_USER";

    private static final String TAG = "TrustedCredentialsSettings";

    private UserManager mUserManager;
    private KeyguardManager mKeyguardManager;
//    TODO: b/166248334
//      private int mTrustAllCaUserId;
    private int mUserId;

    private static final String SAVED_CONFIRMED_CREDENTIAL_USERS = "ConfirmedCredentialUsers";
    private static final String SAVED_CONFIRMING_CREDENTIAL_USER = "ConfirmingCredentialUser";
    private static final int REQUEST_CONFIRM_CREDENTIALS = 1;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TRUSTED_CREDENTIALS;
    }

    private final int mProgress = R.id.system_progress;
    private final int mContentView = R.id.system_content;
    private LinearLayout mLayout;
    private ListAdapter mAdapter;
    private AliasOperation mAliasOperation;
    private ArraySet<Integer> mConfirmedCredentialUsers;
    private int mConfirmingCredentialUser;
    private IntConsumer mConfirmingCredentialListener;
    private KeyChainConnection mKeyChainConnection;
    private List<CertHolder> mCertHolders = new ArrayList<>();
    @GuardedBy("mLock")
    private Object mLock = new Object();

    private BroadcastReceiver mWorkProfileChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action)
                    || Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action)
                    || Intent.ACTION_MANAGED_PROFILE_UNLOCKED.equals(action)) {
                load();
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        mUserManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mUserId =  getArguments().getInt(TrustedCredentialsController.USER_ID);
        mKeyguardManager = (KeyguardManager) activity
                .getSystemService(Context.KEYGUARD_SERVICE);
//        TODO: b/166248334
//          mTrustAllCaUserId = activity.getIntent().getIntExtra(ARG_SHOW_NEW_FOR_USER,
//                UserHandle.USER_NULL);
        mConfirmedCredentialUsers = new ArraySet<>(2);
        mConfirmingCredentialUser = UserHandle.USER_NULL;
        if (savedInstanceState != null) {
            mConfirmingCredentialUser = savedInstanceState.getInt(SAVED_CONFIRMING_CREDENTIAL_USER,
                    UserHandle.USER_NULL);
            ArrayList<Integer> users = savedInstanceState.getIntegerArrayList(
                    SAVED_CONFIRMED_CREDENTIAL_USERS);
            if (users != null) {
                mConfirmedCredentialUsers.addAll(users);
            }
        }

        mConfirmingCredentialListener = null;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);
        activity.registerReceiver(mWorkProfileChangedReceiver, filter);

        activity.setTitle(R.string.trusted_credentials);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(SAVED_CONFIRMED_CREDENTIAL_USERS, new ArrayList<>(
                mConfirmedCredentialUsers));
        outState.putInt(SAVED_CONFIRMING_CREDENTIAL_USER, mConfirmingCredentialUser);
    }

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mLayout = (LinearLayout) inflater.inflate(R.layout.trusted_credentials,
                parent, false);
        mAdapter = new ListAdapter();

        // Add a transition for non-visibility events like resizing the pane.
        final ViewGroup contentView = (ViewGroup) mLayout.findViewById(mContentView);
        contentView.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        final LinearLayout containerView = (LinearLayout) inflater
                .inflate(R.layout.trusted_credential_list_container,
                        contentView, false);
        mAdapter.setContainerView(containerView);
        contentView.addView(containerView);
        return mLayout;
    }


    @Override public void onResume() {
        super.onResume();
        load();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mWorkProfileChangedReceiver);
        if (mAliasOperation != null) {
            mAliasOperation.cancel(true);
            mAliasOperation = null;
        }
        closeKeyChainConnections();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONFIRM_CREDENTIALS) {
            int userId = mConfirmingCredentialUser;
            IntConsumer listener = mConfirmingCredentialListener;
            // reset them before calling the listener because the listener may call back to start
            // activity again. (though it should never happen.)
            mConfirmingCredentialUser = UserHandle.USER_NULL;
            mConfirmingCredentialListener = null;
            if (resultCode == Activity.RESULT_OK) {
                mConfirmedCredentialUsers.add(userId);
                if (listener != null) {
                    listener.accept(userId);
                }
            }
        }
    }

    private void closeKeyChainConnections() {
        synchronized (mLock) {
            mKeyChainConnection.close();
        }
        mKeyChainConnection = null;
    }

    private void load() {
        new AliasLoader().execute();
    }

    /**
     * Adapter for expandable list view of certificates.
     */
    private class ListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener,
            View.OnClickListener {

        private final DataSetObserver mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
            @Override
            public void onInvalidated() {
                super.onInvalidated();
                notifyDataSetInvalidated();
            }
        };

        private LinearLayout mContainerView;
        private ListView mListView;

        private ListAdapter() {
            load();
            registerDataSetObserver(mObserver);
        }

        @Override
        public int getCount() {
            return mCertHolders.size();
        }

        @Override
        public CertHolder getItem(int position) {
            return mCertHolders.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            CertHolder certHolder = mCertHolders.get(position);

            ViewHolder holder;

            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                convertView = inflater.inflate(R.layout.trusted_credential,
                        container, false);
                convertView.setTag(holder);
                holder.mSubjectPrimaryView = (TextView)
                        convertView.findViewById(R.id.trusted_credential_subject_primary);
                holder.mSubjectSecondaryView = (TextView)
                        convertView.findViewById(R.id.trusted_credential_subject_secondary);
                holder.mSwitch = (Switch) convertView.findViewById(
                        R.id.trusted_credential_status);
                holder.mSwitch.setOnClickListener(this);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mSubjectPrimaryView.setText(certHolder.getPrimary());
            holder.mSubjectSecondaryView.setText(certHolder.getSecondary());
            holder.mSwitch.setChecked(!certHolder.isDeleted());
            holder.mSwitch.setEnabled(!mUserManager.hasUserRestriction(
                    UserManager.DISALLOW_CONFIG_CREDENTIALS,
                    new UserHandle(certHolder.getUserId())));
            holder.mSwitch.setVisibility(View.VISIBLE);
            holder.mSwitch.setTag(certHolder);

            return convertView;
        }

        private class ViewHolder {
            private TextView mSubjectPrimaryView;
            private TextView mSubjectSecondaryView;
            private Switch mSwitch;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            showCertDialog(getItem(pos));
        }

        /**
         * Called when the switch on a system certificate is clicked. This will toggle whether it
         * is trusted as a credential.
         */
        @Override
        public void onClick(View view) {
            CertHolder cert = (CertHolder) view.getTag();
            removeOrInstallCert(cert);
        }

        public void setContainerView(LinearLayout containerView) {
            mContainerView = containerView;

            mListView = (ListView) mContainerView.findViewById(R.id.cert_list);
            mListView.setAdapter(this);
            mListView.setOnItemClickListener(this);
            mListView.setItemsCanFocus(true);
        }

    }

    private class AliasLoader extends AsyncTask<Void, Integer, List<CertHolder>> {
        private ProgressBar mProgressBar;
        private View mView;
        private Context mContext;

        AliasLoader() {
            mContext = getActivity();
        }

        private boolean shouldSkipProfile(UserHandle userHandle) {
            return mUserManager.isQuietModeEnabled(userHandle)
                    || !mUserManager.isUserUnlocked(userHandle.getIdentifier());
        }

        @Override protected void onPreExecute() {
            View content = mLayout;
            mProgressBar = (ProgressBar) content.findViewById(mProgress);
            mView = content.findViewById(mContentView);
            mProgressBar.setVisibility(View.VISIBLE);
            mView.setVisibility(View.GONE);
        }
        @Override protected List<CertHolder> doInBackground(Void... params) {
            List<CertHolder> certHolders = new ArrayList<>();
            try {
                synchronized (mLock) {
                    List<String> aliases;
                    int progress = 0;
                    UserHandle profile = UserHandle.of(mUserId);
                    if (shouldSkipProfile(profile)) {
                        return new ArrayList<>();
                    }
                    KeyChainConnection keyChainConnection = KeyChain.bindAsUser(mContext,
                            profile);
                    // Saving the connection for later use on the certificate dialog.
                    mKeyChainConnection = keyChainConnection;

                    IKeyChainService service = keyChainConnection.getService();
                    aliases = service.getSystemCaAliases().getList();
                    if (isCancelled() || aliases == null || keyChainConnection == null) {
                        return new ArrayList<>();
                    }

                    final int aliasMax = aliases.size();
                    for (int j = 0; j < aliasMax; ++j) {
                        String alias = aliases.get(j);
                        byte[] encodedCertificate = service.getEncodedCaCertificate(alias,
                                true);
                        X509Certificate cert = KeyChain.toCertificate(encodedCertificate);
                        certHolders.add(new CertHolder(service,
                                alias, cert, mUserId));
                        publishProgress(++progress, aliasMax); //TODO what is aliasMax for
                    }
                    Collections.sort(certHolders);
                    return certHolders;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Remote exception while loading aliases.", e);
                return new ArrayList<>();
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while loading aliases.", e);
                return new ArrayList<>();
            }
        }

        @Override protected void onProgressUpdate(Integer... progressAndMax) {
            int progress = progressAndMax[0];
            int max = progressAndMax[1];
            if (max != mProgressBar.getMax()) {
                mProgressBar.setMax(max);
            }
            mProgressBar.setProgress(progress);
        }

        @Override protected void onPostExecute(List<CertHolder> certHolders) {
            mCertHolders.clear();
            mCertHolders = certHolders;
            mAdapter.notifyDataSetChanged();
            mProgressBar.setVisibility(View.GONE);
            mView.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(0);
//                TODO: b/166248334
//                  showTrustAllCaDialogIfNeeded();
        }

        /*
            TODO: b/166248334
        private boolean isUserTabAndTrustAllCertMode() {
            return isTrustAllCaCertModeInProgress() && mTab == Tab.USER;
        }

        @UiThread
        private void showTrustAllCaDialogIfNeeded() {
            if (!isUserTabAndTrustAllCertMode()) {
                return;
            }
            List<CertHolder> certHolders = mCertHoldersByUserId.get(mTrustAllCaUserId);
            if (certHolders == null) {
                return;
            }

            List<CertHolder> unapprovedUserCertHolders = new ArrayList<>();
            final DevicePolicyManager dpm = mContext.getSystemService(
                    DevicePolicyManager.class);
            for (CertHolder cert : certHolders) {
                if (cert != null && !dpm.isCaCertApproved(cert.mAlias, mTrustAllCaUserId)) {
                    unapprovedUserCertHolders.add(cert);
                }
            }

            if (unapprovedUserCertHolders.size() == 0) {
                Log.w(TAG, "no cert is pending approval for user " + mTrustAllCaUserId);
                return;
            }
            showTrustAllCaDialog(unapprovedUserCertHolders);
        }
        */
    }

    static class CertHolder implements Comparable<CertHolder> {
        private final IKeyChainService mService;
        private final String mAlias;
        private final X509Certificate mX509Cert;
        private int mProfileId;

        private final SslCertificate mSslCert;
        private final String mSubjectPrimary;
        private final String mSubjectSecondary;
        private boolean mDeleted;

        private CertHolder(IKeyChainService service,
                           String alias,
                           X509Certificate x509Cert,
                           int profileId) {
            mService = service;
            mAlias = alias;
            mX509Cert = x509Cert;
            mProfileId = profileId;

            mSslCert = new SslCertificate(x509Cert);

            String cn = mSslCert.getIssuedTo().getCName();
            String o = mSslCert.getIssuedTo().getOName();
            String ou = mSslCert.getIssuedTo().getUName();
            // if we have a O, use O as primary subject, secondary prefer CN over OU
            // if we don't have an O, use CN as primary, empty secondary
            // if we don't have O or CN, use DName as primary, empty secondary
            if (!o.isEmpty()) {
                mSubjectPrimary = o;
                mSubjectSecondary = !cn.isEmpty() ? cn : ou;

            } else {
                mSubjectPrimary = !cn.isEmpty() ? cn : mSslCert.getIssuedTo().getDName();
                mSubjectSecondary = "";
            }
            try {
                mDeleted = !mService.containsCaAlias(mAlias);
            } catch (RemoteException e) {
                Log.e(TAG, "Remote exception while checking if alias "
                                + mAlias + " is deleted.", e);
                mDeleted = false;
            }
        }
        @Override public int compareTo(CertHolder o) {
            int primary = this.mSubjectPrimary.compareToIgnoreCase(o.mSubjectPrimary);
            if (primary != 0) {
                return primary;
            }
            return this.mSubjectSecondary.compareToIgnoreCase(o.mSubjectSecondary);
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof CertHolder)) {
                return false;
            }
            CertHolder other = (CertHolder) o;
            return mAlias.equals(other.mAlias);
        }
        @Override public int hashCode() {
            return mAlias.hashCode();
        }

        public int getUserId() {
            return mProfileId;
        }

        public String getAlias() {
            return mAlias;
        }

        public String getPrimary() {
            return mSubjectPrimary;
        }

        public String getSecondary() {
            return mSubjectSecondary;
        }

        public void setDeleted(boolean bool) {
            mDeleted = bool;
        }

        public boolean isDeleted() {
            return mDeleted;
        }
    }

//    TODO: b/166248334
//    private boolean isTrustAllCaCertModeInProgress() {
//        return mTrustAllCaUserId != UserHandle.USER_NULL;
//    }

    private void showTrustAllCaDialog(List<CertHolder> unapprovedCertHolders) {
        final CertHolder[] arr = unapprovedCertHolders.toArray(
                new CertHolder[unapprovedCertHolders.size()]);

        //TODO: b/166248334
        // Implement trust all using TrustedCredentialsDetailsPreference instead of first cert
        CertHolder certHolder = arr[0];
        showCertDialog(certHolder);
    }

    private void showCertDialog(final CertHolder certHolder) {
        Bundle args = new Bundle();
        args.putString(TrustedCredentialsDetailsPreference.ARG_ALIAS, certHolder.getAlias());
        args.putInt(TrustedCredentialsDetailsPreference.ARG_PROFILE_ID, certHolder.getUserId());
        args.putBoolean(TrustedCredentialsDetailsPreference.ARG_IS_SYSTEM, true);

        new SubSettingLauncher(this.getContext())
                .setDestination(TrustedCredentialsDetailsPreference.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .setArguments(args)
                .setResultListener(this, 1)
                .launch();
    }

    /**
     * Triggers an Async Task to remove the CertHolder certificate,
     * or install it if it doesn't currently exist.
     * @param certHolder
     */
    public void removeOrInstallCert(CertHolder certHolder) {
        new AliasOperation(certHolder).execute();
    }

    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private final CertHolder mCertHolder;

        private AliasOperation(CertHolder certHolder) {
            mCertHolder = certHolder;
            mAliasOperation = this;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                synchronized (mLock) {
                    KeyChainConnection keyChainConnection = mKeyChainConnection;
                    IKeyChainService service = keyChainConnection.getService();
                    if (mCertHolder.isDeleted()) {
                        byte[] bytes = mCertHolder.mX509Cert.getEncoded();
                        service.installCaCertificate(bytes);
                        return true;
                    } else {
                        return service.deleteCaCertificate(mCertHolder.getAlias());
                    }
                }
            } catch (CertificateEncodingException | SecurityException | IllegalStateException
                    | RemoteException e) {
                Log.w(TAG, "Error while toggling alias " + mCertHolder.getAlias(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            if (ok) {
                mCertHolder.setDeleted(!mCertHolder.isDeleted());
                mAdapter.notifyDataSetChanged();
            } else {
                // bail, reload to reset to known state
                load();
            }
            mAliasOperation = null;
        }
    }
}
