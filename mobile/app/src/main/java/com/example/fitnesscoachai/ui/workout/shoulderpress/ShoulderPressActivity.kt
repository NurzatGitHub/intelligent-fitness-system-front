package com.example.fitnesscoachai.ui.workout.shoulderpress

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import com.example.fitnesscoachai.ui.workout.shared.OverlayView
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

    private var isWorkoutActive = false
    private var elapsedSeconds: Long = 0
    private var timer: CountDownTimer? = null
    private var repCount = 0

    private val lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        initViews()
        setupListeners()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
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
        overlayView.updatePose(emptyList(), emptyList())

        tvExerciseName.text = intent.getStringExtra("exercise_name") ?: "Shoulder Press"
        tvTimer.text = "00:00"
        tvReps.text = "0"
        tvFeedback.text = "Tap Start. Tap reps number to count."
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

        tvReps.setOnClickListener {
            if (!isWorkoutActive) {
                Toast.makeText(this, "Tap Start first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repCount++
            tvReps.text = repCount.toString()
            tvFeedback.text = "Rep added"
        }

        tvReps.setOnLongClickListener {
            if (repCount > 0) {
                repCount--
                tvReps.text = repCount.toString()
                tvFeedback.text = "Rep removed"
            }
            true
        }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        btnStartPause.text = "Pause"
        tvFeedback.text = "Workout active. Tap reps number to count."

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

    private fun pauseWorkout() {
        isWorkoutActive = false
        btnStartPause.text = "Resume"
        tvFeedback.text = "Paused"
        timer?.cancel()
    }

    private fun finishWorkout() {
        timer?.cancel()

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Shoulder Press"
        val exerciseSlug = intent.getStringExtra("exercise_slug") ?: "shoulder-press"
        val weeklyPlanDayId = intent.getIntExtra("weekly_plan_day_id", -1)

        val summaryIntent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("exercise_name", exerciseName)
            putExtra("exercise_slug", exerciseSlug)
            putExtra("duration", elapsedSeconds.toInt())
            putExtra("reps", repCount)

            if (weeklyPlanDayId > 0) {
                putExtra("weekly_plan_day_id", weeklyPlanDayId)
            }
        }

        startActivity(summaryIntent)
        finish()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({
            cameraProvider = future.get()
            bindCameraPreview()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(this, selector, preview)
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 10
    }
}