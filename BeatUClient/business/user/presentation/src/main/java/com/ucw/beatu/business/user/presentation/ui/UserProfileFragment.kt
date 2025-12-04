package com.ucw.beatu.business.user.presentation.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.presentation.R
import com.ucw.beatu.business.user.presentation.ui.adapter.UserWorkUiModel
import com.ucw.beatu.business.user.presentation.ui.adapter.UserWorksAdapter
import com.ucw.beatu.business.user.presentation.ui.UserWorksViewerFragment
import com.ucw.beatu.business.user.presentation.viewmodel.UserProfileViewModel
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.router.UserProfileVideoClickHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 用户主页Fragment
 * 显示用户头像、昵称、作品列表等信息
 */
@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private val viewModel: UserProfileViewModel by viewModels()

    // UI 元素
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvLikesCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvWorks: RecyclerView
    private lateinit var btnFollow: com.google.android.material.button.MaterialButton
    
    // 头像上传相关
    private var currentAvatarFile: File? = null
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelection(it) } ?: run {
            Log.w(TAG, "Image selection cancelled or failed")
            Toast.makeText(requireContext(), "未选择图片", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 标签按钮
    private lateinit var tabWorks: TextView
    private lateinit var tabCollections: TextView
    private lateinit var tabLikes: TextView
    private lateinit var tabHistory: TextView
    
    // 当前选中的标签
    private var selectedTab: TextView? = null
    
    private val worksAdapter by lazy {
        UserWorksAdapter { work -> navigateToUserWorksViewer(work.id) }
    }
    private var latestUser: User? = null
    private var latestUserWorks: List<UserWork> = emptyList()

    // 用户名（从参数获取，默认为当前用户）
    private val userName: String
        get() = arguments?.getString(ARG_USER_NAME) ?: "current_user"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 UI 元素
        initViews(view)

        // 设置头像圆角裁剪
        setupAvatarRoundCorner(view)

        // 只读模式下禁用编辑功能
        if (!isReadOnly) {
            // 设置头像点击上传
            setupAvatarUpload()

            // 设置名字和名言的点击编辑功能
            setupEditableFields()
            // 非只读模式：隐藏关注按钮（自己的主页不需要关注按钮）
            btnFollow.visibility = View.GONE
        } else {
            // 只读模式：禁用头像、名称、名言的点击
            ivAvatar.isClickable = false
            tvUsername.isClickable = false
            tvBio.isClickable = false
            // 只读模式：显示关注按钮
            btnFollow.visibility = View.VISIBLE
            btnFollow.isClickable = true
            setupFollowButton()
            Log.d(TAG, "Follow button initialized in read-only mode")
        }

        // 初始化标签切换
        initTabs(view)

        // 只读模式下调整文本颜色为黑色，以便在白色背景上可见
        if (isReadOnly) {
            applyReadOnlyTextColors(view)
        }

        // 初始化作品列表
        initWorksList()

        // 观察 ViewModel 数据
        observeViewModel()

        // 初始化并加载用户数据（使用用户名或用户ID）
        Log.d(TAG, "Loading user with userName: $userName")
        viewModel.loadUser(userName)
        // 默认加载"作品"标签的数据
        // 如果 userName 是 "current_user"，需要等待用户加载完成后获取实际用户名
        // 否则直接使用 userName 作为 authorName
        if (userName == "current_user") {
            // 等待用户加载完成后再加载作品
            // 作品加载会在 observeViewModel 中的用户加载完成后触发
        } else {
            viewModel.switchTab(UserProfileViewModel.TabType.WORKS, authorName = userName, currentUserId = "current_user")
        }
        
        // 只读模式下，开始观察关注同步结果
        if (isReadOnly) {
            viewModel.startObservingFollowSyncResult()
        }
    }

    /**
     * 初始化作品列表
     */
    private fun initWorksList() {
        rvWorks.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = worksAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * 初始化 UI 元素
     */
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_user_profile)
        ivAvatar = view.findViewById(R.id.iv_avatar)
        tvUsername = view.findViewById(R.id.tv_username)
        tvBio = view.findViewById(R.id.tv_bio)
        tvLikesCount = view.findViewById(R.id.tv_likes_count)
        tvFollowingCount = view.findViewById(R.id.tv_following_count)
        tvFollowersCount = view.findViewById(R.id.tv_followers_count)
        rvWorks = view.findViewById(R.id.rv_works)
        btnFollow = view.findViewById(R.id.btn_follow)

        // 只读模式下隐藏返回按钮
        if (isReadOnly) {
            // 完全隐藏 toolbar，避免占用空间
            toolbar.visibility = View.GONE
            // 设置背景为黑色，以便在Dialog中正常显示
            view.setBackgroundColor(android.graphics.Color.BLACK)
            // 移除 fitsSystemWindows，避免系统窗口间距
            val rootLayout = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(com.ucw.beatu.business.user.presentation.R.id.root_layout)
            rootLayout?.fitsSystemWindows = false
        } else {
            toolbar.setNavigationOnClickListener {
                // 优先走导航栈返回，兜底走 Activity 的 onBackPressedDispatcher
                val navController = runCatching { findNavController() }.getOrNull()
                if (navController != null && navController.popBackStack()) {
                    return@setNavigationOnClickListener
                }
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /**
    * 设置名字和名言的点击编辑功能
    */
    private fun setupEditableFields() {
        // 名字点击编辑
        tvUsername.setOnClickListener {
            android.util.Log.d("UserProfileFragment", "用户名被点击")
            showEditNameDialog()
        }
        
        // 名言点击编辑
        tvBio.setOnClickListener {
            android.util.Log.d("UserProfileFragment", "简介被点击")
            showEditBioDialog()
        }
        
        // 确保 TextView 可点击
        tvUsername.isClickable = true
        tvUsername.isFocusable = true
        tvBio.isClickable = true
        tvBio.isFocusable = true
    }
    /**
    * 显示编辑名字对话框
    */
    private fun showEditNameDialog() {
        val currentName = tvUsername.text.toString()
        val input = android.widget.EditText(requireContext()).apply {
            setText(currentName)
            setSelection(currentName.length)
            hint = "请输入名字"
            textSize = 16f
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("编辑名字")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        val currentUserId = latestUser?.id ?: return@setPositiveButton
                        viewModel.updateName(currentUserId, newName)
                    }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
    * 显示编辑名言对话框
    */
    private fun showEditBioDialog() {
        val currentBio = tvBio.text.toString()
        val input = android.widget.EditText(requireContext()).apply {
            setText(currentBio)
            setSelection(currentBio.length)
            hint = "请输入一句话介绍自己"
            textSize = 14f
            minLines = 2
            maxLines = 4
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("编辑简介")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val newBio = input.text.toString().trim()
                val currentUserId = latestUser?.id ?: return@setPositiveButton
                viewModel.updateBio(currentUserId, newBio.ifEmpty { null })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 观察 ViewModel 数据
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { user ->
                        if (user != null) {
                            Log.d(TAG, "User loaded: ${user.name}, id: ${user.id}")
                            latestUser = user
                            updateUserInfo(user)
                            // 用户加载完成后，更新标签数据
                            // 使用用户的实际名称作为 authorName（因为作品是通过 authorName 查询的）
                            val authorName = if (userName == "current_user") user.name else userName
                            viewModel.switchTab(UserProfileViewModel.TabType.WORKS, authorName = authorName, currentUserId = user.id)
                            // 开始观察关注状态（只读模式）
                            if (isReadOnly) {
                                val currentUserId = "current_user" // TODO: 从用户会话获取当前用户ID
                                viewModel.startObservingFollowStatus(currentUserId, user.id)
                            }
                        } else {
                            Log.w(TAG, "User is null, userName: $userName")
                        }
                    }
                }
                launch {
                    viewModel.userWorks.collect { works ->
                        latestUserWorks = works
                        worksAdapter.submitList(works.map { it.toUiModel() })
                    }
                }
                launch {
                    // 观察关注状态（从本地数据库，通过 ViewModel StateFlow）
                    if (isReadOnly) {
                        viewModel.isFollowing.collect { isFollowing ->
                            updateFollowButton(isFollowing)
                        }
                    }
                }
                launch {
                    // 观察关注操作错误
                    if (isReadOnly) {
                        viewModel.followOperationError.collect { error ->
                            error?.let {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                                viewModel.clearFollowOperationError()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置关注按钮
     */
    private fun setupFollowButton() {
        btnFollow.setOnClickListener {
            val targetUserId = latestUser?.id
            if (targetUserId == null) {
                Log.e(TAG, "Cannot follow/unfollow: user ID is null")
                Toast.makeText(requireContext(), "无法获取用户信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUserId = "current_user" // TODO: 从用户会话获取当前用户ID
            Log.d(TAG, "Follow button clicked, userName: $userName, userId: $targetUserId, currentUserId: $currentUserId")
            val isFollowing = viewModel.isFollowing.value ?: false
            Log.d(TAG, "Current follow status: $isFollowing")
            if (isFollowing) {
                Log.d(TAG, "Unfollowing user: $targetUserId")
                viewModel.unfollowUser(targetUserId, currentUserId)
            } else {
                Log.d(TAG, "Following user: $targetUserId")
                viewModel.followUser(targetUserId, currentUserId)
            }
        }
        // 确保按钮可以接收点击事件
        btnFollow.isClickable = true
        btnFollow.isFocusable = true
        btnFollow.isFocusableInTouchMode = true
        btnFollow.isEnabled = true
        // 确保按钮在最上层，不被其他视图遮挡
        btnFollow.bringToFront()
        Log.d(TAG, "Follow button setup completed, visibility: ${btnFollow.visibility}, clickable: ${btnFollow.isClickable}, enabled: ${btnFollow.isEnabled}")
    }

    /**
     * 更新关注按钮状态
     */
    private fun updateFollowButton(isFollowing: Boolean?) {
        when (isFollowing) {
            true -> {
                btnFollow.text = "取消关注"
                btnFollow.isEnabled = true
                btnFollow.isClickable = true
                btnFollow.alpha = 1.0f
            }
            false -> {
                btnFollow.text = "关注"
                btnFollow.isEnabled = true
                btnFollow.isClickable = true
                btnFollow.alpha = 1.0f
            }
            null -> {
                btnFollow.text = "关注"
                btnFollow.isEnabled = false
                btnFollow.isClickable = false
                btnFollow.alpha = 0.5f
            }
        }
        // 确保按钮可以接收点击事件
        btnFollow.bringToFront()
        Log.d(TAG, "Follow button updated: text=${btnFollow.text}, enabled=${btnFollow.isEnabled}, clickable=${btnFollow.isClickable}")
    }

    /**
     * 更新用户信息 UI
     */
    private fun updateUserInfo(user: User) {
        tvUsername.text = user.name
        tvBio.text = user.bio ?: ""
        
        // 格式化数字显示
        tvLikesCount.text = formatCount(user.likesCount)
        tvFollowingCount.text = formatCount(user.followingCount)
        tvFollowersCount.text = formatCount(user.followersCount)
        
        // 加载头像
        user.avatarUrl?.let { avatarPath ->
            loadAvatar(avatarPath)
        }
        
        // 确保点击监听器仍然有效（数据更新后重新设置）
        tvUsername.isClickable = true
        tvUsername.isFocusable = true
        tvBio.isClickable = true
        tvBio.isFocusable = true
    }
    
    /**
     * 加载头像
     */
    private fun loadAvatar(avatarPath: String) {
        try {
            val file = File(avatarPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(avatarPath)
                ivAvatar.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 格式化数字显示（如：5.6万）
     */
    private fun formatCount(count: Long): String {
        return when {
            count >= 100000000 -> String.format("%.1f亿", count / 100000000.0)
            count >= 10000 -> String.format("%.1f万", count / 10000.0)
            count >= 1000 -> String.format("%.1f千", count / 1000.0)
            else -> count.toString()
        }
    }

    /**
     * 初始化标签切换
     */
    private fun initTabs(view: View) {
        tabWorks = view.findViewById(R.id.tab_works)
        tabCollections = view.findViewById(R.id.tab_collections)
        tabLikes = view.findViewById(R.id.tab_likes)
        tabHistory = view.findViewById(R.id.tab_history)

        // 只读模式（弹窗）下，只保留“作品”一个选项，其它标签隐藏
        if (isReadOnly) {
            tabCollections.visibility = View.GONE
            tabLikes.visibility = View.GONE
            tabHistory.visibility = View.GONE

            // 让“作品”标签在父布局中居中显示
            (tabWorks.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { lp ->
                lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                lp.startToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                lp.endToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                tabWorks.layoutParams = lp
            }
        } else {
            // 非只读模式下，显式设置其他标签为未选中状态
            updateTabState(tabCollections, false)
            updateTabState(tabLikes, false)
            updateTabState(tabHistory, false)
        }

        // 默认选中"作品"
        selectedTab = tabWorks
        updateTabState(tabWorks, true)
        
        // 设置点击监听（只读模式下，其它标签不可见，不会被点击）
        tabWorks.setOnClickListener { switchTab(it as TextView, UserProfileViewModel.TabType.WORKS) }
        tabCollections.setOnClickListener { switchTab(it as TextView, UserProfileViewModel.TabType.COLLECTIONS) }
        tabLikes.setOnClickListener { switchTab(it as TextView, UserProfileViewModel.TabType.LIKES) }
        tabHistory.setOnClickListener { switchTab(it as TextView, UserProfileViewModel.TabType.HISTORY) }
    }
    
    /**
     * 应用只读模式下的文本颜色（黑色背景，白色文本）
     */
    private fun applyReadOnlyTextColors(view: View) {
        // 调整文本颜色为白色，以便在黑色背景上可见
        tvUsername.setTextColor(android.graphics.Color.WHITE)
        tvBio.setTextColor(android.graphics.Color.parseColor("#80FFFFFF")) // 半透明白色
        tvLikesCount.setTextColor(android.graphics.Color.WHITE)
        tvFollowingCount.setTextColor(android.graphics.Color.WHITE)
        tvFollowersCount.setTextColor(android.graphics.Color.WHITE)
        // 调整标签文本颜色
        view.findViewById<TextView>(R.id.tv_likes_label)?.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
        view.findViewById<TextView>(R.id.tv_following_label)?.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
        view.findViewById<TextView>(R.id.tv_followers_label)?.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
        // 调整标签按钮颜色（标签按钮已在initTabs中初始化）
        tabWorks.setTextColor(android.graphics.Color.WHITE)
        tabCollections.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
        tabLikes.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
        tabHistory.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"))
    }
    
    /**
     * 切换标签
     */
    private fun switchTab(tab: TextView, tabType: UserProfileViewModel.TabType) {
        if (selectedTab == tab) return

        // 更新之前选中的标签
        selectedTab?.let { updateTabState(it, false) }

        // 更新新选中的标签
        selectedTab = tab
        updateTabState(tab, true)

        // 切换 ViewModel 的数据源
        // authorName用于作品查询，currentUserId用于收藏、点赞、历史记录查询
        val currentUserId = latestUser?.id ?: "current_user"
        // 如果 userName 是 "current_user"，使用用户的实际名称作为 authorName
        val authorName = if (userName == "current_user" && latestUser != null) {
            latestUser!!.name
        } else {
            userName
        }
        viewModel.switchTab(tabType, authorName = authorName, currentUserId = currentUserId)
    }
    
    /**
     * 更新标签状态
     */
    private fun updateTabState(tab: TextView, isSelected: Boolean) {
        if (isSelected) {
            tab.setBackgroundColor(0xFFFF0000.toInt()) // 红色
            tab.setTextColor(0xFFFFFFFF.toInt())       // 白字
        } else {
            tab.setBackgroundColor(0x00000000.toInt()) // 透明背景
            tab.setTextColor(0x80FFFFFF.toInt())       // 白色 50% 透明
        }
    }

    private fun UserWork.toUiModel(): UserWorkUiModel = UserWorkUiModel(
        id = id,
        thumbnailUrl = coverUrl,
        playCount = viewCount,
        playUrl = playUrl,
        title = title
    )

    /**
     * 设置头像圆角裁剪（使用 post 解决宽高=0 的问题）
     */
    private fun setupAvatarRoundCorner(view: View) {
        ivAvatar.post {
            val size = ivAvatar.width.coerceAtMost(ivAvatar.height)
            ivAvatar.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setOval(0, 0, size, size)
                }
            }
            ivAvatar.clipToOutline = true
        }
    }

    /**
     * 设置头像点击上传功能
     */
    private fun setupAvatarUpload() {
        ivAvatar.setOnClickListener {
            openImagePicker()
        }
    }

    /**
     * 打开图片选择器
     */
    private fun openImagePicker() {
        try {
            pickImageLauncher.launch("image/*")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open image picker", e)
            Toast.makeText(requireContext(), "无法打开图片选择器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理图片选择结果
     */
    private fun handleImageSelection(uri: Uri) {
        try {
            Log.d(TAG, "Handling image selection: $uri")
            
            // 检查用户ID是否存在
            val currentUserId = latestUser?.id
            if (currentUserId == null) {
                Log.e(TAG, "Cannot update avatar: currentUserId is null")
                Toast.makeText(requireContext(), "无法更新头像：用户信息未加载", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 读取图片
            val inputStream = requireContext().contentResolver.openInputStream(uri)
                ?: run {
                    Log.e(TAG, "Failed to open input stream for URI: $uri")
                    Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show()
                    return
                }
            
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                Toast.makeText(requireContext(), "无法解析图片", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Bitmap decoded successfully: ${bitmap.width}x${bitmap.height}")
            
            // 保存到本地文件
            val avatarFile = saveAvatarToLocal(bitmap)
            if (avatarFile != null) {
                Log.d(TAG, "Avatar saved to: ${avatarFile.absolutePath}")
                
                // 更新数据库
                viewModel.updateAvatar(currentUserId, avatarFile.absolutePath)
                
                // 更新 UI
                ivAvatar.setImageBitmap(bitmap)
                
                Toast.makeText(requireContext(), "头像更新成功", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "Failed to save avatar to local file")
                Toast.makeText(requireContext(), "保存头像失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling image selection", e)
            Toast.makeText(requireContext(), "处理图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 保存头像到本地文件
     */
    private fun saveAvatarToLocal(bitmap: Bitmap): File? {
        return try {
            // 创建头像目录
            val avatarDir = File(requireContext().filesDir, "avatars")
            if (!avatarDir.exists()) {
                val created = avatarDir.mkdirs()
                Log.d(TAG, "Created avatar directory: $created, path: ${avatarDir.absolutePath}")
            }

            // 创建头像文件
            val currentUserId = latestUser?.id ?: "current_user"
            val avatarFile = File(avatarDir, "avatar_${currentUserId}.jpg")
            
            // 压缩并保存
            FileOutputStream(avatarFile).use { outputStream ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                if (!compressed) {
                    Log.e(TAG, "Failed to compress bitmap")
                    return null
                }
                outputStream.flush()
            }
            
            // 验证文件是否创建成功
            if (!avatarFile.exists() || avatarFile.length() == 0L) {
                Log.e(TAG, "Avatar file not created or empty: ${avatarFile.absolutePath}")
                return null
            }
            
            Log.d(TAG, "Avatar file saved successfully: ${avatarFile.absolutePath}, size: ${avatarFile.length()} bytes")
            avatarFile
        } catch (e: IOException) {
            Log.e(TAG, "IOException while saving avatar", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while saving avatar", e)
            null
        }
    }

    private fun navigateToUserWorksViewer(selectedWorkId: String) {
        val works = latestUserWorks
        if (works.isEmpty()) {
            Toast.makeText(requireContext(), "暂无可播放的视频", Toast.LENGTH_SHORT).show()
            return
        }
        
        val authorName = latestUser?.name ?: "BeatU 用户"
        val videoItems = ArrayList(works.map { it.toVideoItem(authorName) })
        val initialIndex = works.indexOfFirst { it.id == selectedWorkId }.let { index ->
            if (index == -1) 0 else index
        }
        
        // 在只读模式下（从DialogFragment中显示），通过接口回调父 Fragment；否则使用findNavController()导航
        if (isReadOnly) {
            val host = parentFragment as? UserProfileVideoClickHost
            if (host != null) {
                val currentUserId = latestUser?.id ?: "current_user"
                host.onUserWorkClicked(currentUserId, authorName, videoItems, initialIndex)
                Log.d(TAG, "Notified parent fragment via UserProfileVideoClickHost")
            } else {
                Log.e(TAG, "Parent fragment does not implement UserProfileVideoClickHost")
                Toast.makeText(requireContext(), "无法打开视频播放器: 父Fragment未实现回调接口", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 非只读模式，使用findNavController()导航
            val navController = runCatching { findNavController() }.getOrNull()
            if (navController == null) {
                Log.e(TAG, "NavController not found, cannot open user works viewer")
                return
            }
            
            val actionId = NavigationHelper.getResourceId(
                requireContext(),
                NavigationIds.ACTION_USER_PROFILE_TO_USER_WORKS_VIEWER
            )
            if (actionId == 0) {
                Log.e(TAG, "Navigation action not found for user works viewer")
                return
            }
            
            val currentUserId = latestUser?.id ?: "current_user"
            val bundle = bundleOf(
                UserWorksViewerFragment.ARG_USER_ID to currentUserId,
                UserWorksViewerFragment.ARG_INITIAL_INDEX to initialIndex,
                UserWorksViewerFragment.ARG_VIDEO_LIST to videoItems
            )
            navController.navigate(actionId, bundle)
        }
    }

    private fun UserWork.toVideoItem(authorName: String): VideoItem {
        return VideoItem(
            id = id,
            videoUrl = playUrl,
            title = title,
            authorName = authorName,
            likeCount = likeCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            commentCount = commentCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            favoriteCount = favoriteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            shareCount = shareCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            orientation = VideoOrientation.PORTRAIT
        )
    }

    companion object {
        private const val TAG = "UserProfileFragment"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_USER_ID = "user_id" // 保留用于兼容
        private const val ARG_READ_ONLY = "read_only"

        /**
         * 创建 Fragment 实例
         * @param userName 用户名，默认为当前用户
         * @param readOnly 是否只读模式（隐藏返回按钮、禁用编辑功能），默认 false
         */
        fun newInstance(userName: String? = null, readOnly: Boolean = false): UserProfileFragment {
            return UserProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_NAME, userName)
                    putBoolean(ARG_READ_ONLY, readOnly)
                }
            }
        }
    }

    // 是否只读模式
    private val isReadOnly: Boolean
        get() = arguments?.getBoolean(ARG_READ_ONLY, false) ?: false
}
