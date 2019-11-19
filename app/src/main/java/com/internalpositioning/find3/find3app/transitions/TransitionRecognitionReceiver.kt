package com.internalpositioning.find3.find3app.transitions

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.internalpositioning.find3.find3app.MainActivity
import com.internalpositioning.find3.find3app.R
import com.internalpositioning.find3.find3app.jobs.JobScheduleUtil
import com.internalpositioning.find3.find3app.util.BundleUtil

class TransitionRecognitionReceiver : BroadcastReceiver() {
    companion object {
        val INTENT_ACTION = "com.internalpositioning.find3.find3app.ACTION_PROCESS_ACTIVITY_TRANSITIONS"
    }

    private val TAG = TransitionRecognitionReceiver::class.java!!.getSimpleName()

    lateinit var mContext: Context
    private var bundle: PersistableBundle? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("DEBUG", "Debugging could be better");

        if (intent != null && INTENT_ACTION.equals(intent.getAction())) {
            mContext = context!!


            Log.d("TransitionReceiver", "onReceive")
            bundle = PersistableBundle(BundleUtil.toPersistableBundle(intent!!.getExtras()))



            if (ActivityTransitionResult.hasResult(intent)) {
                var result = ActivityTransitionResult.extractResult(intent)

                if (result != null) {
                    processTransitionResult(result)
                }
            }
        }
    }


    fun processTransitionResult(result: ActivityTransitionResult) {
        for (event in result.transitionEvents) {
            onDetectedTransitionEvent(event)
        }
    }

    private fun onDetectedTransitionEvent(activity: ActivityTransitionEvent) {
        when (activity.activityType) {
            DetectedActivity.STILL -> {
                JobScheduleUtil.cancelAll(mContext)
            }
            else -> {
                // Cancel running jobs on activity change.
                // Basically prevent the job from running multiple times concurrently.
                // There's probably a better way to do this but it's late...
                JobScheduleUtil.cancelAll(mContext)
                // Re-schedule job
                JobScheduleUtil.scheduleJob(mContext, bundle)
            }
        }
    }
}