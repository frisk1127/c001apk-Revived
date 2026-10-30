package com.example.c001apk.compose.ui.feed.reply

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import com.example.c001apk.compose.ui.component.design.setMiuixBlurBehind
import android.content.Intent
import android.graphics.Color
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Build.VERSION.SDK_INT
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.transform.CircleCropTransformation
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.databinding.ActivityReplyBinding
import com.example.c001apk.compose.constant.Constants.seedColors
import com.example.c001apk.compose.logic.model.OSSUploadPrepareModel
import com.example.c001apk.compose.ui.feed.reply.emoji.EmojiPagerAdapter
import com.example.c001apk.compose.util.CookieUtil.materialYou
import com.example.c001apk.compose.util.CookieUtil.seedColor
import com.example.c001apk.compose.util.CookieUtil.themeType
import com.example.c001apk.compose.util.EmojiTextWatcher
import com.example.c001apk.compose.util.EmojiUtils
import com.example.c001apk.compose.util.EmojiUtils.coolBList
import com.example.c001apk.compose.util.EmojiUtils.emojiList
import com.example.c001apk.compose.util.FastDeleteAtUserKeyListener
import com.example.c001apk.compose.util.OSSUtil.getImageDimensionsAndMD5
import com.example.c001apk.compose.util.OSSUtil.toHex
import com.example.c001apk.compose.util.OssUploadUtil.ossUpload
import com.example.c001apk.compose.util.dp
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.performConfiguredHapticFeedback
import com.example.c001apk.compose.view.SmoothInputLayout
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.example.c001apk.compose.ui.component.design.showAppDialog
import com.example.c001apk.compose.ui.component.design.DialogAction
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.c001apk.compose.ui.component.design.AppGlyphs
import com.example.c001apk.compose.view.VectorGlyphDrawable
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.File
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import com.example.c001apk.compose.ui.theme.observeViewTheme
import com.google.android.material.elevation.SurfaceColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.c001apk.compose.UiStyle
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.roundToInt


/*
* Copyright (C) 2018 AlexMofer
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/ /**
 * 输入面板
 */

