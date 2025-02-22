package com.boolder.boolder.utils

import com.boolder.boolder.domain.model.BoolderMapConfig
import com.boolder.boolder.domain.model.CircuitColor
import com.mapbox.maps.extension.style.StyleContract.StyleExtension
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.types.PromoteId

class MapboxStyleFactory {

    fun buildStyle(): StyleExtension {
        return style(BoolderMapConfig.styleUri) {
            +vectorSource(LAYER_PROBLEMS) {
                url(BoolderMapConfig.vectorSourceUrl)
                promoteId(PromoteId("id"))
            }
            +circleLayer(LAYER_PROBLEMS, "problems") {
                sourceLayer(BoolderMapConfig.problemsSourceLayerId)
                minZoom(15.0)
                filter(Expression.match {
                    geometryType()
                    get("Point")
                    literal(true)
                    literal(false)
                })

                circleRadius(
                    interpolate {
                        linear()
                        zoom()
                        literal(15.0)
                        literal(2.0)
                        literal(18.0)
                        literal(4.0)
                        literal(22.0)
                        switchCase {
                            boolean {
                                has("circuitNumber")
                                literal(false)
                            }
                            literal(16.0)
                            literal(10.0)
                        }
                    }
                )

                circleColor(
                    match {
                        get { literal("circuitColor") }
                        literal("yellow")
                        CircuitColor.YELLOW.rgb(this)
                        literal("purple")
                        CircuitColor.PURPLE.rgb(this)
                        literal("orange")
                        CircuitColor.ORANGE.rgb(this)
                        literal("green")
                        CircuitColor.GREEN.rgb(this)
                        literal("blue")
                        CircuitColor.BLUE.rgb(this)
                        literal("skyblue")
                        CircuitColor.SKYBLUE.rgb(this)
                        literal("salmon")
                        CircuitColor.SALMON.rgb(this)
                        literal("red")
                        CircuitColor.RED.rgb(this)
                        literal("black")
                        CircuitColor.BLACK.rgb(this)
                        literal("white")
                        CircuitColor.WHITE.rgb(this)
                        CircuitColor.OFF_CIRCUIT.rgb(this)
                    }
                )

                circleStrokeWidth(
                    Expression.switchCase {
                        boolean {
                            featureState { literal("selected") }
                            literal(false)
                        }
                        literal(3.0)
                        literal(0.0)
                    }
                )

                circleStrokeColor(rgb(101.0, 196.0, 102.0))
                circleSortKey(
                    switchCase {
                        boolean {
                            has { literal("circuitId") }
                            literal(false)
                        }
                        literal(2)
                        literal(1)
                    }
                )
            }
            +symbolLayer(LAYER_PROBLEMS_TEXT, "problems") {
                sourceLayer(BoolderMapConfig.problemsSourceLayerId)
                minZoom(19.0)
                filter(Expression.match {
                    geometryType()
                    get("Point")
                    literal(true)
                    literal(false)
                })
                textAllowOverlap(true)
                textField(
                    get("circuitNumber")
                )
                textSize(interpolate {
                    linear()
                    zoom()
                    literal(19)
                    literal(10)
                    literal(22)
                    literal(20)
                })
                textColor(switchCase {
                    match {
                        get("circuitColor")
                        literal("white")
                        literal(true)
                        literal(false)
                    }
                    rgb(0.0, 0.0, 0.0)
                    rgb(255.0, 255.0, 255.0)

                })
            }
        }
    }

    companion object {
        const val LAYER_PROBLEMS = "problems"
        const val LAYER_PROBLEMS_TEXT = "problems-text"
    }
}