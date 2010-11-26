/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import android.os.SystemProperties;
import android.util.Log;

/**
 *  Receive sound setting change broadcast. Now only handle sound mode change
 *  and STREAM_MUSIC(media sound) volume change. Use a persistent system property
 *  to record the current sound setting, 0 indicates the target is in silent
 *  or silent+vibrate mode, other value in range of 1-100 indicates the percent
 *  value of the max STREAM_MUSIC volume.
 *  Currently a native module will read this system property when playing a sound
 *  file at system starting up, and set the volume to follow the global sound
 *  setting. Since the native module has only one interface to convert percent
 *  to a real volume for audio device, here need to convert the index of
 *  STREAM_MUSIC to percent.
 */
public class BootSoundSettingReceiver extends BroadcastReceiver {

    final private static String SOUND_VAL_PROPERTY = "persist.sys.boot.sound.volume";
    final private static String TAG = "BootSoundSettingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null == context || null == intent) {
            return;
        }

        Log.d(TAG, "Receive broadcast Intent action:" + intent.getAction());
        Log.d(TAG, "system property:persist.sys.boot.sound.volume=" + SystemProperties.get(
                SOUND_VAL_PROPERTY, null));

        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int maxVal = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
            // Handle sound mode change
            int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1);

            switch(ringerMode) {
                case AudioManager.RINGER_MODE_SILENT:
                case AudioManager.RINGER_MODE_VIBRATE:
                    // In silent or silent+vibrate mode
                    Log.d(TAG, "Set system property:persist.sys.boot.sound.volume=0");
                    SystemProperties.set(SOUND_VAL_PROPERTY, String.valueOf(0));
                    break;
                case AudioManager.RINGER_MODE_NORMAL:
                    // Normal mode, align with music volume
                    int newVal = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    // Change to percent of volume
                    newVal = (newVal * 100) / maxVal;
                    Log.d(TAG, "Set system property:persist.sys.boot.sound.volume=" + newVal);
                    SystemProperties.set(SOUND_VAL_PROPERTY, String.valueOf(newVal));
                    break;
                default:
                    break;
            }
        } else if (intent.getAction().equals(AudioManager.VOLUME_CHANGED_ACTION)) {
            // Only handle the case in normal mode, nothing to do in silent mode
            if (AudioManager.RINGER_MODE_NORMAL != am.getRingerMode()) {
                return;
            }

            int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
            int newVal = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
            // Only handle STREAM_MUSIC(media) volume change
            if ((AudioManager.STREAM_MUSIC == streamType) &&
                    // The valid value should be in [0,maxVal]
                    (newVal >= 0 && newVal <= maxVal)) {
                // Change to percent of volume
                newVal = (newVal * 100) / maxVal;
                Log.d(TAG, "Set system property:persist.sys.boot.sound.volume=" + newVal);
                SystemProperties.set(SOUND_VAL_PROPERTY, String.valueOf(newVal));
            }
        }
    }
}
