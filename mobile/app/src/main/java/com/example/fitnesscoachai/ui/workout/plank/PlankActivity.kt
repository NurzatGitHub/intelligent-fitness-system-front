package com.example.fitnesscoachai.ui.workout.plank

import com.example.fitnesscoachai.ui.workout.shared.*

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageButton
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

class PlankActivity : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────
    private lateinit var previewView:     PreviewView
    private lateinit var overlayView:     OverlayView
    private lateinit var tvTimer:         TextView
    private lateinit var tvExerciseName:  TextView
    private lateinit var tvReps:          TextView
    private lateinit var tvFeedback:      TextView
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnStartPause:   MaterialButton
    private lateinit var btnFinish:       MaterialButton

    // ── Camera ─────────────────────────────────────────────────
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    // ── ML ─────────────────────────────────────────────────────
    private lateinit var plankModel: PlankModel
    private var poseHelper: PoseLandmarkerHelper? = null

    // ── Helpers ────────────────────────────────────────────────
    private val stabilizer = PoseStabilizer()
    private val readyCheck = PlankReadyCheck()

    // ── Constants ──────────────────────────────────────────────
    private val WORKOUT_DURATION_SEC   = 120L   // 2 минуты
    private val CORRECT_STREAK_START   = 5
    private val INCORRECT_STREAK_BREAK = 8
    private val READY_STREAK_NEED      = 3

    // ── Workout state ──────────────────────────────────────────
    private var isWorkoutActive  = false
    private var elapsedSeconds   = 0L
    private var holdSeconds      = 0L
    private var remainingSeconds = WORKOUT_DURATION_SEC
    private var timer: CountDownTimer? = null

    // ── Hold tracking ──────────────────────────────────────────
    private var isHolding       = false
    private var correctStreak   = 0
    private var incorrectStreak = 0

    // ── Ready gate ─────────────────────────────────────────────
    private var readyStreak = 0
    private var isReady     = false

    // ══════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plank)

        initViews()

        plankModel = PlankModel(this)
        poseHelper = PoseLandmarkerHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), 10
        )

        setupListeners()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        cameraExecutor.shutdown()
        poseHelper?.close()
        plankModel.close()
    }

    // ══════════════════════════════════════════════════════════
    //  Init
    // ══════════════════════════════════════════════════════════

    private fun initViews() {
        previewView     = findViewById(R.id.previewView)
        overlayView     = findViewById(R.id.overlayView)
        tvTimer         = findViewById(R.id.tvTimer)
        tvReps          = findViewById(R.id.tvReps)
        tvFeedback      = findViewById(R.id.tvFeedback)
        btnStartPause   = findViewById(R.id.btnStartPause)
        btnFinish       = findViewById(R.id.btnFinish)
        tvExerciseName  = findViewById(R.id.tvExerciseName)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)

        overlayView.mirrorX = false
        tvTimer.text        = "02:00"
        tvReps.text         = "00:00"
        tvFeedback.text     = "Tap Start"
        btnStartPause.text  = "Start"
        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Plank"
    }

    private fun setupListeners() {

        btnStartPause.setOnClickListener {
            when {
                isWorkoutActive      -> pauseWorkout()
                elapsedSeconds == 0L -> startWorkout()
                else                 -> resumeWorkout()
            }
        }

        btnFinish.setOnClickListener {
            finishWorkout()
        }

        btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK

            // Зеркалим скелет для фронтальной камеры
            overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)

            bindCameraUseCases()
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Workout control
    // ══════════════════════════════════════════════════════════

    private fun startWorkout() {
        isWorkoutActive  = true
        elapsedSeconds   = 0L
        holdSeconds      = 0L
        remainingSeconds = WORKOUT_DURATION_SEC
        correctStreak    = 0
        incorrectStreak  = 0
        readyStreak      = 0
        isReady          = false
        isHolding        = false

        stabilizer.reset()
        readyCheck.reset()

        tvTimer.text       = "02:00"
        tvReps.text        = "00:00"
        tvFeedback.text    = "Get into plank position"
        btnStartPause.text = "Pause"

        launchTimer(WORKOUT_DURATION_SEC * 1000L)
    }

    private fun pauseWorkout() {
        isWorkoutActive    = false
        isHolding          = false
        btnStartPause.text = "Resume"
        timer?.cancel()
    }

    private fun resumeWorkout() {
        isWorkoutActive    = true
        btnStartPause.text = "Pause"
        launchTimer(remainingSeconds * 1000L)
    }

    /**
     * Запускает CountDownTimer на [durationMs] миллисекунд.
     * Каждую секунду:
     *   - tvTimer  — обратный отсчёт (02:00 → 00:00)
     *   - tvReps   — суммарное время удержания правильной планки
     * При достижении 0 → finishWorkout()
     */
    private fun launchTimer(durationMs: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(durationMs, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = millisUntilFinished / 1000
                elapsedSeconds++
                if (isHolding) holdSeconds++

                val min = remainingSeconds / 60
                val sec = remainingSeconds % 60
                tvTimer.text = String.format("%02d:%02d", min, sec)

                val hMin = holdSeconds / 60
                val hSec = holdSeconds % 60
                runOnUiThread {
                    tvReps.text = String.format("%02d:%02d", hMin, hSec)
                }
            }

            override fun onFinish() {
                tvTimer.text = "00:00"
                finishWorkout()
            }
        }.start()
    }

    private fun finishWorkout() {
        timer?.cancel()
        isWorkoutActive = false

        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("duration",      elapsedSeconds.toInt())
            putExtra("reps",          holdSeconds.toInt())
            putExtra("exercise_type", "plank")
        }
        startActivity(intent)
        finish()
    }

    // ══════════════════════════════════════════════════════════
    //  Camera
    // ══════════════════════════════════════════════════════════

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                cameraProvider = future.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this))
        }
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
                processFrame(imageProxy)
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

    // ══════════════════════════════════════════════════════════
    //  Frame processing
    // ══════════════════════════════════════════════════════════

    private fun processFrame(imageProxy: ImageProxy) {
        if (!isWorkoutActive) return

        val rotation = imageProxy.imageInfo.rotationDegrees
        val (dispW, dispH) = if (rotation == 90 || rotation == 270)
            imageProxy.height to imageProxy.width
        else
            imageProxy.width to imageProxy.height

        overlayView.setImageSize(dispW, dispH)

        val bitmap  = RgbaToBitmap.toBitmap(imageProxy)
        val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
        val result  = poseHelper?.detectVideo(mpImage, System.currentTimeMillis())
        val pose    = result?.landmarks()?.firstOrNull() ?: return

        val mapped = PoseMapper.mapTo18(pose)
        val stable = stabilizer.apply(mapped) ?: mapped
        val fixed  = PoseRotation.rotate(stable, rotation)

        val (segments, feedbackText) = if (!isReady) {
            processReadyGate(fixed)
        } else {
            processPlankAnalysis(fixed)
        }

        runOnUiThread {
            tvFeedback.text = feedbackText
            overlayView.updatePose(fixed, segments)
        }
    }

    /**
     * Ворота готовности — ждём пока пользователь встанет в планку перед стартом.
     */
    private fun processReadyGate(fixed: List<PosePoint>): Pair<List<Segment>, String> {
        val readyResult  = readyCheck.check(fixed)
        val feedbackText = readyResult.hint.ifEmpty { "Get into plank position" }

        return if (readyResult.isReady) {
            readyStreak++
            if (readyStreak >= READY_STREAK_NEED) {
                isReady         = true
                isHolding       = false
                correctStreak   = 0
                incorrectStreak = 0
            }
            PoseSkeleton.segments.map { it.copy(color = "#AAAAAA") } to feedbackText
        } else {
            readyStreak = 0
            emptyList<Segment>() to feedbackText
        }
    }

    /**
     * Основной анализ позы во время планки.
     * Зелёный скелет + "Hold!" — правильная позиция.
     * Красный скелет — нарушение формы.
     */
    private fun processPlankAnalysis(fixed: List<PosePoint>): Pair<List<Segment>, String> {
        val features = PlankFeatureExtractor.extract(fixed) ?: run {
            isHolding       = false
            correctStreak   = 0
            incorrectStreak = 0
            return emptyList<Segment>() to "Show full body"
        }

        val prediction   = plankModel.predict(features)
        var segments     = PoseSkeleton.segments
        var feedbackText = prediction.label

        if (prediction.label == "correct") {
            correctStreak++
            incorrectStreak = 0

            if (!isHolding && correctStreak >= CORRECT_STREAK_START) {
                isHolding     = true
                correctStreak = 0
            }

            if (isHolding) {
                segments     = segments.map { it.copy(color = "#00C853") }
                feedbackText = "Hold!"
            }

        } else {
            incorrectStreak++
            correctStreak = 0
            segments      = segments.map { it.copy(color = "#FF0000") }

            if (isHolding && incorrectStreak >= INCORRECT_STREAK_BREAK) {
                isHolding = false
            }
        }

        return segments to feedbackText
    }

    // ══════════════════════════════════════════════════════════
    //  Permissions
    // ══════════════════════════════════════════════════════════

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
}