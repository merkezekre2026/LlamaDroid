package com.llamadroid.domain.model

import android.app.ActivityManager
import android.content.Context

class EstimateModelMemoryWarningUseCase(private val context: Context) {
    operator fun invoke(model: LocalModel): String? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val available = memoryInfo.availMem
        return when {
            model.sizeBytes > available * 0.75 -> "This model is close to or larger than currently available RAM."
            model.sizeBytes > 4L * 1024L * 1024L * 1024L -> "Large models may load slowly or fail on mid-range devices."
            else -> null
        }
    }
}
