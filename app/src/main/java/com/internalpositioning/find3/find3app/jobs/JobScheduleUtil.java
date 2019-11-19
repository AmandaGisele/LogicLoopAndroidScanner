package com.internalpositioning.find3.find3app;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

public class JobScheduleUtil {

    // schedule the start of the service every 1 - 3 seconds
    public static void scheduleJob(Context context, PersistableBundle extrasBundle) {
        ComponentName serviceComponent = new ComponentName(context, ScanService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);

        builder.setMinimumLatency(1 * 1000); // wait at least
        builder.setOverrideDeadline(3 * 1000); // maximum delay
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        builder.setRequiresDeviceIdle(false); // device should be idle
        builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);

        JobInfo jobInfo = new JobInfo.Builder(0, serviceComponent)
                .setExtras(extrasBundle)
                .build();

        jobScheduler.schedule(builder.build());
    }
}