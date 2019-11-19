package com.internalpositioning.find3.find3app.transitions

import android.content.Context
import android.content.Intent

abstract class TransitionRecognitionAbstract {
    abstract fun startTracking(context: Context)
    abstract fun stopTracking()
}