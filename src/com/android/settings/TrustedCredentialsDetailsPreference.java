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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;

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

    private SwitchBar mSwitchBar;
    private int mCurrentCertIndex = 0; //TODO should be -1 at init
    private DelegateInterface mDelegate; //TODO should be final var
    private CertHolder[] mCertHolders = new CertHolder[0];
    private String mCertName;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
//        activity.setTitle(com.android.internal.R.string.ssl_certificate);

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);

    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TRUSTED_CREDENTIALS_DETAILS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState)     {
        SettingsActivity activity = (SettingsActivity) getActivity();
        View view = inflater.inflate(R.layout.trusted_credential_details,
                container, false);
        LinearLayout content = view.findViewById(R.id.sslcert_details);
//        if(content == null) System.out.println("CONTENT IS NULL");

        CertHolder certHolder = getCurrentCertInfo();
        X509Certificate certificate = mDelegate.getX509CertsFromCertHolder(certHolder).get(0);
        SslCertificate sslCert = new SslCertificate(certificate);
        content.addView(sslCert.inflateCertificateView(activity));

        mCertName = sslCert.getIssuedTo().getCName();
        activity.setTitle(mCertName);

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
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBar.setSwitchBarText(
                R.string.credential_enabled,
                R.string.credential_disabled);
        mSwitchBar.show();
        mSwitchBar.setChecked(isCertificateEnabled(getActivity()));

        updateDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setCertificateEnabled(isChecked);
        updateDisplay();
    }

    private void updateDisplay() {
        // TODO implement trust logic with switchbar
    }

    private void setCertificateEnabled(boolean isChecked) {
        // TODO implement trust logic with switchbar
    }

    private static boolean isCertificateEnabled(Context context) {
        // TODO implement trust logic with switchbar
        return true;
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
        mCertHolders = (certHolder == null ? new CertHolder[0] : new CertHolder[]{certHolder});
    }

    private CertHolder getCurrentCertInfo() {
        return mCurrentCertIndex < mCertHolders.length ? mCertHolders[mCurrentCertIndex] : null;
    }
}
