package com.android.settings.siminfo;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.telephony.SubscriptionController;
import android.telephony.SubscriptionController.SubInfoRecord;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.ColorPreference;

public class SubscriptionInfoSettings extends SettingsPreferenceFragment implements 
                                                        OnPreferenceChangeListener {

    private static final String TAG = "SubscriptionSettings";
    private static final String KEY_SIM_NAME = "sim_name";
    private static final String KEY_SIM_NUM = "sim_number";
    private static final String KEY_SIM_COLOR = "sim_color";
    private static final String KEY_SIM_NUM_FORMAT = "sim_number_format";
    
    private long mSubId;
    private SubscriptionSettingUtility mUtility;
    private EditTextPreference mNamePreference;
    private EditTextPreference mNumPreference;
    private ListPreference mNumFormatPreference;
    private ColorPreference mColorPicker;
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.sim_info_settings);
        initPreference();
        
        mUtility = SubscriptionSettingUtility.createUtilityByFragment(this);
        boolean hasSubActivated = SubscriptionController.getActivatedSubInfoList(getActivity()).size() > 0;
        if (hasSubActivated) {
            mUtility.initSubscriptionId(bundle, getActivity().getIntent());
            Log.d(TAG,"subId = " + mUtility.getSubId());
        } else {
            getPreferenceScreen().removeAll();
        }
    }

    private void initPreference() {
        mNamePreference = (EditTextPreference) findPreference(KEY_SIM_NAME);
        mNamePreference.setOnPreferenceChangeListener(this);
        
        mNumPreference = (EditTextPreference) findPreference(KEY_SIM_NUM);
        mNumPreference.setOnPreferenceChangeListener(this);
        
        mNumFormatPreference = (ListPreference) findPreference(KEY_SIM_NUM_FORMAT);
        mNumFormatPreference.setOnPreferenceChangeListener(this);
       
        mColorPicker = (ColorPreference) findPreference(KEY_SIM_COLOR);
        mColorPicker.setOnPreferenceChangeListener(this);
        initColorPreference(mColorPicker);
    }

    private void initColorPreference(ColorPreference preference) {
        final Resources res = getResources();
        final int[] colorValues = res.getIntArray(R.array.sim_info_picker_color_values);
        final String[] colorTitles = res.getStringArray(R.array.sim_info_picker_color_titles);
        preference.setTitles(colorTitles);
        preference.setValues(colorValues);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(
                    R.layout.sim_info_no_inserted, contentRoot, false);
        TextView textView = (TextView) emptyView.findViewById(R.id.no_sim_message);
        textView.setText(R.string.sim_info_no_inserted_msg);
        //Set empty view when no SIM cards inserted
        contentRoot.addView(emptyView);
        getListView().setEmptyView(emptyView);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreference();
        mUtility.registerReceiver();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mUtility.unregisterReceiver();
    }

    /**
     * Get the Sub id from Activity and update fragment, if resultCode != OK,
     * means no select so finish fragment
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mUtility.onActivityResult(requestCode, resultCode, data);
        if (mUtility.isValidSubId()) {
            updatePreference();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUtility.onSaveInstanceState(outState);
    }

    private void updatePreference() {
        final SubInfoRecord subInfoRecord = SubscriptionController.getSubInfoUsingSubId(getActivity(), mUtility.getSubId());
        if (subInfoRecord != null) {
            final String subName = subInfoRecord.mDisplayName;
            if (!TextUtils.isEmpty(subName)) {
                mNamePreference.setSummary(subName);
                mNamePreference.setText(subName);
            }
            String subNum = subInfoRecord.mNumber;
            if (!TextUtils.isEmpty(subNum)) {
                mNumPreference.setSummary(subNum);
                mNumPreference.setText(subNum);
            }
            mNumFormatPreference.setValueIndex(subInfoRecord.mDispalyNumberFormat);
            mNumFormatPreference.setSummary(getNumFormatSummary(subInfoRecord.mDispalyNumberFormat));
            mColorPicker.setValue(getColorValue(subInfoRecord.mColor));
        }
    }
    
    private CharSequence getNumFormatSummary(int formatIndex) {
        final String[] array = getResources().getStringArray(R.array.sim_info_num_format_entries);
        String summary = "";
        if (formatIndex >= 0 && formatIndex < array.length) {
            summary =  array[formatIndex];
        }
        return summary;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        Log.d(TAG,"key = " + key);
        if (KEY_SIM_NAME.equals(key)) {
            setSubscriptionName((EditTextPreference)preference);
        } else if (KEY_SIM_NUM.equals(key)) {
            setSubscriptionNumber((EditTextPreference)preference);
        } else if (KEY_SIM_NUM_FORMAT.equals(key)) {
            setSubscriptionNumFormat((ListPreference)preference, value);
        } else if (KEY_SIM_COLOR.equals(key)) {
            setSubscriptionColor((ColorPreference) preference, value);
        }
        return true;
    }

    /**
     * Return the index of value by passing actual color values to set in SubInfoRecord
     * @param colorValue actual color value
     * @return the array index of the color value
     */
    private int getColorIndex(Integer colorValue) {
        final int[] colorCode = getResources().getIntArray(R.array.sim_info_picker_color_values);
        for (int i = 0; i < colorCode.length; i ++) {
            if (colorValue.equals(colorCode[i])) {
                return i;
            }
        }
        return 0;
    }
    
    /**
     * Get the actual color value by passing index getting from SubscriptionController
     * @param colorIndex color index getting from SubscriptionController
     * @return actual color value
     */
    private int getColorValue(int colorIndex) {
        final int[] colorValue = getResources().getIntArray(R.array.sim_info_picker_color_values);
        return colorValue[colorIndex];
    }

    private void setSubscriptionColor(ColorPreference preference, Object value) {
        int valueIndex = getColorIndex(Integer.valueOf(String.valueOf(value)));
        SubscriptionController.setColor(getActivity(), valueIndex, mUtility.getSubId());
    }

    private void setSubscriptionNumFormat(ListPreference preference, Object value) {
        int valueIndex = Integer.valueOf(String.valueOf(value));
        int result = SubscriptionController.setDispalyNumberFormat(getActivity(), valueIndex, mUtility.getSubId());
        if (result > 0) {
            preference.setSummary(getNumFormatSummary(valueIndex));
        }    
    }

    private void setSubscriptionNumber(EditTextPreference preference) {
        final String number = preference.getEditText().getText().toString();
        int result = SubscriptionController.setDispalyNumber(getActivity(), number, mUtility.getSubId());
        if (result > 0) {
            preference.setSummary(number);
        }
    }

    private void setSubscriptionName(EditTextPreference preference) {
        final String name = preference.getEditText().getText().toString();
        int result = SubscriptionController.setDisplayName(
                getActivity(), name, mUtility.getSubId(), SubscriptionController.USER_INPUT);
        if (result > 0) {
            preference.setSummary(name);
        }
    }
}