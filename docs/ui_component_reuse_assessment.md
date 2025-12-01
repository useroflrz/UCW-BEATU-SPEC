# UI 组件复用评估报告

> 评估日期：2025-01-XX  
> 评估目标：识别可提取到 `shared/designsystem` 层的可复用 UI 组件

## 一、评估原则

根据项目架构文档（`docs/architecture.md`），`shared/designsystem` 模块的职责是：
- 主题、颜色、字体
- **通用组件**、动画资源
- 统一的视觉设计系统

**复用判断标准**：
1. ✅ 在多个业务模块中使用（推荐页、用户主页、横屏模式等）
2. ✅ 具有统一的交互逻辑和视觉样式
3. ✅ 不包含业务特定的数据绑定逻辑
4. ✅ 可以抽象为独立的 View 组件或布局

## 二、可复用组件清单

### 2.1 高优先级（强烈建议提取）

#### 1. **交互按钮组（InteractionButtonGroup）**
**使用场景**：
- ✅ 推荐页视频流（`item_video.xml`）
- ✅ 横屏模式（需求文档明确说明）
- ✅ 个人主页作品列表（可能）
- ✅ 评论区视频卡片（可能）

**组件结构**：
```xml
<!-- 点赞按钮 -->
<LinearLayout>
    <ImageView id="iv_like" />
    <TextView id="tv_like_count" />
</LinearLayout>

<!-- 收藏按钮 -->
<LinearLayout>
    <ImageView id="iv_favorite" />
    <TextView id="tv_favorite_count" />
</LinearLayout>

<!-- 评论按钮 -->
<LinearLayout>
    <ImageView id="iv_comment" />
    <TextView id="tv_comment_count" />
</LinearLayout>

<!-- 分享按钮 -->
<LinearLayout>
    <ImageView id="iv_share" />
    <TextView id="tv_share_count" />
</LinearLayout>
```

**建议实现**：
- 创建 `InteractionButtonView` 自定义 View
- 支持配置：图标资源、数量文本、选中/未选中状态
- 内置点击动画和状态切换逻辑
- 支持乐观 UI（立即更新 UI，后台请求）

**文件位置**：
- 当前：`business/videofeed/presentation/src/main/res/layout/item_video.xml` (164-273行)
- 建议：`shared/designsystem/src/main/java/.../widget/InteractionButtonView.kt`
- 布局：`shared/designsystem/src/main/res/layout/widget_interaction_button.xml`

---

#### 2. **关注按钮（FollowButton）**
**使用场景**：
- ✅ MainActivity 导航栏（`activity_main.xml`）
- ✅ 用户主页（`fragment_user_profile.xml`）
- ✅ 推荐页作者信息区域（可能）

**组件结构**：
```xml
<TextView
    android:id="@+id/btn_follow"
    android:text="关注" / "已关注"
    android:textColor="#CCFFFFFF" / "#FFFFFFFF"
    android:textStyle="bold" />
```

**建议实现**：
- 创建 `FollowButton` 自定义 View（继承 TextView 或 Button）
- 支持状态：未关注 / 已关注 / 互相关注
- 内置状态切换动画
- 支持点击回调接口

**文件位置**：
- 当前：多处重复定义
- 建议：`shared/designsystem/src/main/java/.../widget/FollowButton.kt`

---

#### 3. **Tab 导航按钮组（TabNavigationGroup）**
**使用场景**：
- ✅ MainActivity 顶部导航（`activity_main.xml`）
- ✅ 用户主页顶部导航（`fragment_user_profile.xml`）

**组件结构**：
```xml
<LinearLayout>
    <TextView id="btn_follow" text="关注" />
    <TextView id="btn_recommend" text="推荐" />
    <TextView id="btn_me" text="我" />
</LinearLayout>
```

**建议实现**：
- 创建 `TabNavigationView` 自定义 ViewGroup
- 支持配置 Tab 列表（文字、图标可选）
- 内置选中状态样式切换
- 与 `TabIndicatorView` 配合使用（已存在）

**文件位置**：
- 当前：`app/src/main/res/layout/activity_main.xml` (28-101行)
- 当前：`business/user/presentation/src/main/res/layout/fragment_user_profile.xml` (25-82行)
- 建议：`shared/designsystem/src/main/java/.../widget/TabNavigationView.kt`

