package com.internalpositioning.find3.find3app

import android.content.Context

abstract class TransitionRecognitionAbstract {
    abstract fun startTracking(context: Context)
    abstract fun stopTracking()
}