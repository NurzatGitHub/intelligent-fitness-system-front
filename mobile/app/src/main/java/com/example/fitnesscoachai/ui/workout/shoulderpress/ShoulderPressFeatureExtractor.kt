package com.example.fitnesscoachai.ui.workout.shoulderpress

import com.example.fitnesscoachai.ui.workout.shared.*
import kotlin.math.*

/**
 * Извлекает 14 фич из одного кадра — зеркало feature_utils_shoulderpress.py.
 *
 * Индексы в points (PoseMapper.mapTo18):
 *  3  chest         4  left_shoulder   5  right_shoulder
 *  6  left_elbow    7  right_elbow
 *  8  left_wrist    9  right_wrist
 * 10  left_hip     11  right_hip
 *
 * Выходной вектор (14 фич):
 *  0  left_elbow_angle
 *  1  right_elbow_angle
 *  2  elbow_diff
 *  3  left_shoulder_angle
 *  4  right_shoulder_angle
 *  5  shoulder_diff
 *  6  elbow_fwd_left       ◀ топ-2 по signal
 *  7  elbow_fwd_right      ◀ топ-1 по signal
 *  8  trunk_lean
 *  9  wrist_width_ratio
 * 10  elbow_height_diff
 * 11  elbow_fwd_avg        ◀ новая
 * 12  elbow_fwd_sign_diff  ◀ новая
 * 13  wrist_above_elbow    ◀ новая
 *
 * Для модели нужны ДВА кадра (DOWN + UP), конкатенированные → 28 фич.
 * Этим занимается ShoulderPressRepBuffer.
 */
object ShoulderPressFeatureExtractor {

    private const val EPS    = 1e-6f
    private const val MIN_VIS = 0.20f

    // Нужные индексы (seated press — колени могут не видны)
    private val NEEDED_IDS = intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11)

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x; val bay = a.y - b.y
        val bcx = c.x - b.x; val bcy = c.y - b.y
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

    private fun mid(a: PosePoint, b: PosePoint) =
        PosePoint((a.x + b.x) / 2f, (a.y + b.y) / 2f, min(a.v, b.v))

    fun extract(points: List<PosePoint>): FloatArray? {
        if (points.size < 18) return null

        // Проверяем видимость нужных точек
        if (NEEDED_IDS.any { points[it].v < MIN_VIS }) return null

        val chest  = points[3]
        val lSh    = points[4];  val rSh = points[5]
        val lEl    = points[6];  val rEl = points[7]
        val lWr    = points[8];  val rWr = points[9]
        val lHip   = points[10]; val rHip = points[11]
        val hipMid = mid(lHip, rHip)

        // 0,1 — углы локтей
        val leftElbowAngle  = angle(lSh, lEl, lWr)
        val rightElbowAngle = angle(rSh, rEl, rWr)
        if (!leftElbowAngle.isFinite() || !rightElbowAngle.isFinite()) return null

        // 2 — разница локтей
        val elbowDiff = abs(leftElbowAngle - rightElbowAngle)

        // 3,4 — углы отведения плеча (hipMid -> shoulder -> elbow)
        val leftShoulderAngle  = angle(hipMid, lSh, lEl)
        val rightShoulderAngle = angle(hipMid, rSh, rEl)
        if (!leftShoulderAngle.isFinite() || !rightShoulderAngle.isFinite()) return null

        // 5 — разница плеч
        val shoulderDiff = abs(leftShoulderAngle - rightShoulderAngle)

        // 6,7 — forward offset локтей (X-смещение локтя от плеча)
        // correct: lEl уходит влево (+), rEl вправо (-)
        val elbowFwdLeft  = lEl.x - lSh.x
        val elbowFwdRight = rEl.x - rSh.x

        // 8 — наклон корпуса к вертикали
        val chestToHipX = hipMid.x - chest.x
        val chestToHipY = hipMid.y - chest.y
        val len = sqrt(chestToHipX * chestToHipX + chestToHipY * chestToHipY) + EPS
        // вертикаль = (0, 1), dot = chestToHipY
        val trunkLean = Math.toDegrees(
            acos((chestToHipY / len).coerceIn(-1f, 1f).toDouble())
        ).toFloat()

        // 9 — wrist_width_ratio
        val shoulderWidth    = dist(lSh, rSh) + EPS
        val wristWidth       = dist(lWr, rWr)
        val wristWidthRatio  = wristWidth / shoulderWidth

        // 10 — разница высот локтей (Y растёт вниз)
        val elbowHeightDiff = abs(lEl.y - rEl.y)

        // 11 — elbow_fwd_avg
        val elbowFwdAvg = (abs(elbowFwdLeft) + abs(elbowFwdRight)) / 2f

        // 12 — elbow_fwd_sign_diff
        // correct: L>0, R<0 → большое положительное значение (~0.12)
        // incorrect: оба ~0 → около нуля
        val elbowFwdSignDiff = elbowFwdLeft - elbowFwdRight

        // 13 — wrist_above_elbow
        // UP: запястья выше локтей → Y_wrist < Y_elbow → отрицательное
        // DOWN: запястья у плеч или ниже → ~0 или положительное
        val wristAboveElbow = ((lWr.y - lEl.y) + (rWr.y - rEl.y)) / 2f

        return floatArrayOf(
            leftElbowAngle,     // 0
            rightElbowAngle,    // 1
            elbowDiff,          // 2
            leftShoulderAngle,  // 3
            rightShoulderAngle, // 4
            shoulderDiff,       // 5
            elbowFwdLeft,       // 6
            elbowFwdRight,      // 7
            trunkLean,          // 8
            wristWidthRatio,    // 9
            elbowHeightDiff,    // 10
            elbowFwdAvg,        // 11
            elbowFwdSignDiff,   // 12
            wristAboveElbow     // 13
        )
    }

    /** min(left_elbow, right_elbow) — для определения фазы DOWN/UP */
    fun minElbow(features: FloatArray) = min(features[0], features[1])
}
