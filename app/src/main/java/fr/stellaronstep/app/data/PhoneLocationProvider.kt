package fr.stellaronstep.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PhoneLocationProvider(
    private val context: Context
) {
    fun hasLocationPermission(): Boolean {
        val fine =
            context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        val manager =
            context.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        val enabledProviders =
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            ).filter { provider ->
                runCatching {
                    manager.isProviderEnabled(provider)
                }.getOrDefault(false)
            }

        val lastKnown =
            enabledProviders
                .mapNotNull { provider ->
                    runCatching {
                        manager.getLastKnownLocation(provider)
                    }.getOrNull()
                }
                .maxByOrNull { it.time }

        if (enabledProviders.isEmpty()) {
            return lastKnown
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return lastKnown
        }

        val preferredProvider =
            when {
                enabledProviders.contains(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER

                enabledProviders.contains(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER

                else -> null
            }

        if (preferredProvider == null) {
            return lastKnown
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal =
                CancellationSignal()

            continuation.invokeOnCancellation {
                cancellationSignal.cancel()
            }

            manager.getCurrentLocation(
                preferredProvider,
                cancellationSignal,
                context.mainExecutor
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(
                        location ?: lastKnown
                    )
                }
            }
        }
    }
}