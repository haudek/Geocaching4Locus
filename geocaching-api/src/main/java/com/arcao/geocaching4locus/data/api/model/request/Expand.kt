package com.arcao.geocaching4locus.data.api.model.request

interface Expand<T> {
    companion object {
        const val EXPAND_FIELD_TRACKABLES = "trackables"
        const val EXPAND_FIELD_GEOCACHE_LOGS = "geocachelogs"
        const val EXPAND_FIELD_IMAGES = "images"
        const val EXPAND_FIELD_USER_WAYPOINTS = "userWaypoints"
        const val EXPAND_FIELD_SEPARATOR = ","
    }

    fun all() : T

    fun String.expand(value: Int) = if (value != 0) "$this:$value" else this
}