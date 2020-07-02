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

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

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
     * Interface providing ShadowLogic for handling certificates used in TrustedCredentialsSettings
     */
    public interface DelegateInterface {
        /**
         *
         * @param certHolder contains the certificate
         * @return list of certificates
         */
        List<X509Certificate> getX509CertsFromCertHolder(
                CertHolder certHolder);

        /**
         * Installs or removes a certificate
         * @param certHolder contains the certificate
         */
        void removeOrInstallCert(CertHolder certHolder);

        /**
         *
         * @param userId describes the current user
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
    private KeyguardManager mKeyguardManager;

    private SwitchBar mSwitchBar;
    private LinearLayout mContainerView;

    private boolean mDeleted;
    private boolean mNeedsApproval;
    private int mProfileId;
    private String mAlias;
    private Object mLock = new Object();

    public static final String ARG_ALIAS = "alias";
    public static final String ARG_PROFILE_ID = "profileId";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TRUSTED_CREDENTIALS_DETAILS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mActivity = (SettingsActivity) getActivity();
        mDpm = mActivity.getSystemService(DevicePolicyManager.class);
        mUserManager = mActivity.getSystemService(UserManager.class);
        mKeyguardManager = (KeyguardManager) mActivity
                .getSystemService(Context.KEYGUARD_SERVICE);

        mSwitchBar = mActivity.getSwitchBar();
        mSwitchBar.setSwitchBarText(
                R.string.trusted_credential_enabled,
                R.string.trusted_credential_disabled);
        mSwitchBar.addOnSwitchChangeListener(this);

        Bundle args = getArguments();
        mAlias = (String) args.get(ARG_ALIAS);
        mProfileId = (int) args.get(ARG_PROFILE_ID);

        View view = inflater.inflate(R.layout.trusted_credential_details,
                container, false);
        mContainerView = view.findViewById(R.id.sslcert_details);

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
        mSwitchBar.show();
        new AliasLoader().execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        //confirmation dialog only displayed when disabling certificate

        if (isChecked == mDeleted) {
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
    }

    private void onClickTrust() {
        if (!startConfirmCredentialIfNotConfirmed(mProfileId,
                this::onCredentialConfirmed)) {
            mDpm.approveCaCert(mAlias, mProfileId, true);

        }
    }

    private void onClickDisable() {
        DialogInterface.OnClickListener onDisable =
                (dialog, id) -> removeOrInstallCert();

        DialogInterface.OnClickListener onCancel =
                (diaShadowLog, id) -> mSwitchBar.setChecked(true);

        DialogInterface.OnCancelListener onDismiss =
                diaShadowLog -> mSwitchBar.setChecked(true);

        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.trusted_credentials_disable_title)
                .setMessage(R.string.trusted_credentials_disable_message)
                .setPositiveButton(R.string.trusted_credential_dialog_positive, onDisable)
                .setNegativeButton(R.string.trusted_credential_dialog_negative, onCancel)
                .setOnCancelListener(onDismiss)
                .show();
    }

    private void onCredentialConfirmed(int userId) {
        if (mNeedsApproval && mProfileId == userId) {
            // Treat it as user just clicks "trust" for this cert
            onClickTrust();
        }
    }

    private boolean startConfirmCredentialIfNotConfirmed(int userId,
            IntConsumer onCredentialConfirmedListener) {
        final Intent newIntent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null,
                userId);
        if (newIntent == null) {
            return false;
        }
        startActivityForResult(newIntent, 1);
        return true;
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
        new AliasOperation().execute();
    }

    private class AliasLoader extends AsyncTask<Void, Void, SslCertificate> {
        private Context mContext;

        AliasLoader() {
            mContext = getActivity();
        }

        @Override protected void onPreExecute() {
            mContainerView.setVisibility(View.GONE);
        }
        @Override protected SslCertificate doInBackground(Void... params) {
            try {
                synchronized (mLock) {
                    KeyChainConnection connection = KeyChain.bindAsUser(mContext,
                            UserHandle.of(mProfileId));
                    IKeyChainService service = connection.getService();

                    byte[] encodedCertificate = service.getEncodedCaCertificate(mAlias, true);
                    X509Certificate cert = KeyChain.toCertificate(encodedCertificate);
                    mDeleted = !service.containsCaAlias(mAlias);
                    return new SslCertificate(cert);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Remote exception while checking if alias " + mAlias
                        + " is deleted.");
                return null;
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while binding KeyChain profile.");
                return null;
            }
        }

        @Override protected void onPostExecute(SslCertificate cert) {
            if (cert == null) {
                Toast.makeText(mContext, "Empty SSL Certificate", Toast.LENGTH_SHORT).show();
                return;
            }
            mContainerView.addView(cert.inflateCertificateView(mActivity));
            mActivity.setTitle(cert.getIssuedTo().getCName());

            mSwitchBar.setChecked(!mDeleted);

            mContainerView.setVisibility(View.VISIBLE);
        }

    }

    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private Context mContext;

        private AliasOperation() {
            mContext = getActivity();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                synchronized (mLock) {
                    KeyChainConnection connection = KeyChain.bindAsUser(mContext,
                            UserHandle.of(mProfileId));
                    IKeyChainService service = connection.getService();
                    if (mDeleted) {

                        byte[] cert = service.getEncodedCaCertificate(mAlias, true);
                        service.installCaCertificate(cert);
                        mDeleted = !service.containsCaAlias(mAlias);
                        return true;
                    } else {
                        boolean deleted = service.deleteCaCertificate(mAlias);
                        mDeleted = !service.containsCaAlias(mAlias);
                        return deleted;
                    }
                }
            } catch (SecurityException | IllegalStateException
                    | RemoteException e) {
                Log.e(TAG, "Error while toggling alias " + mAlias);
                return false;
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException while binding KeyChain profile.");
                return false;
            }
        }
    }
}
