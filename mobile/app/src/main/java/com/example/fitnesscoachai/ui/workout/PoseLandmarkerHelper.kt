package com.example.fitnesscoachai.ui.workout

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions

class PoseLandmarkerHelper(
    context: Context,
) {
    private val landmarker: PoseLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker.task")
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE) // проще начать с IMAGE
            .setNumPoses(1)
            .build()

        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun detect(mpImage: MPImage): PoseLandmarkerResult? {
        // Для IMAGE режима timestamp не нужен
        return landmarker.detect(mpImage)
    }

    fun close() {
        landmarker.close()
    }
}
