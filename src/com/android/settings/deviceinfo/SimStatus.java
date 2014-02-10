/*
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
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;
import com.android.settings.siminfo.SubscriptionSettingUtility;

import java.lang.ref.WeakReference;


/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Operator info (area info cell broadcast for Brazil)
 * # Signal Strength
 *
 */
public class SimStatus extends PreferenceActivity {

    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_ICC_ID = "icc_id";

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";

    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private TelephonyManager mTelephonyManager;
    private Phone mPhone = null;
    private Resources mRes;
    private Preference mSignalStrength;
    private boolean mShowLatestAreaInfo;

    private String sUnknown;

    private SubscriptionSettingUtility mUtility;

    private PhoneStateListener mPhoneStateListener;
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
                if (cbMessage != null 
                        && cbMessage.getServiceCategory() == 50
                        && mUtility.getSubId() == cbMessage.getSubId()) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_sim_status);

        mRes = getResources();
        sUnknown = mRes.getString(R.string.device_info_default);
        // Note - missing in zaku build, be careful later...
        mSignalStrength = findPreference(KEY_SIGNAL_STRENGTH);

        mUtility = SubscriptionSettingUtility.createUtilityByActivity(this);
        mUtility.initSubscriptionId(icicle, getIntent());
        initPhoneInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPhone != null) {
            updatePreference();

            mUtility.registerReceiver();

            updateSignalStrength(mPhone.getSignalStrength());
            updateServiceState(mPhone.getServiceState());
            updateDataState();
            mTelephonyManager.listen(mPhoneStateListener, 
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    | PhoneStateListener.LISTEN_SERVICE_STATE);
            if (mShowLatestAreaInfo) {
                registerReceiver(mAreaInfoReceiver, new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                        CB_AREA_INFO_SENDER_PERMISSION, null);
                // Ask CellBroadcastReceiver to broadcast the latest area info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        CB_AREA_INFO_SENDER_PERMISSION);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPhone != null) {
            mUtility.unregisterReceiver();
            mTelephonyManager.listen(mPhoneStateListener, 
                    PhoneStateListener.LISTEN_NONE);
        }
        if (mShowLatestAreaInfo) {
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

    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
        String networktype = null;
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN 
                != mTelephonyManager.getNetworkType(mUtility.getSubId())) {
            networktype = mTelephonyManager.getNetworkTypeName(
                    mTelephonyManager.getNetworkType(mUtility.getSubId()));
        }
        setSummaryText(KEY_NETWORK_TYPE, networktype);
    }

    private void updateDataState() {
        //TODO divide data state by sub id
        int state = mTelephonyManager.getDataState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }

        setSummaryText(KEY_DATA_STATE, display);
    }

    private void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = mRes.getString(R.string.radioInfo_unknown);

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

        if (serviceState.getRoaming()) {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText(KEY_OPERATOR_NAME, serviceState.getOperatorAlphaLong());
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText(KEY_LATEST_AREA_INFO, areaInfo);
        }
    }

    void updateSignalStrength(SignalStrength signalStrength) {
        if (mSignalStrength != null) {
            int state = mPhone.getServiceState().getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSignalStrength.setSummary("0");
            }

            int signalDbm = signalStrength.getDbm();

            if (-1 == signalDbm) signalDbm = 0;

            int signalAsu = signalStrength.getAsuLevel();

            if (-1 == signalAsu) signalAsu = 0;

            mSignalStrength.setSummary(String.valueOf(signalDbm) + " "
                        + r.getString(R.string.radioInfo_display_dbm) + "   "
                        + String.valueOf(signalAsu) + " "
                        + r.getString(R.string.radioInfo_display_asu));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUtility.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mUtility.onActivityResult(requestCode, resultCode, data);
        initPhoneInstance();
    }

    private void initPhoneInstance() {
        if (UserHandle.myUserId() == UserHandle.USER_OWNER 
                && mUtility.isValidSubId()) {
            mPhone = PhoneFactory.getPhone(mUtility.getSimId());
            initPhoneStateListener();
        }
    }

    private void updatePreference() {
        // NOTE "imei" is the "Device ID" since it represents
        //  the IMEI in GSM and the MEID in CDMA
        if (mPhone.getPhoneName().equals("CDMA")) {
            setSummaryText(KEY_MEID_NUMBER, mPhone.getMeid());
            setSummaryText(KEY_MIN_NUMBER, mPhone.getCdmaMin());
            if (getResources().getBoolean(R.bool.config_msid_enable)) {
                findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
            }
            setSummaryText(KEY_PRL_VERSION, mPhone.getCdmaPrlVersion());
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

            setSummaryText(KEY_IMEI_SV,
                    ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                    .getDeviceSoftwareVersion());

            // device is not CDMA, do not display CDMA features
            // check Null in case no specified preference in overlay xml
            removePreferenceFromScreen(KEY_PRL_VERSION);
            removePreferenceFromScreen(KEY_MEID_NUMBER);
            removePreferenceFromScreen(KEY_MIN_NUMBER);
            removePreferenceFromScreen(KEY_ICC_ID);

            // only show area info when SIM country is Brazil
            if ("br".equals(mTelephonyManager.getSimCountryIso(mUtility.getSubId()))) {
                mShowLatestAreaInfo = true;
            }
        }

        String rawNumber = mPhone.getLine1Number();  // may be null or empty
        String formattedNumber = null;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        }
        // If formattedNumber is null or empty, it'll display as "Unknown".
        setSummaryText(KEY_PHONE_NUMBER, formattedNumber);

        if (!mShowLatestAreaInfo) {
            removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
        }
    }

    private void initPhoneStateListener() {
        mPhoneStateListener = new PhoneStateListener(mUtility.getSubId()) {
            @Override
            public void onDataConnectionStateChanged(int state) {
                updateDataState();
                updateNetworkType();
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                updateSignalStrength(signalStrength);
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                updateServiceState(serviceState);
            }
        };
    }
}
