/*
 * Copyright (c) 2011-13 The Linux Foundation. All rights reserved
 * Not a Contribution.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.CellBroadcastMessage;
import android.telephony.MSimTelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.SelectSubscription;
import com.codeaurora.telephony.msim.MSimPhoneFactory;

import java.lang.ref.WeakReference;

/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Signal Strength
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class MSimSubscriptionStatus extends PreferenceActivity {

    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_ESN_NUMBER = "esn_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";

    private static final String[] PHONE_RELATED_ENTRIES = {
        KEY_SERVICE_STATE,
        KEY_OPERATOR_NAME,
        KEY_ROAMING_STATE,
        KEY_PHONE_NUMBER,
        KEY_IMEI,
        KEY_IMEI_SV,
        KEY_PRL_VERSION,
        KEY_MIN_NUMBER,
        KEY_ESN_NUMBER,
        KEY_MEID_NUMBER,
        KEY_SIGNAL_STRENGTH,
        KEY_ICC_ID,
        KEY_LATEST_AREA_INFO
    };

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";

    private MSimTelephonyManager mTelephonyManager;
    private Phone mPhone = null;
    private Resources mRes;
    private Preference mSigStrength;
    SignalStrength mSignalStrength;
    ServiceState mServiceState;
    private int mSub = 0;
    private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private PhoneStateListener mPhoneStateListener;

    private static String sUnknown;

    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                if (cbMessage != null && cbMessage.getServiceCategory() == 50) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Preference removablePref;

        mTelephonyManager = (MSimTelephonyManager)getSystemService(MSIM_TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_subscription_status);

        // getting selected subscription
        mSub = getIntent().getIntExtra(SelectSubscription.SUBSCRIPTION_KEY, 0);
        Log.d("Status","OnCreate mSub =" + mSub);

        mPhoneStateListener = getPhoneStateListener(mSub);
        mTelephonyManager.listen(mPhoneStateListener,
                                PhoneStateListener.LISTEN_SERVICE_STATE
                                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        mRes = getResources();
        if (sUnknown == null) {
            sUnknown = mRes.getString(R.string.device_info_default);
        }

        mPhone = MSimPhoneFactory.getPhone(mSub);
        // Note - missing in zaku build, be careful later...
        mSigStrength = findPreference(KEY_SIGNAL_STRENGTH);

        if (Utils.isWifiOnly(getApplicationContext())) {
            for (String key : PHONE_RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
        } else {

            if ((SystemProperties.getBoolean("ro.config.multimode_cdma", false))
                    || (mPhone.getPhoneName().equals("CDMA"))) {
                setSummaryText(KEY_PRL_VERSION, mPhone.getCdmaPrlVersion());
            } else {
                // device does not support CDMA, do not display PRL
                removePreferenceFromScreen(KEY_PRL_VERSION);
            }

            // NOTE "imei" is the "Device ID" since it represents
            //  the IMEI in GSM and the MEID in CDMA
            if (mPhone.getPhoneName().equals("CDMA")) {
                setSummaryText(KEY_ESN_NUMBER, mPhone.getEsn());
                setSummaryText(KEY_MEID_NUMBER, mPhone.getMeid());
                setSummaryText(KEY_MIN_NUMBER, mPhone.getCdmaMin());
                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                }

                removePreferenceFromScreen(KEY_IMEI_SV);

                if (mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID, mPhone.getIccSerialNumber());
                    setSummaryText(KEY_IMEI, mPhone.getImei());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI);
                    removePreferenceFromScreen(KEY_ICC_ID);
                }
            } else {
                setSummaryText(KEY_IMEI, mPhone.getDeviceId());

                setSummaryText(KEY_IMEI_SV,mPhone.getDeviceSvn());

                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_ESN_NUMBER);
                removePreferenceFromScreen(KEY_MEID_NUMBER);
                removePreferenceFromScreen(KEY_MIN_NUMBER);
                removePreferenceFromScreen(KEY_ICC_ID);
            }

            String rawNumber = mPhone.getLine1Number();  // may be null or empty
            String formattedNumber = null;
            if (!TextUtils.isEmpty(rawNumber)) {
                formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
            }
            // If formattedNumber is null or empty, it'll display as "Unknown".
            setSummaryText(KEY_PHONE_NUMBER, formattedNumber);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            mTelephonyManager.listen(mPhoneStateListener,
                                     PhoneStateListener.LISTEN_SERVICE_STATE
                                     | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            updateSignalStrength();
            updateServiceState();
            registerReceiver(mAreaInfoReceiver, new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                    CB_AREA_INFO_SENDER_PERMISSION, null);
            // Ask CellBroadcastReceiver to broadcast the latest area info received
            Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
            getLatestIntent.putExtra(MSimConstants.SUBSCRIPTION_KEY, mSub);
            sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                    CB_AREA_INFO_SENDER_PERMISSION);

        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            unregisterReceiver(mAreaInfoReceiver);
        }
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private PhoneStateListener getPhoneStateListener(int subscription) {
        PhoneStateListener phoneStateListener = new PhoneStateListener(subscription) {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                mSignalStrength = signalStrength;
                updateSignalStrength();
            }
            @Override
            public void onServiceStateChanged(ServiceState state) {
                mServiceState = state;
                updateServiceState();
            }
        };
        return phoneStateListener;
    }
    /**
     * @param preference The key for the Preference item
     * @param property The system property to fetch
     * @param alt The default value, if the property doesn't exist
     */
    private void setSummary(String preference, String property, String alt) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, alt));
        } catch (RuntimeException e) {

        }
    }

    private void setSummaryText(String preference, String text) {
            if (TextUtils.isEmpty(text)) {
               text = sUnknown;
             }
             // some preferences may be missing
             if (findPreference(preference) != null) {
                 findPreference(preference).setSummary(text);
             }
    }

    private void updateServiceState() {
        String display = mRes.getString(R.string.radioInfo_unknown);

        if (mServiceState != null) {
            int state = mServiceState.getState();

            switch (state) {
                case ServiceState.STATE_IN_SERVICE:
                    display = mRes.getString(R.string.radioInfo_service_in);
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    display = mRes.getString(R.string.radioInfo_service_out);
                    break;
                case ServiceState.STATE_POWER_OFF:
                    display = mRes.getString(R.string.radioInfo_service_off);
                    break;
            }

            setSummaryText(KEY_SERVICE_STATE, display);

            if (mServiceState.getRoaming()) {
                setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
            } else {
                setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
            }
            setSummaryText(KEY_OPERATOR_NAME, mServiceState.getOperatorAlphaLong());
        }
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText(KEY_LATEST_AREA_INFO, areaInfo);
        }
    }

    void updateSignalStrength() {

        if (mSignalStrength != null) {
            int state = mServiceState.getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSigStrength.setSummary("0");
            }

            int signalDbm = mSignalStrength.getDbm();
            if (-1 == signalDbm) signalDbm = 0;

            int signalAsu = mSignalStrength.getGsmSignalStrength();
            if (-1 == signalAsu) signalAsu = 0;

            mSigStrength.setSummary(String.valueOf(signalDbm) + " "
                        + r.getString(R.string.radioInfo_display_dbm) + "   "
                        + String.valueOf(signalAsu) + " "
                        + r.getString(R.string.radioInfo_display_asu));
        }
    }

}
