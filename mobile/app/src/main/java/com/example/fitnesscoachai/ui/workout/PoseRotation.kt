package com.example.fitnesscoachai.ui.workout

object PoseRotation {

    /** rotate normalized (x,y) by camera rotationDegrees (0/90/180/270) */
    fun rotate(points: List<PosePoint>, rotationDegrees: Int): List<PosePoint> {
        val rot = ((rotationDegrees % 360) + 360) % 360
        return points.map { p ->
            val (x, y) = when (rot) {
                0 -> p.x to p.y
                90 -> p.y to (1f - p.x)
                180 -> (1f - p.x) to (1f - p.y)
                270 -> (1f - p.y) to p.x
                else -> p.x to p.y
            }
            PosePoint(x, y, p.v)
        }
    }

    fun mirrorX(points: List<PosePoint>): List<PosePoint> {
        return points.map { p -> PosePoint(1f - p.x, p.y, p.v) }
    }
}
