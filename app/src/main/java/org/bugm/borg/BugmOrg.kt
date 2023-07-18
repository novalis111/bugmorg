package org.bugm.borg

import android.app.Application

class BugmOrg : Application() {
    override fun onCreate() {
        super.onCreate()
        AppInitialization.init(this)
    }
}
