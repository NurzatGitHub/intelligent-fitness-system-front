package com.example.fitnesscoachai.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.model.ExerciseMedia

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
        when (val m = exercise.media) {
            is ExerciseMedia.LocalAsset -> {
                if (m.path.startsWith("drawable/")) {
                    val name = m.path.removePrefix("drawable/")
                    val resId = holder.itemView.context.resources.getIdentifier(name, "drawable", holder.itemView.context.packageName)
                    if (resId != 0) holder.ivExerciseGif.setImageResource(resId)
                    else holder.ivExerciseGif.setImageDrawable(null)
                } else {
                    holder.ivExerciseGif.setImageDrawable(null)
                }
            }
            else -> holder.ivExerciseGif.setImageDrawable(null)
        }
        holder.itemView.setOnClickListener { onExerciseClick(exercise) }
    }

    override fun getItemCount() = exercises.size

    fun updateData(newExercises: List<Exercise>) {
        exercises = newExercises
        notifyDataSetChanged()
    }
}
