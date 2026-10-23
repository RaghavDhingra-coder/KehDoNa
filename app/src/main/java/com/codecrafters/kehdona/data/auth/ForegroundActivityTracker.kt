package com.codecrafters.kehdona.data.auth

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Phone verification (reCAPTCHA fallback) and Credential Manager both need an Activity.
 * This keeps a weak reference to the resumed one so ViewModels never hold an Activity.
 */
class ForegroundActivityTracker(application: Application) : Application.ActivityLifecycleCallbacks {
    private var current = WeakReference<Activity>(null)

    val activity: Activity? get() = current.get()

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (current.get() == null) current = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (current.get() === activity) current = WeakReference(null)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
