package com.ucw.beatu.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding

/**
 * Base Activity 类
 * 提供通用的 Activity 功能
 */
abstract class BaseActivity<VB : ViewBinding, VM : ViewModel> : AppCompatActivity() {
    
    protected lateinit var binding: VB
    protected lateinit var viewModel: VM
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding.root)
        viewModel = getViewModel()
        initViews()
        observeViewModel()
    }
    
    /**
     * 获取 ViewBinding
     */
    protected abstract fun getViewBinding(): VB
    
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

