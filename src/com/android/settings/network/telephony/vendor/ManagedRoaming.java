/*
 * Copyright (c) 2016 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.android.phone.vendor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.android.phone.R;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import static com.android.internal.telephony.PhoneConstants.SUBSCRIPTION_KEY;

public class ManagedRoaming extends Activity {
    private static final String LOG_TAG = "ManagedRoaming";

    private int mSubscription = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
    private boolean mIsMRDialogShown = false;

    // Key used to read and write the saved network selection numeric value
    private static final String NETWORK_SELECTION_KEY = "network_selection_key";
    private final int NETWORK_SCAN_ACTIVITY_REQUEST_CODE = 0;
    private static final String EXTRA_SLOT_ID = "slot_id";
    private Phone mPhone;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        int subscription = intent.getIntExtra(SUBSCRIPTION_KEY,
                SubscriptionManager.getDefaultSubscriptionId());
        mPhone = PhoneFactory.getPhone(
                SubscriptionManager.getSlotIndex(subscription));
        if (mPhone == null) {
            // Do the best we can
            mPhone = PhoneFactory.getDefaultPhone();
        }
        createManagedRoamingDialog(subscription);
    }

    /*
     * Show Managed Roaming dialog if user preferred Network Selection mode is 'Manual'
     */
    private void createManagedRoamingDialog(int subscription) {
        Resources r = Resources.getSystem();
        String networkSelection = PreferenceManager.getDefaultSharedPreferences(mPhone.getContext())
                .getString(NETWORK_SELECTION_KEY + subscription, "");
        log(" Received Managed Roaming intent, networkSelection "
                + networkSelection + " Is Dialog Displayed " + mIsMRDialogShown
                + " sub = " + subscription);
        // networkSelection will be empty for 'Automatic' mode.
        if (!TextUtils.isEmpty(networkSelection) && !mIsMRDialogShown) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
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
                    networkSettingIntent.setClassName("com.android.phone",
                            "com.android.phone.MobileNetworkSettings");
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
