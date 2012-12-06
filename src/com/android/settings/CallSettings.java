package com.android.settings;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RILConstants.SimCardID;
import android.os.SystemProperties;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import android.provider.Settings;


public class CallSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "CallSettings";

    private ListPreference mDefaultSimForVoiceCall;
    private static final String KEY_DEFAULT_SIM_FOR_VOICE_CALL = "default_sim_for_voice_call_key";
    private static final String KEY_CALL_SETTINGS_1 = "call_settings_1";
    private static final String KEY_CALL_SETTINGS_2 = "call_settings_2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.call_settings);

        mDefaultSimForVoiceCall = (ListPreference) findPreference(KEY_DEFAULT_SIM_FOR_VOICE_CALL);
        mDefaultSimForVoiceCall.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDefaultSimForVoiceCall.setEnabled(!AirplaneModeEnabler.isAirplaneModeOn(this));
        findPreference(KEY_CALL_SETTINGS_1).setEnabled(!AirplaneModeEnabler.isAirplaneModeOn(this));
        findPreference(KEY_CALL_SETTINGS_2).setEnabled(!AirplaneModeEnabler.isAirplaneModeOn(this));

        updateIccCardState();
    }

/*
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDefaultSimForVoiceCall) {
            mDefaultSimForVoiceCall.setValue(SystemProperties.get(TelephonyProperties.PROPERTY_CALL_DEFAULT_SIM_ID,
            String.valueOf(SimCardID.ID_ZERO.toInt())));
        }
        return true;
    }
*/
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return true;
    }

    private void updateIccCardState() {
        Phone phone1 = PhoneFactory.getDefaultPhone(SimCardID.ID_ZERO);
        Phone phone2 = PhoneFactory.getDefaultPhone(SimCardID.ID_ONE);
        Preference p1 = findPreference(KEY_CALL_SETTINGS_1);
        Preference p2 = findPreference(KEY_CALL_SETTINGS_2);

        boolean phone1On = (Settings.Global.getInt(getContentResolver(), Settings.Global.PHONE1_ON, 1)!=0);
        boolean phone2On = (Settings.Global.getInt(getContentResolver(), Settings.Global.PHONE2_ON, 1)!=0);

        if (null != phone1) {
            IccCard iccCard = phone1.getIccCard();
            if(iccCard.getState() == IccCardConstants.State.READY)
            {
                p1.setEnabled((!iccCard.hasIccCard()
                               || IccCardConstants.State.ABSENT == iccCard.getState()) ? false : true);
            }
            else
            {
                p1.setEnabled(false);
            }
        }

        if (null != phone2) {
            IccCard iccCard = phone2.getIccCard();
            if(iccCard.getState() == IccCardConstants.State.READY)
            {
                p2.setEnabled((!iccCard.hasIccCard()
                               || IccCardConstants.State.ABSENT == iccCard.getState()) ? false : true);
            }
            else
            {
                p2.setEnabled(false);
            }
        }

        findPreference(KEY_DEFAULT_SIM_FOR_VOICE_CALL).setEnabled((!p1.isEnabled()
                                                                   && !p2.isEnabled()) ? false : true);
        if(!p1.isEnabled() || !phone1On)
        {
            mDefaultSimForVoiceCall.setEntries(R.array.sim1_absent_voice_call_entries);
            mDefaultSimForVoiceCall.setEntryValues(R.array.sim1_absent_voice_call_values);
        }
        else if (!p2.isEnabled() || !phone2On)
        {
            mDefaultSimForVoiceCall.setEntries(R.array.sim2_absent_voice_call_entries);
            mDefaultSimForVoiceCall.setEntryValues(R.array.sim2_absent_voice_call_values);
        }
    }
}
