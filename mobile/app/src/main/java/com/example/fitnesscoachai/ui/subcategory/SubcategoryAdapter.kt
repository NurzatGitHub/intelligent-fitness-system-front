package com.example.fitnesscoachai.ui.subcategory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.SubCategory

class SubcategoryAdapter(
    private var items: List<SubCategory>,
    private val onSubcategoryClick: (SubCategory) -> Unit
) : RecyclerView.Adapter<SubcategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubcategoryName: TextView = view.findViewById(R.id.tvSubcategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subcategory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = items[position]
        holder.tvSubcategoryName.text = sub.titleEn
        holder.itemView.setOnClickListener { onSubcategoryClick(sub) }
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: List<SubCategory>) {
        items = newItems
        notifyDataSetChanged()
    }
}
