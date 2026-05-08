package com.nicos.map_compose_showcase.map_compose

import com.google.android.gms.maps.model.LatLng

sealed interface GoogleMapComposeEvents {
    data object None : GoogleMapComposeEvents
    data class CameraZoomIn(val latLng: LatLng) : GoogleMapComposeEvents
}