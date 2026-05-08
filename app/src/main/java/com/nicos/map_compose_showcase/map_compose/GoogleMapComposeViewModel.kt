package com.nicos.map_compose_showcase.map_compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.nicos.map_compose_showcase.R
import com.nicos.map_compose_showcase.map_compose.GoogleMapComposeConstants.ROUTE_POINTS
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

@Stable
@HiltViewModel
class GoogleMapComposeViewModel @Inject constructor(
    @ApplicationContext val context: Context
) : ViewModel() {

    var state by mutableStateOf(GoogleMapComposeStates())
        private set

    private val eventChannel = Channel<GoogleMapComposeEvents>()
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.Main) {
            state = state.copy(
                loading = true
            )
            bitmapDescriptorFromVector(vectorResId = R.drawable.ic_baseline_android_24).collect {
                state = state.copy(
                    loading = false,
                    markerIcon = it
                )
            }
        }
    }

    fun onAction(googleMapComposeAction: GoogleMapComposeAction) {
        when (googleMapComposeAction) {
            is GoogleMapComposeAction.UpdateLatLng -> updateLatLng(latLng = googleMapComposeAction.latLng)
            is GoogleMapComposeAction.UpdateLocationByLocationName -> updateLocationByLocationName(
                locationName = googleMapComposeAction.locationName
            )

            is GoogleMapComposeAction.Polyline -> polylines()
        }
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int, scale: Float = 1.5f) = flow {
        ContextCompat.getDrawable(context, vectorResId)?.run {
            val width = (intrinsicWidth * scale).toInt()
            val height = (intrinsicHeight * scale).toInt()
            DrawableCompat.setTint(this, Color.GREEN)
            setBounds(0, 0, width, height)
            val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            emit(BitmapDescriptorFactory.fromBitmap(bitmap))
        }
    }.flowOn(Dispatchers.IO)

    private fun updateLatLng(latLng: LatLng) {
        state = state.copy(
            loading = true,
        )
        viewModelScope.launch(Dispatchers.Default) {
            val locationName = getLocationName(latLng)
            viewModelScope.launch(Dispatchers.Main) {
                state = state.copy(
                    loading = false,
                    latLng = latLng,
                    locationName = locationName
                )
            }
            eventChannel.send(GoogleMapComposeEvents.CameraZoomIn(latLng))
        }
    }

    private suspend fun getLocationName(latLng: LatLng): String =
        suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(context, Locale.getDefault())

            // Android 13 (API 33) and above use a callback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val name = address?.locality ?: address?.subLocality ?: address?.adminArea
                        ?: "Unknown"
                        continuation.resume(name)
                    }
                } catch (e: Exception) {
                    continuation.resume("Address not found")
                }
            } else {
                // Older versions use the synchronous call
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    val address = addresses?.firstOrNull()
                    val name =
                        address?.locality ?: address?.subLocality ?: address?.adminArea ?: "Unknown"
                    continuation.resume(name)
                } catch (e: Exception) {
                    continuation.resume("Address not found")
                }
            }
        }

    private fun updateLocationByLocationName(locationName: String) {
        state = state.copy(
            loading = true
        )
        viewModelScope.launch(Dispatchers.Default) {
            val latLng: LatLng = getLatLngFromName(locationName = locationName)
            viewModelScope.launch(Dispatchers.Main) {
                state = state.copy(
                    latLng = latLng,
                    loading = false,
                )
            }
            eventChannel.send(GoogleMapComposeEvents.CameraZoomIn(latLng))
        }
    }

    private suspend fun getLatLngFromName(locationName: String): LatLng =
        suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    geocoder.getFromLocationName(locationName, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val latLng = if (address != null) {
                            LatLng(address.latitude, address.longitude)
                        } else {
                            LatLng(0.0, 0.0)
                        }
                        continuation.resume(latLng)
                    }
                } catch (e: Exception) {
                    continuation.resume(LatLng(0.0, 0.0))
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(locationName, 1)
                    val address = addresses?.firstOrNull()
                    val latLng = if (address != null) {
                        LatLng(address.latitude, address.longitude)
                    } else {
                        LatLng(0.0, 0.0)
                    }
                    continuation.resume(latLng)
                } catch (e: Exception) {
                    continuation.resume(LatLng(0.0, 0.0))
                }
            }
        }

    private fun polylines() {
        viewModelScope.launch(Dispatchers.IO) {
            val latLng = mutableListOf<LatLng>()
            ROUTE_POINTS.forEachIndexed { index, rootPoint ->
                if (index == 0) {
                    viewModelScope.launch(Dispatchers.Main) {
                        state = state.copy(
                            loading = true
                        )
                    }
                }
                delay(500)
                latLng.add(rootPoint)
                viewModelScope.launch(Dispatchers.Main) {
                    state = state.copy(
                        loading = false,
                        polylinesLatLng = latLng.toList(),
                    )
                }
            }
        }
    }
}