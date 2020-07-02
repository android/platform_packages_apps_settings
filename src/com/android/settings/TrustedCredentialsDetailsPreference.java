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
import android.os.Bundle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustedCredentialsSettings.CertHolder;
import com.android.settings.widget.SwitchBar;

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
    private SettingsActivity mActivity;
    private DevicePolicyManager mDpm;
    private UserManager mUserManager;
    private DelegateInterface mDelegate;

    private SwitchBar mSwitchBar;
    private CertHolder mCertHolder;
    private boolean mNeedsApproval;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = (SettingsActivity) getActivity();
        mDpm = (getActivity()).getSystemService(DevicePolicyManager.class);
        mUserManager = mActivity.getSystemService(UserManager.class);

        mSwitchBar = mActivity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);

    }

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
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchBar = mActivity.getSwitchBar();
        mSwitchBar.setSwitchBarText(
                R.string.trusted_credential_enabled,
                R.string.trusted_credential_disabled);
        mSwitchBar.show();
        mSwitchBar.setChecked(isCertificateEnabled());
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
            mDelegate.removeOrInstallCert(mCertHolder);
        } else {
            onClickDisable(switchView);
        }

        mNeedsApproval = !mCertHolder.isSystemCert()
                && isUserSecure(mCertHolder.getUserId())
                && !mDpm.isCaCertApproved(mCertHolder.getAlias(), mCertHolder.getUserId());

    }

    private void onClickTrust() {
        if (!mDelegate.startConfirmCredentialIfNotConfirmed(mCertHolder.getUserId(),
                this::onCredentialConfirmed)) {
            mDpm.approveCaCert(mCertHolder.getAlias(), mCertHolder.getUserId(), true);

        }
    }

    private void onClickDisable(Switch switchView) {
        DialogInterface.OnClickListener onDisable = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mDelegate.removeOrInstallCert(mCertHolder);
            }
        };
        DialogInterface.OnClickListener onCancel = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mSwitchBar.setChecked(true);
//                switchView.setChecked(true);
            }
        };
        new AlertDialog.Builder(mActivity)
                .setMessage(R.string.trusted_credentials_disable_confirmation)
                .setPositiveButton(R.string.trusted_credential_dialog_positive, onDisable)
                .setNegativeButton(R.string.trusted_credential_dialog_negative, onCancel)
                .show();
    }

    private void onCredentialConfirmed(int userId) {
        if (mNeedsApproval && mCertHolder != null
                && mCertHolder.getUserId() == userId) {
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

    private boolean isCertificateEnabled() {
        return !mCertHolder.isDeleted();
    }

    /**
     * Used to initalise when called from TrustedCredentialsSettings
     * @param delegate
     */
    public void setDelegate(DelegateInterface delegate) {
        mDelegate = delegate;
    }

    /**
     * Used to initialise when called from TrustedCredentialsSettings
     * @param certHolder
     */
    public void setCertHolder(CertHolder certHolder) {
        mCertHolder = certHolder;
    }
}
