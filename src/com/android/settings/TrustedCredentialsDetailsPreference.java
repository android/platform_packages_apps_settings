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

package com.android.settings;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustedCredentialsSettings.CertHolder;
import com.android.settings.widget.SwitchBar;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.IntConsumer;


/**
 * Screen containing certificate details of trusted credential clicked
 */
public class TrustedCredentialsDetailsPreference extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener {

    /**
     * Interface providing logic for handling certificates
     */
    public interface DelegateInterface {
        /**
         *
         * @param certHolder
         * @return list of certificates
         */
        List<X509Certificate> getX509CertsFromCertHolder(
                CertHolder certHolder);

        /**
         * Installs or removes a certificate
         * @param certHolder
         */
        void removeOrInstallCert(CertHolder certHolder);

        /**
         *
         * @param userId
         * @param onCredentialConfirmedListener
         * @return whether credential confirmed
         */
        boolean startConfirmCredentialIfNotConfirmed(int userId,
                IntConsumer onCredentialConfirmedListener);
    }
    private static final String TAG = "TrustedCredentialsDetailsPreference";

    private SettingsActivity mActivity;
    private DevicePolicyManager mDpm;
    private UserManager mUserManager;
    private DelegateInterface mDelegate;

    private SwitchBar mSwitchBar;
    private CertHolder mCertHolder;
    private boolean mNeedsApproval;
    private KeyChain.KeyChainConnection mConnection;
    private IKeyChainService mService;
    private int mProfileId;
    private String mAlias;
    private X509Certificate mX509Cert;
    private SslCertificate mSslCert;
    private boolean mDeleted;
    private AliasOperation mAliasOperation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = (SettingsActivity) getActivity();
        mDpm = (getActivity()).getSystemService(DevicePolicyManager.class);
        mUserManager = mActivity.getSystemService(UserManager.class);

        mSwitchBar = mActivity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);

        Bundle args = getArguments();
        mAlias = (String) args.get("alias");
        mProfileId = (int) args.get("profileId");
        mX509Cert = KeyChain.toCertificate((byte[]) args.get("cert"));
        mSslCert = new SslCertificate(mX509Cert);

        mConnection = KeyChain.bindAsUser(getContext(), UserHandle.USER_NULL);
        mService = mConnection.getService();

        try {
            mDeleted = !mService.containsCaAlias(mAlias);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception while checking if alias " + mAlias + " is deleted.", e);
        }
    }

//    @Override
//    public void onDestroy() {
//        if (mAliasOperation != null) {
//            mAliasOperation.cancel(true);
//            mAliasOperation = null;
//        }
//        closeKeyChainConnection():
//        super.onDestroy();
//    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TRUSTED_CREDENTIALS_DETAILS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState)     {

        View view = inflater.inflate(R.layout.trusted_credential_details,
                container, false);
        LinearLayout content = view.findViewById(R.id.sslcert_details);

        X509Certificate certificate = mDelegate.getX509CertsFromCertHolder(mCertHolder).get(0);
        SslCertificate sslCert = new SslCertificate(certificate);
        content.addView(sslCert.inflateCertificateView(mActivity));

        mActivity.setTitle(sslCert.getIssuedTo().getCName());

        return view;
    }

    @Override
    public void onDestroyView() {
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
        super.onDestroyView();

    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchBar = mActivity.getSwitchBar();
        mSwitchBar.setSwitchBarText(
                R.string.trusted_credential_enabled,
                R.string.trusted_credential_disabled);
        mSwitchBar.show();
        mSwitchBar.setChecked(!mDeleted);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        //confirmation dialog only displayed when disabling certificate
        if (isChecked) {
            if (mNeedsApproval) {
                onClickTrust();
            }
            removeOrInstallCert();
        } else {
            onClickDisable();
        }

        mNeedsApproval = isUserSecure(mProfileId)
                && !mDpm.isCaCertApproved(mAlias, mProfileId);

    }

    private void onClickTrust() {
        if (!mDelegate.startConfirmCredentialIfNotConfirmed(mProfileId,
                this::onCredentialConfirmed)) {
            mDpm.approveCaCert(mAlias, mProfileId, true);

        }
    }

    private void onClickDisable() {
        DialogInterface.OnClickListener onDisable =
                (dialog, id) -> removeOrInstallCert();

        DialogInterface.OnClickListener onCancel = (dialog, id) -> mSwitchBar.setChecked(true);
//.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialogInterface) {
//                        // Avoid starting dialog again after Activity restart.
//                        getActivity().getIntent().removeExtra(ARG_SHOW_NEW_FOR_USER);
//                        mTrustAllCaUserId = UserHandle.USER_NULL;
//                    }
//                })
//                .show();
        new AlertDialog.Builder(mActivity)
                .setMessage(R.string.trusted_credentials_disable_confirmation)
                .setPositiveButton(R.string.trusted_credential_dialog_positive, onDisable)
                .setNegativeButton(R.string.trusted_credential_dialog_negative, onCancel)
                .show();
    }

    private void onCredentialConfirmed(int userId) {
        if (mNeedsApproval && mCertHolder != null
                && mProfileId == userId) {
            // Treat it as user just clicks "trust" for this cert
            onClickTrust();
        }
    }

    /**
     * @return true if current user or parent user is guarded by screenlock
     */
    private boolean isUserSecure(int userId) {
        final LockPatternUtils lockPatternUtils = new LockPatternUtils(mActivity);
        if (lockPatternUtils.isSecure(userId)) {
            return true;
        }
        UserInfo parentUser = mUserManager.getProfileParent(userId);
        if (parentUser == null) {
            return false;
        }
        return lockPatternUtils.isSecure(parentUser.id);
    }

    private void removeOrInstallCert() {
        new AliasOperation(mProfileId, mAlias, mX509Cert).execute();
    }


    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private int mProfileId;
        private String mAlias;
        private X509Certificate mX509Cert;

        private AliasOperation(int profileId, String alias, X509Certificate cert) {
            mProfileId = profileId;
            mAlias = alias;
            mX509Cert = cert;
            mAliasOperation = this;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                synchronized (mConnection) {
                    if (mDeleted) {
                        byte[] bytes = mX509Cert.getEncoded();
                        mService.installCaCertificate(bytes);
                        return true;
                    } else {
                        return mService.deleteCaCertificate(mAlias);
                    }
                }
            } catch (CertificateEncodingException | SecurityException | IllegalStateException
                    | RemoteException e) {
                Log.w(TAG, "Error while toggling alias " + mAlias, e);
                return false;
            }
        }
    }
}
