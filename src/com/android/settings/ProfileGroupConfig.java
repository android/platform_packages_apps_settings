/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.Profile;
import android.app.ProfileGroup;
import android.app.ProfileGroup.Mode;
import android.app.ProfileManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class ProfileGroupConfig extends PreferenceActivity implements OnPreferenceChangeListener {

    private static final CharSequence KEY_SOUNDMODE = "sound_mode";

    private static final CharSequence KEY_VIBRATEMODE = "vibrate_mode";

    private static final CharSequence KEY_LIGHTSMODE = "lights_mode";

    private static final CharSequence KEY_RINGERMODE = "ringer_mode";

    private static final CharSequence KEY_SOUNDTONE = "soundtone";

    private static final CharSequence KEY_RINGTONE = "ringtone";

    Profile mProfile;

    ProfileGroup mProfileGroup;

    private ListPreference mSoundMode;

    private ListPreference mRingerMode;

    private ListPreference mVibrateMode;

    private ListPreference mLightsMode;

    private ProfileRingtonePreference mRingTone;

    private ProfileRingtonePreference mSoundTone;

    private ProfileManager mProfileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.profile_settings);

        mProfile = (Profile) getIntent().getParcelableExtra("Profile");
        mProfileGroup = mProfile.getProfileGroup(getIntent().getStringExtra("ProfileGroup"));

        setTitle(getString(R.string.profile_group_header,  mProfile.getName(), mProfileGroup.getName()));

        mRingerMode = (ListPreference) findPreference(KEY_RINGERMODE);
        mSoundMode = (ListPreference) findPreference(KEY_SOUNDMODE);
        mVibrateMode = (ListPreference) findPreference(KEY_VIBRATEMODE);
        mLightsMode = (ListPreference) findPreference(KEY_LIGHTSMODE);
        mRingTone = (ProfileRingtonePreference) findPreference(KEY_RINGTONE);
        mSoundTone = (ProfileRingtonePreference) findPreference(KEY_SOUNDTONE);

        mRingTone.setShowSilent(false);
        mSoundTone.setShowSilent(false);

        mSoundMode.setOnPreferenceChangeListener(this);
        mRingerMode.setOnPreferenceChangeListener(this);
        mVibrateMode.setOnPreferenceChangeListener(this);
        mLightsMode.setOnPreferenceChangeListener(this);
        mSoundTone.setOnPreferenceChangeListener(this);
        mRingTone.setOnPreferenceChangeListener(this);

        updateState();

        mProfileManager = (ProfileManager) getSystemService(PROFILE_SERVICE);
    }

    private void updateState() {

        mVibrateMode.setValue(mProfileGroup.getVibrateMode().name());
        mSoundMode.setValue(mProfileGroup.getSoundMode().name());
        mRingerMode.setValue(mProfileGroup.getRingerMode().name());
        mLightsMode.setValue(mProfileGroup.getLightsMode().name());

        mVibrateMode.setSummary(mVibrateMode.getEntry());
        mSoundMode.setSummary(mSoundMode.getEntry());
        mRingerMode.setSummary(mRingerMode.getEntry());
        mLightsMode.setSummary(mLightsMode.getEntry());

        if (mProfileGroup.getSoundOverride() != null) {
            mSoundTone.setRingtone(mProfileGroup.getSoundOverride());
        }

        if (mProfileGroup.getRingerOverride() != null) {
            mRingTone.setRingtone(mProfileGroup.getRingerOverride());
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVibrateMode) {
            mProfileGroup.setVibrateMode(Mode.valueOf((String) newValue));
        }
        if (preference == mSoundMode) {
            mProfileGroup.setSoundMode(Mode.valueOf((String) newValue));
        }
        if (preference == mRingerMode) {
            mProfileGroup.setRingerMode(Mode.valueOf((String) newValue));
        }
        if (preference == mLightsMode) {
            mProfileGroup.setLightsMode(Mode.valueOf((String) newValue));
        }
        if (preference == mRingTone) {
            Uri uri = Uri.parse((String) newValue);
            mProfileGroup.setRingerOverride(uri);
        }
        if (preference == mSoundTone) {
            Uri uri = Uri.parse((String) newValue);
            mProfileGroup.setSoundOverride(uri);
        }

        mProfileManager.addProfile(mProfile);

        updateState();
        return true;
    }

}
