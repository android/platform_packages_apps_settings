package com.android.settings.dashboard.conditional;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;
import android.content.Context;


/**
 * Service to handle callbacks from the JobScheduler. Requests scheduled with the JobScheduler
 * ultimately land on this service's "onStartJob" method. HandleAirplaneModeChangedService handles
 * changes to the settings value AIRPLANE_MODE_ON
 */
public class HandleAirplaneModeChangedService extends JobService {

    private static final String TAG = "HandleAirplaneModeChangedService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(TAG, "onStartJob: +");
        ConditionManager.get(this).getCondition(AirplaneModeCondition.class)
                .refreshState();

        AirplaneModeCondition.scheduleHandleAirplaneModeJob(this);
        Log.d(TAG, "onStartJob: returning false");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // called if JobScheduler has to stop the job in the middle

        // Return true to keep the job on the queue
        Log.d(TAG, "onStopJob: returning true");
        return true;
    }
}
