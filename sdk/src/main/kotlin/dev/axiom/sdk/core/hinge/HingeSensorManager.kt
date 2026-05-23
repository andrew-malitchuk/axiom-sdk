package dev.axiom.sdk.core.hinge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * Bridges [Sensor.TYPE_HINGE_ANGLE] into a [Flow]-based reactive stream.
 *
 * This is the raw sensor adapter. It is intentionally `internal` because it exposes
 * Android's callback-based sensor API directly — callers must manage the coroutine
 * scope themselves. The public surface uses [rememberHingeAngle][dev.axiom.sdk.source.hinge.rememberHingeAngle],
 * which ties the sensor lifecycle to the Compose composition via [androidx.compose.runtime.DisposableEffect].
 *
 * [Sensor.TYPE_HINGE_ANGLE] is available from API 30. On older devices or non-foldable
 * hardware [getDefaultSensor] returns null and the flow completes immediately without
 * emitting any values.
 *
 * @param context used only to obtain [SensorManager]; an application context is sufficient.
 */
internal class HingeSensorManager(private val context: Context) {

    /**
     * Emits hinge angle readings (0f..180f) as a cold [Flow].
     *
     * Uses [callbackFlow] to safely wrap [SensorEventListener] in a coroutine channel.
     * The flow is cold — the sensor is registered only when collection starts and
     * unregistered via [awaitClose] when the collector cancels, preventing sensor leaks.
     *
     * A noise gate of [noiseThreshold] degrees filters out minor sensor jitter.
     * Without this gate, even a device sitting still on a table would produce a
     * continuous stream of sub-degree fluctuations, triggering unnecessary recompositions.
     *
     * [trySend] is used instead of [send][kotlinx.coroutines.channels.Channel.send]
     * because sensor events can arrive faster than the collector processes them.
     * Dropping a reading under backpressure is acceptable — the next reading will
     * carry the up-to-date angle anyway.
     *
     * @param sensorDelay sensor sampling rate; defaults to [SensorManager.SENSOR_DELAY_UI]
     *   (~60 Hz), which balances responsiveness and CPU load.
     * @param noiseThreshold minimum angle change in degrees to emit a new value;
     *   defaults to [DEFAULT_NOISE_THRESHOLD].
     * @return cold [Flow] of hinge angle degrees; completes immediately on devices
     *   without [Sensor.TYPE_HINGE_ANGLE].
     */
    fun observeHingeAngle(
        sensorDelay: Int = SensorManager.SENSOR_DELAY_UI,
        noiseThreshold: Float = DEFAULT_NOISE_THRESHOLD,
    ): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

        if (sensorManager == null || hingeSensor == null) {
            close()
            return@callbackFlow
        }

        // Null initial value intentionally forces the very first sensor reading to always
        // be emitted regardless of the noise threshold. Without this, a device whose first
        // reading happens to be exactly the fallback value (180f) would never fire, leaving
        // the UI stuck on the initial state even when the sensor is working correctly.
        var lastEmittedAngle: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val newAngle = event.values[0]
                val last = lastEmittedAngle
                if (last == null || abs(newAngle - last) > noiseThreshold) {
                    lastEmittedAngle = newAngle
                    trySend(newAngle)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, hingeSensor, sensorDelay)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    companion object {
        /**
         * Default minimum angle delta (degrees) required to emit a new value.
         *
         * 0.3° was chosen empirically: it suppresses the sub-degree thermal/electronic
         * noise observed on Pixel Fold and Galaxy Z Fold hardware while still tracking
         * deliberate movement with negligible lag.
         */
        const val DEFAULT_NOISE_THRESHOLD: Float = 0.3f

        /**
         * Angle used when no hinge sensor is present (180° = fully flat / non-foldable).
         *
         * 180f is the "safe" default because it represents a fully open device — UI
         * layouts designed around [AdaptiveLayoutSwitcher][dev.axiom.sdk.source.window.AdaptiveLayoutSwitcher]
         * will fall through to the `compact` slot on non-foldable phones, which is
         * the expected behaviour.
         */
        const val DEFAULT_ANGLE: Float = 180f
    }
}
