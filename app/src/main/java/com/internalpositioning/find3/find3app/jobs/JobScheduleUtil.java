package com.internalpositioning.find3.find3app.jobs;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import com.internalpositioning.find3.find3app.services.PositionUpdateService;

import java.util.concurrent.TimeUnit;

public class JobScheduleUtil {

    public static void scheduleJob(Context context, PersistableBundle extrasBundle) {
        ComponentName serviceComponent = new ComponentName(context, PositionUpdateService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);

        builder.setPeriodic(TimeUnit.SECONDS.toMillis(5));
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        builder.setRequiresDeviceIdle(false); // device should be idle
        builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);

        JobInfo jobInfo = new JobInfo.Builder(0, serviceComponent)
                .setExtras(extrasBundle)
                .build();

        jobScheduler.schedule(builder.build());
    }

    public static void cancelAll(Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancelAll();
    }
}