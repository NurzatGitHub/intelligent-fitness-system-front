package com.example.fitnesscoachai.ui.assistant

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R

class AssistantFragment : Fragment(R.layout.fragment_assistant) {

    private val vm: AssistantViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvChat)
        val et = view.findViewById<EditText>(R.id.etMessage)
        val btn = view.findViewById<ImageButton>(R.id.btnSend)
        val progress = view.findViewById<ProgressBar>(R.id.progressTyping)

        adapter = ChatAdapter()
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        vm.messages.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            if (list.isNotEmpty()) {
                rv.scrollToPosition(list.size - 1)
            }
        }

        vm.typing.observe(viewLifecycleOwner) { isTyping ->
            progress.visibility = if (isTyping) View.VISIBLE else View.GONE
        }

        btn.setOnClickListener {
            val text = et.text.toString()
            et.setText("")
            vm.sendUserMessage(text)
        }

        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = et.text.toString()
                et.setText("")
                vm.sendUserMessage(text)
                true
            } else {
                false
            }
        }
    }
}

