package com.example.fitnesscoachai.ui.workout.shoulderpress

import com.example.fitnesscoachai.ui.workout.shared.*

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShoulderPressActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    private lateinit var tvTimer: TextView
    private lateinit var tvExerciseName: TextView
    private lateinit var tvReps: TextView
    private lateinit var tvFeedback: TextView

    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnFinish: MaterialButton

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var shoulderPressModel: ShoulderPressModel
    private var poseHelper: PoseLandmarkerHelper? = null

    private val stabilizer = PoseStabilizer()
    private val readyCheck = ShoulderPressReadyCheck()
    private val repBuffer = ShoulderPressRepBuffer()

    private var isWorkoutActive = false
    private var elapsedSeconds: Long = 0
    private var timer: CountDownTimer? = null

    private var repCount = 0

    private enum class ShoulderPhase { DOWN, UP, IDLE }

    private var phase = ShoulderPhase.IDLE
    private var downStreak = 0
    private var upStreak = 0

    private val DOWN_T = ShoulderPressRepBuffer.DOWN_THRESHOLD
    private val UP_T = ShoulderPressRepBuffer.UP_THRESHOLD

    private var readyStreak = 0
    private val READY_STREAK_NEED = 3
    private var isReady = false

    private var lastLabel = "correct"

    private val lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        initViews()

        shoulderPressModel = ShoulderPressModel(this)
        poseHelper = PoseLandmarkerHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), 10
        )

        setupListeners()
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvTimer = findViewById(R.id.tvTimer)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)
        tvExerciseName = findViewById(R.id.tvExerciseName)

        overlayView.mirrorX = true
        tvReps.text = "0"
        tvFeedback.text = "Tap Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Shoulder Press"
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            if (!isWorkoutActive) startWorkout() else pauseWorkout()
        }
        btnFinish.setOnClickListener { finishWorkout() }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        elapsedSeconds = 0
        tvTimer.text = "00:00"

        stabilizer.reset()
        readyCheck.reset()
        repBuffer.reset()

        repCount = 0
        downStreak = 0
        upStreak = 0
        readyStreak = 0
        isReady = false
        phase = ShoulderPhase.IDLE
        lastLabel = "correct"

        btnStartPause.text = "Pause"

        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                val m = TimeUnit.SECONDS.toMinutes(elapsedSeconds)
                val s = elapsedSeconds % 60
                tvTimer.text = String.format("%02d:%02d", m, s)
            }

            override fun onFinish() {}
        }.start()
    }

    private fun pauseWorkout() {
        isWorkoutActive = false
        btnStartPause.text = "Resume"
        timer?.cancel()
    }

    private fun finishWorkout() {
        timer?.cancel()
        val intent = Intent(this, SummaryActivity::class.java)
        intent.putExtra("duration", elapsedSeconds.toInt())
        intent.putExtra("reps", repCount)
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            cameraProvider = future.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                if (!isWorkoutActive) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val rotation = imageProxy.imageInfo.rotationDegrees
                val (dispW, dispH) = if (rotation == 90 || rotation == 270) {
                    imageProxy.height to imageProxy.width
                } else {
                    imageProxy.width to imageProxy.height
                }
                overlayView.setImageSize(dispW, dispH)

                val bitmap = RgbaToBitmap.toBitmap(imageProxy)
                val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                val result = poseHelper?.detectVideo(mpImage, System.currentTimeMillis())
                val pose = result?.landmarks()?.firstOrNull() ?: run {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mapped = PoseMapper.mapTo18(pose)
                val stable = stabilizer.apply(mapped) ?: mapped
                val fixed = PoseRotation.rotate(stable, rotation)

                var segments = emptyList<Segment>()
                var feedbackText = ""

                if (!isReady) {
                    val readyResult = readyCheck.check(fixed)
                    feedbackText = readyResult.hint.ifEmpty { "Hold weights at shoulders" }

                    if (readyResult.isReady) {
                        readyStreak++
                        segments = PoseSkeleton.segments.map { it.copy(color = "#AAAAAA") }
                    } else {
                        readyStreak = 0
                        segments = emptyList()
                    }

                    if (readyStreak >= READY_STREAK_NEED) {
                        isReady = true
                        phase = ShoulderPhase.DOWN
                        downStreak = 0
                        upStreak = 0
                        repBuffer.reset()
                    }

                } else {
                    val frameFeats = ShoulderPressFeatureExtractor.extract(fixed)

                    if (frameFeats != null) {
                        val minElbow = ShoulderPressFeatureExtractor.minElbow(frameFeats)
                        val modelInput = repBuffer.push(frameFeats)

                        if (modelInput != null) {
                            val prediction = shoulderPressModel.predict(modelInput)
                            lastLabel = prediction.label
                            feedbackText = prediction.label
                        } else {
                            feedbackText = when {
                                !repBuffer.hasDown() -> "Lower weights to shoulders"
                                !repBuffer.hasUp() -> "Press up fully"
                                else -> lastLabel
                            }
                        }

                        segments = buildSegments(frameFeats)
                        if (lastLabel == "incorrect") {
                            segments = segments.map { it.copy(color = "#FF0000") }
                        }

                        when {
                            minElbow <= DOWN_T -> {
                                downStreak++
                                upStreak = 0
                            }
                            minElbow >= UP_T -> {
                                upStreak++
                                downStreak = 0
                            }
                            else -> {
                                downStreak = 0
                                upStreak = 0
                            }
                        }

                        if (phase == ShoulderPhase.DOWN && upStreak >= 3) {
                            phase = ShoulderPhase.UP
                            upStreak = 0
                        }

                        if (phase == ShoulderPhase.UP && downStreak >= 3) {
                            phase = ShoulderPhase.DOWN
                            downStreak = 0
                            repCount++
                            repBuffer.reset()
                        }

                    } else {
                        feedbackText = "Show upper body"
                        segments = emptyList()
                        downStreak = 0
                        upStreak = 0
                    }
                }

                val finalSegments = segments
                val finalFeedback = feedbackText

                runOnUiThread {
                    tvReps.text = repCount.toString()
                    tvFeedback.text = finalFeedback
                    overlayView.updatePose(fixed, finalSegments)
                }

            } finally {
                imageProxy.close()
            }
        }

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(this, selector, preview, analysis)
    }

    private fun buildSegments(feats: FloatArray): List<Segment> {
        val elbowDiff = feats[2]
        val trunkLean = feats[8]

        val symmetryColor = when {
            elbowDiff < 15f -> "#00C853"
            elbowDiff < 30f -> "#FFD600"
            else -> "#FF6D00"
        }

        val trunkColor = when {
            trunkLean < 15f -> "#00C853"
            trunkLean < 30f -> "#FFD600"
            else -> "#FF6D00"
        }

        return PoseSkeleton.segments.map { seg ->
            val color = when {
                seg.involves(4) || seg.involves(5) ||
                        seg.involves(6) || seg.involves(7) ||
                        seg.involves(8) || seg.involves(9) -> symmetryColor

                seg.involves(3) || seg.involves(10) || seg.involves(11) -> trunkColor

                else -> "#AAAAAA"
            }
            seg.copy(color = color)
        }
    }

    private fun Segment.involves(pointId: Int): Boolean {
        val candidateValues = mutableListOf<Int>()

        fun tryField(name: String) {
            try {
                val field = javaClass.getDeclaredField(name)
                field.isAccessible = true
                val value = field.get(this)
                if (value is Int) candidateValues.add(value)
            } catch (_: Exception) {
            }
        }

        fun tryMethod(name: String) {
            try {
                val method = javaClass.methods.firstOrNull {
                    it.name == name && it.parameterCount == 0
                } ?: return
                val value = method.invoke(this)
                if (value is Int) candidateValues.add(value)
            } catch (_: Exception) {
            }
        }

        tryField("startIdx")
        tryField("endIdx")
        tryField("start")
        tryField("end")
        tryField("from")
        tryField("to")
        tryField("a")
        tryField("b")

        tryMethod("getStartIdx")
        tryMethod("getEndIdx")
        tryMethod("getStart")
        tryMethod("getEnd")
        tryMethod("getFrom")
        tryMethod("getTo")
        tryMethod("getA")
        tryMethod("getB")

        return pointId in candidateValues
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        cameraExecutor.shutdown()
        poseHelper?.close()
        shoulderPressModel.close()
    }
}