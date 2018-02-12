package chaosmail.at.phonetracker

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.telephony.*
import android.telephony.gsm.GsmCellLocation
import android.telephony.TelephonyManager


data class NormalizedCellInfo(val technology: Technologie, val isRegistered: Boolean? = false,
                              val cellIdentity: CellIdentity, val signalStrength: SignalStrength? = null)

enum class Technologie {GSM, UMTS, LTE, UNKNOWN}
data class CellIdentity(val cid: Int?, val lac: Int?, val mcc: Int? = null, val mnc: Int? = null)
data class SignalStrength(val dbm: Int?, val level: Int? = null, val timingAdvance: Int? = null)

@SuppressLint("Registered")
class NetworkInfo(private val activity: Activity, private val telephonyManager: TelephonyManager) : Service() {

    class SignalStrengthListener : PhoneStateListener() {

        var currSignal: android.telephony.SignalStrength? = null

        override fun onCellLocationChanged(location: CellLocation) {}

        override fun onSignalStrengthsChanged(signalStrength: android.telephony.SignalStrength?) {
            currSignal = signalStrength
        }
    }

    var phoneistener: SignalStrengthListener? = null

    init {
        phoneistener = SignalStrengthListener()

        @Suppress("DEPRECATION")
        telephonyManager.listen(phoneistener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTH
                        or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        or PhoneStateListener.LISTEN_CELL_LOCATION)
    }

    override fun onBind(p0: Intent?): IBinder {
        @Suppress("CAST_NEVER_SUCCEEDS")
        return null as IBinder
    }

    fun getAllCells(): List<NormalizedCellInfo> {
        // for new devices
        var cellInfo = getAllCellInfo()!!.mapNotNull {cell -> normalizeCellInfo(cell)}

        // fallback to different technique on older devices
        // * use neighboring cell information
        // * use active cell information
        // * use a PhoneStateListener to track Signal Strength
        if (cellInfo.isEmpty()) {
            cellInfo = getNeighboringCellInfo()!!.mapNotNull {cell -> normalizeNeighborCellInfo(cell)}.toList()
            cellInfo = cellInfo.union(listOf(getActiveCell()).mapNotNull { cell -> normalizeCellLocation(cell)}).toList()
        }

        return cellInfo
    }

    @Suppress("DEPRECATION")
    private fun getActiveCell(): CellLocation? {
        return SecurityUtil.catchSecurity(telephonyManager::getCellLocation)
    }

    @Suppress("DEPRECATION")
    private fun getNeighboringCellInfo(): MutableList<NeighboringCellInfo>? {
        return SecurityUtil.catchSecurity(telephonyManager::getNeighboringCellInfo, {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, arrayOf(ACCESS_COARSE_LOCATION),
                        SecurityUtil.PERM_ACCESS_COARSE_LOCATION)
            }
        })
    }

    private fun getAllCellInfo(): List<CellInfo>? {
        return SecurityUtil.catchSecurity(telephonyManager::getAllCellInfo, {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(activity, arrayOf(ACCESS_COARSE_LOCATION),
                        SecurityUtil.PERM_ACCESS_COARSE_LOCATION)
            }
        })
    }

    @SuppressLint("NewApi")
    private fun normalizeCellLocation(cell: CellLocation?): NormalizedCellInfo? = when(cell) {
        is GsmCellLocation -> NormalizedCellInfo(
                technology = when(telephonyManager.networkType) {
                    TelephonyManager.NETWORK_TYPE_GSM -> Technologie.GSM
                    TelephonyManager.NETWORK_TYPE_GPRS -> Technologie.GSM
                    TelephonyManager.NETWORK_TYPE_UMTS -> Technologie.UMTS
                    TelephonyManager.NETWORK_TYPE_EDGE -> Technologie.UMTS
                    TelephonyManager.NETWORK_TYPE_HSPA -> Technologie.UMTS
                    TelephonyManager.NETWORK_TYPE_HSDPA -> Technologie.UMTS
                    TelephonyManager.NETWORK_TYPE_HSUPA -> Technologie.UMTS
                    TelephonyManager.NETWORK_TYPE_HSPAP -> Technologie.UMTS
                    TelephonyManager.NETWORK_TYPE_LTE -> Technologie.LTE
                    else -> Technologie.UNKNOWN
                },
                isRegistered = true,
                cellIdentity = CellIdentity(
                        cid = checkInt(cell.cid),
                        lac = checkInt(cell.lac)
                ),
                signalStrength = SignalStrength(
                        dbm = when(telephonyManager.networkType) {
                            TelephonyManager.NETWORK_TYPE_EDGE -> phoneistener!!.currSignal!!.evdoDbm
                            TelephonyManager.NETWORK_TYPE_UMTS -> phoneistener!!.currSignal!!.evdoDbm
                            TelephonyManager.NETWORK_TYPE_HSPA -> phoneistener!!.currSignal!!.evdoDbm
                            TelephonyManager.NETWORK_TYPE_HSDPA -> phoneistener!!.currSignal!!.evdoDbm
                            TelephonyManager.NETWORK_TYPE_HSUPA -> phoneistener!!.currSignal!!.evdoDbm
                            TelephonyManager.NETWORK_TYPE_HSPAP -> phoneistener!!.currSignal!!.evdoDbm
                            else ->  rssiToDbm(phoneistener!!.currSignal!!.gsmSignalStrength)
                        },
                        level = SecurityUtil.sdkGreaterThan(Build.VERSION_CODES.M,
                                phoneistener!!.currSignal!!::getLevel)
                )

        )
        else -> null
    }
}

