package com.ucw.beatu.business.user.presentation.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.presentation.R
import com.ucw.beatu.business.user.presentation.ui.adapter.UserWorkUiModel
import com.ucw.beatu.business.user.presentation.ui.adapter.UserWorksAdapter
import com.ucw.beatu.business.user.presentation.ui.helper.UserProfileAvatarManager
import com.ucw.beatu.business.user.presentation.ui.helper.UserProfileBioEditor
import com.ucw.beatu.business.user.presentation.ui.helper.UserProfileFollowButtonManager
import com.ucw.beatu.business.user.presentation.ui.helper.UserProfileNavigationHelper
import com.ucw.beatu.business.user.presentation.ui.helper.UserProfileTabManager
import com.ucw.beatu.business.user.presentation.viewmodel.UserProfileViewModel
import com.ucw.beatu.shared.common.util.NumberFormatter
import com.ucw.beatu.shared.designsystem.util.IOSButtonEffect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
    private lateinit var btnFollow: MaterialButton
    
    // 头像上传相关
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { avatarManager.handleImageSelection(it) } ?: run {
            Log.w(TAG, "Image selection cancelled or failed")
            Toast.makeText(requireContext(), "未选择图片", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 辅助类实例
    private lateinit var avatarManager: UserProfileAvatarManager
    private lateinit var bioEditor: UserProfileBioEditor
    private lateinit var followButtonManager: UserProfileFollowButtonManager
    private lateinit var navigationHelper: UserProfileNavigationHelper
    private lateinit var tabManager: UserProfileTabManager
    
    private val worksAdapter by lazy {
        UserWorksAdapter { work -> navigateToUserWorksViewer(work.id) }
    }
    private var latestUser: User? = null
    private var latestUserWorks: List<UserWork> = emptyList()

    // 用户名（从参数获取，默认为当前用户）
    private val userName: String
        get() = arguments?.getString(ARG_USER_NAME) ?: CURRENT_USER_NAME

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

        // 初始化辅助类
        initHelpers(view)

        // 设置头像圆角裁剪
        avatarManager.setupAvatarRoundCorner()

        // 非只读模式：允许修改头像和简介，但「用户名不可编辑」
        if (!isReadOnly) {
            // 设置头像点击上传
            avatarManager.setupAvatarUpload()

            // 只允许编辑简介，不允许编辑用户名
            setupEditableFields()
            // 自己的主页不需要关注按钮
            btnFollow.visibility = View.GONE
        } else {
            // 只读模式：禁用头像、名称、名言的点击
            ivAvatar.isClickable = false
            tvUsername.isClickable = false
            tvBio.isClickable = false
            // 只读模式：显示关注按钮
            btnFollow.visibility = View.VISIBLE
            btnFollow.isClickable = true
            followButtonManager.setupFollowButton()
            Log.d(TAG, "Follow button initialized in read-only mode")
        }

        // 初始化标签切换
        tabManager.initTabs()

        // 只读模式下调整文本颜色为黑色，以便在白色背景上可见
        if (isReadOnly) {
            applyReadOnlyTextColors(view)
            tabManager.applyReadOnlyTabColors()
        }

        // 初始化作品列表
        initWorksList()

        // 观察 ViewModel 数据
        observeViewModel()

        // 初始化并加载用户数据
        val userData = arguments?.getBundle(ARG_USER_DATA)
        if (userData != null) {
            // 如果提供了完整用户数据，直接使用
            Log.d(TAG, "Using provided user data: id=${userData.getString("id")}, name=${userData.getString("name")}")
            viewModel.setUserData(
                id = userData.getString("id") ?: "",
                name = userData.getString("name") ?: "",
                avatarUrl = userData.getString("avatarUrl"),
                bio = userData.getString("bio"),
                likesCount = userData.getLong("likesCount", 0),
                followingCount = userData.getLong("followingCount", 0),
                followersCount = userData.getLong("followersCount", 0)
            )
        } else {
            // 否则通过用户名或用户ID加载
            Log.d(TAG, "Loading user with userName: $userName")
            viewModel.loadUser(userName)
            // 注意：作品列表的加载会在 observeViewModel 中的用户加载完成后触发，使用实际的用户名
        }
        
        // 只读模式下，开始观察关注同步结果
        if (isReadOnly) {
            viewModel.startObservingFollowSyncResult()
        }
    }

    /**
     * 初始化辅助类
     */
    private fun initHelpers(view: View) {
        avatarManager = UserProfileAvatarManager(
            fragment = this,
            ivAvatar = ivAvatar,
            viewModel = viewModel,
            pickImageLauncher = pickImageLauncher,
            getCurrentUserId = { latestUser?.id }
        )

        bioEditor = UserProfileBioEditor(
            fragment = this,
            viewModel = viewModel,
            getCurrentBio = { tvBio.text.toString() },
            getCurrentUserId = { latestUser?.id }
        )

        followButtonManager = UserProfileFollowButtonManager(
            fragment = this,
            btnFollow = btnFollow,
            viewModel = viewModel,
            getTargetUserId = { latestUser?.id },
            getCurrentUserId = { CURRENT_USER_ID }
        )

        navigationHelper = UserProfileNavigationHelper(
            fragment = this,
            isReadOnly = isReadOnly,
            getUser = { latestUser },
            getUserWorks = { latestUserWorks }
        )

        tabManager = UserProfileTabManager(
            view = view,
            viewModel = viewModel,
            isReadOnly = isReadOnly,
            onTabSwitched = { tabType, authorId, currentUserId ->
                val actualAuthorId = if (authorId.isEmpty()) {
                    // 如果没有提供authorId，使用当前用户的ID
                    latestUser?.id ?: (if (userName == "BEATU") "BEATU" else userName)
                } else {
                    authorId
                }
                val actualCurrentUserId = if (currentUserId.isEmpty()) {
                    latestUser?.id ?: "BEATU"
                } else {
                    currentUserId
                }
                viewModel.switchTab(tabType, authorId = actualAuthorId, currentUserId = actualCurrentUserId)
            }
        )
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
    * 设置可编辑字段（仅简介可编辑，用户名禁止编辑）
    */
    private fun setupEditableFields() {
        // 禁止用户名点击与焦点
        tvUsername.isClickable = false
        tvUsername.isFocusable = false

        // 简介点击编辑
        IOSButtonEffect.applyIOSEffect(tvBio) {
            android.util.Log.d("UserProfileFragment", "简介被点击")
            bioEditor.showEditBioDialog()
        }
        
        // 确保简介 TextView 可点击
        tvBio.isClickable = true
        tvBio.isFocusable = true
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
                            Log.d(TAG, "User loaded: name=${user.name}, id=${user.id}")
                            latestUser = user
                            updateUserInfo(user)
                            // 用户加载完成后，从数据库查询该用户的视频数据
                            // 使用用户的ID（user.id）来查询作品
                            Log.d(TAG, "Loading user works from database: authorId=${user.id}, userId=${user.id}, originalUserName=$userName")
                            viewModel.switchTab(UserProfileViewModel.TabType.WORKS, authorId = user.id, currentUserId = user.id)
                            // 开始观察关注状态（只读模式），当前登录用户固定为 BEATU
                            if (isReadOnly) {
                                val currentUserId = CURRENT_USER_ID
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
                    // 观察关注状态和交互状态（从本地数据库，通过 ViewModel StateFlow）
                    if (isReadOnly) {
                        combine(
                            viewModel.isFollowing,
                            viewModel.isInteracting
                        ) { isFollowing: Boolean?, isInteracting: Boolean ->
                            Pair(isFollowing, isInteracting)
                        }.collect { pair: Pair<Boolean?, Boolean> ->
                            val (isFollowing: Boolean?, isInteracting: Boolean) = pair
                            followButtonManager.updateFollowButton(isFollowing, isInteracting)
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
     * 更新用户信息 UI
     */
    private fun updateUserInfo(user: User) {
        // ✅ 显示用户姓名（user.name 映射自后端的 userName）
        val displayName = user.name.ifEmpty { user.id }
        tvUsername.text = displayName
        Log.d(TAG, "更新用户信息：name=${user.name}, id=${user.id}, displayName=$displayName")
        tvBio.text = user.bio ?: ""
        
        // 格式化数字显示
        tvLikesCount.text = NumberFormatter.formatCount(user.likesCount)
        tvFollowingCount.text = NumberFormatter.formatCount(user.followingCount)
        tvFollowersCount.text = NumberFormatter.formatCount(user.followersCount)
        
        // 加载头像
        user.avatarUrl?.let { avatarPath ->
            avatarManager.loadAvatar(avatarPath)
        }
        
        // 用户名在任何模式下都不可编辑
        tvUsername.isClickable = false
        tvUsername.isFocusable = false

        // 非只读模式下允许编辑简介
        if (!isReadOnly) {
            tvBio.isClickable = true
            tvBio.isFocusable = true
        } else {
            tvBio.isClickable = false
            tvBio.isFocusable = false
        }
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
    }

    private fun UserWork.toUiModel(): UserWorkUiModel = UserWorkUiModel(
        id = id,
        thumbnailUrl = coverUrl,
        playCount = viewCount,
        playUrl = playUrl,
        title = title
    )


    private fun navigateToUserWorksViewer(selectedWorkId: Long) {  // ✅ 修改：从 String 改为 Long
        navigationHelper.navigateToUserWorksViewer(selectedWorkId)
    }

    companion object {
        private const val TAG = "UserProfileFragment"
        // 当前登录用户的固定 ID 与用户名
        private const val CURRENT_USER_ID = "BEATU"
        private const val CURRENT_USER_NAME = "BEATU"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_USER_ID = "user_id" // 保留用于兼容
        private const val ARG_READ_ONLY = "read_only"
        private const val ARG_USER_DATA = "user_data" // 完整用户数据

        /**
         * 创建 Fragment 实例
         * @param userName 用户名，默认为当前用户
         * @param readOnly 是否只读模式（隐藏返回按钮、禁用编辑功能），默认 false
         */
        fun newInstance(userName: String? = null, readOnly: Boolean = false): UserProfileFragment {
            return UserProfileFragment().apply {
                arguments = Bundle().apply {
                    // 如果未显式传用户名称，则默认展示当前登录用户 BEATU 的主页
                    putString(ARG_USER_NAME, userName ?: CURRENT_USER_NAME)
                    putBoolean(ARG_READ_ONLY, readOnly)
                }
            }
        }
        
        /**
         * 创建 Fragment 实例，传递完整用户数据
         * @param userData Bundle 包含用户完整数据（id, name, avatarUrl, bio, likesCount, followingCount, followersCount）
         * @param readOnly 是否只读模式（隐藏返回按钮、禁用编辑功能），默认 false
         */
        fun newInstanceWithData(userData: Bundle, readOnly: Boolean = false): UserProfileFragment {
            return UserProfileFragment().apply {
                arguments = Bundle().apply {
                    putBundle(ARG_USER_DATA, userData)
                    putBoolean(ARG_READ_ONLY, readOnly)
                }
            }
        }
    }

    // 是否只读模式
    private val isReadOnly: Boolean
        get() = arguments?.getBoolean(ARG_READ_ONLY, false) ?: false
}
