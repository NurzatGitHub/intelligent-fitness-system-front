package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.repo.ExerciseRepositoryLocal
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

        val exerciseId = intent.getStringExtra(EXTRA_EXERCISE_ID)
        if (exerciseId == null) {
            finish()
            return
        }

        val repo = ExerciseRepositoryLocal()
        val stepsContainer = findViewById<LinearLayout>(R.id.stepsContainer)
        val tipsContainer = findViewById<LinearLayout>(R.id.tipsContainer)
        val videoContainer = findViewById<View>(R.id.videoContainer)
        playerView = findViewById(R.id.playerView)

        lifecycleScope.launch {
            val exercise = repo.getExerciseById(exerciseId)
            if (exercise == null) {
                finish()
                return@launch
            }
            supportActionBar?.title = exercise.titleEn

            if (!exercise.videoPath.isNullOrBlank()) {
                val cached = cacheVideoIfNeeded(exercise.videoPath!!)
                if (cached != null && cached.exists()) {
                    videoContainer.visibility = View.VISIBLE
                    initializePlayer(cached)
                } else {
                    videoContainer.visibility = View.GONE
                }
            } else {
                videoContainer.visibility = View.GONE
            }

            findViewById<TextView>(R.id.tvExerciseName).text = exercise.titleEn
            findViewById<TextView>(R.id.tvExerciseMeta).text = buildString {
                exercise.equipment?.let { append(it.replaceFirstChar { c -> c.uppercase() }) }
                exercise.difficulty?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it)
                }
                if (isEmpty()) append("-")
            }
            findViewById<TextView>(R.id.tvDescription).text = exercise.description

            stepsContainer.removeAllViews()
            exercise.steps.forEachIndexed { index, step ->
                val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                    .inflate(R.layout.item_step, stepsContainer, false)
                (view as TextView).text = "${index + 1}. $step"
                stepsContainer.addView(view)
            }

            tipsContainer.removeAllViews()
            exercise.tips.forEach { tip ->
                val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                    .inflate(R.layout.item_step, tipsContainer, false)
                (view as TextView).text = "• $tip"
                tipsContainer.addView(view)
            }
        }
    }

    private fun cacheVideoIfNeeded(assetPath: String): File? {
        return try {
            val fileName = assetPath.substringAfterLast('/')
            val outFile = File(cacheDir, fileName)
            // Всегда перезаписываем из assets, чтобы при замене файла показывалась новая версия
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
        const val EXTRA_EXERCISE_ID = "extra_exercise_id"

        fun newIntent(context: Context, exerciseId: String): Intent {
            return Intent(context, ExerciseInstructionActivity::class.java).apply {
                putExtra(EXTRA_EXERCISE_ID, exerciseId)
            }
        }
    }
}
