package dev.axiom.sdk.source.hinge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.axiom.sdk.core.hinge.HingeSensorManager
import kotlin.math.abs

/**
 * Remember and observe the device hinge angle as Compose [State].
 *
 * Registers a [Sensor.TYPE_HINGE_ANGLE] listener on composition and unregisters it on
 * disposal, so the sensor is active only while the composable is in the composition.
 *
 * Returns a static value of [HingeSensorManager.DEFAULT_ANGLE] (180f — fully open) when
 * no hinge sensor is available (non-foldable devices, emulators). Consumers receive a valid
 * [Float] and can render without any branching on their side.
 *
 * @param noiseThreshold minimum angle change in degrees required to trigger recomposition.
 *   Defaults to [HingeSensorManager.DEFAULT_NOISE_THRESHOLD] (0.3°).
 * @param fallbackAngle value returned when the hinge sensor is unavailable.
 *   Defaults to [HingeSensorManager.DEFAULT_ANGLE] (180f).
 * @return a [State] containing the current hinge angle in degrees (0f..180f).
 */
@Composable
public fun rememberHingeAngle(
    noiseThreshold: Float = HingeSensorManager.DEFAULT_NOISE_THRESHOLD,
    fallbackAngle: Float = HingeSensorManager.DEFAULT_ANGLE,
): State<Float> {
    val context = LocalContext.current
    val hingeAngle = remember { mutableFloatStateOf(fallbackAngle) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

        if (sensorManager == null || hingeSensor == null) {
            return@DisposableEffect onDispose { }
        }

        // Initialised to fallbackAngle rather than null so the noise gate is active
        // from the very first sensor reading. This means a reading that equals fallbackAngle
        // exactly won't re-trigger recomposition — which is the desired behaviour because
        // the displayed value is already correct.
        var lastEmitted = fallbackAngle

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val newAngle = event.values[0]
                if (abs(newAngle - lastEmitted) > noiseThreshold) {
                    lastEmitted = newAngle
                    hingeAngle.floatValue = newAngle
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, hingeSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return hingeAngle
}
