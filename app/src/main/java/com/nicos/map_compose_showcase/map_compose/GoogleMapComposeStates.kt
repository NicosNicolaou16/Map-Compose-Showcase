package com.nicos.map_compose_showcase.map_compose

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.nicos.map_compose_showcase.utils.GoogleMapComposeConstants.INITIAL_LATITUDE
import com.nicos.map_compose_showcase.utils.GoogleMapComposeConstants.INITIAL_LONGITUDE

data class GoogleMapComposeStates(
    val loading: Boolean = false,
    val locationName: String? = null,
    val markerIcon: BitmapDescriptor? = null,
    val latLng: LatLng = LatLng(INITIAL_LATITUDE, INITIAL_LONGITUDE),
    val polylinesLatLng: List<LatLng> = emptyList()
)
