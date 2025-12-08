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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucw.beatu.business.search.presentation.R
import com.ucw.beatu.business.search.presentation.ui.widget.FlowLayout
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.dao.SearchHistoryDao
import com.ucw.beatu.shared.database.entity.SearchHistoryEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索页面Fragment
 * 提供搜索框和搜索结果列表（抖音风格）
 * 
 * 功能：
 * 1. 显示搜索历史（从 beatu_search_history 表读取，最多 5 条，LRU 策略）
 * 2. 保存搜索历史（搜索时自动保存）
 * 3. 清空搜索历史
 * 4. 点击历史记录进行搜索
 */
@AndroidEntryPoint
class SearchFragment : Fragment() {

    @Inject
    lateinit var database: BeatUDatabase
    
    private lateinit var searchHistoryDao: SearchHistoryDao
    
    private lateinit var searchEditText: EditText
    private lateinit var clearButton: View
    private lateinit var searchButton: TextView
    private lateinit var backButton: View
    private lateinit var scrollBeforeSearch: View
    private lateinit var llHotSearch: FlowLayout
    private lateinit var llHistorySearch: FlowLayout
    private lateinit var tvClearHistory: TextView
    private lateinit var rvSearchSuggestions: RecyclerView
    
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter

    // ✅ 当前用户ID，根据需求文档，userId "BEATU" 对应的 userName 也是 "BEATU"
    private val currentUserId: String = "BEATU"

    // 搜索状态
    private enum class SearchState {
        BEFORE_SEARCH,      // 搜索前（显示历史和热门）
        TYPING              // 输入中（显示搜索建议）
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
        
        // ✅ 初始化搜索历史 DAO
        searchHistoryDao = database.searchHistoryDao()
        
        initViews(view)
        // 先初始化列表和适配器，再绑定 TextWatcher，避免 NPE
        initSearchSuggestions()
        initSearchBox(view)
        initHotSearch()
        initSearchHistory()
    }

