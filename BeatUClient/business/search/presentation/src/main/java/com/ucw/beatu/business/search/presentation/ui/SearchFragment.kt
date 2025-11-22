package com.ucw.beatu.business.search.presentation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucw.beatu.business.search.presentation.R
import com.ucw.beatu.business.search.presentation.ui.widget.FlowLayout

/**
 * 搜索页面Fragment
 * 提供搜索框和搜索结果列表（抖音风格）
 */
class SearchFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: View
    private lateinit var cancelButton: TextView
    private lateinit var scrollBeforeSearch: View
    private lateinit var llSearchHistory: FlowLayout
    private lateinit var llHotSearch: FlowLayout
    private lateinit var rvSearchSuggestions: RecyclerView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var emptyState: View
    
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter
    private lateinit var searchResultAdapter: SearchResultGridAdapter

    // 搜索状态
    private enum class SearchState {
        BEFORE_SEARCH,      // 搜索前（显示历史和热门）
        TYPING,             // 输入中（显示搜索建议）
        SEARCHING,          // 搜索中
        SEARCH_RESULT       // 搜索结果
    }
    
    private var currentState = SearchState.BEFORE_SEARCH

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        initSearchBox(view)
        initSearchHistory()
        initHotSearch()
        initSearchSuggestions()
        initSearchResults()
    }

    /**
     * 初始化视图
     */
    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.et_search)
        clearButton = view.findViewById(R.id.btn_clear)
        cancelButton = view.findViewById(R.id.tv_cancel)
        scrollBeforeSearch = view.findViewById(R.id.scroll_before_search)
        rvSearchSuggestions = view.findViewById(R.id.rv_search_suggestions)
        rvSearchResults = view.findViewById(R.id.rv_search_results)
        emptyState = view.findViewById(R.id.empty_state)
    }

    /**
     * 初始化搜索框
     */
    private fun initSearchBox(view: View) {
        // 搜索框文本变化监听
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                clearButton.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                
                if (query.isEmpty()) {
                    switchToState(SearchState.BEFORE_SEARCH)
                } else {
                    switchToState(SearchState.TYPING)
                    // TODO: 实时搜索建议
                    updateSearchSuggestions(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 清除按钮点击
        clearButton.setOnClickListener {
            searchEditText.text?.clear()
            clearButton.visibility = View.GONE
            switchToState(SearchState.BEFORE_SEARCH)
        }
        
        // 取消按钮点击
        cancelButton.setOnClickListener {
            searchEditText.text?.clear()
            activity?.onBackPressed()
        }
        
        // 搜索框回车键监听
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.text.toString())
                true
            } else {
                false
            }
        }
        
        // 返回按钮点击
        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    /**
     * 初始化搜索历史
     */
    private fun initSearchHistory() {
        val historyItems = getMockSearchHistory()
        llSearchHistory.removeAllViews()
        
        historyItems.forEach { tag ->
            val tagView = createTagView(tag) {
                searchEditText.setText(tag)
                performSearch(tag)
            }
            llSearchHistory.addView(tagView)
        }
    }

    /**
     * 初始化热门搜索
     */
    private fun initHotSearch() {
        val hotItems = getMockHotSearch()
        llHotSearch.removeAllViews()
        
        hotItems.forEach { tag ->
            val tagView = createTagView(tag) {
                searchEditText.setText(tag)
                performSearch(tag)
            }
            llHotSearch.addView(tagView)
        }
    }

    /**
     * 创建标签视图（流式布局）
     */
    private fun createTagView(tag: String, onClick: () -> Unit): TextView {
        val tagView = LayoutInflater.from(context)
            .inflate(R.layout.item_search_tag, llSearchHistory, false) as TextView
        tagView.text = tag
        tagView.setOnClickListener { onClick() }
        return tagView
    }

    /**
     * 初始化搜索建议
     */
    private fun initSearchSuggestions() {
        val layoutManager = LinearLayoutManager(context)
        rvSearchSuggestions.layoutManager = layoutManager
        
        searchSuggestionAdapter = SearchSuggestionAdapter(emptyList()) { suggestion ->
            searchEditText.setText(suggestion)
            performSearch(suggestion)
        }
        rvSearchSuggestions.adapter = searchSuggestionAdapter
    }

    /**
     * 初始化搜索结果
     */
    private fun initSearchResults() {
        val spanCount = 2
        val layoutManager = GridLayoutManager(context, spanCount)
        rvSearchResults.layoutManager = layoutManager
        
        searchResultAdapter = SearchResultGridAdapter(emptyList()) { item ->
            // TODO: 跳转到视频详情
        }
        rvSearchResults.adapter = searchResultAdapter
    }

    /**
     * 更新搜索建议
     */
    private fun updateSearchSuggestions(query: String) {
        val suggestions = getMockSearchSuggestions(query)
        searchSuggestionAdapter.updateSuggestions(suggestions)
    }

    /**
     * 执行搜索
     */
    private fun performSearch(query: String) {
        if (query.isBlank()) {
            return
        }
        
        // 隐藏键盘
        val imm = activity?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        
        switchToState(SearchState.SEARCHING)
        
        // TODO: 调用 ViewModel 或 Repository 执行搜索
        // 模拟搜索延迟
        searchEditText.postDelayed({
            val results = getMockSearchResults()
            if (results.isEmpty()) {
                switchToState(SearchState.SEARCH_RESULT)
                emptyState.visibility = View.VISIBLE
            } else {
                searchResultAdapter.updateResults(results)
                switchToState(SearchState.SEARCH_RESULT)
                emptyState.visibility = View.GONE
            }
        }, 500)
    }

    /**
     * 切换搜索状态
     */
    private fun switchToState(state: SearchState) {
        currentState = state
        
        when (state) {
            SearchState.BEFORE_SEARCH -> {
                scrollBeforeSearch.visibility = View.VISIBLE
                rvSearchSuggestions.visibility = View.GONE
                rvSearchResults.visibility = View.GONE
                emptyState.visibility = View.GONE
            }
            SearchState.TYPING -> {
                scrollBeforeSearch.visibility = View.GONE
                rvSearchSuggestions.visibility = View.VISIBLE
                rvSearchResults.visibility = View.GONE
                emptyState.visibility = View.GONE
            }
            SearchState.SEARCHING -> {
                scrollBeforeSearch.visibility = View.GONE
                rvSearchSuggestions.visibility = View.GONE
                rvSearchResults.visibility = View.GONE
                emptyState.visibility = View.GONE
            }
            SearchState.SEARCH_RESULT -> {
                scrollBeforeSearch.visibility = View.GONE
                rvSearchSuggestions.visibility = View.GONE
                rvSearchResults.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 获取假数据
     */
    private fun getMockSearchHistory(): List<String> {
        return listOf("搞笑视频", "美食", "旅行", "音乐")
    }

    private fun getMockHotSearch(): List<String> {
        return listOf("热门话题1", "热门话题2", "热门话题3", "热门话题4", "热门话题5")
    }

    private fun getMockSearchSuggestions(query: String): List<String> {
        return listOf(
            "$query 视频",
            "$query 用户",
            "$query 音乐",
            "$query 搞笑"
        )
    }

    private fun getMockSearchResults(): List<SearchResultItem> {
        return listOf(
            SearchResultItem("视频1", "00:30", "https://example.com/thumb1.jpg"),
            SearchResultItem("视频2", "01:20", "https://example.com/thumb2.jpg"),
            SearchResultItem("视频3", "00:45", "https://example.com/thumb3.jpg"),
            SearchResultItem("视频4", "02:10", "https://example.com/thumb4.jpg"),
            SearchResultItem("视频5", "00:55", "https://example.com/thumb5.jpg"),
            SearchResultItem("视频6", "01:30", "https://example.com/thumb6.jpg"),
        )
    }

    /**
     * 搜索结果数据模型
     */
    data class SearchResultItem(
        val title: String,
        val duration: String,
        val thumbnailUrl: String
    )

    /**
     * 搜索建议适配器
     */
    private class SearchSuggestionAdapter(
        private var suggestions: List<String>,
        private val onSuggestionClick: (String) -> Unit
    ) : RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder>() {

        fun updateSuggestions(newSuggestions: List<String>) {
            suggestions = newSuggestions
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_suggestion, parent, false)
            return SuggestionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
            holder.bind(suggestions[position])
        }

        override fun getItemCount(): Int = suggestions.size

        inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvSuggestion: TextView = itemView.findViewById(R.id.tv_suggestion)
            
            fun bind(suggestion: String) {
                tvSuggestion.text = suggestion
                itemView.setOnClickListener {
                    onSuggestionClick(suggestion)
                }
            }
        }
    }

    /**
     * 搜索结果网格适配器
     */
    private class SearchResultGridAdapter(
        private var results: List<SearchResultItem>,
        private val onItemClick: (SearchResultItem) -> Unit
    ) : RecyclerView.Adapter<SearchResultGridAdapter.ResultViewHolder>() {

        fun updateResults(newResults: List<SearchResultItem>) {
            results = newResults
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result_grid, parent, false)
            return ResultViewHolder(view)
        }

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            holder.bind(results[position])
        }

        override fun getItemCount(): Int = results.size

        inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
            private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
            
            fun bind(item: SearchResultItem) {
                tvDuration.text = item.duration
                // TODO: 加载缩略图
                itemView.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }
}