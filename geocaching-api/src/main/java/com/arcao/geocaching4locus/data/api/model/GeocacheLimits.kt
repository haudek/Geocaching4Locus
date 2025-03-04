package com.arcao.geocaching4locus.data.api.model

import org.threeten.bp.Duration

data class GeocacheLimits(
    val liteCallsRemaining: Int, // 0
    val liteCallsSecondsToLive: Duration?, // 0
    val fullCallsRemaining: Int, // 0
    val fullCallsSecondsToLive: Duration? // 0
)
