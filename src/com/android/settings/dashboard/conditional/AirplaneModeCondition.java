/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.dashboard.conditional;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Settings.WirelessSettingsActivity;
import com.android.settingslib.WirelessUtils;
import com.android.settings.dashboard.conditional.HandleAirplaneModeChangedService;

import static android.app.job.JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS;
import static android.os.UserHandle.SYSTEM;

public class AirplaneModeCondition extends Condition {
    private static final String TAG = "AirplaneModeCondition";

    public AirplaneModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
    }

    @Override
    public void refreshState() {
        setActive(WirelessUtils.isAirplaneModeOn(mManager.getContext()));
    }

    @Override
    protected Class<?> getReceiverClass() {
        return Receiver.class;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(mManager.getContext(), R.drawable.ic_airplane);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_airplane_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_airplane_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] { mManager.getContext().getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        mManager.getContext().startActivity(new Intent(mManager.getContext(),
                WirelessSettingsActivity.class));
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            ConnectivityManager.from(mManager.getContext()).setAirplaneMode(false);
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_AIRPLANE_MODE;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent startServiceIntent =
                        new Intent(context, HandleAirplaneModeChangedService.class);
                if (context.startServiceAsUser(startServiceIntent, SYSTEM) == null) {
                    Log.e(TAG, "Unable to start service");
                }
                scheduleHandleAirplaneModeJob(context);
            }
        }
    }

    /*
     * Queue up the HandleAirplaneModeChangedService job
     */
    public static void scheduleHandleAirplaneModeJob(Context context) {
        try {
            Log.d(TAG, "Setting up JobScheduler");
            JobInfo.TriggerContentUri trigger = new JobInfo.TriggerContentUri(
                    Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
                    FLAG_NOTIFY_FOR_DESCENDANTS);
            final int HANDLE_AIRPLANE_MODE_CHANGE_JOB_ID = 0;
            JobInfo.Builder builder = new JobInfo.Builder(HANDLE_AIRPLANE_MODE_CHANGE_JOB_ID,
                    new ComponentName(context, HandleAirplaneModeChangedService.class));
            builder.addTriggerContentUri(trigger);
            JobScheduler tm =
                    (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            tm.schedule(builder.build());
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException caught while trying to set up JobScheduler: " + e);
        }
    }
}
