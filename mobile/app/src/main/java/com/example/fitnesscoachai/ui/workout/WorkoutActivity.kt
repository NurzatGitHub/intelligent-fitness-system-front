package com.example.fitnesscoachai.ui.workout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import com.example.fitnesscoachai.ui.workout.WorkoutViewModel.WorkoutState
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WorkoutActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var overlayView: OverlayView
    private lateinit var previewView: PreviewView
    private lateinit var tvExerciseName: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvAIStatus: TextView
    private lateinit var tvReps: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvGuidance: TextView
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnFinish: MaterialButton
    private lateinit var btnSwitchCamera: android.widget.ImageButton

    private val viewModel: WorkoutViewModel by viewModels()
    private var exerciseName: String = ""
    private var isWorkoutActive = false
    private var timer: CountDownTimer? = null
    private var elapsedSeconds: Long = 0

    private var poseHelper: PoseLandmarkerHelper? = null
    private var lastSendMs = 0L
    private var lastSentPoints: List<PosePoint>? = null

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraProvider: ProcessCameraProvider? = null
    private val stabilizer = PoseStabilizer()

    @Volatile private var lastSegments: List<Segment> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        exerciseName = intent.getStringExtra("exercise_name") ?: "Exercise"

        initializeViews()
        poseHelper = PoseLandmarkerHelper(this)
        setupObservers()
        setupListeners()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)

        viewModel.connectWebSocket()
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvTimer = findViewById(R.id.tvTimer)
        tvAIStatus = findViewById(R.id.tvAIStatus)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)
        tvGuidance = findViewById(R.id.tvGuidance)

        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)

        overlayView = findViewById(R.id.overlayView)
        overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)

        tvExerciseName.text = "$exerciseName Training"
        tvAIStatus.text = "AI: Ready"
        tvReps.text = "0"
        tvFeedback.text = "Tap Start to begin training"
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.workoutState.collect { state ->
                when (state) {
                    is WorkoutState.Idle -> {
                        tvAIStatus.text = "AI: Ready"
                        tvFeedback.text = "Tap Start to begin training"
                        tvGuidance.visibility = android.view.View.GONE
                        lastSegments = emptyList()
                        overlayView.updatePose(null, emptyList())
                    }
                    is WorkoutState.Connecting -> {
                        tvAIStatus.text = "AI: Connecting..."
                        tvFeedback.text = "Connecting..."
                    }
                    is WorkoutState.Connected -> {
                        tvAIStatus.text = "AI: Connected"
                        tvFeedback.text = "Ready"
                        tvGuidance.visibility = android.view.View.VISIBLE
                        tvGuidance.text = "Stand fully in frame\nMake sure your whole body is visible"
                    }
                    is WorkoutState.Setup -> {
                        tvAIStatus.text = "AI: Detecting"
                        tvFeedback.text = state.hint
                        tvGuidance.visibility = android.view.View.VISIBLE
                        tvGuidance.text = state.hint
                        lastSegments = emptyList()
                        overlayView.updatePose(lastSentPoints, emptyList())
                    }
                    is WorkoutState.Active -> {
                        tvAIStatus.text = "AI: Tracking"
                        val res = state.res
                        tvReps.text = (res.rep_count ?: 0).toString()

                        val conf = res.confidence?.let { String.format("%.2f", it) } ?: "-"
                        tvFeedback.text = "${res.overall ?: ""} (conf=$conf)"

                        lastSegments = res.segments ?: emptyList()
                        overlayView.updatePose(lastSentPoints, lastSegments)
                        tvGuidance.visibility = android.view.View.GONE
                    }
                    is WorkoutState.Info -> tvFeedback.text = state.msg
                    is WorkoutState.Error -> {
                        tvAIStatus.text = "AI: Error"
                        tvFeedback.text = state.message
                        Toast.makeText(this@WorkoutActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener { if (!isWorkoutActive) startWorkout() else pauseWorkout() }

        btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT

            overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)
            bindCameraUseCases()
        }

        btnFinish.setOnClickListener { finishWorkout() }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        btnStartPause.text = "Pause"
        tvAIStatus.text = "AI: Tracking"
        tvFeedback.text = "Analyzing posture…"
        tvGuidance.visibility = android.view.View.GONE
        stabilizer.reset()
        lastSegments = emptyList()

        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                val minutes = TimeUnit.SECONDS.toMinutes(elapsedSeconds)
                val seconds = elapsedSeconds % 60
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            }
            override fun onFinish() {}
        }.start()
    }

    private fun pauseWorkout() {
        isWorkoutActive = false
        btnStartPause.text = "Resume"
        timer?.cancel()
        tvAIStatus.text = "AI: Paused"
        tvFeedback.text = "Workout paused"
    }

    private fun finishWorkout() {
        timer?.cancel()
        val reps = viewModel.getCurrentReps()

        val intent = Intent(this, SummaryActivity::class.java)
        intent.putExtra("exercise_name", exerciseName)
        intent.putExtra("duration", elapsedSeconds.toInt())
        intent.putExtra("reps", reps)
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                if (!isWorkoutActive) return@setAnalyzer

                val rot = imageProxy.imageInfo.rotationDegrees

                val now = System.currentTimeMillis()
                if (now - lastSendMs < 100) return@setAnalyzer
                lastSendMs = now

                val bmp = RgbaToBitmap.toBitmap(imageProxy)
                val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bmp).build()

                val ts = System.currentTimeMillis()

                // ⚠️ Тут важно: если твой PoseLandmarkerHelper умеет rotationDegrees — передавай rot
                val result = poseHelper?.detectVideo(mpImage, ts)
                val firstPose = result?.landmarks()?.firstOrNull()

                if (firstPose == null) {
                    runOnUiThread { overlayView.updatePose(null, emptyList()) }
                    return@setAnalyzer
                }

                val raw18 = PoseMapper.mapTo18(firstPose)
                val stable18 = stabilizer.apply(raw18) ?: raw18

                // ✅ Поворачиваем ОДИН раз в сторону rot (НЕ 360-rot)
                val fixed18 = PoseRotation.rotate(stable18, rot)

                lastSentPoints = fixed18
                viewModel.sendLandmarks(fixed18)

                // ✅ размеры кадра для center-crop (если rot 90/270 — меняем местами)
                val imgW = if (rot == 90 || rot == 270) imageProxy.height else imageProxy.width
                val imgH = if (rot == 90 || rot == 270) imageProxy.width else imageProxy.height

                runOnUiThread {
                    overlayView.setImageSize(imgW, imgH)

                    // ✅ mirror делаем ТОЛЬКО через overlayView.mirrorX
                    overlayView.mirrorX = (lensFacing == CameraSelector.LENS_FACING_FRONT)

                    overlayView.updatePose(fixed18, lastSegments)
                }
            } finally {
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        provider.unbindAll()
        provider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && allPermissionsGranted()) startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        cameraExecutor.shutdown()
        viewModel.disconnectWebSocket()
        poseHelper?.close()
        poseHelper = null
    }
}
