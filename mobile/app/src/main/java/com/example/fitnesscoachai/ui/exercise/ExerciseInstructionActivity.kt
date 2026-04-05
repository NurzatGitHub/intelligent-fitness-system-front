package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class ExerciseInstructionActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_instruction)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val exerciseSlug = intent.getStringExtra(EXTRA_EXERCISE_SLUG)
        if (exerciseSlug.isNullOrBlank()) {
            finish()
            return
        }

        playerView = findViewById(R.id.playerView)
        loadExerciseDetail(exerciseSlug)
    }

    private fun getBearerToken(): String? {
        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        return if (token.isNullOrBlank()) null else "Bearer $token"
    }

    private fun loadExerciseDetail(slug: String) {
        val token = getBearerToken()
        if (token == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val stepsContainer = findViewById<LinearLayout>(R.id.stepsContainer)
        val tipsContainer = findViewById<LinearLayout>(R.id.tipsContainer)
        val videoContainer = findViewById<View>(R.id.videoContainer)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getExerciseDetail(
                    bearer = token,
                    slug = slug
                )

                if (!response.isSuccessful || response.body() == null) {
                    Toast.makeText(
                        this@ExerciseInstructionActivity,
                        "Failed to load exercise",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                val exercise = response.body()!!

                supportActionBar?.title = exercise.name
                findViewById<TextView>(R.id.tvExerciseName).text = exercise.name
                findViewById<TextView>(R.id.tvExerciseMeta).text = buildString {
                    if (exercise.equipment.isNotBlank()) {
                        append(exercise.equipment.replaceFirstChar { it.uppercase() })
                    }
                    if (exercise.difficulty.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(exercise.difficulty.replaceFirstChar { it.uppercase() })
                    }
                    if (isEmpty()) append("-")
                }
                findViewById<TextView>(R.id.tvDescription).text = exercise.description

                val videoAssetName = exercise.asset_video_name.trim()
                if (videoAssetName.isNotEmpty()) {
                    val cached = cacheVideoIfNeeded("videos/${videoAssetName}.mp4")
                    if (cached != null && cached.exists()) {
                        videoContainer.visibility = View.VISIBLE
                        initializePlayer(cached)
                    } else {
                        videoContainer.visibility = View.GONE
                    }
                } else {
                    videoContainer.visibility = View.GONE
                }

                stepsContainer.removeAllViews()
                exercise.steps.forEachIndexed { index, step ->
                    val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                        .inflate(R.layout.item_step, stepsContainer, false)
                    (view as TextView).text = "${index + 1}. $step"
                    stepsContainer.addView(view)
                }

                tipsContainer.removeAllViews()
                exercise.tips_list.forEach { tip ->
                    val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                        .inflate(R.layout.item_step, tipsContainer, false)
                    (view as TextView).text = "• $tip"
                    tipsContainer.addView(view)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExerciseInstructionActivity,
                    "Network error",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun cacheVideoIfNeeded(assetPath: String): File? {
        return try {
            val fileName = assetPath.substringAfterLast('/')
            val outFile = File(cacheDir, fileName)
            assets.open(assetPath).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: IOException) {
            null
        }
    }

    private fun initializePlayer(file: File) {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exoPlayer ->
                playerView?.player = exoPlayer
                val mediaItem = MediaItem.fromUri(file.toURI().toString())
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } else {
            val exoPlayer = player!!
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    private fun releasePlayer() {
        playerView?.player = null
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    companion object {
        const val EXTRA_EXERCISE_SLUG = "extra_exercise_slug"

        fun newIntent(context: Context, exerciseSlug: String): Intent {
            return Intent(context, ExerciseInstructionActivity::class.java).apply {
                putExtra(EXTRA_EXERCISE_SLUG, exerciseSlug)
            }
        }
    }
}