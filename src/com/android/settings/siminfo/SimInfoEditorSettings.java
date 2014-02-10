package com.android.settings.siminfo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.telephony.SimInfoManager;
import android.telephony.SimInfoManager.SimInfoRecord;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.ColorPreference;

public class SimInfoEditorSettings extends SettingsPreferenceFragment implements 
                                                        OnPreferenceChangeListener {

    private static final String TAG = "SimInfoEditorSettings";
    private static final String KEY_SIM_NAME = "sim_name";
    private static final String KEY_SIM_NUM = "sim_number";
    private static final String KEY_SIM_COLOR = "sim_color";
    private static final String KEY_SIM_NUM_FORMAT = "sim_number_format";
    
    private int mSimId = SimInfoManager.SIM_NOT_INSERTED;
    private long mSimIndex;
    private EditTextPreference mNamePreference;
    private EditTextPreference mNumPreference;
    private ListPreference mNumFormatPreference;
    private ColorPreference mColorPicker;
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.sim_info_settings);
        initPreference();
        
        boolean hasSimInsert = SimInfoManager.getInsertedSimCount(getActivity()) > 0;
        if (hasSimInsert) {
            mSimId = getSimCardId(bundle);
            Log.d(TAG,"mSimId = " + mSimId);
            if (mSimId == SimInfoManager.SIM_NOT_INSERTED) {
                startActivityForResult(SimInfoUtils.getSelectSimIntent(),0); 
            } else {
                mSimIndex = SimInfoUtils.getSimIndexBySimId(getActivity(),mSimId);
            }
        } else {
            getPreferenceScreen().removeAll();
        }
    }
    
    /**
     * Return SIM Id. Two case exist, if SIM_ID_STATE exist in bundle restore the SIM Id, or
     * intent include extra SIM_ID
     * @param bundle bundle from create
     * @return Return SIM Id or -1
     */
    private int getSimCardId(Bundle bundle) {
        if (bundle != null && bundle.containsKey(SimInfoUtils.SIM_ID_STATE)) {
            return bundle.getInt(SimInfoUtils.SIM_ID_STATE);
        }
        return SimInfoUtils.getSimId(getActivity(),getActivity().getIntent());
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
    }
    
    /**
     * Get the SIM id from Select SIM Activity and update fragment, if resultCode != OK,
     * means no select so finish fragment
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            handleSelectSimId(data);
        } else {
            finish();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(SimInfoUtils.SIM_ID_STATE, mSimId);
    }

    private void handleSelectSimId(Intent data) {
        mSimId = data.getIntExtra(SimInfoUtils.SIM_ID, SimInfoManager.SIM_NOT_INSERTED);
        mSimIndex = data.getLongExtra(SimInfoUtils.SIM_INDEX, 0);
        updatePreference();
    }

    private void updatePreference() {
        final SimInfoRecord simInfoRecord = SimInfoManager.getSimInfoBySimId(getActivity(), mSimId);
        if (simInfoRecord != null) {
            final String simName = simInfoRecord.mDisplayName;
            if (!TextUtils.isEmpty(simName)) {
                mNamePreference.setSummary(simName);
                mNamePreference.setText(simName);
            }
            String simNum = simInfoRecord.mNumber;
            if (!TextUtils.isEmpty(simNum)) {
                mNumPreference.setSummary(simNum);
                mNumPreference.setText(simNum);
            }
            mNumFormatPreference.setValueIndex(simInfoRecord.mDispalyNumberFormat);
            mNumFormatPreference.setSummary(getNumFormatSummary(simInfoRecord.mDispalyNumberFormat));
            mColorPicker.setValue(getColorValue(simInfoRecord.mColor));
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
        if (KEY_SIM_NAME.equals(key)) {
            setSimName((EditTextPreference)preference);
        } else if (KEY_SIM_NUM.equals(key)) {
            setSimNumber((EditTextPreference)preference);
        } else if (KEY_SIM_NUM_FORMAT.equals(key)) {
            setSimNumFormat((ListPreference)preference, value);
        } else if (KEY_SIM_COLOR.equals(key)) {
            setSimColor((ColorPreference) preference, value);
        }
        return true;
    }

    /**
     * Return the index of value by passing actual color values to set in SimInfoManager
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
     * Get the actual color value by passing index getting from SimInfoManager
     * @param colorIndex color index getting from SimInfoManager
     * @return actual color value
     */
    private int getColorValue(int colorIndex) {
        final int[] colorValue = getResources().getIntArray(R.array.sim_info_picker_color_values);
        return colorValue[colorIndex];
    }

    private void setSimColor(ColorPreference preference, Object value) {
        int valueIndex = getColorIndex(Integer.valueOf(String.valueOf(value)));
        SimInfoManager.setColor(getActivity(), valueIndex, mSimIndex);
    }

    private void setSimNumFormat(ListPreference preference, Object value) {
        int valueIndex = Integer.valueOf(String.valueOf(value));
        int result = SimInfoManager.setDispalyNumberFormat(getActivity(), valueIndex, mSimIndex);
        if (result > 0) {
            preference.setSummary(getNumFormatSummary(valueIndex));
        }    
    }

    private void setSimNumber(EditTextPreference preference) {
        final String number = preference.getEditText().getText().toString();
        int result = SimInfoManager.setDispalyNumber(getActivity(), number, mSimIndex);
        if (result > 0) {
            preference.setSummary(number);
        }
    }

    private void setSimName(EditTextPreference preference) {
        final String name = preference.getEditText().getText().toString();
        int result = SimInfoManager.setDisplayName(getActivity(), name, mSimIndex, SimInfoManager.USER_INPUT);
        if (result > 0) {
            preference.setSummary(name);
        }
    }
}