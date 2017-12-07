package com.android.settings.network;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;


import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnitRunner;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.AppTask;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.UiScrollable;
import android.text.format.DateUtils;
import android.view.KeyEvent;

import com.android.settings.network.ApnMimsiEditor;

import com.android.settings.Settings;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;


import static com.android.settings.network.ApnSettings.mimsi_insert_action;
import static com.android.settings.network.ApnSettings.mimsi_edit_action;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link com.android.settings.apn.ApnMimsiEditorUITest}
 * <p>
 * m SettingsUnitTests &&
 * adb install \
 * -r -g  ${ANDROID_PRODUCT_OUT}/data/app/SettingsUnitTests/SettingsUnitTests.apk &&
 * adb shell am instrument -e class com.android.settings.network.ApnMimsiEditorUITest \
 * -w com.android.settings.tests.unit/android.support.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ApnMimsiEditorUITest {
    private static final long TIMEOUT = 5 * DateUtils.SECOND_IN_MILLIS;

    private UiDevice mDevice;
    private Context mTargetContext;
    private String mSettingPackage;
    private String apn;
    private String mccMnc;

    @Rule
    public ActivityTestRule<Settings.ApnMimsiEditorActivity> mApnMimsiEditor =
            new ActivityTestRule<>(
                    Settings.ApnMimsiEditorActivity.class,
                    true /* enable touch at launch */,
                    false /* don't launch at every test */);

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(getInstrumentation());
        mTargetContext = getInstrumentation().getTargetContext();
        mSettingPackage = mTargetContext.getPackageName();
        apn = "test.test.com";
        mccMnc = "310260, 21010";
        Intent newApnIntent = new Intent(mTargetContext,
                Settings.ApnSettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);



        getInstrumentation().getContext().startActivity(newApnIntent);
        mDevice.waitForIdle();

        mDevice.pressMenu();
        mDevice.waitForIdle();
        Thread.sleep(1000);

        UiSelector selectorSave = new UiSelector().text(mTargetContext.getString(R.string.menu_restore));
        UiObject objectSave = mDevice.findObject(selectorSave);
        objectSave.exists();
        objectSave.click();
        mDevice.waitForIdle();

        Thread.sleep(1000);

    }

    private UiSelector getPreferenceTitleSelector(String description) {
        return new UiSelector().description(description)
                .childSelector(new UiSelector()
                        .className("android.widget.RelativeLayout")
                        .childSelector(new UiSelector().index(0)));
    }

    private boolean enterValue(String value) throws Exception {
        mDevice.waitForIdle();
        UiSelector selectorEntry = new UiSelector().focused(true);
        UiObject viewEntry = mDevice.findObject(selectorEntry);

        viewEntry.setText(value);

        mDevice.waitForIdle();
        Thread.sleep(500);

        UiSelector selectorEnter = new UiSelector().text(mTargetContext.getString(R.string.dlg_ok));
        UiObject viewEnter = mDevice.findObject(selectorEnter);
        viewEnter.click();
        return viewEnter.waitUntilGone(TIMEOUT);
    }

    private void goToMccMnc() throws Exception {
        UiScrollable appViews1 = new UiScrollable(new UiSelector().scrollable(true));
        appViews1.scrollTextIntoView(mTargetContext
                .getString(R.string.apn_mccmnc));

    }

    @Test
    public void saveMccMnc_shouldNotCrash() throws Throwable {

        Intent newApnIntent = new Intent(mimsi_insert_action).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        getInstrumentation().getContext().startActivity(newApnIntent);
        mDevice.waitForIdle();


        // Ignore any interstitial options

        UiSelector selectorName = new UiSelector().text(mTargetContext.getString(R.string.apn_name));
        UiObject viewName = mDevice.findObject(selectorName);

        assertTrue(viewName.clickAndWaitForNewWindow(TIMEOUT));

        assertTrue(enterValue("test"));

        mDevice.waitForIdle();

        UiSelector selectorApn = new UiSelector().text(mTargetContext.getString(R.string.apn_apn));
        UiObject viewApn = mDevice.findObject(selectorApn);

        assertTrue(viewApn.clickAndWaitForNewWindow(TIMEOUT));

        assertTrue(enterValue(apn));

        mDevice.waitForIdle();


        goToMccMnc();

        UiSelector selectorMccMnc = new UiSelector().text(mTargetContext
                .getString(R.string.apn_mccmnc));
        UiObject viewMccMnc = mDevice.findObject(selectorMccMnc);

        assertTrue(viewMccMnc.clickAndWaitForNewWindow(TIMEOUT));

        //retrieve text
        UiSelector selectorEntry = new UiSelector().focused(true);
        UiObject viewEntry = mDevice.findObject(selectorEntry);

        assertTrue(enterValue(mccMnc));

        mDevice.waitForIdle();


        mDevice.pressMenu();

        UiSelector selectorSave = new UiSelector().text(mTargetContext.getString(R.string.save));
        mDevice.findObject(selectorSave).click();
        mDevice.waitForIdle();

        Thread.sleep(1000);

        editMccMnc_shouldNotCrash();

    }

    public void editMccMnc_shouldNotCrash() throws Throwable {

        Intent newApnIntent = new Intent(mimsi_edit_action).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newApnIntent.putExtra("APN", apn);

        getInstrumentation().getContext().startActivity(newApnIntent);
        mDevice.waitForIdle();


        goToMccMnc();

        UiSelector selectorMccMnc = new UiSelector().text(mTargetContext
                .getString(R.string.apn_mccmnc));
        UiObject viewMccMnc = mDevice.findObject(selectorMccMnc);

        assertTrue(viewMccMnc.clickAndWaitForNewWindow(TIMEOUT));

        //retrieve text
        UiSelector selectorEntry = new UiSelector().focused(true);
        UiObject viewEntry = mDevice.findObject(selectorEntry);
        assertEquals(mccMnc.replace(" ", ""), viewEntry.getText());
        assertTrue(enterValue("10010,10010"));

        mDevice.waitForIdle();
        mDevice.pressMenu();

        UiSelector selectorSave = new UiSelector().clickable(true).index(2);
        mDevice.findObject(selectorSave).click();
        mDevice.waitForIdle();

    }

    @Test
    public void saveMccMnc_withNoName() throws Throwable {

        Intent newApnIntent = new Intent(mimsi_insert_action).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        getInstrumentation().getContext().startActivity(newApnIntent);
        mDevice.waitForIdle();


        // Ignore any interstitial options


        UiSelector selectorApn = new UiSelector().text(mTargetContext.getString(R.string.apn_apn));
        UiObject viewApn = mDevice.findObject(selectorApn);

        assertTrue(viewApn.clickAndWaitForNewWindow(TIMEOUT));

        assertTrue(enterValue("test2.apn.fr"));

        mDevice.waitForIdle();


        goToMccMnc();

        UiSelector selectorMccMnc = new UiSelector().text(mTargetContext
                .getString(R.string.apn_mccmnc));
        UiObject viewMccMnc = mDevice.findObject(selectorMccMnc);

        assertTrue(viewMccMnc.clickAndWaitForNewWindow(TIMEOUT));

        //retrieve text
        UiSelector selectorEntry = new UiSelector().focused(true);
        UiObject viewEntry = mDevice.findObject(selectorEntry);

        assertTrue(enterValue(mccMnc));

        mDevice.waitForIdle();


        mDevice.pressMenu();

        UiSelector selectorSave = new UiSelector().text(mTargetContext.getString(R.string.save));
        mDevice.findObject(selectorSave).click();
        mDevice.waitForIdle();


        UiSelector selectorError = new UiSelector().className("android.widget.TextView");
        assertEquals(mTargetContext.getString(R.string.error_name_empty), mDevice.findObject(selectorError).getText());


        Thread.sleep(1000);


    }

    @Test
    public void saveMccMnc_noApn() throws Throwable {

        Intent newApnIntent = new Intent(mimsi_insert_action).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        getInstrumentation().getContext().startActivity(newApnIntent);
        mDevice.waitForIdle();


        // Ignore any interstitial options

        UiSelector selectorName = new UiSelector().text(mTargetContext.getString(R.string.apn_name));
        UiObject viewName = mDevice.findObject(selectorName);

        assertTrue(viewName.clickAndWaitForNewWindow(TIMEOUT));

        assertTrue(enterValue("test"));

        mDevice.waitForIdle();


        goToMccMnc();

        UiSelector selectorMccMnc = new UiSelector().text(mTargetContext
                .getString(R.string.apn_mccmnc));
        UiObject viewMccMnc = mDevice.findObject(selectorMccMnc);

        assertTrue(viewMccMnc.clickAndWaitForNewWindow(TIMEOUT));

        //retrieve text
        UiSelector selectorEntry = new UiSelector().focused(true);
        UiObject viewEntry = mDevice.findObject(selectorEntry);

        assertTrue(enterValue(mccMnc));

        mDevice.waitForIdle();


        mDevice.pressMenu();

        UiSelector selectorSave = new UiSelector().text(mTargetContext.getString(R.string.save));
        mDevice.findObject(selectorSave).click();
        mDevice.waitForIdle();

        UiSelector selectorError = new UiSelector().className("android.widget.TextView");
        assertEquals(mTargetContext.getString(R.string.error_apn_empty), mDevice.findObject(selectorError).getText());


        Thread.sleep(1000);


    }


    @Test
    public void saveMccMnc_badMccMnc() throws Throwable {

        Intent newApnIntent = new Intent(mimsi_insert_action).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        getInstrumentation().getContext().startActivity(newApnIntent);
        mDevice.waitForIdle();


        // Ignore any interstitial options

        UiSelector selectorName = new UiSelector().text(mTargetContext.getString(R.string.apn_name));
        UiObject viewName = mDevice.findObject(selectorName);

        assertTrue(viewName.clickAndWaitForNewWindow(TIMEOUT));

        assertTrue(enterValue("test"));

        mDevice.waitForIdle();

        UiSelector selectorApn = new UiSelector().text(mTargetContext.getString(R.string.apn_apn));
        UiObject viewApn = mDevice.findObject(selectorApn);

        assertTrue(viewApn.clickAndWaitForNewWindow(TIMEOUT));

        assertTrue(enterValue(apn));

        mDevice.waitForIdle();


        goToMccMnc();

        UiSelector selectorMccMnc = new UiSelector().text(mTargetContext
                .getString(R.string.apn_mccmnc));
        UiObject viewMccMnc = mDevice.findObject(selectorMccMnc);

        assertTrue(viewMccMnc.clickAndWaitForNewWindow(TIMEOUT));

        //retrieve text
        UiSelector selectorEntry = new UiSelector().focused(true);
        UiObject viewEntry = mDevice.findObject(selectorEntry);

        assertTrue(enterValue("10010, 20"));

        mDevice.waitForIdle();


        mDevice.pressMenu();

        UiSelector selectorSave = new UiSelector().text(mTargetContext.getString(R.string.save));
        mDevice.findObject(selectorSave).click();
        mDevice.waitForIdle();

        UiSelector selectorError = new UiSelector().className("android.widget.TextView");
        assertEquals(mTargetContext.getString(R.string.error_mcc_not5), mDevice.findObject(selectorError).getText());

        Thread.sleep(1000);

    }
}