@AndroidEntryPoint
class ReplyActivity : AppCompatActivity(),
    View.OnClickListener, OnTouchListener, SmoothInputLayout.OnVisibilityChangeListener {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var viewStyle: ReplyViewStyle
    private var predictiveReplyBack: OnBackPressedCallback? = null
    private var currentUiStyle = com.example.c001apk.compose.util.CookieUtil.uiStyle
    private var viewPalette: androidx.compose.material3.ColorScheme? = null
    private var replySessionGeneration: Long? = null
    private var originalPublishColors: android.content.res.ColorStateList? = null
    private var originalHintColors: android.content.res.ColorStateList? = null
    private var originalCheckTint: android.content.res.ColorStateList? = null
    private var originalIcons = emptyMap<android.widget.ImageView, android.graphics.drawable.Drawable?>()
    private var nativeIcons = emptyMap<android.widget.ImageView, VectorGlyphDrawable>()
    private var originalIconScales = emptyMap<android.widget.ImageView, android.widget.ImageView.ScaleType>()
    private var originalIconTints = emptyMap<android.widget.ImageView, android.content.res.ColorStateList?>()
    private var originalTextSizes = emptyMap<android.widget.TextView, Float>()
    private var styleDarkMode = com.example.c001apk.compose.util.CookieUtil.isDarkMode

    private lateinit var binding: ActivityReplyBinding
    private val viewModel by viewModels<ReplyViewModel>()
    private val type: String? by lazy { intent.getStringExtra("type") }
    private val rid: String? by lazy { intent.getStringExtra("rid") }
    private val username: String? by lazy { intent.getStringExtra("username") }
    private val avatar: String? by lazy { intent.getStringExtra("avatar") }

    private val targetType: String? by lazy { intent.getStringExtra("targetType") }
    private val targetId: String? by lazy { intent.getStringExtra("targetId") }
    private val title: String? by lazy { intent.getStringExtra("title") }

    private val imm by lazy {
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    private val color: Int
        get() = if (currentUiStyle == UiStyle.MIUIX) {
            viewPalette?.surfaceContainer?.toArgb() ?: resolveSurfaceContainerColor()
        } else resolveSurfaceContainerColor()
    private val imeScrimDrawable by lazy { ColorDrawable(color) }
    private val externalLaunchScrimDrawable by lazy { ColorDrawable(color) }
    private val recentList = ArrayList<List<Pair<String, Int>>>()
    private val list = listOf(recentList, emojiList, coolBList, EmojiUtils.niuniuList)
    private lateinit var pickContent: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickDocument: ActivityResultLauncher<Array<String>>
    private var uriList: MutableList<Uri> = ArrayList()
    private var imageList = ArrayList<OSSUploadPrepareModel>()
    private var typeList = ArrayList<String>()
    private var md5List = ArrayList<ByteArray?>()
    private var dialog: android.content.DialogInterface? = null
    private lateinit var atTopicResultLauncher: ActivityResultLauncher<Intent>
    private var isFromAt = false
    private var pendingShowKeyboard = true
    private var isReplyThemeReady = false
    private var hasStartedEntrance = false
    private var imeAnimating = false
    private val imeRetryHandler = Handler(Looper.getMainLooper())
    private var imeRetryCount = 0
    private val maxImeRetry = 8
    private val imeRetryDelayMs = 120L
    private val imeRetryRunnable = Runnable { retryShowIme() }
    private var isEmojiPanelVisible = false
    private var isEmojiPanelRequested = false
    private var animateEmojiInputDescent = false
    private var emojiInputDescentStart = 0f
    private var isEmojiPanelEntranceAnimating = false
    private var pendingExternalLaunch: (() -> Unit)? = null
    private var isWaitingForImeHide = false
    private var imeHideSuppressed = false
    private var isExternalInputDescentAnimating = false
    private var externalInputDescentAnimator: ValueAnimator? = null
    private val externalLaunchHandler = Handler(Looper.getMainLooper())
    private val externalLaunchTimeout = Runnable { runPendingExternalLaunch(force = true) }
    private var baseRootPaddingBottom = 0
    private var isExiting = false
    private val exitHandler = Handler(Looper.getMainLooper())
    private val finishFallback = Runnable { finishAfterExitAnimation() }
    private var pendingExternalReturn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (materialYou)
            DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityReplyBinding.inflate(layoutInflater)
        viewStyle = ReplyViewStyle(binding)
        originalTextSizes = listOfNotNull(binding.title, binding.editText, binding.publish,
            binding.replyTargetName, binding.checkBox).associateWith { it.textSize }
        originalPublishColors = binding.publish.textColors
        originalHintColors = binding.editText.hintTextColors
        originalCheckTint = binding.checkBox.buttonTintList
        originalIconTints = listOfNotNull(binding.emojiBtn, binding.imageBtn, binding.otherImageBtn, binding.atBtn, binding.tagBtn)
            .associateWith { it.imageTintList }
        originalIcons = originalIconTints.keys.associateWith { it.drawable }
        originalIconScales = originalIconTints.keys.associateWith { it.scaleType }
        nativeIcons = listOfNotNull(
            binding.emojiBtn to AppGlyphs.Emoji,
            binding.imageBtn to MiuixIcons.Medium.Image,
            binding.otherImageBtn?.let { it to MiuixIcons.Medium.File },
            binding.atBtn to AppGlyphs.Mention,
            binding.tagBtn to AppGlyphs.Topic,
        ).mapNotNull { (view, vector) -> view?.let { it to VectorGlyphDrawable(vector, resources.displayMetrics.density) } }.toMap()
        binding.emojiBtn?.contentDescription = "表情"
        binding.imageBtn?.contentDescription = "选择图片"
        binding.otherImageBtn?.contentDescription = "选择文件"
        binding.atBtn?.contentDescription = "提及用户"
        binding.tagBtn?.contentDescription = "添加话题"
        binding.main.alpha = 0f
        setContentView(binding.root)
        installPredictiveReplyBack()
        window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )
        binding.main.isFocusable = false
        binding.main.isFocusableInTouchMode = false
        baseRootPaddingBottom = binding.main.paddingBottom

        ViewCompat.setWindowInsetsAnimationCallback(binding.main, object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0) {
                    imeAnimating = true
                    imeRetryHandler.removeCallbacks(imeRetryRunnable)
                }
                if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0 &&
                    isEmojiPanelRequested && isEmojiPanelVisible
                ) {
                    animateEmojiInputDescent = true
                    emojiInputDescentStart = binding.inputLayout.translationY
                }
                super.onPrepare(animation)
            }
            override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
                val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val useImeInset = insets.isVisible(WindowInsetsCompat.Type.ime()) || imeInset > 0
                val imeAnimation = runningAnimations.lastOrNull {
                    (it.typeMask and WindowInsetsCompat.Type.ime()) != 0
                }
                if (!isExternalInputDescentAnimating && !isEmojiPanelEntranceAnimating && !imeHideSuppressed) {
                    val translation = if (animateEmojiInputDescent && imeAnimation != null) {
                        val fraction = imeAnimation.fraction.coerceIn(0f, 1f)
                        emojiInputDescentStart * (1f - smoothStep(fraction))
                    } else {
                        calculateInputTranslation(
                            imeInset = if (useImeInset) imeInset else 0,
                            navInset = navInset
                        )
                    }
                    updateReplyPanelTranslation(translation)
                }
                if (useImeInset && imeInset > 0 && binding.bottomLayout == null &&
                    !isEmojiPanelVisible && !imeHideSuppressed
                ) {
                    imeScrimDrawable.setBounds(
                        0,
                        binding.main.height - (imeInset - navInset),
                        binding.main.width,
                        binding.main.height
                    )
                    binding.main.overlay.remove(imeScrimDrawable)
                    binding.main.overlay.add(imeScrimDrawable)
                } else {
                    binding.main.overlay.remove(imeScrimDrawable)
                }
                return insets
            }
            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0) {
                    imeAnimating = false
                    if (animateEmojiInputDescent && isEmojiPanelRequested) {
                        updateReplyPanelTranslation(0f)
                    }
                    animateEmojiInputDescent = false
                    if (!isExiting) {
                        imeHideSuppressed = false
                    }
                    if (pendingExternalLaunch != null && isWaitingForImeHide) {
                        isWaitingForImeHide = false
                        runPendingExternalLaunch()
                    }
                }
                val isImeVisible = ViewCompat.getRootWindowInsets(binding.editText)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
                if (isImeVisible && isEmojiPanelVisible && !isEmojiPanelRequested &&
                    pendingExternalLaunch == null && !imeHideSuppressed
                ) {
                    closeEmojiPanelForIme()
                }
                ViewCompat.requestApplyInsets(binding.main)
                super.onEnd(animation)
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val sysInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val useImeInset = isImeVisible || imeInset > 0
            if (isImeVisible) {
                if (!imeAnimating && !isEmojiPanelRequested && !imeHideSuppressed &&
                    binding.bottomLayout == null
                ) {
                    closeEmojiPanelForIme(imeInset, navInset)
                }
            }
            if (!imeAnimating && !isEmojiPanelEntranceAnimating &&
                !isExternalInputDescentAnimating
            ) {
                if (!imeHideSuppressed) {
                    val translation = calculateInputTranslation(
                        imeInset = if (useImeInset) imeInset else 0,
                        navInset = navInset
                    )
                    updateReplyPanelTranslation(translation)
                }
                if (useImeInset && imeInset > 0 && binding.bottomLayout == null &&
                    !isEmojiPanelVisible && !imeHideSuppressed
                ) {
                    imeScrimDrawable.setBounds(
                        0,
                        binding.main.height - (imeInset - navInset),
                        binding.main.width,
                        binding.main.height
                    )
                    binding.main.overlay.remove(imeScrimDrawable)
                    binding.main.overlay.add(imeScrimDrawable)
                } else {
                    binding.main.overlay.remove(imeScrimDrawable)
                }
            }
            binding.main.updatePadding(
                bottom = baseRootPaddingBottom + navInset,
                top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            )
            if (!isImeVisible && !imeAnimating && pendingExternalLaunch != null) {
                isWaitingForImeHide = false
                runPendingExternalLaunch()
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.main)

        viewModel.type = type
        viewModel.rid = rid

        initView()
        initEditText()
        initPage()
        initEmojiPanel()
        initObserve()
        initPhotoPick()
        initAtUser()

    }

    private fun resolvePrimaryColor(): Int {
        if (currentUiStyle == UiStyle.MIUIX) viewPalette?.let { return it.primary.toArgb() }
        if (materialYou) {
            return MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimary,
                0
            )
        }
        val seed = seedColors.getOrNull(ThemeType.entries.indexOf(themeType))
            ?: "FF$seedColor".toLongOrNull(16)
            ?: seedColors[0]
        return seed.toInt()
    }

    private fun resolveSurfaceContainerColor(): Int {
        if (materialYou) {
            return SurfaceColors.SURFACE_1.getColor(this)
        }
        val base = ContextCompat.getColor(this, R.color.color_surface)
        val primary = resolvePrimaryColor()
        return ColorUtils.blendARGB(base, primary, 0.08f)
    }

    private fun resolveSurfaceColor(): Int {
        return if (materialYou) {
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurface,
                0
            )
        } else {
            ContextCompat.getColor(this, R.color.color_surface)
        }
    }

    private fun resolvePrimaryDarkColor(primary: Int): Int {
        return if (materialYou) {
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimaryDark,
                primary
            )
        } else {
            ColorUtils.blendARGB(primary, Color.BLACK, 0.2f)
        }
    }

    private fun initAtUser() {
        atTopicResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    val list = result.data?.getStringExtra("data")
                    if (isFromAt) {
                        isFromAt = false
                        with(binding.editText.selectionStart) {
                            binding.editText.editableText.replace(this - 1, this, list)
                        }
                    } else {
                        binding.editText.editableText.append(list)
                    }
                }
            }
    }

    override fun onPostResume() {
        super.onPostResume()
        requestInitialKeyboard()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (pendingExternalReturn) {
            pendingExternalReturn = false
            animateExternalActivityReturn()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) requestInitialKeyboard()
        if (hasFocus && imeHideSuppressed) {
            // 窗口失焦期间 IME 动画回调可能丢失，恢复时重置状态避免输入框位置冻结
            imeHideSuppressed = false
            ViewCompat.requestApplyInsets(binding.main)
        }
    }


    private fun initPhotoPick() {
        fun handlePickedUris(uris: List<Uri>) {
            if (uris.isNotEmpty()) {
                runCatching {
                    uris.forEach { uri ->
                        if (uriList.size == 9) {
                            Toast.makeText(this, "最多选择9张图片", Toast.LENGTH_SHORT).show()
                            return@runCatching
                        }

                        val result = getImageDimensionsAndMD5(contentResolver, uri)
                        val md5Byte = result.second
                        val md5 = md5Byte?.toHex() ?: ""
                        val width = result.first?.first ?: 0
                        val height = result.first?.second ?: 0
                        val type = result.first?.third ?: ""

                        typeList.add(type)
                        md5List.add(md5Byte)
                        imageList.add(
                            OSSUploadPrepareModel(
                                name = "${
                                    UUID.randomUUID().toString().replace("-", "")
                                }.${if (type.startsWith("image/")) type.substring(6) else type}",
                                resolution = "${width}x${height}",
                                md5 = md5,
                            )
                        )
                        uriList.add(uri)

                        val imageView = ImageView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                (65.dp * width.toFloat() / height.toFloat()).toInt(), 65.dp
                            ).apply {
                                setMargins(5.dp, 0, 0, 0)
                            }
                            setOnClickListener {
                                with(binding.imageLayout.indexOfChild(this)) {
                                    binding.imageLayout.removeViewAt(this)
                                    uriList.removeAt(this)
                                    typeList.removeAt(this)
                                    md5List.removeAt(this)
                                    imageList.removeAt(this)
                                    binding.imageLayout.isVisible = uriList.isNotEmpty()
                                }
                            }
                        }
                        //Glide.with(this).load(uri).into(imageView)
                        imageView.load(uri) {
                            crossfade(true)
                        }
                        binding.imageLayout.addView(imageView)
                    }
                }.onFailure {
                    val error = it
                    showAppDialog(this, "获取图片信息失败", error.message, actions = listOf(
                        DialogAction("日志") { showAppDialog(this, "日志", error.stackTraceToString()) },
                        DialogAction(getString(android.R.string.ok), primary = true),
                    ))
                }
            }
            binding.imageLayout.isVisible = uriList.isNotEmpty()
        }

        pickContent =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                animateExternalActivityReturnIfPending()
                uri?.let {
                    handlePickedUris(listOf(it))
                }
            }

        pickDocument =
            registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                animateExternalActivityReturnIfPending()
                handlePickedUris(uris)
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding.emojiBtn?.setOnClickListener(this)
        binding.imageBtn.setOnClickListener(this)
        binding.otherImageBtn?.setOnClickListener(this)
        binding.atBtn.setOnClickListener(this)
        binding.tagBtn.setOnClickListener(this)
        binding.checkBox.setOnClickListener(this)
        binding.publish.setOnClickListener(this)
        binding.replyCancel.setOnClickListener { finish() }
        binding.replySend.setOnClickListener { if (binding.replySend.isEnabled) binding.publish.performClick() }
        var dragStartY = 0f
        var dragStartTime = 0L
        binding.replyDragArea.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    binding.main.animate().cancel()
                    dragStartY = event.rawY
                    dragStartTime = event.eventTime
                }
                MotionEvent.ACTION_MOVE -> binding.main.translationY = (event.rawY - dragStartY).coerceAtLeast(0f)
                MotionEvent.ACTION_UP -> {
                    val distance = (event.rawY - dragStartY).coerceAtLeast(0f)
                    val elapsed = (event.eventTime - dragStartTime).coerceAtLeast(1L)
                    val density = resources.displayMetrics.density
                    if (distance > 72 * density || (distance > 24 * density && distance / elapsed > density)) finish()
                    else binding.main.animate().translationY(0f).setDuration(200L).start()
                }
                MotionEvent.ACTION_CANCEL -> binding.main.animate().translationY(0f).setDuration(200L).start()
            }
            true
        }
        binding.editText.setOnTouchListener(this)
        binding.out.setOnTouchListener(this)
        (binding.main as? SmoothInputLayout)?.setOnVisibilityChangeListener(this)
        (binding.main as? SmoothInputLayout)?.setOnKeyboardChangeListener(null)
        applyUiStyle()
    }

    private fun applyUiStyle() {
        nativeIcons.forEach { (view, icon) ->
            val target = if (currentUiStyle == UiStyle.MIUIX) icon else originalIcons[view]
            if (view.drawable !== target) view.setImageDrawable(target)
            view.minimumHeight = if (currentUiStyle == UiStyle.MIUIX) 48.dp else 0
            view.scaleType = if (currentUiStyle == UiStyle.MIUIX) android.widget.ImageView.ScaleType.CENTER_INSIDE
                else originalIconScales[view] ?: android.widget.ImageView.ScaleType.FIT_CENTER
        }
        val miuix = currentUiStyle == UiStyle.MIUIX
        val topRadius = if (miuix) 28.dp.toFloat() else 16.dp.toFloat()
        val radius = listOf(topRadius, topRadius, 0f, 0f)
        val radiusBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(this@ReplyActivity.color)
            cornerRadii = floatArrayOf(
                radius[0], radius[0],
                radius[1], radius[1],
                radius[2], radius[2],
                radius[3], radius[3]
            )
        }
        if (binding.main is SmoothInputLayout) {
            binding.inputLayout.background = radiusBg
            binding.emojiLayout.setBackgroundColor(color)
        } else
            binding.bottomLayout?.background = radiusBg
        binding.title.setTypeface(null, if (miuix) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        binding.publish.background = if (miuix) GradientDrawable().apply {
            cornerRadius = 12.dp.toFloat()
            setColor(viewPalette?.primary?.toArgb() ?: resolvePrimaryColor())
        } else null
        binding.textInputLayout.boxBackgroundColor = color
        val textColor = if (miuix) {
            viewPalette?.onSurface?.toArgb() ?: MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.BLACK)
        } else MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.BLACK)
        binding.title.setTextColor(textColor)
        binding.editText.setTextColor(textColor)
        binding.replyTargetName?.setTextColor(textColor)
        binding.checkBox.setTextColor(textColor)
        viewPalette?.takeIf { miuix }?.let { palette ->
            binding.publish.setTextColor(palette.onPrimary.toArgb())
            binding.editText.setHintTextColor(palette.onSurfaceVariant.toArgb())
            listOfNotNull(binding.emojiBtn, binding.imageBtn, binding.otherImageBtn, binding.atBtn, binding.tagBtn)
                .forEach { it.imageTintList = android.content.res.ColorStateList.valueOf(palette.onSurfaceVariant.toArgb()) }
            binding.checkBox.buttonTintList = android.content.res.ColorStateList.valueOf(palette.primary.toArgb())
        }
        if (!miuix) {
            if (binding.editText.text?.isNotBlank() == true) binding.publish.setTextColor(resolvePrimaryColor())
            else originalPublishColors?.let { binding.publish.setTextColor(it) }
            originalHintColors?.let { binding.editText.setHintTextColor(it) }
            binding.checkBox.buttonTintList = originalCheckTint
            val toolbarTint = ColorStateList.valueOf(viewPalette?.onSurfaceVariant?.toArgb() ?: textColor)
            // A null ImageView tint clears the XML vector's tint and exposes its black paths.
            originalIconTints.forEach { (view, tint) -> view.imageTintList = tint ?: toolbarTint }
        }
        viewStyle.apply(miuix, viewPalette)
        imeScrimDrawable.color = color
        externalLaunchScrimDrawable.color = color
    }

    private fun initObserve() {
        observeViewTheme(userPreferencesRepository) { prefs, dark, palette, typography ->
            if (replySessionGeneration != null && replySessionGeneration != prefs.sessionGeneration) {
                finish()
                return@observeViewTheme
            }
            replySessionGeneration = prefs.sessionGeneration
            currentUiStyle = prefs.uiStyle
            window.setMiuixBlurBehind(prefs.uiStyle == UiStyle.MIUIX && prefs.blurEnabled && prefs.blurScope == com.example.c001apk.compose.BlurScope.GLOBAL && SDK_INT >= 33)
            predictiveReplyBack?.isEnabled = currentUiStyle == UiStyle.MIUIX
            styleDarkMode = dark
            viewPalette = palette
            applyUiStyle()
            if (prefs.uiStyle == UiStyle.MIUIX) {
                val scale = prefs.fontScale * prefs.contentScale
                binding.title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, typography.title4.fontSize.value * scale)
                binding.editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, typography.main.fontSize.value * scale)
                binding.publish.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, typography.button.fontSize.value * scale)
                binding.replyTargetName?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, typography.body2.fontSize.value * scale)
                binding.checkBox.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, typography.body2.fontSize.value * scale)
            } else originalTextSizes.forEach { (view, size) -> view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size) }
            if (!isReplyThemeReady) {
                isReplyThemeReady = true
                binding.main.doOnPreDraw {
                    if (!isFinishing && !isDestroyed) {
                        hasStartedEntrance = true
                        binding.main.translationY = (binding.bottomLayout ?: binding.inputLayout).height.toFloat()
                        binding.main.alpha = 1f
                        binding.main.animate()
                            .translationY(0f)
                            .setDuration(300L)
                            .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))
                            .start()
                        requestInitialKeyboard()
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uploadImage.collect { responseData ->
                    responseData?.let {
                        viewModel.replyAndFeedData["pic"] =
                            responseData.fileInfo.joinToString(separator = ",") {
                                responseData.uploadPrepareInfo.uploadImagePrefix + "/" + it.uploadFileName
                            }
                        ossUpload(
                            this@ReplyActivity, responseData, uriList, typeList, md5List,
                            iOnSuccess = { index ->
                                if (BuildConfig.DEBUG) {
                                    Log.i("OSSUpload", "uploadSuccess")
                                }
                                if (index == uriList.lastIndex) {
                                    if (type == "createFeed")
                                        viewModel.onPostCreateFeed()
                                    else
                                        viewModel.onPostReply()
                                }
                            },
                            iOnFailure = {
                                if (BuildConfig.DEBUG) {
                                    Log.i("OSSUpload", "uploadFailed")
                                }
                                runOnUiThread {
                                    closeDialog()
                                    Toast.makeText(
                                        this@ReplyActivity,
                                        "图片上传失败",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            },
                            closeDialog = {
                                closeDialog()
                            }
                        )
                        viewModel.resetUpload()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.recentList.collect {
                    viewModel.size = it.size
                    viewModel.last = it.lastOrNull()?.data
                    if (binding.emojiPanel.currentItem == 0 && recentList.isNotEmpty())
                        return@collect
                    recentList.clear()
                    if (it.isEmpty()) {
                        if (viewModel.isInit) {
                            viewModel.isInit = false
                            binding.emojiPanel.setCurrentItem(1, false)
                        }
                        recentList.add(0, emptyList())
                    } else {
                        recentList.add(0, it.map { item ->
                            Pair(item.data, EmojiUtils.emojiMap[item.data] ?: R.mipmap.ic_launcher)
                        })
                    }
                    binding.emojiPanel.adapter?.notifyItemChanged(0)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.postFinished.collect {
                    if (it) {
                        closeDialog()
                        val intent = Intent()
                        if (type == "createFeed") {
                            this@ReplyActivity.makeToast("发布成功")
                        } else {
                            intent.putExtra("response_data", viewModel.responseData)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.captchaImg.collect { img ->
                    img?.let {
                        closeDialog()
                        val code = mutableStateOf("")
                        showAppDialog(this@ReplyActivity, "验证", actions = listOf(
                            DialogAction(getString(android.R.string.cancel)),
                            DialogAction("验证并继续", primary = true) {
                                viewModel.requestValidateData = hashMapOf("type" to "err_request_captcha",
                                    "code" to code.value, "mobile" to "", "idcard" to "", "name" to "")
                                viewModel.onPostRequestValidate()
                            },
                        )) {
                            androidx.compose.foundation.Image(img.asImageBitmap(), "验证码图片")
                            com.example.c001apk.compose.ui.component.design.TextField(
                                value = code.value, onValueChange = { code.value = it },
                                labelText = "验证码", singleLine = true)
                        }
                        viewModel.resetCaptcha()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.toastText.collect { text ->
                    text?.let {
                        this@ReplyActivity.makeToast(it)
                        viewModel.resetToast()
                        closeDialog()
                    }
                }
            }
        }

    }

    private fun closeDialog() {
        dialog?.dismiss()
        dialog = null
    }

    private fun initPage() {
        binding.checkBox.text = if (type == "createFeed") "仅自己可见"
        else "回复并转发"
        binding.title.text = if (type == "createFeed") "发布动态"
        else "回复"
        if (type != "createFeed" && !username.isNullOrEmpty()) {
            binding.editText.hint = "回复: " + username
            binding.replyTargetName?.isVisible = true
            binding.replyTargetName?.text = username
            binding.replyAvatar?.apply {
                isVisible = !avatar.isNullOrEmpty()
                setImageDrawable(null)
                if (!avatar.isNullOrEmpty()) {
                    load(avatar) {
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }
        binding.publish.isClickable = false
        title?.let {
            binding.editText.editableText.append("#${title}# ")
        }
    }

    private fun initEmojiPanel() {
        for (i in EmojiUtils.groupTitles.indices) {
            binding.indicator.addView(
                TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        weight = 1f
                    }
                    gravity = Gravity.CENTER
                    text = EmojiUtils.groupTitles[i]
                    background = AppCompatResources.getDrawable(
                        this@ReplyActivity,
                        R.drawable.selector_bg_trans
                    )
                    setOnClickListener {
                        binding.emojiPanel.setCurrentItem(i, false)
                    }
                    if (i == 0 && BuildConfig.DEBUG) {
                        setOnLongClickListener {
                            viewModel.deleteAll()
                            true
                        }
                    }
                }
            )
            if (i != EmojiUtils.groupTitles.lastIndex) {
                binding.indicator.addView(
                    View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            1.dp,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(
                            MaterialColors.getColor(
                                this@ReplyActivity,
                                com.google.android.material.R.attr.colorSurfaceVariant, 0
                            )
                        )
                    }
                )
            }
        }
        binding.emojiPanel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                for (i in 0 until binding.indicator.childCount) {
                    with(binding.indicator.getChildAt(i)) {
                        if (this is TextView) {
                            background = AppCompatResources.getDrawable(
                                this@ReplyActivity,
                                if (i / 2 == position) R.drawable.selector_emoji_indicator_selected
                                else R.drawable.selector_emoji_indicator
                            )
                            setTextColor(
                                if (i / 2 == position)
                                    MaterialColors.getColor(
                                        this@ReplyActivity,
                                        com.google.android.material.R.attr.colorOnPrimary, 0
                                    )
                                else
                                    MaterialColors.getColor(
                                        this@ReplyActivity,
                                        com.google.android.material.R.attr.colorControlNormal, 0
                                    )
                            )
                        }
                    }
                }
            }
        })

        binding.emojiPanel.adapter = EmojiPagerAdapter(
            list,
            onClickEmoji = {
                with(binding.editText) {
                    if (it == "[c001apk]") {
                        onBackSpace()
                    } else {
                        editableText.replace(selectionStart, selectionEnd, it)
                        viewModel.updateRecentEmoji(it)
                    }
                }
            },
            onCountStart = {
                countDownTimer.start()
            },
            onCountStop = {
                countDownTimer.cancel()
            }
        )
    }

    private val countDownTimer: CountDownTimer = object : CountDownTimer(100000, 50) {
        override fun onTick(millisUntilFinished: Long) {
            onBackSpace()
        }

        override fun onFinish() {}
    }

    private fun onBackSpace() {
        dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ViewCompat.performHapticFeedback(binding.editText, HapticFeedbackConstantsCompat.CONFIRM)
    }

    private fun initEditText() {
        val primaryColor = resolvePrimaryColor()
        val primaryDarkColor = resolvePrimaryDarkColor(primaryColor)
        val boxColor = color
        binding.editText.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            highlightColor = ColorUtils.setAlphaComponent(
                primaryDarkColor,
                128
            )
            post { setCursorColor(this, primaryColor) }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    setCursorColor(this, primaryColor)
                }
            }
            addTextChangedListener(EmojiTextWatcher(
                this@ReplyActivity, binding.editText.textSize,
                primaryColor
            ) {
                if (binding.editText.text.toString().trim().isBlank()) {
                    binding.publish.isClickable = false
                    binding.publish.setTextColor(getColor(android.R.color.darker_gray))
                } else {
                    binding.publish.isClickable = true
                    binding.publish.setTextColor(
                        primaryColor
                    )
                }
                if (currentUiStyle == UiStyle.MIUIX) viewPalette?.let {
                    viewStyle.updatePublish(binding.editText.text?.isNotBlank() == true, it)
                }
            })
            setOnKeyListener(FastDeleteAtUserKeyListener)
        }
        binding.textInputLayout.defaultHintTextColor = ColorStateList.valueOf(primaryColor)
        binding.textInputLayout.setBoxStrokeColor(primaryColor)
        binding.textInputLayout.setCounterTextColor(ColorStateList.valueOf(primaryColor))
        binding.textInputLayout.boxBackgroundColor = boxColor
        if (SDK_INT >= 29) {
            binding.textInputLayout.setCursorColor(ColorStateList.valueOf(primaryColor))
        }
    }

    private fun setCursorColor(editText: TextView, color: Int) {
        if (SDK_INT >= 29) {
            val width = max(2.dp, 2)
            val height = max(editText.textSize.toInt(), 1)
            val cursorDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
                setSize(width, height)
            }
            editText.textCursorDrawable = cursorDrawable
        }
    }

    override fun onDestroy() {
        exitHandler.removeCallbacks(finishFallback)
        super.onDestroy()
        cancelEmojiPanelEntranceAnimation()
        binding.main.animate().cancel()
        binding.inputLayout.animate().cancel()
        binding.emojiLayout.animate().cancel()
        closeDialog()
        countDownTimer.cancel()
        imeRetryHandler.removeCallbacks(imeRetryRunnable)
        externalLaunchHandler.removeCallbacks(externalLaunchTimeout)
        externalInputDescentAnimator?.apply {
            removeAllListeners()
            cancel()
        }
        externalInputDescentAnimator = null
        binding.main.overlay.remove(externalLaunchScrimDrawable)
        isWaitingForImeHide = false
        imeHideSuppressed = false
        isExternalInputDescentAnimating = false
        pendingExternalLaunch = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = SurfaceColors.SURFACE_1.getColor(this)
    }

    private fun requestInitialKeyboard() {
        if (!pendingShowKeyboard || !hasStartedEntrance || !hasWindowFocus() ||
            isExiting || isFinishing || isDestroyed) return
        pendingShowKeyboard = false
        showInput()
        startImeRetry()
    }

    private fun showInput() {
        if (isExiting || isFinishing || isDestroyed) return
        cancelEmojiPanelEntranceAnimation()
        isEmojiPanelRequested = false
        animateEmojiInputDescent = false
        if (binding.main is SmoothInputLayout)
            (binding.main as? SmoothInputLayout)?.showKeyboard()
        else
            binding.editText.let {
                it.requestFocus()
                it.requestFocusFromTouch()
                imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
                WindowCompat.getInsetsController(window, it)
                    .show(WindowInsetsCompat.Type.ime())
            }
        ViewCompat.requestApplyInsets(binding.main)
    }

    private fun startImeRetry() {
        imeRetryCount = 0
        imeRetryHandler.removeCallbacks(imeRetryRunnable)
        imeRetryHandler.postDelayed(imeRetryRunnable, imeRetryDelayMs)
    }

    private fun retryShowIme() {
        val isImeVisible = ViewCompat.getRootWindowInsets(binding.editText)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        if (isImeVisible || imeAnimating || isEmojiPanelRequested ||
            !hasWindowFocus() || isExiting || isFinishing || isDestroyed) {
            imeRetryHandler.removeCallbacks(imeRetryRunnable)
            return
        }
        showInput()
        if (imeRetryCount++ >= maxImeRetry) {
            imm.showSoftInput(binding.editText, InputMethodManager.SHOW_IMPLICIT)
            imeRetryHandler.removeCallbacks(imeRetryRunnable)
            return
        }
        imeRetryHandler.postDelayed(imeRetryRunnable, imeRetryDelayMs)
    }

    private fun showEmoji() {
        // A pending startup IME retry must not reopen the keyboard over an explicit panel request.
        imeRetryHandler.removeCallbacks(imeRetryRunnable)
        pendingShowKeyboard = false
        val insets = ViewCompat.getRootWindowInsets(binding.editText)
        val animatePanelEntrance = binding.main is SmoothInputLayout &&
            !isEmojiPanelVisible &&
            insets?.isVisible(WindowInsetsCompat.Type.ime()) != true
        isEmojiPanelEntranceAnimating = animatePanelEntrance
        isEmojiPanelRequested = true
        (binding.main as? SmoothInputLayout)?.showEmojiPanelBehindKeyboard(true)
        binding.main.doOnPreDraw {
            if (isEmojiPanelRequested) {
                if (animatePanelEntrance) {
                    animateEmojiPanelEntrance()
                } else if (insets != null) {
                    updateReplyPanelTranslation(
                        calculateInputTranslation(
                            imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,
                            navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        )
                    )
                }
                if (insets?.isVisible(WindowInsetsCompat.Type.ime()) == true) {
                    hideImeForEmoji()
                    ViewCompat.requestApplyInsets(binding.main)
                }
            }
        }
    }

    private fun animateEmojiPanelEntrance() {
        val panelHeight = max(
            binding.emojiLayout.measuredHeight,
            binding.emojiLayout.layoutParams.height
        ).toFloat()
        if (panelHeight <= 0f) {
            isEmojiPanelEntranceAnimating = false
            updateReplyPanelTranslation(0f)
            return
        }

        binding.inputLayout.animate().cancel()
        binding.emojiLayout.animate().cancel()
        binding.inputLayout.translationY = panelHeight
        binding.emojiLayout.translationY = panelHeight - 1f
        binding.inputLayout.animate()
            .translationY(0f)
            .setDuration(260L)
            .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))
            .start()
        binding.emojiLayout.animate()
            .translationY(-1f)
            .setDuration(260L)
            .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))
            .withEndAction {
                isEmojiPanelEntranceAnimating = false
                updateReplyPanelTranslation(0f)
                ViewCompat.requestApplyInsets(binding.main)
            }
            .start()
    }

    private fun cancelEmojiPanelEntranceAnimation() {
        // Keyboard retries must not reset an in-flight IME translation.
        if (!isEmojiPanelEntranceAnimating) return
        binding.inputLayout.animate().cancel()
        binding.emojiLayout.animate().cancel()
        isEmojiPanelEntranceAnimating = false
        binding.emojiLayout.alpha = 1f
        if (binding.bottomLayout == null) {
            binding.inputLayout.translationY = 0f
            binding.emojiLayout.translationY = 0f
        }
    }

    private fun hideImeForEmoji() {
        imm.hideSoftInputFromWindow(binding.editText.windowToken, 0)
        WindowCompat.getInsetsController(window, binding.editText)
            .hide(WindowInsetsCompat.Type.ime())
    }

    private fun closeEmojiPanelForIme(
        imeInset: Int = ViewCompat.getRootWindowInsets(binding.editText)
            ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0,
        navInset: Int = ViewCompat.getRootWindowInsets(binding.editText)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
    ) {
        (binding.main as? SmoothInputLayout)?.closeEmojiPanel()
        updateReplyPanelTranslation(-max(0, imeInset - navInset).toFloat())
    }

    private fun calculateInputTranslation(imeInset: Int, navInset: Int): Float {
        if (binding.bottomLayout != null) return 0f
        val imeHeight = max(0, imeInset - navInset)
        val emojiHeight = if (isEmojiPanelVisible) {
            max(binding.emojiLayout.measuredHeight, binding.emojiLayout.layoutParams.height)
        } else {
            0
        }
        return -max(0, imeHeight - emojiHeight).toFloat()
    }

    private fun smoothStep(fraction: Float): Float {
        return fraction * fraction * (3f - 2f * fraction)
    }

    @SuppressLint("InflateParams")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.atBtn -> {
                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                launchAtTopic("user")
            }

            R.id.tagBtn -> {
                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                launchAtTopic("topic")
            }

            R.id.imageBtn -> {
                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                launchPick()
            }

            R.id.otherImageBtn -> {
                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                launchDocumentPick()
            }

            R.id.emojiBtn -> {
                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                showEmoji()
            }

            R.id.checkBox ->
                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)

            R.id.publish -> {
                performConfiguredHapticFeedback(
                    fallback = {
                        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                    }
                )
                if (type == "createFeed") {
                    viewModel.replyAndFeedData["id"] = ""
                    viewModel.replyAndFeedData["message"] = binding.editText.text.toString()
                    viewModel.replyAndFeedData["type"] = "feed"
                    // 可见范围独立于 status，旧的 status=-1 已不再支持。
                    viewModel.replyAndFeedData["status"] = "1"
                    viewModel.replyAndFeedData["publish_status"] =
                        if (binding.checkBox.isChecked) "1" else "0"

                    targetType?.let {
                        if (it == "apk")
                            viewModel.replyAndFeedData["type"] = "comment"
                        viewModel.replyAndFeedData["targetType"] = it
                    }
                    targetId?.let {
                        viewModel.replyAndFeedData["targetId"] = it
                    }

                    if (uriList.isNotEmpty()) {
                        viewModel.onPostOSSUploadPrepare(imageList)
                    } else {
                        viewModel.onPostCreateFeed()
                    }
                } else {
                    viewModel.replyAndFeedData["message"] = binding.editText.text.toString()
                    viewModel.replyAndFeedData["replyAndForward"] =
                        if (binding.checkBox.isChecked) "1" else "0"
                    if (uriList.isNotEmpty()) {
                        viewModel.onPostOSSUploadPrepare(imageList)
                    } else {
                        viewModel.onPostReply()
                    }
                }
                showDialog()
            }

        }
    }

    private fun showDialog() {
        dialog = showAppDialog(this, "正在提交", actions = emptyList(), cancelable = false) {
            com.example.c001apk.compose.ui.component.design.ProgressContent()
        }
    }

    private fun launchAtTopic(type: String) {
        val intent = Intent(this, AtTopicActivity::class.java)
        intent.putExtra("type", type)
        launchAfterImeHidden {
            atTopicResultLauncher.launch(intent, createStationaryBackgroundOptions())
        }
    }

    private fun launchPick() {
        launchAfterImeHidden {
            try {
                pendingExternalReturn = true
                pickContent.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    createStationaryBackgroundOptions()
                )
            } catch (e: ActivityNotFoundException) {
                pendingExternalReturn = false
                makeToast("Activity Not Found")
                e.printStackTrace()
            }
        }
    }

    private fun launchDocumentPick() {
        launchAfterImeHidden {
            try {
                pendingExternalReturn = true
                pickDocument.launch(
                    arrayOf("image/*"),
                    createStationaryBackgroundOptions()
                )
            } catch (e: ActivityNotFoundException) {
                pendingExternalReturn = false
                makeToast("Activity Not Found")
                e.printStackTrace()
            }
        }
    }

    private fun launchAfterImeHidden(launch: () -> Unit) {
        pendingExternalLaunch = launch
        externalLaunchHandler.removeCallbacks(externalLaunchTimeout)
        val insets = ViewCompat.getRootWindowInsets(binding.editText)
        val imeVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true

        if (imeVisible) {
            isEmojiPanelRequested = false
            animateEmojiInputDescent = false
            isWaitingForImeHide = true
            imeHideSuppressed = true
            startExternalInputDescent()
            (binding.main as? SmoothInputLayout)?.closeKeyboard(false)
            WindowCompat.getInsetsController(window, binding.editText)
                .hide(WindowInsetsCompat.Type.ime())
            externalLaunchHandler.postDelayed(externalLaunchTimeout, 450L)
        } else {
            isWaitingForImeHide = false
            imeHideSuppressed = false
            isExternalInputDescentAnimating = false
            if (isEmojiPanelVisible) {
                (binding.main as? SmoothInputLayout)?.closeEmojiPanel()
                updateReplyPanelTranslation(0f)
            }
            binding.main.postOnAnimation { runPendingExternalLaunch() }
        }
    }

    private fun startExternalInputDescent() {
        externalInputDescentAnimator?.apply {
            removeAllListeners()
            cancel()
        }
        if (binding.bottomLayout != null || binding.inputLayout.translationY == 0f) {
            isExternalInputDescentAnimating = false
            return
        }

        val startTranslation = binding.inputLayout.translationY
        val showGapScrim = !isEmojiPanelVisible
        isExternalInputDescentAnimating = true
        binding.main.overlay.remove(externalLaunchScrimDrawable)
        if (showGapScrim) {
            updateExternalLaunchScrim(startTranslation)
            binding.main.overlay.add(externalLaunchScrimDrawable)
        }
        externalInputDescentAnimator = ValueAnimator.ofFloat(startTranslation, 0f).apply {
            duration = 240L
            interpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { animator ->
                val translation = animator.animatedValue as Float
                updateReplyPanelTranslation(translation)
                if (showGapScrim) {
                    updateExternalLaunchScrim(translation)
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    externalInputDescentAnimator = null
                    binding.main.overlay.remove(externalLaunchScrimDrawable)
                    updateReplyPanelTranslation(0f)
                    isExternalInputDescentAnimating = false
                    if (isExiting) {
                        animateExitSlideOut()
                    } else {
                        // 下降动画结束即启动二级界面，不必等 IME 收起动画完成
                        runPendingExternalLaunch(force = true)
                    }
                }
            })
            start()
        }
    }

    private fun updateExternalLaunchScrim(translation: Float) {
        val top = (binding.inputLayout.bottom + translation).roundToInt()
            .coerceIn(0, binding.main.height)
        externalLaunchScrimDrawable.setBounds(
            0,
            top,
            binding.main.width,
            binding.main.height
        )
    }

    private fun runPendingExternalLaunch(force: Boolean = false) {
        val launch = pendingExternalLaunch ?: return
        if (!force && (isWaitingForImeHide || isExternalInputDescentAnimating)) return
        pendingExternalLaunch = null
        externalLaunchHandler.removeCallbacks(externalLaunchTimeout)
        if (force) {
            externalInputDescentAnimator?.apply {
                removeAllListeners()
                cancel()
            }
            externalInputDescentAnimator = null
            binding.main.overlay.remove(externalLaunchScrimDrawable)
            isWaitingForImeHide = false
            isExternalInputDescentAnimating = false
        }
        updateReplyPanelTranslation(0f)
        launch()
    }

    private fun createStationaryBackgroundOptions(): ActivityOptionsCompat {
        return ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.activity_slide_in_right,
            R.anim.activity_stay
        )
    }

    @Suppress("DEPRECATION")
    private fun animateExternalActivityReturn() {
        overridePendingTransition(
            R.anim.activity_stay,
            R.anim.activity_slide_out_right
        )
    }

    private fun animateExternalActivityReturnIfPending() {
        if (pendingExternalReturn) {
            pendingExternalReturn = false
            animateExternalActivityReturn()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (view.id) {
            R.id.out -> {
                finish()
            }

            R.id.editText -> {
                if (motionEvent.action == MotionEvent.ACTION_DOWN && isEmojiPanelVisible) {
                    showInput()
                }
            }
        }
        return false
    }

    private fun updateReplyPanelTranslation(translation: Float) {
        // In the landscape layout, the editor stays beside the split IME instead of above it.
        binding.bottomLayout?.translationY = 0f
        if (binding.bottomLayout == null) {
            val alignedTranslation = translation.roundToInt().toFloat()
            val seamOverlap = if (isEmojiPanelVisible && alignedTranslation != 0f) 1f else 0f
            binding.inputLayout.translationY = alignedTranslation
            binding.emojiLayout.translationY = if (isEmojiPanelVisible) {
                alignedTranslation - seamOverlap
            } else {
                0f
            }
        }
    }

    override fun onVisibilityChange(visibility: Int) { // 0->visible, 8->gone
        isEmojiPanelVisible = visibility == VISIBLE
        if (!isEmojiPanelVisible && isEmojiPanelEntranceAnimating) {
            cancelEmojiPanelEntranceAnimation()
        }
        ViewCompat.requestApplyInsets(binding.main)
    }


    private fun installPredictiveReplyBack() {
        val initialDim = window.attributes.dimAmount
        val callback = object : OnBackPressedCallback(currentUiStyle == UiStyle.MIUIX) {
            private var progress = 0f
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                progress = 0f
                binding.main.animate().cancel()
            }
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (isExiting || currentUiStyle != UiStyle.MIUIX) return
                progress = backEvent.progress.coerceIn(0f, 1f)
                binding.main.translationY = binding.main.height * progress
                window.setDimAmount(initialDim * (1f - progress))
            }
            override fun handleOnBackCancelled() {
                progress = 0f
                binding.main.animate().translationY(0f).setDuration(180L).start()
                window.setDimAmount(initialDim)
            }
            override fun handleOnBackPressed() {
                if (progress == 0f || currentUiStyle != UiStyle.MIUIX) {
                    finish()
                } else if (!isExiting) {
                    isExiting = true
                    imeHideSuppressed = true
                    exitHandler.postDelayed(finishFallback, 800L)
                    binding.main.animate().translationY(binding.main.height.toFloat())
                        .setDuration((200L * (1f - progress)).toLong().coerceAtLeast(60L))
                        .withEndAction { finishAfterExitAnimation() }.start()
                }
            }
        }
        predictiveReplyBack = callback
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun finish() {
        if (isExiting) return
        isExiting = true
        // IME 回调可能取消退出动画，仍需保证 Activity 能正常结束。
        exitHandler.postDelayed(finishFallback, 800L)
        imeHideSuppressed = true
        if (isEmojiPanelVisible) {
            cancelEmojiPanelEntranceAnimation()
            binding.main.overlay.remove(imeScrimDrawable)
            binding.main.overlay.remove(externalLaunchScrimDrawable)
            animateExitSlideOut(includeEmojiPanel = true)
            return
        }
        val imeVisible = ViewCompat.getRootWindowInsets(binding.editText)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        if (imeVisible && binding.inputLayout.translationY != 0f) {
            startExternalInputDescent()
            (binding.main as? SmoothInputLayout)?.closeKeyboard(false)
            WindowCompat.getInsetsController(window, binding.editText)
                .hide(WindowInsetsCompat.Type.ime())
        } else {
            animateExitSlideOut(includeEmojiPanel = false)
        }
    }

    private fun animateExitSlideOut(includeEmojiPanel: Boolean = false) {
        binding.main.overlay.remove(imeScrimDrawable)
        binding.main.overlay.remove(externalLaunchScrimDrawable)
        val animatedView = if (includeEmojiPanel) binding.main else binding.inputLayout
        val height = if (includeEmojiPanel) {
            binding.main.height.toFloat()
        } else {
            binding.inputLayout.height.toFloat()
        }
        animatedView.animate()
            .translationY(height)
            .setDuration(200L)
            .setInterpolator(android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f))
            .withEndAction {
                finishAfterExitAnimation()
            }
            .start()
    }

    private fun finishAfterExitAnimation() {
        exitHandler.removeCallbacks(finishFallback)
        if (isFinishing || isDestroyed) return
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

}
