package com.boolder.boolder.view.map

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.boolder.boolder.domain.model.BoolderMapConfig
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.StyleContract.StyleExtension
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar

//TODO Document this
class BoolderMap @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MapView(context, attrs, defStyle) {

    companion object {
        private const val TAG = "Boolder Map"
    }

    interface BoolderClickListener {
        fun onProblemSelected(problemId: Int)
        fun onPoisSelected(poisId: String, stringProperty: String, geometry: Geometry?)
    }

    private var listener: BoolderClickListener? = null

    private var previousSelectedFeatureId: String? = null
    private var shouldClick = false

    init {
        init()
    }

    fun setup(listener: BoolderClickListener, buildStyle: StyleExtension) {
        this.listener = listener
        getMapboxMap().loadStyle(buildStyle)
    }

    private fun init() {
        val cameraOptions = CameraOptions.Builder()
//            .center(Point.fromLngLat(2.5968216, 48.3925623))
            .center(Point.fromLngLat(2.605752646923065, 48.408801229423915)) // TODO to be removed
//            .zoom(10.2)
            .zoom(25.0)
            .build()

        getMapboxMap().apply {
            setCamera(cameraOptions)
        }

        gestures.pitchEnabled = false
        scalebar.enabled = false

        addClickEvent()
    }

    @SuppressLint("ClickableViewAccessibility")
    // 1. Catch a tap on screen
    private fun addClickEvent() {
        setOnTouchListener { _, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    shouldClick = true
                }
                MotionEvent.ACTION_UP -> {
                    if (shouldClick) {
                        installRenderedFeatures(event.x.toDouble(), event.y.toDouble())
                    }
                }
            }
            false
        }
    }

    // 2. Use x and y to determine whether or not it is relevant
    private fun installRenderedFeatures(x: Double, y: Double) {
        val geometry = RenderedQueryGeometry(
            ScreenCoordinate(x, y)
        )
        getMapboxMap().apply {
            queryAreaRenderedFeatures(geometry)
            queryClusterRenderedFeatures(geometry)
            queryPoisRenderedFeatures(geometry)
            queryProblemRenderedFeatures(geometry)
        }
    }

    private fun queryAreaRenderedFeatures(geometry: RenderedQueryGeometry) {
        val areaOption = RenderedQueryOptions(
            listOf("areas", "areas-hulls"),
            Expression.lt(
                Expression.zoom(),
                Expression.literal(15.0)
            )
        )
        getMapboxMap().queryRenderedFeatures(
            geometry,
            areaOption
        ) { features: Expected<String, MutableList<QueriedFeature>> ->
            buildCoordinateBounds(features)
        }
    }

    private fun queryClusterRenderedFeatures(geometry: RenderedQueryGeometry) {
        val clusterOption = RenderedQueryOptions(listOf("cluster"), null)

        getMapboxMap().queryRenderedFeatures(
            geometry,
            clusterOption
        ) { features: Expected<String, MutableList<QueriedFeature>> ->
            buildCoordinateBounds(features)
        }
    }

    private fun queryProblemRenderedFeatures(geometry: RenderedQueryGeometry) {

        val problemGeometry = RenderedQueryGeometry(
            ScreenBox(
                ScreenCoordinate(
                    geometry.screenCoordinate.x - 12.0, geometry.screenCoordinate.y - 12.0
                ),
                ScreenCoordinate(
                    geometry.screenCoordinate.x + 12.0,
                    geometry.screenCoordinate.y + 12.0
                )
            )
        )

        val problemsOption = RenderedQueryOptions(listOf("problems"), null)

        getMapboxMap().queryRenderedFeatures(
            problemGeometry,
            problemsOption
        ) { features: Expected<String, MutableList<QueriedFeature>> ->
            if (features.isValue) {

                val feature = features.value?.firstOrNull()?.feature ?: return@queryRenderedFeatures

                if (feature.hasProperty("id") && feature.geometry() != null) {
                    selectProblem(feature.getNumberProperty("id").toString())
                    listener?.onProblemSelected(feature.getNumberProperty("id").toInt())

                    // Move camera is problem is hidden by bottomSheet
                    if (geometry.screenCoordinate.y >= height / 2) {

                        val cameraOption = CameraOptions.Builder()
                            .center(feature.geometry() as Point)
                            .padding(EdgeInsets(40.0, 8.8, (height / 2).toDouble(), 8.8))
                            .build()
                        val mapAnimationOption = MapAnimationOptions.Builder()
                            .duration(500L)
                            .build()

                        getMapboxMap().easeTo(cameraOption, mapAnimationOption)
                    }

                } else {
                    unselectProblem()
                }

            } else {
                Log.w(TAG, features.error ?: "No message")
            }
        }
    }

    private fun queryPoisRenderedFeatures(
        geometry: RenderedQueryGeometry,
    ) {
        val poisOption = RenderedQueryOptions(listOf("pois"), null)
        getMapboxMap().queryRenderedFeatures(
            geometry,
            poisOption
        ) { features: Expected<String, MutableList<QueriedFeature>> ->
            if (features.isValue) {
                features.value?.firstOrNull()?.feature?.let {
                    if (it.hasProperty("name") &&
                        it.hasProperty("googleUrl")
                    ) {
                        listener?.onPoisSelected(
                            it.getStringProperty("name"),
                            it.getStringProperty("googleUrl"),
                            it.geometry()
                        )
                    }
                }
            } else {
                Log.w(TAG, features.error ?: "No message")
            }
        }
    }

    fun selectProblem(featureId: String) {
        getMapboxMap().setFeatureState(
            "problems",
            BoolderMapConfig.problemsSourceLayerId,
            featureId,
            Value.fromJson("""{"selected": true}""").value!!
        )

        if (featureId != previousSelectedFeatureId) {
            unselectProblem()
        }
        previousSelectedFeatureId = featureId
    }

    private fun unselectProblem() {
        previousSelectedFeatureId?.let {
            getMapboxMap().setFeatureState(
                "problems",
                BoolderMapConfig.problemsSourceLayerId,
                it,
                Value.fromJson("""{"selected": false}""").value!!
            )
        }
    }

    // 3A. Build bounds around coordinate
    private fun buildCoordinateBounds(
        features: Expected<String, MutableList<QueriedFeature>>
    ) {
        if (features.isValue) {
            features.value?.firstOrNull()?.feature?.let {
                if (it.hasProperty("southWestLon") &&
                    it.hasProperty("southWestLat") &&
                    it.hasProperty("northEastLon") &&
                    it.hasProperty("northEastLat")
                ) {
                    val southWest = Point.fromLngLat(
                        it.getStringProperty("southWestLon").toDouble(),
                        it.getStringProperty("southWestLat").toDouble()
                    )
                    val northEst = Point.fromLngLat(
                        it.getStringProperty("northEastLon").toDouble(),
                        it.getStringProperty("northEastLat").toDouble()
                    )
                    val coordinateBound = CoordinateBounds(southWest, northEst)
                    moveCamera(coordinateBound)
                }
            } ?: unselectProblem()
        } else {
            Log.w(TAG, features.error ?: "No message")
        }
    }

    // Triggered when user click on a Area or Cluster on Map
    private fun moveCamera(coordinates: CoordinateBounds) {
        val cameraOption = CameraOptions.Builder()
            .center(coordinates.center())
            .bearing(0.0)
            .padding(EdgeInsets(60.0, 8.0, 8.0, 8.0))
            .pitch(0.0)
            .build()
        val mapAnimationOption = MapAnimationOptions.Builder()
            .duration(500L)
            .build()

        getMapboxMap().flyTo(cameraOption, mapAnimationOption)
    }
}