package com.nicos.map_compose_showcase.map_compose

import com.google.android.gms.maps.model.LatLng

sealed interface GoogleMapComposeAction {
    data class UpdateLatLng(val latLng: LatLng): GoogleMapComposeAction
    data class UpdateLocationByLocationName(val locationName: String): GoogleMapComposeAction
    data object Polyline: GoogleMapComposeAction
}