    /**
     * 初始化视图
     */
    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.et_search)
        clearButton = view.findViewById(R.id.btn_clear)
        backButton = view.findViewById(R.id.btn_back)
        searchButton = view.findViewById(R.id.tv_search)
        scrollBeforeSearch = view.findViewById(R.id.scroll_before_search)
        llHotSearch = view.findViewById(R.id.ll_hot_search)
        llHistorySearch = view.findViewById(R.id.ll_history_search)
        tvClearHistory = view.findViewById(R.id.tv_clear_history)
        rvSearchSuggestions = view.findViewById(R.id.rv_search_suggestions)
        clearButton.isVisible = searchEditText.text?.isNotEmpty() == true
        
        // ✅ 清空历史按钮点击事件
        tvClearHistory.setOnClickListener {
            clearSearchHistory()
        }
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
                clearButton.isVisible = query.isNotEmpty()
                
                if (query.isEmpty()) {
                    switchToState(SearchState.BEFORE_SEARCH)
                } else {
                    switchToState(SearchState.TYPING)
                    updateSearchSuggestions(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 清除按钮点击
        clearButton.setOnClickListener {
            searchEditText.text?.clear()
            switchToState(SearchState.BEFORE_SEARCH)
        }
        // 搜索按钮点击
        searchButton.setOnClickListener {
            performSearch(searchEditText.text.toString())
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
        backButton.setOnClickListener {
            searchEditText.text?.clear()
            if (!findNavController().popBackStack()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }


    /**
     * 返回首页 Feed
     */
    private fun navigateBackToFeed() {
        val navController = findNavController()
        runCatching {
            NavigationHelper.navigateByStringId(
                navController,
                NavigationIds.ACTION_SEARCH_TO_FEED,
                requireContext()
            )
        }.onFailure {
            if (!navController.popBackStack()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
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
     * 创建一个热门搜索标签 View
     */
    private fun createTagView(text: String, onClick: () -> Unit): View {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.item_search_tag, llHotSearch, false) as TextView
        view.text = text
        view.setOnClickListener { onClick() }
        return view
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
     * 更新搜索建议
     */
    private fun updateSearchSuggestions(query: String) {
        if (!::searchSuggestionAdapter.isInitialized) return
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
        
        // ✅ 保存搜索历史
        saveSearchHistory(query)
        
        // 隐藏键盘
        val imm = activity?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        
        navigateToSearchResult(query)
    }
    
    /**
     * ✅ 初始化搜索历史
     */
    private fun initSearchHistory() {
        // 观察搜索历史变化
        lifecycleScope.launch {
            searchHistoryDao.observeSearchHistory(currentUserId).collect { historyList ->
                updateSearchHistoryUI(historyList)
            }
        }
    }
    
    /**
     * ✅ 更新搜索历史UI
     */
    private fun updateSearchHistoryUI(historyList: List<SearchHistoryEntity>) {
        llHistorySearch.removeAllViews()
        
        if (historyList.isEmpty()) {
            // 如果没有历史记录，隐藏历史搜索区域
            llHistorySearch.isVisible = false
            view?.findViewById<TextView>(R.id.tv_history_search_title)?.isVisible = false
            tvClearHistory.isVisible = false
            return
        }
        
        // 显示历史搜索区域
        llHistorySearch.isVisible = true
        view?.findViewById<TextView>(R.id.tv_history_search_title)?.isVisible = true
        tvClearHistory.isVisible = true
        
        // 创建历史记录标签
        historyList.forEach { history ->
            val tagView = createTagView(history.query) {
                searchEditText.setText(history.query)
                performSearch(history.query)
            }
            llHistorySearch.addView(tagView)
        }
    }
    
    /**
     * ✅ 保存搜索历史
     */
    private fun saveSearchHistory(query: String) {
        lifecycleScope.launch {
            try {
                val history = SearchHistoryEntity(
                    query = query.trim(),
                    userId = currentUserId,
                    createdAt = System.currentTimeMillis()
                )
                searchHistoryDao.insert(history)
                // 删除超出限制的旧记录（保留最新的 5 条，LRU 策略）
                // LRU 策略：删除超出限制的旧记录（保留最新的 5 条）
                while (searchHistoryDao.getHistoryCount(currentUserId) > 5) {
                    searchHistoryDao.deleteOldestRecord(currentUserId)
                }
            } catch (e: Exception) {
                // 静默失败，不影响搜索功能
                android.util.Log.e("SearchFragment", "保存搜索历史失败", e)
            }
        }
    }
    
    /**
     * ✅ 清空搜索历史
     */
    private fun clearSearchHistory() {
        lifecycleScope.launch {
            try {
                searchHistoryDao.deleteByUserId(currentUserId)
            } catch (e: Exception) {
                android.util.Log.e("SearchFragment", "清空搜索历史失败", e)
            }
        }
    }

    /**
     * 导航到常规搜索结果
     */
    private fun navigateToSearchResult(query: String) {
        runCatching {
            val args = bundleOf("search_query" to query)
            val navController = findNavController()
            val context = requireContext()
            val actionId = NavigationHelper.getResourceId(
                context,
                NavigationIds.ACTION_SEARCH_TO_SEARCH_RESULT
            )
            if (actionId != 0) {
                navController.navigate(actionId, args)
            } else {
                navController.navigateUp()
            }
        }.onFailure {
            findNavController().navigateUp()
        }
    }


    /**
     * 切换搜索状态
     */
    private fun switchToState(state: SearchState) {
        currentState = state
        
        scrollBeforeSearch.isVisible = state == SearchState.BEFORE_SEARCH
        rvSearchSuggestions.isVisible = state == SearchState.TYPING
    }

    private fun getMockHotSearch(): List<String> {
        return listOf("高清", "横屏", "奇幻冒险", "视频")
    }

    private fun getMockSearchSuggestions(query: String): List<String> {
        return listOf(
            "$query 视频",
            "$query 用户",
            "$query 音乐",
            "$query 搞笑"
        )
    }

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
}