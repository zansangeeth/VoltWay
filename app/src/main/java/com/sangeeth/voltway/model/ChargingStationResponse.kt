package com.sangeeth.voltway.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChargingStationResponse(
    val type: String,
    val features: List<Feature>
)

@JsonClass(generateAdapter = true)
data class Feature(
    val type: String,
    val geometry: Geometry,
    val properties: Properties
)

@JsonClass(generateAdapter = true)
data class Geometry(
    val type: String,
    val coordinates: List<Double>
)

@JsonClass(generateAdapter = true)
data class Properties(
    val name: String?,
    @Json(name = "full_address") val fullAddress: String?,
    val distance: Double?,
    val coordinates: MapboxCoordinates?,
    val metadata: MapboxMetadata?
)

@JsonClass(generateAdapter = true)
data class MapboxMetadata(
    val phone: String?,
    val website: String?
)

@JsonClass(generateAdapter = true)
data class MapboxCoordinates(
    val latitude: Double?,
    val longitude: Double?
)
