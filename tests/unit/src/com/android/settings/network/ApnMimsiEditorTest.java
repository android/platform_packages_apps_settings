package com.android.settings.network;


import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.settings.Settings.ApnMimsiEditorActivity;
import com.android.settings.network.ApnSettings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static com.android.settings.network.ApnSettings.mimsi_insert_action;

/**
 * Tests for {@link com.android.settings.apn.ApnMimsiEditorTest}
 *
 * m SettingsUnitTests &&
 * adb install \
 * -r -g  ${ANDROID_PRODUCT_OUT}/data/app/SettingsUniTests/SettingsUnitTests.apk &&
 * adb shell am instrument -e class com.android.settings.network.ApnMimsiEditorUITest \
 * -w com.android.settings.tests.unit/android.support.test.runner.AndroidJUnitRunner
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ApnMimsiEditorTest {

    private Context mTargetContext;

    @Before
    public void setUp() throws Exception {
	Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
	mTargetContext = instrumentation.getTargetContext();
    }

    @Test
    public void nightDisplaySettingsIntent_resolvesCorrectly() {
	final PackageManager pm = mTargetContext.getPackageManager();
	final Intent intent = new Intent(mimsi_insert_action);
	final ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

	Assert.assertNotNull("No activity for " + mimsi_insert_action, ri);
	Assert.assertEquals(mTargetContext.getPackageName(), ri.activityInfo.packageName);
	Assert.assertEquals(ApnMimsiEditorActivity.class.getName(),
		    ri.activityInfo.name);

    }
}
