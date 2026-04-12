package com.example.fitnesscoachai.ui.workout.crunch

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

object CrunchFeatureExtractor {

    private const val EPS     = 1e-6f
    private const val MIN_VIS = 0.15f   // расслаблено: съёмка сбоку, часть точек частично видна

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x;  val bay = a.y - b.y
        val bcx = c.x - b.x;  val bcy = c.y - b.y
        val magBA = sqrt(bax * bax + bay * bay)
        val magBC = sqrt(bcx * bcx + bcy * bcy)
        if (magBA < EPS || magBC < EPS) return Float.NaN
        val cos = (bax * bcx + bay * bcy) / (magBA * magBC)
        return Math.toDegrees(acos(cos.coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    private fun dist(a: PosePoint, b: PosePoint): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun mid(a: PosePoint, b: PosePoint) = PosePoint(
        (a.x + b.x) / 2f, (a.y + b.y) / 2f, min(a.v, b.v)
    )

    /**
     * Извлекает 7 признаков для crunch (съёмка сбоку).
     *
     * [0] trunk_angle      — угол chest-hip-knee   (мал = глубокий crunch, ≈80–100° в DOWN)
     * [1] neck_angle       — угол mouth-chest-hip   (мал = голова поднята; fallback=180 если рот не виден)
     * [2] hip_lift         — (knee_y - hip_y) / torso_len
     * [3] elbow_to_knee    — min(dist(elbow,knee)) / torso_len
     * [4] symmetry_diff    — |L_sh.y - R_sh.y|
     * [5] knee_angle_avg   — средний угол hip-knee-ankle
     * [6] shoulder_tilt    — угол линии плеч к горизонтали
     *
     * Обязательные точки: chest(3), hips(10,11), knees(12,13).
     * Shoulders(4,5) желательны но не обязательны — используем грудь как fallback.
     * Mouth(2) — опциональный: если не виден, neck_angle = 180f (нейтральный).
     */
    fun extract(points: List<PosePoint>): FloatArray? {
        if (points.size < 18) return null

        // ОБЯЗАТЕЛЬНЫЕ: chest, hips, knees — минимум для crunch
        val required = listOf(3, 10, 11, 12, 13)
        if (required.any { points[it].v < MIN_VIS }) return null

        val mouth   = points[2]
        val chest   = points[3]
        val lSh     = points[4]
        val rSh     = points[5]
        val lEl     = points[6]
        val rEl     = points[7]
        val lHip    = points[10]
        val rHip    = points[11]
        val lKnee   = points[12]
        val rKnee   = points[13]
        val lAnk    = points[14]
        val rAnk    = points[15]

        val hipMid   = mid(lHip,  rHip)
        val kneeMid  = mid(lKnee, rKnee)
        val shMid    = if (lSh.v >= MIN_VIS && rSh.v >= MIN_VIS) mid(lSh, rSh) else chest

        val torsoLen = dist(shMid, hipMid).coerceAtLeast(EPS)

        // 0: trunk_angle — chest-hip-knee
        val trunkAngle = angle(chest, hipMid, kneeMid)
        if (!trunkAngle.isFinite()) return null

        // 1: neck_angle — mouth-chest-hip
        // ФИКС: mouth необязателен. При съёмке сбоку рот часто не виден.
        // Если рот не виден — используем нейтральное значение 180f.
        val neckAngle = if (mouth.v >= MIN_VIS) {
            val a = angle(mouth, chest, hipMid)
            if (a.isFinite()) a else 180f
        } else {
            180f
        }

        // 2: hip_lift
        val hipLift = (kneeMid.y - hipMid.y) / torsoLen

        // 3: elbow_to_knee
        val dL = if (lEl.v >= MIN_VIS) dist(lEl, lKnee) else Float.MAX_VALUE
        val dR = if (rEl.v >= MIN_VIS) dist(rEl, rKnee) else Float.MAX_VALUE
        val minEK = minOf(dL, dR)
        val elbowToKnee = if (minEK == Float.MAX_VALUE) 1.5f else minEK / torsoLen

        // 4: symmetry_diff
        val symmetryDiff = if (lSh.v >= MIN_VIS && rSh.v >= MIN_VIS)
            abs(lSh.y - rSh.y) else 0f

        // 5: knee_angle_avg
        val kaL = if (lAnk.v >= MIN_VIS) { val a = angle(lHip, lKnee, lAnk); if (a.isFinite()) a else 90f } else 90f
        val kaR = if (rAnk.v >= MIN_VIS) { val a = angle(rHip, rKnee, rAnk); if (a.isFinite()) a else 90f } else 90f
        val kneeAngleAvg = (kaL + kaR) / 2f

        // 6: shoulder_tilt
        val shoulderTilt = if (lSh.v >= MIN_VIS && rSh.v >= MIN_VIS) {
            val shDx = abs(rSh.x - lSh.x)
            val shDy = abs(rSh.y - lSh.y)
            Math.toDegrees(atan2(shDy, shDx + EPS).toDouble()).toFloat()
        } else 0f

        return floatArrayOf(
            trunkAngle,    // 0
            neckAngle,     // 1
            hipLift,       // 2
            elbowToKnee,   // 3
            symmetryDiff,  // 4
            kneeAngleAvg,  // 5
            shoulderTilt   // 6
        )
    }
}