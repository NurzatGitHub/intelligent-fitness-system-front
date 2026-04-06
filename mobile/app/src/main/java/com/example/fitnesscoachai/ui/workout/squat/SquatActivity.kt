package com.example.fitnesscoachai.ui.workout.squat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import com.example.fitnesscoachai.ui.workout.shared.OverlayView
import com.example.fitnesscoachai.ui.workout.shared.PoseLandmarkerHelper
import com.example.fitnesscoachai.ui.workout.shared.PoseMapper
import com.example.fitnesscoachai.ui.workout.shared.PosePoint
import com.example.fitnesscoachai.ui.workout.shared.PoseRotation
import com.example.fitnesscoachai.ui.workout.shared.PoseSkeleton
import com.example.fitnesscoachai.ui.workout.shared.PoseStabilizer
import com.example.fitnesscoachai.ui.workout.shared.RgbaToBitmap
import com.example.fitnesscoachai.ui.workout.shared.Segment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SquatActivity : AppCompatActivity() {

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

    private lateinit var squatModel: SquatModel
    private var poseHelper: PoseLandmarkerHelper? = null

    private val stabilizer = PoseStabilizer()
    private val readyCheck = SquatReadyCheck()

    private var isWorkoutActive = false
    private var elapsedSeconds: Long = 0
    private var timer: CountDownTimer? = null
    private var repCount = 0

    private enum class SquatPhase { UP, DOWN }

    private var phase = SquatPhase.UP
    private var downStreak = 0
    private var upStreak = 0

    private var reachedDepthInCurrentRep = false
    private var bottomHoldStreak = 0

    private val downThreshold = 145f
    private val upThreshold = 154f
    private val depthThreshold = 0.92f
    private val kneeDiffThreshold = 40f
    private val bottomHoldNeed = 1

    private val ankleLevelTolerance = 0.18f
    private val kneeLevelTolerance = 0.20f
    private val minKneeWidthRatio = 0.22f
    private val maxKneeWidthRatio = 2.50f

    private val trunkLeanThreshold = 42f
    private val kneeCaveThreshold = 3.1f

    private var readyStreak = 0
    private val readyStreakNeed = 3
    private var isReady = false

    private val lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        initViews()
        initDependencies()
        setupListeners()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                10
            )
        }
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        tvTimer = findViewById(R.id.tvTimer)
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)

        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)

        overlayView.mirrorX = true
        tvTimer.text = "00:00"
        tvReps.text = "0"
        tvFeedback.text = "Tap Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Squat Training"
    }

    private fun initDependencies() {
        squatModel = SquatModel(this)
        poseHelper = PoseLandmarkerHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            if (isWorkoutActive) pauseWorkout() else startWorkout()
        }

        btnFinish.setOnClickListener {
            finishWorkout()
        }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        elapsedSeconds = 0
        tvTimer.text = "00:00"
        btnStartPause.text = "Pause"

        resetWorkoutState()
        startTimer()
    }

    private fun pauseWorkout() {
        isWorkoutActive = false
        btnStartPause.text = "Resume"
        timer?.cancel()
    }

    private fun finishWorkout() {
        timer?.cancel()

        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", tvExerciseName.text.toString())
            putExtra("duration", elapsedSeconds.toInt())
            putExtra("reps", repCount)
        }

        startActivity(intent)
        finish()
    }

    private fun resetWorkoutState() {
        stabilizer.reset()
        readyCheck.reset()

        repCount = 0
        phase = SquatPhase.UP
        downStreak = 0
        upStreak = 0
        reachedDepthInCurrentRep = false
        bottomHoldStreak = 0

        readyStreak = 0
        isReady = false

        tvReps.text = "0"
        tvFeedback.text = "Stand up straight to begin"
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                val minutes = TimeUnit.SECONDS.toMinutes(elapsedSeconds)
                val seconds = elapsedSeconds % 60
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() = Unit
        }.start()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                cameraProvider = future.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(this)
        )
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
                if (!isWorkoutActive) return@setAnalyzer

                val rotation = imageProxy.imageInfo.rotationDegrees
                val (displayWidth, displayHeight) =
                    if (rotation == 90 || rotation == 270) {
                        imageProxy.height to imageProxy.width
                    } else {
                        imageProxy.width to imageProxy.height
                    }

                overlayView.setImageSize(displayWidth, displayHeight)

                val bitmap = RgbaToBitmap.toBitmap(imageProxy)
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = poseHelper?.detectVideo(mpImage, System.currentTimeMillis())
                val pose = result?.landmarks()?.firstOrNull() ?: return@setAnalyzer

                val mapped = PoseMapper.mapTo18(pose)
                val stable = stabilizer.apply(mapped) ?: mapped
                val fixed = PoseRotation.rotate(stable, rotation)

                val uiState = if (!isReady) {
                    handleReadyState(fixed)
                } else {
                    handleActiveState(fixed)
                }

                runOnUiThread {
                    tvReps.text = repCount.toString()
                    tvFeedback.text = uiState.feedback
                    overlayView.updatePose(fixed, uiState.segments)
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

    private fun handleReadyState(points: List<PosePoint>): UiState {
        val readyResult = readyCheck.check(points)

        if (readyResult.isReady) {
            readyStreak++
        } else {
            readyStreak = 0
        }

        if (readyStreak >= readyStreakNeed) {
            isReady = true
            return UiState(
                feedback = "Start squatting",
                segments = PoseSkeleton.segments
            )
        }

        return UiState(
            feedback = if (readyResult.isReady) "Ready" else readyResult.hint.ifBlank { "Stand upright" },
            segments = if (readyResult.isReady) PoseSkeleton.segments else emptyList()
        )
    }

    private fun handleActiveState(fixed: List<PosePoint>): UiState {
        val features = SquatFeatureExtractor.extract(fixed)
            ?: return UiState(
                feedback = "Show shoulders, hips, knees and ankles clearly",
                segments = PoseSkeleton.segments
            )

        val minKnee = features[0]
        val leftKnee = features[1]
        val rightKnee = features[2]
        val kneeDiff = features[3]
        val kneeCaveRatio = features[5]
        val depthRatio = features[6]
        val trunkAngle = features[7]

        val lHip = fixed[10]
        val rHip = fixed[11]
        val lKneeP = fixed[12]
        val rKneeP = fixed[13]
        val lAnk = fixed[14]
        val rAnk = fixed[15]

        val ankleLevelOk = abs(lAnk.y - rAnk.y) < ankleLevelTolerance
        val kneeLevelOk = abs(lKneeP.y - rKneeP.y) < kneeLevelTolerance

        val hipWidth = abs(lHip.x - rHip.x)
        val kneeWidth = abs(lKneeP.x - rKneeP.x)
        val stanceRatio = kneeWidth / (hipWidth + 1e-6f)
        val stanceOk = hipWidth > 0.01f && stanceRatio in minKneeWidthRatio..maxKneeWidthRatio

        // мягкая двухопорная проверка
        val twoLegSupportOk = listOf(ankleLevelOk, kneeLevelOk, stanceOk).count { it } >= 2

        // низ: достаточно, чтобы ОБЕ ноги реально согнулись, но не требуем идеала
        val bottomReached =
            leftKnee < downThreshold &&
                    rightKnee < downThreshold &&
                    depthRatio < depthThreshold &&
                    kneeDiff < kneeDiffThreshold &&
                    twoLegSupportOk

        // верх: полное выпрямление
        val standingReached =
            leftKnee > upThreshold &&
                    rightKnee > upThreshold

        if (bottomReached) {
            bottomHoldStreak++
            if (bottomHoldStreak >= bottomHoldNeed) {
                reachedDepthInCurrentRep = true
            }
        } else {
            bottomHoldStreak = 0
        }

        when {
            bottomReached -> {
                downStreak++
                upStreak = 0
            }
            standingReached -> {
                upStreak++
                downStreak = 0
            }
            else -> {
                downStreak = 0
                upStreak = 0
            }
        }

        if (phase == SquatPhase.UP && downStreak >= 1) {
            phase = SquatPhase.DOWN
            downStreak = 0
        }

        if (phase == SquatPhase.DOWN && upStreak >= 1) {
            phase = SquatPhase.UP
            upStreak = 0

            if (reachedDepthInCurrentRep) {
                repCount++
            }

            reachedDepthInCurrentRep = false
            bottomHoldStreak = 0
        }

        val feedback = when {
            !stanceOk -> "Stand evenly"
            !twoLegSupportOk -> "Keep both feet grounded"
            depthRatio >= depthThreshold -> "Go lower"
            trunkAngle > trunkLeanThreshold -> "Keep your chest up"
            kneeCaveRatio >= kneeCaveThreshold -> "Keep your knees out"
            else -> "Good squat"
        }

        val prediction = squatModel.predict(features)

        var segments = buildSquatSegments(
            kneeGood = kneeCaveRatio < kneeCaveThreshold,
            depthGood = depthRatio < depthThreshold,
            trunkGood = trunkAngle < trunkLeanThreshold
        )

        if (prediction.label.lowercase() == "incorrect") {
            segments = segments.map { it.copy(color = "#FF0000") }
        }

        return UiState(
            feedback = feedback,
            segments = segments
        )
    }

    private fun buildSquatSegments(
        kneeGood: Boolean,
        depthGood: Boolean,
        trunkGood: Boolean
    ): List<Segment> {
        val green = "#00FF00"
        val red = "#FF0000"

        val kneeColor = if (kneeGood) green else red
        val bodyColor = if (depthGood && trunkGood) green else red

        return listOf(
            Segment(4, 5, bodyColor),
            Segment(10, 11, bodyColor),
            Segment(4, 10, bodyColor),
            Segment(5, 11, bodyColor),
            Segment(10, 12, kneeColor),
            Segment(12, 14, kneeColor),
            Segment(11, 13, kneeColor),
            Segment(13, 15, kneeColor),
        )
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        cameraExecutor.shutdown()
        poseHelper?.close()
        squatModel.close()
    }

    private data class UiState(
        val feedback: String,
        val segments: List<Segment>
    )
}