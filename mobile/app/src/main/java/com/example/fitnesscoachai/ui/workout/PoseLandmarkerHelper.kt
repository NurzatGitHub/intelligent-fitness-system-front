package com.example.fitnesscoachai.ui.workout

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseLandmarkerHelper(context: Context) {

    private val landmarker: PoseLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_full.task")
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.6f)
            .setMinPosePresenceConfidence(0.6f)
            .setMinTrackingConfidence(0.6f)
            .build()

        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun detectVideo(mpImage: MPImage, rotationDegrees: Int, timestampMs: Long): PoseLandmarkerResult? {
        val opts = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()

        // ВАЖНО: порядок аргументов именно такой
        return landmarker.detectForVideo(mpImage, opts, timestampMs)
    }

    fun close() = landmarker.close()
}
