package com.llamadroid

import android.app.Application
import com.llamadroid.app.AppGraph

class LlamaDroidApp : Application() {
    val graph: AppGraph by lazy { AppGraph.get(this) }
}
