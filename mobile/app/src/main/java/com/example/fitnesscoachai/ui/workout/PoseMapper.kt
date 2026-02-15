package com.example.fitnesscoachai.ui.workout

import kotlin.math.max
import kotlin.math.min

object PoseMapper {

    // MediaPipe Pose indices
    private const val L_EAR = 7
    private const val R_EAR = 8
    private const val MOUTH_L = 9
    private const val MOUTH_R = 10
    private const val L_SHOULDER = 11
    private const val R_SHOULDER = 12
    private const val L_ELBOW = 13
    private const val R_ELBOW = 14
    private const val L_WRIST = 15
    private const val R_WRIST = 16
    private const val L_HIP = 23
    private const val R_HIP = 24
    private const val L_KNEE = 25
    private const val R_KNEE = 26
    private const val L_ANKLE = 27
    private const val R_ANKLE = 28
    private const val L_FOOT = 31
    private const val R_FOOT = 32

    // твой формат 0..17
    // 0 left_ear, 1 right_ear, 2 mouth(mid 9&10), 3 chest(mid 11&12),
    // 4 L_shoulder, 5 R_shoulder, 6 L_elbow, 7 R_elbow, 8 L_wrist, 9 R_wrist,
    // 10 L_hip, 11 R_hip, 12 L_knee, 13 R_knee, 14 L_ankle, 15 R_ankle,
    // 16 L_foot, 17 R_foot
    fun mapTo18(landmarks33: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): List<PosePoint> {
        fun p(i: Int): PosePoint {
            val lm = landmarks33[i]
            val v = lm.visibility().orElse(1.0f) // visibility может быть Optional
            return PosePoint(lm.x(), lm.y(), v)
        }

        fun mid(a: PosePoint, b: PosePoint): PosePoint {
            return PosePoint(
                (a.x + b.x) / 2f,
                (a.y + b.y) / 2f,
                min(a.v, b.v)
            )
        }

        val le = p(L_EAR)
        val re = p(R_EAR)
        val mouth = mid(p(MOUTH_L), p(MOUTH_R))
        val chest = mid(p(L_SHOULDER), p(R_SHOULDER))

        return listOf(
            le, re, mouth, chest,
            p(L_SHOULDER), p(R_SHOULDER),
            p(L_ELBOW), p(R_ELBOW),
            p(L_WRIST), p(R_WRIST),
            p(L_HIP), p(R_HIP),
            p(L_KNEE), p(R_KNEE),
            p(L_ANKLE), p(R_ANKLE),
            p(L_FOOT), p(R_FOOT),
        )
    }
}