---

#### 4. **搜索图标按钮（SearchIconButton）**
**使用场景**：
- ✅ MainActivity 导航栏（`activity_main.xml`）
- ✅ 用户主页导航栏（`fragment_user_profile.xml`）

**组件结构**：
```xml
<FrameLayout>
    <ImageView
        android:id="@+id/iv_search"
        android:src="@android:drawable/ic_menu_search" />
</FrameLayout>
```

**建议实现**：
- 创建 `SearchIconButton` 自定义 View（继承 FrameLayout 或 ImageView）
- 统一图标资源、点击反馈、无障碍支持

**文件位置**：
- 当前：多处重复
- 建议：`shared/designsystem/src/main/java/.../widget/SearchIconButton.kt`

---

### 2.2 中优先级（建议提取）

#### 5. **播放按钮（PlayButton）**
**使用场景**：
- ✅ 推荐页视频流（`item_video.xml`）
- ✅ 横屏模式（可能）

**组件结构**：
```xml
<ImageView
    android:id="@+id/iv_play_button"
    android:src="@drawable/ic_play_button" />
```

**建议实现**：
- 创建 `PlayButton` 自定义 View
- 支持播放/暂停状态切换
- 内置淡入淡出动画

**文件位置**：
- 当前：`business/videofeed/presentation/src/main/res/layout/item_video.xml` (18-28行)
- 建议：`shared/designsystem/src/main/java/.../widget/PlayButton.kt`

---

#### 6. **全屏按钮（FullscreenButton）**
**使用场景**：
- ✅ 推荐页视频流（`item_video.xml`）
- ✅ 横屏模式（退出全屏）

**组件结构**：
```xml
<ImageView
    android:id="@+id/iv_fullscreen"
    android:src="@android:drawable/ic_menu_crop" />
```

**建议实现**：
- 创建 `FullscreenButton` 自定义 View
- 支持全屏/退出全屏状态切换
- 图标自动切换

**文件位置**：
- 当前：`business/videofeed/presentation/src/main/res/layout/item_video.xml` (65-73行)
- 建议：`shared/designsystem/src/main/java/.../widget/FullscreenButton.kt`

---

#### 7. **Tab 切换按钮组（TabSwitchGroup）**
**使用场景**：
- ✅ 用户主页（作品/收藏/点赞/历史）
- ✅ 可能在其他列表页面使用

**组件结构**：
```xml
<ConstraintLayout>
    <TextView id="tab_works" background="@drawable/bg_tab_selected" />
    <TextView id="tab_collections" background="@drawable/bg_tab_unselected" />
    <TextView id="tab_likes" background="@drawable/bg_tab_unselected" />
    <TextView id="tab_history" background="@drawable/bg_tab_unselected" />
</ConstraintLayout>
```

**建议实现**：
- 创建 `TabSwitchView` 自定义 ViewGroup
- 支持配置 Tab 列表
- 内置选中/未选中样式切换
- 支持点击回调

**文件位置**：
- 当前：`business/user/presentation/src/main/res/layout/fragment_user_profile.xml` (283-375行)
- 建议：`shared/designsystem/src/main/java/.../widget/TabSwitchView.kt`

---

### 2.3 低优先级（可选提取）

#### 8. **头像组件（AvatarView）**
**使用场景**：
- ✅ 推荐页作者头像
- ✅ 用户主页头像
- ✅ 评论区用户头像

**组件结构**：
```xml
<FrameLayout>
    <ImageView
        android:id="@+id/iv_avatar"
        android:background="@drawable/bg_avatar_circle" />
</FrameLayout>
```

**建议实现**：
- 创建 `AvatarView` 自定义 View
- 支持圆形/圆角矩形样式
- 支持占位图、边框、大小配置
- 集成图片加载（Glide/Coil）

**文件位置**：
- 当前：多处重复
- 建议：`shared/designsystem/src/main/java/.../widget/AvatarView.kt`

---

#### 9. **统计信息展示（StatsView）**
**使用场景**：
- ✅ 用户主页（获赞/关注/粉丝）
- ✅ 可能在其他统计场景使用

