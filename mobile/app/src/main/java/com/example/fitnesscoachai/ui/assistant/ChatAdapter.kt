package com.example.fitnesscoachai.ui.assistant

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatMessage>()

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    fun submitList(newItems: List<ChatMessage>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(msg: ChatMessage) {
        items.add(msg)
        notifyItemInserted(items.lastIndex)
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isUser) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            val v = inflater.inflate(R.layout.item_message_user, parent, false)
            UserVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_ai, parent, false)
            AiVH(v)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        val timeStr = formatTime(msg.timeMillis)

        if (holder is UserVH) {
            holder.tvText.text = msg.text
            holder.tvTime.text = timeStr
        } else if (holder is AiVH) {
            holder.tvText.text = renderSimpleMarkdown(msg.text)
            holder.tvTime.text = timeStr
        }
    }

    private fun formatTime(ms: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(ms))
    }

    private fun renderSimpleMarkdown(text: String): CharSequence {
        val normalized = text
            .replace(Regex("""(?m)^\* (.+)$"""), "• $1")

        val builder = SpannableStringBuilder(normalized)

        val pattern = Pattern.compile("\\*\\*(.+?)\\*\\*")
        val matcher = pattern.matcher(normalized)

        val boldRanges = mutableListOf<Pair<Int, Int>>()
        while (matcher.find()) {
            boldRanges.add(matcher.start() to matcher.end())
        }

        for (i in boldRanges.indices.reversed()) {
            val (start, end) = boldRanges[i]
            val inner = normalized.substring(start + 2, end - 2)
            builder.replace(start, end, inner)
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                start + inner.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }

    class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    class AiVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }
}