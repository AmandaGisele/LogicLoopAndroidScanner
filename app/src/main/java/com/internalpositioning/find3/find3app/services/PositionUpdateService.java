package com.internalpositioning.find3.find3app.services;

import android.os.Bundle;
import android.app.job.JobParameters;
import android.app.job.JobService;

import android.content.Intent;

import com.internalpositioning.find3.find3app.jobs.JobScheduleUtil;


public class PositionUpdateService extends JobService {
    private static final String TAG = "PositionUpdateService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent service = new Intent(getApplicationContext(), ScanService.class);

        service.putExtras(new Bundle(params.getExtras()));
        getApplicationContext().startService(service);

        // reschedule the job (probably not needed with a periodic job?)
        // JobScheduleUtil.scheduleJob(getApplicationContext(), params.getExtras());
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}