/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;

import android.util.Log;

public class UsbModeChooserReceiver extends BroadcastReceiver {

    private static final String PROPERTY_SYS_BOOT_COMPLETE = "sys.boot_completed";
    private static final String PROPERTY_DEV_BOOT_COMPLETE = "dev.bootcomplete";
    private static final String USB_MODE_CHOOSER_PACKAGE_NAME = "com.android.settings";
    private static final String USB_MODE_CHOOSER_ACTIVITY_NAME =
            "com.android.settings.deviceinfo.UsbModeChooserActivity";
    /*
     * Boolean extra indicating whether USB is connected or disconnected as
     * host. It is defined in UsbManager.java after Android N.
     */
    private static final String USB_HOST_CONNECTED = "host_connected";

    protected static boolean mSoftSwitch = false;
    private static boolean mIsPowerSupply = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action != null) {
            if (UsbManager.ACTION_USB_STATE.equals(action)) {
                boolean plugged = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                boolean configured = intent.getBooleanExtra(UsbManager.USB_CONFIGURED, false);
                // The following step is valid to only device that Type-C is supported.
                // If the device doesn't support Type-C, "hostConnected" is set to "false".
                boolean hostConnected = intent.getBooleanExtra(USB_HOST_CONNECTED, false);
                if (configured) {
                    //Since system is not ready to show the dialog during boot up phase.
                    //Don't show dialog before boot up is completed.
                    //Don't show the dialog when AOA is automatically enabled.
                    if (!mSoftSwitch
                            && SystemProperties.getBoolean(PROPERTY_SYS_BOOT_COMPLETE, false)
                            && SystemProperties.getBoolean(PROPERTY_DEV_BOOT_COMPLETE, false)
                            && !intent.getBooleanExtra(UsbManager.USB_FUNCTION_AUDIO_SOURCE, false)
                            && !intent.getBooleanExtra(UsbManager.USB_FUNCTION_ACCESSORY, false)) {
                        Intent modeChooserIntent = Intent.makeRestartActivityTask(
                                new ComponentName(USB_MODE_CHOOSER_PACKAGE_NAME,
                                        USB_MODE_CHOOSER_ACTIVITY_NAME));
                        try{
                            context.startActivity(modeChooserIntent);
                        } catch (ActivityNotFoundException anfe) {
                            Log.d(this.getClass() + "",
                            "Unable to start UsbModeChooserActivity");
                        }
                        // Initialize mIsPowerSupply since phone already became Peripheral mode
                        // when configured is notified with "true".
                        mIsPowerSupply = false;
                    }
                    mSoftSwitch = true;
                }
                // When unplugged, reset mSoftSwitch and mIsPowerSupply.
                if(!plugged) {
                    mSoftSwitch = false;
                    mIsPowerSupply = false;
                    return;
                }

                // When "Power supply" was selected during connecting to MUT,
                // hostConnected is changed from true to false.
                // So set mIsPowerSupply to judge whether "Power supply" was selected
                // when hostConnected is true.
                // The following step is valid to only device that Type-C is supported.
                // If the device doesn't support Type-C, hostConnected is notified with "false".
                if (hostConnected) {
                    mIsPowerSupply = true;
                }

                // The following step is valid to only device that Type-C is supported.
                // If the device doesn't support Type-C, "mIsPowerSupply" remains "false".
                if (mIsPowerSupply && !hostConnected && !configured) {
                    // Initialize mSoftSwitch to show "Use USB for" dialog
                    // only when "Power supply" was selected.
                    mSoftSwitch = false;
                }
            }
        }
    }
}
