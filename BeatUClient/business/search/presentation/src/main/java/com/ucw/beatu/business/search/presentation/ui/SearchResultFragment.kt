package com.ucw.beatu.business.search.presentation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucw.beatu.business.search.presentation.R
import com.ucw.beatu.business.search.presentation.viewmodel.SearchResultVideoViewModel
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.common.model.VideoItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 常规搜索结果页面
 * 单列图文流，点击进入视频播放列表（匹配搜索词）
 */
@AndroidEntryPoint
class SearchResultFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: View
    private lateinit var searchButton: TextView
    private lateinit var backButton: View
    private val viewModel: SearchResultVideoViewModel by viewModels()

    private lateinit var resultAdapter: SearchResultListAdapter
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentQuery = arguments?.getString(ARG_QUERY).orEmpty()
        setupSearchHeader(view)
        setupResultList(view)
        observeViewModel()
        viewModel.initSearch(currentQuery, titleKeyword = "")
    }

    private fun setupSearchHeader(view: View) {
        searchEditText = view.findViewById(R.id.et_search)
        clearButton = view.findViewById(R.id.btn_clear)
        searchButton = view.findViewById(R.id.tv_search)
        backButton = view.findViewById(R.id.btn_back)

        searchEditText.setText(currentQuery)
        searchEditText.setSelection(searchEditText.text?.length ?: 0)
        clearButton.isVisible = currentQuery.isNotEmpty()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearButton.isVisible = !s.isNullOrEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                triggerSearch(searchEditText.text.toString())
                true
            } else {
                false
            }
        }

        clearButton.setOnClickListener {
            searchEditText.text?.clear()
        }

        searchButton.setOnClickListener {
            triggerSearch(searchEditText.text.toString())
        }

        backButton.setOnClickListener {
            if (!findNavController().popBackStack()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupResultList(view: View) {
        val rvResults = view.findViewById<RecyclerView>(R.id.rv_search_result_list)
        rvResults.layoutManager = LinearLayoutManager(requireContext())
        resultAdapter = SearchResultListAdapter { item ->
            navigateToVideoViewer(currentQuery, item.title)
        }
        rvResults.adapter = resultAdapter
    }

    private fun triggerSearch(query: String) {
        if (query.isBlank()) return
        currentQuery = query
        viewModel.initSearch(query, titleKeyword = "")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.videoList.isNotEmpty()) {
                        val uiModels = state.videoList.map { it.toUiModel() }
                        resultAdapter.submitList(uiModels)
                    }
                    // 省略 loading/error 展示，后续可扩展
                }
            }
        }
    }

    data class SearchResultUiModel(
        val title: String,
        val description: String,
        val author: String
    )

    private fun VideoItem.toUiModel(): SearchResultUiModel {
        return SearchResultUiModel(
            title = title,
            description = "点赞 $likeCount · 评论 $commentCount",
            author = authorName
        )
    }

    private class SearchResultListAdapter(
        private val onClick: (SearchResultUiModel) -> Unit
    ) : RecyclerView.Adapter<SearchResultListAdapter.ResultViewHolder>() {

        private var items: List<SearchResultUiModel> = emptyList()

        fun submitList(newItems: List<SearchResultUiModel>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
            return ResultViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ResultViewHolder(
            itemView: View,
            private val onClick: (SearchResultUiModel) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            private val title: TextView = itemView.findViewById(R.id.tv_title)
            private val desc: TextView = itemView.findViewById(R.id.tv_description)
            private val author: TextView = itemView.findViewById(R.id.tv_author)

            init {
                itemView.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onClick(items[bindingAdapterPosition])
                    }
                }
            }

            fun bind(model: SearchResultUiModel) {
                title.text = model.title
                desc.text = model.description
                author.text = model.author
            }
        }
    }

    companion object {
        private const val ARG_QUERY = "search_query"
        private const val ARG_RESULT_TITLE = "result_title"
        private const val ARG_SEARCH_TITLE = "search_title"
        private const val ARG_SOURCE_TAB = "source_tab"
    }

    /**
     * 跳转到用户作品播放器（复用个人主页组件）
     * 使用搜索结果过滤后的视频列表作为播放源
     */
    private fun navigateToVideoViewer(searchQuery: String, resultTitle: String) {
        val videos = viewModel.uiState.value.videoList
        if (videos.isEmpty()) return

        val targetIndex = videos.indexOfFirst { it.title == resultTitle }.let {
            if (it >= 0) it else 0
        }

        // 搜索来源：用目标视频作者作为 userId（若为空则回退作者名，再不行用空字符串）
        val targetVideo = videos.getOrNull(targetIndex)
        val userId = when {
            targetVideo?.authorId?.isNotBlank() == true -> targetVideo.authorId
            targetVideo?.authorName?.isNotBlank() == true -> targetVideo.authorName
            else -> ""
        }

        val args = bundleOf(
            "user_id" to userId,
            "initial_index" to targetIndex,
            "video_list" to ArrayList(videos),
            "search_title" to "“$searchQuery” 的搜索结果",
            "source_tab" to "search"
        )

        val navController = findNavController()
        val context = requireContext()
        val actionId = NavigationHelper.getResourceId(
            context,
            NavigationIds.ACTION_SEARCH_RESULT_TO_USER_WORKS_VIEWER
        )
        if (actionId != 0) {
            navController.navigate(actionId, args)
        }
    }
}

