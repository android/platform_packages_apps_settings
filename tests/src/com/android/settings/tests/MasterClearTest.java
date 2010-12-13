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

package com.android.settings.tests;

import com.android.internal.widget.LockPatternUtils;

import com.android.settings.MasterClear;
import com.android.settings.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Run tests on class com.android.settings.MasterClear
 *
 * To run tests:
 * $make SettingsTests
 * $adb uninstall com.android.settings.tests
 * $adb push SettingsTests.apk /data/app/SettingsTests.apk
 * $adb shell am instrument -w -e class com.android.settings.tests.MasterClearTest /
 *  com.android.settings.tests/android.test.InstrumentationTestRunner
 */
public class MasterClearTest extends ActivityInstrumentationTestCase2<MasterClear> {

    private static final String TAG = "MasterClearTest";
    private static final int DLG_MASTER_CLEAR_BATTERY_SHORT = 1;

    private MasterClear mMasterClear;
    private Instrumentation mInstrumentation;
    private LockPatternUtils mLockUtils;
    private boolean mIsBroadcastReceived;
    private BroadcastReceiver mPhonyReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mIsBroadcastReceived = true;
            }
        }
    };

    public MasterClearTest() {
        super(MasterClear.class.getPackage().getName(), MasterClear.class);
    }

    @Override
    protected final void setUp() throws Exception {
        super.setUp();
        mMasterClear = getActivity();
        assertNotNull("Could not get activity MasterClear", mMasterClear);
        mInstrumentation = getInstrumentation();
        assertNotNull("Could not get instrumentation object", mInstrumentation);
        mIsBroadcastReceived = false;
        ContentResolver resolver = mMasterClear.getContentResolver();
        mLockUtils = new LockPatternUtils(getActivity());
        mLockUtils.setLockPatternEnabled(false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mMasterClear != null) {
            mMasterClear.finish();
        }
        mMasterClear = null;
        mInstrumentation = null;
        mLockUtils = null;
    }

    /**
     * Test by going through the different states:
     * 1. Initial state when first enter activity
     * 2. Final state when pressing button in initial state
     * 3. press back button to enter initial state
     * @throws Exception
     */
    public void testUiIsInCorrectState() throws Exception {
        // Setup master clear to be in a final state
        Button button = (Button)mMasterClear.findViewById(R.id.initiate_master_clear);
        assertNotNull("Could not find initial button", button);
        TouchUtils.clickView(this, button);
        // We should now be in final state
        button = (Button)mMasterClear.findViewById(R.id.execute_master_clear);
        assertTrue(
            "Final button should be found and visible.", button != null && button.isShown());
        // Press back and go to initial state
        sendKeys(KeyEvent.KEYCODE_BACK);
        button = (Button)mMasterClear.findViewById(R.id.initiate_master_clear);
        assertTrue(
            "Initial button should be found and visible.", button != null && button.isShown());
    }

    /**
     * Test that a phony BroadcastReceiver registered to MasterClear does
     * receive intent with Intent.ACTION_BATTERY_CHANGED action.
     * @throws Exception
     */
    public void testIntentReceived() throws Exception {
        // Replace with a phony BroadcastReceiver
        setMember(mMasterClear, "mBatteryReceiver", mPhonyReceiver);
        // Change to final view
        Button button = (Button)mMasterClear.findViewById(R.id.initiate_master_clear);
        // When pressing the button we register for Intent.ACTION_BATTERY_CHANGED
        TouchUtils.clickView(this, button);
        // Assert that intent is received
        assertTrue("Broadcast was not received.", mIsBroadcastReceived);
    }

    /**
     * Test battery level by running the BroadcastReceiver method onReceive
     * with a predefined intent.
     *
     * @throws Exception
     */
    public void testLowBatteryCalculation_LowLevel() throws Exception {
        // Setup battery info intent
        final int scale = 100;
        final int level = 29;
        runOnReceive(scale, level);
        boolean isBatteryLevelOk = (Boolean)getMember(mMasterClear, "mBatteryLevelOk");
        assertFalse("Battery should be low.", isBatteryLevelOk);
    }

    /**
     * Test battery level by running the BroadcastReceiver method onReceive
     * with a predefined intent. Test with scale = 0
     *
     * @throws Exception
     */
    public void testLowBatteryCalculation_ScaleZero() throws Exception {
        // Setup battery info intent
        final int scale = 0;
        final int level = 29;
        runOnReceive(scale, level);
        boolean isBatteryLevelOk = (Boolean)getMember(mMasterClear, "mBatteryLevelOk");
        assertFalse("Battery should be low.", isBatteryLevelOk);
    }

    /**
     * Test executing a master clear. Battery level is set to low so
     * a dialog should pop up informing of this. Dismiss the dialog
     * using the Ok button on dialog.
     * @throws Exception
     */
    public void testShowLowBatteryDialog_DismissWithOkButton() throws Exception {
        Log.v(TAG, "Run test testShowLowBatteryDialog_DismissWithOkButton");
        // Set phony receiver to prevent change of battery level.
        setMember(mMasterClear, "mBatteryReceiver", mPhonyReceiver);
        // Set battery in a low state.
        setMember(mMasterClear, "mBatteryLevelOk", new Boolean(false));
        assertFalse("Battery should be low.", (Boolean)getMember(mMasterClear, "mBatteryLevelOk"));

        Button button = (Button)mMasterClear.findViewById(R.id.initiate_master_clear);
        TouchUtils.clickView(this, button);
        button = (Button)mMasterClear.findViewById(R.id.execute_master_clear);
        TouchUtils.clickView(this, button);

        final AlertDialog dialog = getLowBatteryDialog();
        assertTrue("Dialog is not showing.", dialog.isShowing());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                dialog.getButton(Dialog.BUTTON_POSITIVE).performClick();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue("Dialog is showing.",!dialog.isShowing());
    }

    /**
     * Test executing a master clear. Battery level is set to low so
     * a dialog should pop up informing of this. Dismiss the dialog
     * using the back button.
     * @throws Exception
     */
    public void testShowLowBatteryDialog_DismissWithBackButton() throws Exception {
        Log.v(TAG, "Run test testShowLowBatteryDialog_DismissWithBackButton");
        // Set phony receiver to prevent change of battery level.
        setMember(mMasterClear, "mBatteryReceiver", mPhonyReceiver);
        // Set battery in a low state.
        setMember(mMasterClear, "mBatteryLevelOk", new Boolean(false));
        assertFalse("Battery should be low.", (Boolean)getMember(mMasterClear, "mBatteryLevelOk"));

        Button button = (Button)mMasterClear.findViewById(R.id.initiate_master_clear);
        TouchUtils.clickView(this, button);
        button = (Button)mMasterClear.findViewById(R.id.execute_master_clear);
        TouchUtils.clickView(this, button);

        final AlertDialog dialog = getLowBatteryDialog();
        sendKeys(KeyEvent.KEYCODE_BACK);
        mInstrumentation.waitForIdleSync();

        assertTrue("Dialog is showing.", !dialog.isShowing());
    }

    /**
     * Run the onReceive method on MasterClear BroadcastReceiver
     * @param scale
     * @param level
     */
    private void runOnReceive(int scale, int level) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra("scale", scale);
        intent.putExtra("level", level);
        BroadcastReceiver receiver = (BroadcastReceiver)getMember(mMasterClear, "mBatteryReceiver");
        receiver.onReceive(mMasterClear, intent);
    }

    /**
     * Get the low battery dialog
     * @return
     * @throws Exception
     */
    public AlertDialog getLowBatteryDialog() throws Exception {
        SparseArray<Object> managedDialogs = getDialogs(mMasterClear);

        Object managedDialog = managedDialogs.get(DLG_MASTER_CLEAR_BATTERY_SHORT);
        assertNotNull("Could not get the dialog.", managedDialog);

        // Extract the mDialog field from managedDialog
        Field[] fields = managedDialog.getClass().getDeclaredFields();
        Field dialogField = null;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals("mDialog")) {
                dialogField = fields[i];
                break;
            }
        }
        assertNotNull("Dialog field renamed", dialogField);

        dialogField.setAccessible(true);
        Dialog dialog = (Dialog)dialogField.get(managedDialog);
        assertNotNull("A managed dialog is missing its dialog.", dialog);
        assertTrue("Dialog not an AlertDialog", dialog instanceof AlertDialog);

        return (AlertDialog)dialog;
    }

    //// Reflections ////

    /**
     * Get dialogs from the managedDialogs in super class Activity
     * @param activity
     * @return
     */
    public SparseArray<Object> getDialogs(Object activity) {
        try {
            Field managedDialogField = null;
            Object instance = activity;
            Class clazz = instance.getClass();
            Class superclazz = clazz.getSuperclass();
            Field[] fields = superclazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if ("mManagedDialogs".equals(field.getName())) {
                    managedDialogField = field;
                    break;
                }
            }
            managedDialogField.setAccessible(true);
            return (SparseArray<Object>)managedDialogField.get(instance);
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    /**
     * Set a private member
     * @param instance
     * @param member
     * @param value
     */
    public static void setMember(Object instance, String member, Object value) {
        try {
            Class clazz = instance.getClass();
            Field field = clazz.getDeclaredField(member);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Get a private member
     * @param instance
     * @param member
     * @return
     */
    public static Object getMember(Object instance, String member) {
        try {
            Class clazz = instance.getClass();
            Field field = clazz.getDeclaredField(member);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            fail();
        }
        return null;
    }
}
