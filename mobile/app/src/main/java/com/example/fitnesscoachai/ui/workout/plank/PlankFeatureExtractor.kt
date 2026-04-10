package com.example.fitnesscoachai.ui.workout.plank

import com.example.fitnesscoachai.ui.workout.shared.*
import kotlin.math.*

/**
 * Extracts 7 features for the plank ONNX model.
 *
 * Feature layout (matches Scaler offsets in the model):
 *   [0] bodyAngle      – angle(shoulder, hip, knee)         mean≈144°  (straight body line)
 *   [1] hipOffset      – hipMid.y – avg(shoulder.y, knee.y) mean≈0     (hip sag/rise)
 *   [2] leftBodyAngle  – angle(lSh, lHip, lKnee)            mean≈140°
 *   [3] rightBodyAngle – angle(rSh, rHip, rKnee)            mean≈142°
 *   [4] leftKnee       – angle(lHip, lKnee, lAnkle)         mean≈30°   (knee bend)
 *   [5] rightKnee      – angle(rHip, rKnee, rAnkle)         mean≈52°
 *   [6] shoulderOffset – shMid.y – avg(hip.y, elbow.y)      mean≈0     (shoulder sag)
 *
 * Side-view pose is expected (camera on the side of the user).
 * Point mapping follows PoseMapper.mapTo18:
 *   3=chest  4=lSh 5=rSh  6=lEl 7=rEl  8=lWr 9=rWr
 *   10=lHip 11=rHip  12=lKnee 13=rKnee  14=lAnkle 15=rAnkle
 */
object PlankFeatureExtractor {

    private const val EPS = 1e-6f
    private const val MIN_VIS = 0.20f

    private fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val bax = a.x - b.x; val bay = a.y - b.y
        val bcx = c.x - b.x; val bcy = c.y - b.y
        val magBA = sqrt(bax * bax + bay * bay)
        val magBC = sqrt(bcx * bcx + bcy * bcy)
        if (magBA < EPS || magBC < EPS) return Float.NaN
        val cos = (bax * bcx + bay * bcy) / (magBA * magBC)
        return Math.toDegrees(acos(cos.coerceIn(-1f, 1f)).toDouble()).toFloat()
    }

    private fun mid(a: PosePoint, b: PosePoint) = PosePoint(
        (a.x + b.x) / 2f,
        (a.y + b.y) / 2f,
        min(a.v, b.v)
    )

    fun extract(points: List<PosePoint>): FloatArray? {
        if (points.size < 18) return null

        val avgV = points.map { it.v }.average().toFloat()
        if (avgV < MIN_VIS) return null

        val lSh    = points[4];  val rSh    = points[5]
        val lEl    = points[6];  val rEl    = points[7]
        val lHip   = points[10]; val rHip   = points[11]
        val lKnee  = points[12]; val rKnee  = points[13]
        val lAnkle = points[14]; val rAnkle = points[15]

        val shMid   = mid(lSh,   rSh)
        val hipMid  = mid(lHip,  rHip)
        val kneeMid = mid(lKnee, rKnee)
        val elMid   = mid(lEl,   rEl)

        // [0] overall body line through midpoints
        val bodyAngle = angle(shMid, hipMid, kneeMid)
        if (!bodyAngle.isFinite()) return null

        // [1] hip vertical offset from the shoulder–knee midline
        val hipOffset = hipMid.y - ((shMid.y + kneeMid.y) / 2f)

        // [2] left side body angle
        val leftBodyAngle = angle(lSh, lHip, lKnee)
        if (!leftBodyAngle.isFinite()) return null

        // [3] right side body angle
        val rightBodyAngle = angle(rSh, rHip, rKnee)
        if (!rightBodyAngle.isFinite()) return null

        // [4] left knee angle
        val leftKnee = angle(lHip, lKnee, lAnkle)
        if (!leftKnee.isFinite()) return null

        // [5] right knee angle
        val rightKnee = angle(rHip, rKnee, rAnkle)
        if (!rightKnee.isFinite()) return null

        // [6] shoulder vertical offset from hip–elbow midline
        val shoulderOffset = shMid.y - ((hipMid.y + elMid.y) / 2f)

        return floatArrayOf(
            bodyAngle,
            hipOffset,
            leftBodyAngle,
            rightBodyAngle,
            leftKnee,
            rightKnee,
            shoulderOffset
        )
    }
}
