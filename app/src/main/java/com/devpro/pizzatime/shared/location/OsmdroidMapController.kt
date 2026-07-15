package com.devpro.pizzatime.shared.location

import android.annotation.SuppressLint
import android.view.MotionEvent
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

class OsmdroidMapController(
    private val mapView: MapView,
) {
    enum class MarkerSlot {
        SELECTION,
        DESTINATION,
        CURRENT_DEVICE,
        SHIPPER,
    }

    private val markers = ReplaceableMapValues<MarkerSlot, Marker> { Marker(mapView) }
    private val ownedOverlays = mutableListOf<Overlay>()
    var hasUserInteracted: Boolean = false
        private set

    init {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = MIN_ZOOM
        mapView.maxZoomLevel = MAX_ZOOM
        installGestureHandling()
    }

    fun showWorld() {
        mapView.controller.setZoom(WORLD_ZOOM)
    }

    fun center(
        coordinate: DeliveryCoordinate,
        zoom: Double = DETAIL_ZOOM,
        animate: Boolean = false,
        respectUserInteraction: Boolean = false,
    ): Boolean {
        if (respectUserInteraction && hasUserInteracted) {
            return false
        }
        val point = coordinate.toGeoPoint()
        mapView.controller.setZoom(zoom)
        if (animate) {
            mapView.controller.animateTo(point)
        } else {
            mapView.controller.setCenter(point)
        }
        return true
    }

    fun fitCoordinates(
        coordinates: Collection<DeliveryCoordinate>,
        paddingPixels: Int,
        animate: Boolean = false,
        respectUserInteraction: Boolean = true,
    ): Boolean {
        if (respectUserInteraction && hasUserInteracted) {
            return false
        }

        val points = coordinates
            .distinctBy { coordinate -> coordinate.latitude to coordinate.longitude }
            .map { coordinate -> coordinate.toGeoPoint() }
        if (points.isEmpty() || mapView.width <= 0 || mapView.height <= 0) {
            return false
        }
        if (points.size == 1) {
            return center(
                coordinate = coordinates.first(),
                animate = animate,
                respectUserInteraction = respectUserInteraction,
            )
        }

        mapView.zoomToBoundingBox(
            BoundingBox.fromGeoPoints(points),
            animate,
            paddingPixels.coerceAtLeast(0),
        )
        return true
    }

    fun replaceMarker(
        slot: MarkerSlot,
        coordinate: DeliveryCoordinate,
        title: String? = null,
    ) {
        val marker = markers.getOrCreate(slot)
        if (!mapView.overlays.contains(marker)) {
            mapView.overlays.add(marker)
        }
        marker.position = coordinate.toGeoPoint()
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        mapView.invalidate()
    }

    fun removeMarker(slot: MarkerSlot) {
        markers.remove(slot) { marker -> mapView.overlays.remove(marker) }
        mapView.invalidate()
    }

    fun addTapListener(onTap: (DeliveryCoordinate) -> Unit) {
        val overlay = MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    val coordinate = DeliveryCoordinate.from(point.latitude, point.longitude)
                        ?: return false
                    onTap(coordinate)
                    return true
                }

                override fun longPressHelper(point: GeoPoint): Boolean = false
            },
        )
        ownedOverlays += overlay
        mapView.overlays.add(overlay)
    }

    fun onResume() {
        mapView.onResume()
    }

    fun onPause() {
        mapView.onPause()
    }

    fun destroy() {
        mapView.setOnTouchListener(null)
        markers.clear { marker -> mapView.overlays.remove(marker) }
        ownedOverlays.forEach(mapView.overlays::remove)
        ownedOverlays.clear()
        mapView.onDetach()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun installGestureHandling() {
        mapView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_MOVE -> {
                    hasUserInteracted = true
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                    -> view.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun DeliveryCoordinate.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

    private companion object {
        const val WORLD_ZOOM = 3.0
        const val DETAIL_ZOOM = 15.0
        const val MIN_ZOOM = 3.0
        const val MAX_ZOOM = 20.0
    }
}

internal class ReplaceableMapValues<K, V>(
    private val valueFactory: () -> V,
) {
    private val values = mutableMapOf<K, V>()

    val size: Int
        get() = values.size

    fun getOrCreate(key: K): V = values.getOrPut(key, valueFactory)

    fun remove(key: K, onRemove: (V) -> Unit = {}) {
        values.remove(key)?.let(onRemove)
    }

    fun clear(onRemove: (V) -> Unit = {}) {
        values.values.forEach(onRemove)
        values.clear()
    }
}
