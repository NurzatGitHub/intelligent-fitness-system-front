package com.example.fitnesscoachai.ui.workout

data class PosePoint(val x: Float, val y: Float, val v: Float)

data class Segment(val a: Int, val b: Int, val color: String)

data class WsResponse(
    val type: String? = null,        // pong / connected / error
    val server: String? = null,      // NEW_LANDMARKS_PIPELINE (optional)
    val message: String? = null,     // error message (optional)

    val exercise: String? = null,
    val status: String? = null,      // SETUP / ACTIVE
    val hint: String? = null,
    val overall: String? = null,     // correct/incorrect
    val confidence: Float? = null,
    val rep_count: Int? = null,
    val segments: List<Segment> = emptyList()
)