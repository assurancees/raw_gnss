package dev.joshi.raw_gnss

import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.EventChannel

class GnssStatusHandlerImpl(private val locationManager: LocationManager) : EventChannel.StreamHandler {

    companion object {
        private const val TAG = "GNSS_STATUS"
    }

    private var listener: GnssStatus.Callback? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        Log.d(TAG, "onListen")
        listener = createSensorEventListener(events!!)
        locationManager.registerGnssStatusCallback(listener!!)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onCancel(arguments: Any?) {
        Log.d(TAG, "onCancel")
        listener?.let { locationManager.unregisterGnssStatusCallback(it) }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun createSensorEventListener(events: EventChannel.EventSink): GnssStatus.Callback {
        return object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(eventArgs: GnssStatus) {
                Log.d(TAG, "onGnssStatusReceived: $eventArgs")
                super.onSatelliteStatusChanged(eventArgs)

                val resultMap = HashMap<String, Any>()
                resultMap["satelliteCount"] = eventArgs.satelliteCount
                resultMap["hashCode"] = eventArgs.hashCode()

                val statusMapList = ArrayList<HashMap<String, Any>>()

                for (i in 0 until eventArgs.satelliteCount) {
                    val map = HashMap<String, Any>()
                    map["azimuthDegrees"] = eventArgs.getAzimuthDegrees(i)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        map["carrierFrequencyHz"] = eventArgs.getCarrierFrequencyHz(i)
                    }
                    map["cn0DbHz"] = eventArgs.getCn0DbHz(i)
                    map["constellationType"] = eventArgs.getConstellationType(i)
                    map["elevationDegrees"] = eventArgs.getElevationDegrees(i)
                    map["svid"] = eventArgs.getSvid(i)
                    map["hasAlmanacData"] = eventArgs.hasAlmanacData(i)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        map["hasCarrierFrequencyHz"] = eventArgs.hasCarrierFrequencyHz(i)
                    }
                    map["hasEphemerisData"] = eventArgs.hasEphemerisData(i)
                    map["usedInFix"] = eventArgs.usedInFix(i)
                    statusMapList.add(map)
                }

                resultMap["status"] = statusMapList
                uiThreadHandler.post { events.success(resultMap) }
            }
        }
    }
}
