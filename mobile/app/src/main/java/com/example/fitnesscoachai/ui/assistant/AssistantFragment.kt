package com.example.fitnesscoachai.ui.assistant

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R

class AssistantFragment : Fragment(R.layout.fragment_assistant) {

    private val vm: AssistantViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    private fun getAccessToken(): String? {
        val sp = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        return sp.getString("access_token", null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvChat)
        val et = view.findViewById<EditText>(R.id.etMessage)
        val btn = view.findViewById<ImageButton>(R.id.btnSend)
        val progress = view.findViewById<View>(R.id.progressTyping)

        adapter = ChatAdapter()
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        vm.messages.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            if (list.isNotEmpty()) rv.scrollToPosition(list.size - 1)
        }

        vm.typing.observe(viewLifecycleOwner) { isTyping ->
            progress.visibility = if (isTyping) View.VISIBLE else View.GONE
            btn.isEnabled = !isTyping
            et.isEnabled = !isTyping
        }

        fun sendNow() {
            val text = et.text.toString().trim()
            if (text.isEmpty()) return

            et.setText("")
            val token = getAccessToken()

            if (token.isNullOrBlank()) {
                val cur = vm.messages.value.orEmpty().toMutableList()
                cur.add(ChatMessage("Нужно войти в аккаунт, чтобы использовать AI.", isUser = false))
                adapter.submitList(cur)
                rv.scrollToPosition(cur.size - 1)
                return
            }

            vm.sendUserMessage(text, token)
        }

        btn.setOnClickListener { sendNow() }

        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendNow()
                true
            } else {
                false
            }
        }
    }
}