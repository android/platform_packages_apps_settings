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

import com.android.settings.RingerVolumePreference;
import com.android.settings.SoundSettings;

import android.app.Dialog;
import android.app.Instrumentation;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Tests related to the ringer volume dialog.
 */
public class RingerVolumeDialogTests extends ActivityInstrumentationTestCase2<SoundSettings> {

    private static final String RING_VOLUME_PREFERENCE = "ring_volume";

    SoundSettings mSoundSettings;

    Instrumentation mInstrumentation;

    public RingerVolumeDialogTests() {
        super(SoundSettings.class.getPackage().getName(), SoundSettings.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSoundSettings = getActivity();
        mInstrumentation = getInstrumentation();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mSoundSettings = null;
        mInstrumentation = null;
    }

    /**
     * Test that the ringer volume dialog works correctly even after the
     * SoundSettings activity is stopped.
     */
    public void testVolumeDialogBehaviourOnActivityStop() {
        PreferenceGroup soundPreferenceGroup = (PreferenceGroup)getMember(mSoundSettings,
                "mSoundSettings");
        final RingerVolumePreference volumePreference = (RingerVolumePreference)soundPreferenceGroup
                .findPreference(RING_VOLUME_PREFERENCE);
        assertNotNull("Couldn't get a RingerVolumePreference", volumePreference);

        // Click the Volume menu item
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                invokeVoidMethod(volumePreference, Preference.class, "onClick");
            }
        });

        // Verify that the Volume dialog is visible
        Dialog volumeDialog = volumePreference.getDialog();
        assertTrue("Volume dialog is not showing though it should be", volumeDialog.isShowing());

        // Verify that the checkbox for notification volume shows/hides
        // the notification volume title and seekbar correctly even
        // if the activity is stopped and then started
        mInstrumentation.callActivityOnStop(mSoundSettings);
        mInstrumentation.waitForIdleSync();
        mInstrumentation.callActivityOnStart(mSoundSettings);
        mInstrumentation.waitForIdleSync();

        final CheckBox notificationsUseRingVolumeCheckBox = (CheckBox)volumeDialog
                .findViewById(R.id.same_notification_volume);

        // Click to uncheck the notifications volume checkbox (if not already unchecked)
        if (notificationsUseRingVolumeCheckBox.isChecked()) {
            mInstrumentation.runOnMainSync(new Runnable() {
                public void run() {
                    notificationsUseRingVolumeCheckBox.performClick();
                }
            });
        }

        // Verify that the notification volume text and seekbar are showing
        final TextView notificationTextView = (TextView)volumeDialog
                .findViewById(R.id.notification_volume_title);
        assertEquals("Notification title is not visible even though it should be", View.VISIBLE,
                notificationTextView.getVisibility());

        final SeekBar notificationSeekBar = (SeekBar)volumeDialog
                .findViewById(R.id.notification_volume_seekbar);
        assertEquals("Notification seekbar is not visible even though it should be", View.VISIBLE,
                notificationSeekBar.getVisibility());

        // Click to check the notifications volume checkbox
        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                notificationsUseRingVolumeCheckBox.performClick();
            }
        });

        // Verify that things that should now be hidden are not showing
        assertEquals("Notification title is visible even though it should not be", View.GONE,
                notificationTextView.getVisibility());
        assertEquals("Notification seekbar is visible even though it should not be", View.GONE,
                notificationSeekBar.getVisibility());

        volumeDialog.dismiss();
    }

    /**
     * Runs a private/protected method.
     *
     * @param instance Instance of the object on which to invoke the private
     *            method
     * @param clazz The class that declares the method that should be invoked
     * @param method The name of the private/protected method
     */
    private static void invokeVoidMethod(Object instance, Class clazz, String methodName) {
        try {
            Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(instance);
        } catch (Exception e) {
            fail("invokeVoidMethod failed " + e);
        }
    }

    /**
     * Gets a private member.
     *
     * @param instance Instance of the object from which to get the private
     *            member
     * @param member The name of the private member
     * @return the member object
     */
    private static Object getMember(Object instance, String member) {
        try {
            Class clazz = instance.getClass();
            Field field = clazz.getDeclaredField(member);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            fail("getMember failed " + e);
        }
        return null;
    }
}
