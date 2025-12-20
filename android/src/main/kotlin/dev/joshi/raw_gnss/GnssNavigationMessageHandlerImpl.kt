package dev.joshi.raw_gnss

import android.location.GnssNavigationMessage
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.EventChannel

class GnssNavigationMessageHandlerImpl(private val locationManager: LocationManager) : EventChannel.StreamHandler {

    companion object {
        private const val TAG = "GNSS_MESSAGE"
    }

    private var listener: GnssNavigationMessage.Callback? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        Log.d(TAG, "onListen")
        listener = createSensorEventListener(events!!)
        locationManager.registerGnssNavigationMessageCallback(listener!!)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onCancel(arguments: Any?) {
        Log.d(TAG, "onCancel")
        listener?.let { locationManager.unregisterGnssNavigationMessageCallback(it) }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun createSensorEventListener(events: EventChannel.EventSink): GnssNavigationMessage.Callback {
        return object : GnssNavigationMessage.Callback() {
            override fun onGnssNavigationMessageReceived(event: GnssNavigationMessage) {
                Log.d(TAG, "onGnssNavigationMessageReceived: $event")

                super.onGnssNavigationMessageReceived(event)
                val resultMap = HashMap<String, Any>()
                resultMap["data"] = event.data
                resultMap["contents"] = event.describeContents()
                resultMap["messageId"] = event.messageId
                resultMap["status"] = event.status
                resultMap["subMessageId"] = event.submessageId
                resultMap["svid"] = event.svid
                resultMap["type"] = event.type

                uiThreadHandler.post { events.success(resultMap) }
            }

            override fun onStatusChanged(status: Int) {
                Log.d(TAG, "onStatusChanged: $status")
                super.onStatusChanged(status)
            }
        }
    }
}
