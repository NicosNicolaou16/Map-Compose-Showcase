@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nicos.map_compose_showcase.map_compose

import android.util.Log
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.ktx.BuildConfig
import com.nick.samplecomposeandhilt.utils.ObserveAsEvents.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun GoogleMapComposeRoot() {
    Scaffold { paddingValues ->
        GoogleMapComposeView(paddingValues = paddingValues)
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GoogleMapComposeView(
    paddingValues: PaddingValues,
    viewModel: GoogleMapComposeViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val position: LatLng = state.latLng
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, zoomLevel)
    }
    val markerState = remember { MarkerState(position = position) }
    val mapLoaded = remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }
    val styleSpan = StyleSpan(
        StrokeStyle.gradientBuilder(
            Color.Red.toArgb(),
            Color.Green.toArgb(),
        ).build(),
    )
    val styleSpanList = remember { listOf(styleSpan) }
    // Properties
    var mapProperties by remember {
        mutableStateOf(
            MapProperties(
                maxZoomPreference = 19f, minZoomPreference = 5f,
                mapType = MapType.NORMAL
            )
        )
    }
    // Ui Settings
    var mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(mapToolbarEnabled = false)
        )
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is GoogleMapComposeEvents.CameraZoomIn -> {
                scope.launch {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(event.latLng, zoomLevel),
                        durationMs = animationZoomIn
                    )
                }
            }

            GoogleMapComposeEvents.None -> Unit
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            uiSettings = mapUiSettings,
            properties = mapProperties,
            mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
            cameraPositionState = cameraPositionState, // initial Camera Position State
            onMapClick = { latLng ->
                if (BuildConfig.DEBUG) {
                    Log.d("latLng", "${latLng.latitude}, ${latLng.longitude}")
                }
                // Updated Camera Position State
                viewModel.onAction(
                    GoogleMapComposeAction.UpdateLatLng(
                        latLng
                    )
                )
            },
            onMapLoaded = {
                mapLoaded.value = true
            }
        ) {
            MapEffect(key1 = position) {
                markerState.position = position
                locationName = state.locationName ?: ""
            }

            if (mapLoaded.value) {
                /*val pinConfig = PinConfig.builder()
                .setBackgroundColor(Color.GREEN)
                .build()*/

                AdvancedMarker(
                    state = markerState,
                    title = "$locationName ${markerState.position.latitude} ${markerState.position.longitude}",
                    //pinConfig = pinConfig,
                    icon = state.markerIcon
                )
            }
            if (state.polylinesLatLng.size >= 2) {
                Polyline(
                    points = state.polylinesLatLng,
                    spans = styleSpanList,
                )
            }
        }
        val textFieldState = rememberTextFieldState()
        val keyboardController = LocalSoftwareKeyboardController.current
        //https://developer.android.com/develop/ui/compose/text/migrate-state-based#conforming-approach
        //Real time update
        /*LaunchedEffect(textFieldState) {
            snapshotFlow { textFieldState.text.toString() }.collectLatest {
                viewModel.updateLocationByLocationName(it)
            }
        }*/

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp)
                .background(
                    color = Color.Transparent,
                ),
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Blue,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.Blue,
            ),
            state = textFieldState,
            label = { Text(stringResource(R.string.search_country_town_village_etc)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = KeyboardActionHandler {
                keyboardController?.hide()
                viewModel.onAction(
                    GoogleMapComposeAction.UpdateLocationByLocationName(
                        textFieldState.text.toString()
                    )
                )
            }
        )
        if (state.loading) {
            LoadingIndicatorInfinity(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Button(
            onClick = {
                viewModel.onAction(GoogleMapComposeAction.Polyline)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 65.dp),
        ) {
            Text(stringResource(R.string.polylines))
        }
    }
}

@Composable
fun LoadingIndicatorInfinity(
    modifier: Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000)
            ) { value, _ ->
                progress = value
            }
        }
    }

    LoadingIndicator(
        progress = { progress },
        polygons = LoadingIndicatorDefaults.DeterminateIndicatorPolygons,
        modifier = modifier
    )
}