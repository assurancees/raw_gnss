package dev.assurancees.raw_gnss_2025

import android.location.GnssClock
import android.location.GnssMeasurement
import android.location.GnssMeasurementsEvent
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.EventChannel

class GnssMeasurementHandlerImpl(private val locationManager: LocationManager) : EventChannel.StreamHandler {

    companion object {
        private const val TAG = "GNSS_MEASURE"
    }

    private var listener: GnssMeasurementsEvent.Callback? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        Log.d(TAG, "onListen")
        listener = createSensorEventListener(events!!)
        locationManager.registerGnssMeasurementsCallback(listener!!)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onCancel(arguments: Any?) {
        Log.d(TAG, "onCancel")
        listener?.let { locationManager.unregisterGnssMeasurementsCallback(it) }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun createSensorEventListener(events: EventChannel.EventSink): GnssMeasurementsEvent.Callback {
        return object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(eventArgs: GnssMeasurementsEvent) {
                Log.d(TAG, "onGnssMeasurementsReceived: ${eventArgs.measurements.size}")

                super.onGnssMeasurementsReceived(eventArgs)
                val resultMap = HashMap<String, Any>()
                resultMap["contents"] = eventArgs.describeContents()
                resultMap["string"] = eventArgs.toString()

                val clock: GnssClock = eventArgs.clock
                val clockMap = HashMap<String, Any>()

                clockMap["contents"] = clock.describeContents()
                clockMap["biasNanos"] = clock.biasNanos
                clockMap["biasUncertaintyNanos"] = clock.biasUncertaintyNanos
                clockMap["driftNanosPerSecond"] = clock.driftNanosPerSecond
                clockMap["driftUncertaintyNanosPerSecond"] = clock.driftUncertaintyNanosPerSecond
                clockMap["fullBiasNanos"] = clock.fullBiasNanos
                clockMap["hardwareClockDiscontinuityCount"] = clock.hardwareClockDiscontinuityCount
                clockMap["leapSecond"] = clock.leapSecond
                clockMap["timeNanos"] = clock.timeNanos
                clockMap["timeUncertaintyNanos"] = clock.timeUncertaintyNanos

                resultMap["clock"] = clockMap

                val measurements: Collection<GnssMeasurement> = eventArgs.measurements
                val measurementsMapList = ArrayList<HashMap<String, Any>>()

                for (measurement in measurements) {
                    val map = HashMap<String, Any>()

                    map["contents"] = measurement.describeContents()
                    map["accumulatedDeltaRangeMeters"] = measurement.accumulatedDeltaRangeMeters
                    map["accumulatedDeltaRangeState"] = measurement.accumulatedDeltaRangeState
                    map["accumulatedDeltaRangeUncertaintyMeters"] = measurement.accumulatedDeltaRangeUncertaintyMeters
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        map["automaticGainControlLevelDb"] = measurement.automaticGainControlLevelDb
                    }
                    map["carrierFrequencyHz"] = measurement.carrierFrequencyHz
                    map["cn0DbHz"] = measurement.cn0DbHz
                    map["constellationType"] = measurement.constellationType
                    map["multipathIndicator"] = measurement.multipathIndicator
                    map["pseudorangeRateMetersPerSecond"] = measurement.pseudorangeRateMetersPerSecond
                    map["pseudorangeRateUncertaintyMetersPerSecond"] = measurement.pseudorangeRateUncertaintyMetersPerSecond
                    map["receivedSvTimeNanos"] = measurement.receivedSvTimeNanos
                    map["receivedSvTimeUncertaintyNanos"] = measurement.receivedSvTimeUncertaintyNanos
                    map["snrInDb"] = measurement.snrInDb
                    map["state"] = measurement.state
                    map["svid"] = measurement.svid
                    map["timeOffsetNanos"] = measurement.timeOffsetNanos
                    map["string"] = measurement.toString()

                    measurementsMapList.add(map)
                }

                resultMap["measurements"] = measurementsMapList

                uiThreadHandler.post { events.success(resultMap) }
            }

            override fun onStatusChanged(status: Int) {
                Log.d(TAG, "onStatusChanged: $status")
                super.onStatusChanged(status)
            }
        }
    }
}
