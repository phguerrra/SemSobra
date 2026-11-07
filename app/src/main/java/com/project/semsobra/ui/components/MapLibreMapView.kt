package com.project.semsobra.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.annotations.MarkerOptions

@Suppress("DEPRECATION")
@Composable
fun MapLibreMapView(
    latitude: Double,
    longitude: Double,
    address: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { view ->
        view.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { _ ->
                val latLng = LatLng(latitude, longitude)
                map.cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(14.0)
                    .build()

                map.markers.forEach { map.removeMarker(it) }
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(address ?: "Local do Evento")
                )
            }
        }
    }
}
