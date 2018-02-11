package chaosmail.at.phonetracker

import android.os.Build
import android.util.Log

object SecurityUtil {

    const val PERM_READ_PHONE_STATE = 0
    const val PERM_ACCESS_COARSE_LOCATION = 1
    const val PERM_ACCESS_FINE_LOCATION = 2

    private const val TAG = "PhoneTracker::Security"

    fun <T> catchSecurity(f: () -> T, onError: (() -> Unit)? = null): T? {
        return try {
            return f()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to access network data")
            if (onError != null) {
                onError()
            }
            null
        }
    }

    fun <T> sdkGreaterThan(version: Int, f: () -> T): T? {
        return if (Build.VERSION.SDK_INT >= version) f() else null
    }
}