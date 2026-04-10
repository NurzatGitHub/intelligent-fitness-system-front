package com.example.fitnesscoachai.ui.workout.shoulderpress

import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import kotlin.math.*

/**
 * Проверяет, готов ли пользователь к shoulder press (seated).
 *
 * Критерии готовности:
 *  - Плечи, локти, запястья видны с достаточной видимостью
 *  - Корпус виден (ширина плеч достаточна)
 *  - Запястья находятся примерно на уровне плеч или ниже (START позиция)
 *  - Локти согнуты (руки не выпрямлены полностью) — человек держит вес у плеч
 *  - Корпус относительно вертикальный (seated, не наклонён сильно)
 */
class ShoulderPressReadyCheck {

    private val VIS_TH  = 0.15f
    private val MIN_GOOD = 6

    // Индексы нужных точек
    private val NEEDED_IDS = intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11)

    // Геометрические пороги
    private val MIN_SHOULDER_WIDTH  = 0.06f   // плечи не схлопнулись
    private val MAX_TRUNK_LEAN_DEG  = 40f     // корпус не сильно наклонён
    private val MIN_ELBOW_ANGLE     = 50f     // локти согнуты (не выпрямлены)
    private val MAX_ELBOW_ANGLE     = 150f    // но не полностью прямые (старт = DOWN)
    private val MAX_WRIST_ABOVE_SH  = -0.05f  // запястья НЕ слишком высоко над плечами

    private val smoothBuf = ArrayDeque<List<PosePoint>>(5)

    fun reset() { smoothBuf.clear() }

    data class ReadyResult(
        val isReady: Boolean,
        val hint: String
    )

    fun check(points: List<PosePoint>): ReadyResult {
        if (points.size < 18) return notReady("Show full upper body")

        val good = NEEDED_IDS.count { i -> points[i].v >= VIS_TH }
        if (good < MIN_GOOD) return notReady("Show shoulders, arms and torso")

        // Сглаживание
        if (smoothBuf.size >= 5) smoothBuf.removeFirst()
        smoothBuf.addLast(points)

        val p: List<PosePoint> = if (smoothBuf.size >= 3) {
            List(18) { idx ->
                PosePoint(
                    smoothBuf.map { it[idx].x }.average().toFloat(),
                    smoothBuf.map { it[idx].y }.average().toFloat(),
                    smoothBuf.map { it[idx].v }.average().toFloat()
                )
            }
        } else points

        return geometryCheck(p)
    }

    private fun geometryCheck(p: List<PosePoint>): ReadyResult {
        val chest = p[3]
        val lSh = p[4];  val rSh = p[5]
        val lEl = p[6];  val rEl = p[7]
        val lWr = p[8];  val rWr = p[9]
        val lHip = p[10]; val rHip = p[11]

        val hipMid = mid(lHip, rHip)
        val shoulderWidth = dist(lSh, rSh)

        // 1. Ширина плеч — корпус виден
        val torsoVisible = shoulderWidth > MIN_SHOULDER_WIDTH

        // 2. Наклон корпуса
        val chestToHipX = hipMid.x - chest.x
        val chestToHipY = hipMid.y - chest.y
        val len = sqrt(chestToHipX * chestToHipX + chestToHipY * chestToHipY) + 1e-6f
        val trunkLean = Math.toDegrees(
            acos((chestToHipY / len).coerceIn(-1f, 1f).toDouble())
        ).toFloat()
        val torsoUpright = trunkLean < MAX_TRUNK_LEAN_DEG

        // 3. Углы локтей
        val leftElbow  = angle3(lSh, lEl, lWr)
        val rightElbow = angle3(rSh, rEl, rWr)
        val elbowsInRange = leftElbow.isFinite() && rightElbow.isFinite() &&
                leftElbow  in MIN_ELBOW_ANGLE..MAX_ELBOW_ANGLE &&
                rightElbow in MIN_ELBOW_ANGLE..MAX_ELBOW_ANGLE

        // 4. Запястья не высоко над плечами (не в UP позиции при старте)
        val wristAvgY  = (lWr.y + rWr.y) / 2f
        val shoulderAvgY = (lSh.y + rSh.y) / 2f
        // Y растёт вниз: если запястья выше плеч, wristAvgY < shoulderAvgY
        val wristsNotTooHigh = (wristAvgY - shoulderAvgY) > MAX_WRIST_ABOVE_SH

        val isReady = torsoVisible && torsoUpright && elbowsInRange && wristsNotTooHigh

        val hint = when {
            isReady           -> ""
            !torsoVisible     -> "Move back — show full torso"
            !torsoUpright     -> "Sit upright"
            !elbowsInRange    -> "Hold weights at shoulder level, elbows bent"
            !wristsNotTooHigh -> "Lower the weights to shoulder level"
            else              -> "Hold weights at shoulders to start"
        }

        return ReadyResult(isReady, hint)
    }

    private fun notReady(hint: String) = ReadyResult(false, hint)

    private fun mid(a: PosePoint, b: PosePoint) = PosePoint(
        (a.x + b.x) / 2f, (a.y + b.y) / 2f, min(a.v, b.v)
    )

    private fun dist(a: PosePoint, b: PosePoint): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun angle3(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x; val bay = a.y - b.y
        val bcx = c.x - b.x; val bcy = c.y - b.y
        val na = sqrt(bax * bax + bay * bay)
        val nc = sqrt(bcx * bcx + bcy * bcy)
        if (na == 0f || nc == 0f) return Float.NaN
        return Math.toDegrees(
            acos(((bax * bcx + bay * bcy) / (na * nc)).coerceIn(-1f, 1f).toDouble())
        ).toFloat()
    }
}
