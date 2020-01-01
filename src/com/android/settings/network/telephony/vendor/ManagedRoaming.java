/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.network.telephony.vendor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;

import com.android.settings.R;

import static com.android.internal.telephony.PhoneConstants.SUBSCRIPTION_KEY;

public class ManagedRoaming extends Activity {
    private static final String LOG_TAG = "ManagedRoaming";

    private int mSubscription = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
    private boolean mIsMRDialogShown = false;

    private final int NETWORK_SCAN_ACTIVITY_REQUEST_CODE = 0;
    private static final String EXTRA_SLOT_ID = "slot_id";

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        int subscription = intent.getIntExtra(SUBSCRIPTION_KEY,
                SubscriptionManager.getDefaultSubscriptionId());
        createManagedRoamingDialog(subscription);
    }

    /*
     * Show Managed Roaming dialog if user preferred Network Selection mode is 'Manual'
     */
    private void createManagedRoamingDialog(int subscription) {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        TelephonyManager mgr = tm.createForSubscriptionId(subscription);

        ServiceState ss = mgr.getServiceState();
        boolean isManualMode = ss.getIsManualSelection();

        log(" Received Managed Roaming intent, isManualMode "
                + isManualMode + " Is Dialog Displayed " + mIsMRDialogShown
                + " sub = " + subscription);
        if (isManualMode && !mIsMRDialogShown) {
            String title = getString(R.string.managed_roaming_title);

            mSubscription = subscription;
            int phoneId = SubscriptionManager.getSlotIndex(subscription);
            if ((tm.getPhoneCount() > 1) && (tm.getPhoneCount() > phoneId)) {
                title = getResources().getString(R.string.managed_roaming_title_sub, phoneId + 1);
            }

            AlertDialog managedRoamingDialog = new AlertDialog.Builder(ManagedRoaming.this)
                    .setTitle(title)
                    .setMessage(R.string.managed_roaming_dialog_content)
                    .setPositiveButton(android.R.string.yes,
                        onManagedRoamingDialogClick)
                    .setNegativeButton(android.R.string.no,
                        onManagedRoamingDialogClick)
                    .create();

            managedRoamingDialog.setCanceledOnTouchOutside(false);
            managedRoamingDialog.setOnCancelListener(new OnCancelListener(){
                public void onCancel(DialogInterface dialog) {
                    mIsMRDialogShown = false;
                    finish();
                }
            });
            mIsMRDialogShown = true;
            managedRoamingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            managedRoamingDialog.show();
        } else {
            finish();
        }
    }

    DialogInterface.OnClickListener onManagedRoamingDialogClick =
        new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    log("Launch network settings activity sub = " + mSubscription);
                    Intent networkSettingIntent =
                            new Intent(Intent.ACTION_MAIN);
                    networkSettingIntent.setClassName("com.android.settings",
                            "com.android.settings.network.telephony.MobileNetworkActivity");
                    networkSettingIntent.putExtra(EXTRA_SLOT_ID,
                            SubscriptionManager.getSlotIndex(mSubscription));
                    startActivityForResult(networkSettingIntent,
                            NETWORK_SCAN_ACTIVITY_REQUEST_CODE);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    finish();
                    break;
                default:
                    Log.w(LOG_TAG, "received unknown button type: "+ which);
                    finish();
                    break;
            }
            mIsMRDialogShown = false;
        }
    };

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log("On activity result ");
        finish();
    }
}
