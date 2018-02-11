package chaosmail.at.phonetracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.telephony.TelephonyManager

data class NormalizedDeviceInfo(
        val simSerialNumber: String?, val simOperator: String?, val simOperatorName: String?,
        val simCountryIso: String?, val phoneCount: Int?,
        val networkOperator: String?, val networkOperatorName: String?, val subscriberId: String?,
        val deviceSoftwareVersion: String?, val imei: String?, val androidId: String?)

class DeviceInfo (private val activity: Activity, private val telephonyManager: TelephonyManager) {

    @SuppressLint("NewApi")
    fun getDeviceInfo(): NormalizedDeviceInfo? {
        return NormalizedDeviceInfo(
                simSerialNumber = SecurityUtil.catchSecurity(telephonyManager::getSimSerialNumber),
                simOperator = telephonyManager.simOperator,
                simOperatorName = telephonyManager.simOperatorName,
                simCountryIso = telephonyManager.simCountryIso,
                phoneCount = telephonyManager.phoneCount,
                networkOperator = telephonyManager.networkOperator,
                networkOperatorName = telephonyManager.networkOperatorName,
                subscriberId = SecurityUtil.catchSecurity(telephonyManager::getSubscriberId, {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        ActivityCompat.requestPermissions(activity,
                                arrayOf(Manifest.permission.READ_PHONE_STATE),
                                SecurityUtil.PERM_READ_PHONE_STATE)
                    }
                }),
                deviceSoftwareVersion = SecurityUtil.catchSecurity(telephonyManager::getDeviceSoftwareVersion, {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        ActivityCompat.requestPermissions(activity,
                                arrayOf(Manifest.permission.READ_PHONE_STATE),
                                SecurityUtil.PERM_READ_PHONE_STATE)
                    }
                }),
                imei = getImei(),
                androidId = getAndroidId()
        )
    }

    @Suppress("DEPRECATION")
    @SuppressLint("NewApi")
    private fun getImei(): String? {
        val deviceId = SecurityUtil.catchSecurity(telephonyManager::getDeviceId, {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.READ_PHONE_STATE), SecurityUtil.PERM_READ_PHONE_STATE)
            }
        })

        val imei = SecurityUtil.sdkGreaterThan(Build.VERSION_CODES.O, {
            SecurityUtil.catchSecurity(telephonyManager::getMeid, {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.requestPermissions(activity,
                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                            SecurityUtil.PERM_READ_PHONE_STATE)
                }
            })
        })

        return imei ?: deviceId
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String? {
        return Settings.Secure.getString(activity.applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID)
    }
}