fun checkInt(value: Int): Int? {
    if (value < Int.MAX_VALUE) {
        return value
    }
    return null
}

fun normalizeCellInfo(cell: CellInfo): NormalizedCellInfo? = when (cell) {
    is CellInfoGsm -> NormalizedCellInfo(
            technology = Technologie.GSM,
            isRegistered = cell.isRegistered,
            cellIdentity = CellIdentity(
                    cid = checkInt(cell.cellIdentity.cid),
                    lac = checkInt(cell.cellIdentity.lac),
                    mcc = checkInt(cell.cellIdentity.mcc),
                    mnc = checkInt(cell.cellIdentity.mnc)),
            signalStrength = SignalStrength(
                    dbm = cell.cellSignalStrength.dbm,
                    level = cell.cellSignalStrength.level,
                    timingAdvance = getTimingAdvance(cell)))

    is CellInfoWcdma -> NormalizedCellInfo(
            technology = Technologie.UMTS,
            isRegistered = cell.isRegistered,
            cellIdentity = CellIdentity(
                    cid = checkInt(cell.cellIdentity.cid),
                    lac = checkInt(cell.cellIdentity.lac),
                    mcc = checkInt(cell.cellIdentity.mcc),
                    mnc = checkInt(cell.cellIdentity.mnc)),
            signalStrength = SignalStrength(
                    dbm = cell.cellSignalStrength.dbm,
                    level = cell.cellSignalStrength.level))

    is CellInfoLte -> NormalizedCellInfo(
            technology = Technologie.LTE,
            isRegistered = cell.isRegistered,
            cellIdentity = CellIdentity(
                    cid = checkInt(cell.cellIdentity.ci),
                    lac = checkInt(cell.cellIdentity.tac),
                    mcc = checkInt(cell.cellIdentity.mcc),
                    mnc = checkInt(cell.cellIdentity.mnc)),
            signalStrength = SignalStrength(
                    dbm = cell.cellSignalStrength.dbm,
                    level = cell.cellSignalStrength.level,
                    timingAdvance = getTimingAdvance(cell)))

    else -> null
}

fun normalizeNeighborCellInfo(cell: NeighboringCellInfo): NormalizedCellInfo? {
    return NormalizedCellInfo(
            technology = when(cell.networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS -> Technologie.GSM
                TelephonyManager.NETWORK_TYPE_EDGE -> Technologie.GSM
                TelephonyManager.NETWORK_TYPE_UMTS -> Technologie.UMTS
                TelephonyManager.NETWORK_TYPE_HSDPA -> Technologie.UMTS
                TelephonyManager.NETWORK_TYPE_HSUPA -> Technologie.UMTS
                TelephonyManager.NETWORK_TYPE_HSPA -> Technologie.UMTS
                else -> Technologie.UNKNOWN
            },
            cellIdentity = CellIdentity(
                    cid = checkInt(cell.cid),
                    lac = checkInt(cell.lac)
            ),
            signalStrength = SignalStrength(
                    dbm = rssiToDbm(cell.rssi)
            )
    )
}

fun rssiToDbm(value: Int): Int? {
    if (value == NeighboringCellInfo.UNKNOWN_RSSI) {
        return null
    }
    return (0.474 * value - 113).toInt()
}

fun getTimingAdvance(cell: CellInfo): Int? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        when (cell) {
            is CellInfoGsm -> checkInt(cell.cellSignalStrength.timingAdvance)
            is CellInfoLte -> checkInt(cell.cellSignalStrength.timingAdvance)
            else -> null
        }
    } else {
        null
    }
}