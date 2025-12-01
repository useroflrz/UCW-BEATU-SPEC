package com.ucw.beatu.business.search.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.ucw.beatu.business.search.presentation.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AI 搜索页面：集输入、历史对话于一体
 */
class AiSearchFragment : Fragment(R.layout.fragment_ai_search) {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: AiChatAdapter
    private lateinit var inputField: EditText
    private lateinit var sendButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_ai_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar_ai_search)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        chatRecyclerView = view.findViewById(R.id.rv_ai_chat)
        chatAdapter = AiChatAdapter()
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = chatAdapter

        inputField = view.findViewById(R.id.et_ai_query)
        sendButton = view.findViewById(R.id.btn_ai_follow_up)

        val initialPrompt = arguments?.getString(ARG_AI_QUERY).orEmpty()
        if (initialPrompt.isNotBlank()) {
            appendUserAndReply(initialPrompt)
        }

        sendButton.setOnClickListener {
            val query = inputField.text?.toString().orEmpty().trim()
            if (query.isEmpty()) return@setOnClickListener
            appendUserAndReply(query)
            inputField.text?.clear()
        }
    }

    private fun appendUserAndReply(message: String) {
        chatAdapter.addMessage(AiChatMessage.User(message))
        scrollToBottom()
        simulateAiReply()
    }

    private fun simulateAiReply() {
        val target = getString(R.string.ai_search_placeholder_reply)
        val aiPosition = chatAdapter.addMessage(AiChatMessage.Ai(""))
        scrollToBottom()
        viewLifecycleOwner.lifecycleScope.launch {
            var current = ""
            target.forEach { char ->
                current += char
                chatAdapter.updateAiMessage(aiPosition, current)
                scrollToBottom()
                delay(STREAM_DELAY_MS)
            }
        }
    }

    private fun scrollToBottom() {
        chatRecyclerView.post {
            if (chatAdapter.itemCount > 0) {
                chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    companion object {
        const val ARG_AI_QUERY = "ai_query"
        private const val STREAM_DELAY_MS = 28L
    }
}

sealed class AiChatMessage {
    data class User(val content: String) : AiChatMessage()
    data class Ai(val content: String) : AiChatMessage()
}

class AiChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<AiChatMessage>()

    fun addMessage(message: AiChatMessage): Int {
        messages.add(message)
        val position = messages.size - 1
        notifyItemInserted(position)
        return position
    }

    fun updateAiMessage(position: Int, content: String) {
        if (position in messages.indices && messages[position] is AiChatMessage.Ai) {
            messages[position] = AiChatMessage.Ai(content)
            notifyItemChanged(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is AiChatMessage.User -> 0
            is AiChatMessage.Ai -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ai_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ai_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val message = messages[position]) {
            is AiChatMessage.User -> (holder as UserViewHolder).bind(message)
            is AiChatMessage.Ai -> (holder as AiViewHolder).bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvContent: android.widget.TextView = view.findViewById(R.id.tv_user_message)
        fun bind(message: AiChatMessage.User) {
            tvContent.text = message.content
        }
    }

    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvContent: android.widget.TextView = view.findViewById(R.id.tv_ai_message)
        fun bind(message: AiChatMessage.Ai) {
            tvContent.text = message.content
        }
    }
}