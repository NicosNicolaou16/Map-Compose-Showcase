package com.nicos.map_compose_showcase.map_compose

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.nicos.map_compose_showcase.map_compose.GoogleMapComposeConstants.initialLatitude
import com.nicos.map_compose_showcase.map_compose.GoogleMapComposeConstants.initialLongitude

data class GoogleMapComposeStates(
    val loading: Boolean = false,
    val locationName: String? = null,
    val markerIcon: BitmapDescriptor? = null,
    val latLng: LatLng = LatLng(initialLatitude, initialLongitude),
    val polylinesLatLng: List<LatLng> = emptyList()
)
