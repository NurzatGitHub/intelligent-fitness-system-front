package com.example.fitnesscoachai.ui.exercise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.Exercise

class ExerciseSelectAdapter(
    private var items: List<Exercise>,
    private val onClick: (Exercise) -> Unit
) : RecyclerView.Adapter<ExerciseSelectAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = items[position]
        holder.tvName.text = exercise.titleEn
        holder.tvSubtitle.text = holder.itemView.context.getString(
            R.string.exercise_select_subtitle
        )

        holder.itemView.setOnClickListener { onClick(exercise) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Exercise>) {
        items = newItems
        notifyDataSetChanged()
    }
}

