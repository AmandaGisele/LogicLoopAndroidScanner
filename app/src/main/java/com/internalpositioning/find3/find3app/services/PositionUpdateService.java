package com.internalpositioning.find3.find3app;

import android.app.job.JobParameters;
import android.app.job.JobService;

import android.content.Intent;


public class PositionUpdateService extends JobService {
    private static final String TAG = "PositionUpdateService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent service = new Intent(getApplicationContext(), ScanService.class);
        getApplicationContext().startService(service);
        JobScheduleUtil.scheduleJob(getApplicationContext(), params.getExtras()); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}