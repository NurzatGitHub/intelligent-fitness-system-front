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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import androidx.camera.view.PreviewView
import com.example.fitnesscoachai.ui.workout.WorkoutViewModel.WorkoutState
import com.example.fitnesscoachai.ui.camera.AnalysisResult
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WorkoutActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var tvExerciseName: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvAIStatus: TextView
    private lateinit var tvReps: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvGuidance: TextView
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnFinish: MaterialButton

    private val viewModel: WorkoutViewModel by viewModels()
    private var exerciseName: String = ""
    private var isWorkoutActive = false
    private var workoutStartTime: Long = 0
    private var timer: CountDownTimer? = null
    private var elapsedSeconds: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        exerciseName = intent.getStringExtra("exercise_name") ?: "Exercise"

        initializeViews()
        setupObservers()
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

        cameraExecutor = Executors.newSingleThreadExecutor()
        viewModel.connectWebSocket()
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvTimer = findViewById(R.id.tvTimer)
        tvAIStatus = findViewById(R.id.tvAIStatus)
        tvReps = findViewById(R.id.tvReps)
        tvFeedback = findViewById(R.id.tvFeedback)
        tvGuidance = findViewById(R.id.tvGuidance)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)

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
                    }
                    is WorkoutState.Connected -> {
                        tvAIStatus.text = "AI: Detecting"
                        tvFeedback.text = "Analyzing posture…"
                        tvGuidance.visibility = android.view.View.VISIBLE
                        tvGuidance.text = "Stand fully in frame\nMake sure your whole body is visible"
                    }
                    is WorkoutState.AnalysisReceived -> {
                        val result: AnalysisResult = state.result
                        tvReps.text = result.count.toString()
                        tvAIStatus.text = "AI: Tracking"
                        
                        // Show most important feedback (1-2 items max)
                        val feedbackText = when {
                            result.errors.isNotEmpty() -> result.errors.first()
                            result.feedback.isNotEmpty() -> result.feedback
                            else -> "Good form"
                        }
                        tvFeedback.text = feedbackText
                        
                        // Hide guidance when tracking starts
                        if (isWorkoutActive) {
                            tvGuidance.visibility = android.view.View.GONE
                        }
                    }
                    is WorkoutState.Error -> {
                        tvAIStatus.text = "AI: Error"
                        tvFeedback.text = "Error: ${state.message}"
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            if (!isWorkoutActive) {
                startWorkout()
            } else {
                pauseWorkout()
            }
        }

        btnFinish.setOnClickListener {
            finishWorkout()
        }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        workoutStartTime = System.currentTimeMillis()
        btnStartPause.text = "Pause"
        tvAIStatus.text = "AI: Tracking"
        tvFeedback.text = "Analyzing posture…"
        tvGuidance.visibility = android.view.View.GONE

        // Start timer
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
        val duration = elapsedSeconds
        val reps = viewModel.getCurrentReps()

        val intent = Intent(this, SummaryActivity::class.java)
        intent.putExtra("exercise_name", exerciseName)
        intent.putExtra("duration", duration.toInt())
        intent.putExtra("reps", reps)
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (isWorkoutActive) {
                            // TODO: Capture frame and send to viewModel
                            // viewModel.sendFrame(imageProxy)
                        }
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && allPermissionsGranted()) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        cameraExecutor.shutdown()
        viewModel.disconnectWebSocket()
    }
}
