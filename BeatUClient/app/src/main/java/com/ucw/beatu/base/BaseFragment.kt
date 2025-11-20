package com.ucw.beatu.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding

/**
 * Base Fragment 类
 * 提供通用的 Fragment 功能
 */
abstract class BaseFragment<VB : ViewBinding, VM : ViewModel> : Fragment() {
    
    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding is null")
    
    protected lateinit var viewModel: VM
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = getViewModel()
        initViews()
        observeViewModel()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * 获取 ViewBinding
     */
    protected abstract fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VB
    
    /**
     * 获取 ViewModel
     */
    protected abstract fun getViewModel(): VM
    
    /**
     * 初始化视图
     */
    protected open fun initViews() {
        // 子类可以重写此方法进行视图初始化
    }
    
    /**
     * 观察 ViewModel 数据变化
     */
    protected open fun observeViewModel() {
        // 子类可以重写此方法观察 ViewModel 的 LiveData/StateFlow
    }
}