**组件结构**：
```xml
<ConstraintLayout>
    <TextView id="tv_count" textSize="18sp" textStyle="bold" />
    <TextView id="tv_label" textSize="14sp" />
</ConstraintLayout>
```

**建议实现**：
- 创建 `StatsView` 自定义 View
- 支持数字格式化（如 "5.6万"）
- 支持点击事件

**文件位置**：
- 当前：`business/user/presentation/src/main/res/layout/fragment_user_profile.xml` (188-277行)
- 建议：`shared/designsystem/src/main/java/.../widget/StatsView.kt`

---

## 三、Drawable 资源复用

以下 Drawable 资源已在多个模块中重复定义，建议统一到 `shared/designsystem`：

1. **`bg_tab_selected.xml`** / **`bg_tab_unselected.xml`**
   - 当前位置：`business/videofeed/presentation/` 和 `business/user/presentation/`
   - 建议：移动到 `shared/designsystem/src/main/res/drawable/`

2. **`bg_avatar_circle.xml`** / **`bg_avatar_rounded.xml`**
   - 当前位置：多处
   - 建议：统一到 `shared/designsystem/src/main/res/drawable/`

3. **`ic_avatar_placeholder.xml`**
   - 当前位置：多处
   - 建议：统一到 `shared/designsystem/src/main/res/drawable/`

4. **`ic_play_button.xml`**
   - 当前位置：`business/videofeed/presentation/`
   - 建议：移动到 `shared/designsystem/src/main/res/drawable/`

---

## 四、实施建议

### 4.1 优先级排序

**第一阶段（立即实施）**：
1. 交互按钮组（InteractionButtonGroup）
2. 关注按钮（FollowButton）
3. Tab 导航按钮组（TabNavigationGroup）
4. 搜索图标按钮（SearchIconButton）

**第二阶段（后续迭代）**：
5. 播放按钮（PlayButton）
6. 全屏按钮（FullscreenButton）
7. Tab 切换按钮组（TabSwitchGroup）

**第三阶段（可选）**：
8. 头像组件（AvatarView）
9. 统计信息展示（StatsView）

### 4.2 实施步骤

1. **创建组件类**：
   - 在 `shared/designsystem/src/main/java/com/ucw/beatu/shared/designsystem/widget/` 下创建自定义 View
   - 遵循 Android View 最佳实践（支持 XML 属性、状态保存等）

2. **创建布局文件**：
   - 在 `shared/designsystem/src/main/res/layout/` 下创建可复用的布局文件
   - 使用 `<merge>` 标签优化布局层级

3. **统一 Drawable 资源**：
   - 将重复的 Drawable 移动到 `shared/designsystem/src/main/res/drawable/`
   - 更新各业务模块的引用

4. **更新业务模块**：
   - 替换业务模块中的重复实现
   - 使用新的共享组件

5. **测试验证**：
   - 确保所有使用场景正常工作
   - 验证样式一致性

### 4.3 技术要点

- **状态管理**：使用 `StateFlow` 或 `LiveData` 管理组件状态（如点赞/未点赞）
- **动画效果**：使用 `ObjectAnimator` 或 `ValueAnimator` 实现统一的动画效果
- **无障碍支持**：确保所有组件支持 `contentDescription` 和键盘导航
- **性能优化**：避免过度绘制，使用 `ViewStub` 延迟加载非关键 UIa

---

## 五、预期收益

1. **代码复用率提升**：减少重复代码 30-40%
2. **维护成本降低**：UI 样式修改只需在一处进行
3. **一致性保证**：所有业务模块使用统一的视觉设计
4. **开发效率提升**：新功能可直接使用现有组件，减少开发时间

---

## 六、注意事项

1. **业务逻辑分离**：组件只负责 UI 展示和基础交互，业务逻辑（如网络请求）应在 ViewModel 层处理
2. **依赖管理**：`shared/designsystem` 不应依赖业务模块，保持单向依赖
3. **版本兼容**：提取组件时需考虑向后兼容，避免破坏现有功能
4. **文档完善**：为每个组件编写使用文档和示例代码

---

## 七、参考文档

- 架构文档：`docs/architecture.md`
- 开发计划：`docs/development_plan.md`
- 需求文档：`BeatUClient/docs/requirements.md`





