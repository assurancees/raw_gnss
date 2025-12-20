package dev.joshi.raw_gnss

import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel

/** RawGnssPlugin */
class RawGnssPlugin : FlutterPlugin {

    companion object {
        private const val GNSS_MEASUREMENT_CHANNEL_NAME = "dev.joshi.raw_gnss/gnss_measurement"
        private const val GNSS_NAVIGATION_MESSAGE_CHANNEL_NAME = "dev.joshi.raw_gnss/gnss_navigation_message"
        private const val GNSS_STATUS_CHANNEL_NAME = "dev.joshi.raw_gnss/gnss_status"
    }

    private var gnssMeasurementChannel: EventChannel? = null
    private var gnssNavigationMessageChannel: EventChannel? = null
    private var gnssStatusChannel: EventChannel? = null
    private var locationManager: LocationManager? = null
    private var context: Context? = null

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        setupEventChannels(context!!, flutterPluginBinding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        teardownEventChannels()
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun setupEventChannels(context: Context, messenger: BinaryMessenger) {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        gnssMeasurementChannel = EventChannel(messenger, GNSS_MEASUREMENT_CHANNEL_NAME)
        gnssNavigationMessageChannel = EventChannel(messenger, GNSS_NAVIGATION_MESSAGE_CHANNEL_NAME)
        gnssStatusChannel = EventChannel(messenger, GNSS_STATUS_CHANNEL_NAME)

        val gnssMeasurementStreamHandler = GnssMeasurementHandlerImpl(locationManager!!)
        gnssMeasurementChannel?.setStreamHandler(gnssMeasurementStreamHandler)

        val gnssNavigationMessageHandler = GnssNavigationMessageHandlerImpl(locationManager!!)
        gnssNavigationMessageChannel?.setStreamHandler(gnssNavigationMessageHandler)

        val gnssStatusStreamHandler = GnssStatusHandlerImpl(locationManager!!)
        gnssStatusChannel?.setStreamHandler(gnssStatusStreamHandler)
    }

    private fun teardownEventChannels() {
        gnssMeasurementChannel?.setStreamHandler(null)
        gnssNavigationMessageChannel?.setStreamHandler(null)
        gnssStatusChannel?.setStreamHandler(null)
    }
}
