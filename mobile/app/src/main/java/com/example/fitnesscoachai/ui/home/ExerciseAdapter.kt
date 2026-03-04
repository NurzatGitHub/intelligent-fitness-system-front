package com.example.fitnesscoachai.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.Exercise

class ExerciseAdapter(
    private var exercises: List<Exercise>,
    private val onExerciseClick: (Exercise) -> Unit = {}
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvExerciseName: TextView = view.findViewById(R.id.tvExerciseName)
        val tvExerciseTarget: TextView = view.findViewById(R.id.tvExerciseTarget)
        val ivExerciseGif: ImageView = view.findViewById(R.id.ivExerciseGif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.tvExerciseName.text = exercise.titleEn.replaceFirstChar { it.uppercase() }
        holder.tvExerciseTarget.text = buildString {
            append(exercise.sub.titleEn)
            exercise.equipment?.let { append(" | ").append(it) }
        }
        holder.ivExerciseGif.setImageDrawable(null)
        // TODO: загрузка media (LocalAsset / RemoteUrl) когда будет нужно
        holder.itemView.setOnClickListener { onExerciseClick(exercise) }
    }

    override fun getItemCount() = exercises.size

    fun updateData(newExercises: List<Exercise>) {
        exercises = newExercises
        notifyDataSetChanged()
    }
}
