package io.github.a982246809.replyassistant.accessibility


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.doOnPreDraw
import io.github.a982246809.replyassistant.FloatingBallBounds
import io.github.a982246809.replyassistant.FloatingBallDockPosition
import io.github.a982246809.replyassistant.FloatingBallDockSide
import io.github.a982246809.replyassistant.FloatingBallInteractionState
import io.github.a982246809.replyassistant.FloatingBallOperationState
import io.github.a982246809.replyassistant.OverlayPixelDimensions
import io.github.a982246809.replyassistant.OverlaySizePolicy
import io.github.a982246809.replyassistant.FloatingBallTouchHandler
import io.github.a982246809.replyassistant.HoldScrollScheduler
import io.github.a982246809.replyassistant.HoldToScrollChatHistoryController
import io.github.a982246809.replyassistant.AccessibilityRefreshCoordinator
import io.github.a982246809.replyassistant.AccessibilityRefreshScheduler
import io.github.a982246809.replyassistant.AssistantChatKey
import io.github.a982246809.replyassistant.AssistantChatPolicy
import io.github.a982246809.replyassistant.AssistantChatPolicyState
import io.github.a982246809.replyassistant.AndroidFirstUseActivationSuccessOverlayHost
import io.github.a982246809.replyassistant.FirstUseActivationSuccessOverlay
import io.github.a982246809.replyassistant.HandlerFirstUseActivationSuccessOverlayScheduler
import io.github.a982246809.replyassistant.R
import io.github.a982246809.replyassistant.RealtimeSuggestionContext
import io.github.a982246809.replyassistant.RealtimeSuggestionRequestGate
import io.github.a982246809.replyassistant.SuggestionTriggerState
import io.github.a982246809.replyassistant.SuggestionFrameOrigin
import io.github.a982246809.replyassistant.SuggestionOrigin
import io.github.a982246809.replyassistant.VisibleChatMessagesRead
import io.github.a982246809.replyassistant.buildRealtimeSuggestionContext
import io.github.a982246809.replyassistant.buildFloatingBallPresentation
import io.github.a982246809.replyassistant.buildFloatingBallState
import io.github.a982246809.replyassistant.accessibilityRefreshEventTypes
import io.github.a982246809.replyassistant.calculateFloatingBallDockTarget
import io.github.a982246809.replyassistant.calculateFloatingBallPositionFromDock
import io.github.a982246809.replyassistant.clampFloatingBallPosition
import io.github.a982246809.replyassistant.WECHAT_SOURCE_PACKAGE
import io.github.a982246809.replyassistant.data.datastore.UserPreferences
import io.github.a982246809.replyassistant.data.repository.AiRepository
import io.github.a982246809.replyassistant.dto.AiRequestScene
import io.github.a982246809.replyassistant.dto.Msg
import io.github.a982246809.replyassistant.ai.AiProviderFailure
import io.github.a982246809.replyassistant.ai.AiProviderState
import io.github.a982246809.replyassistant.chatcontext.CapturedChatSnapshot
import io.github.a982246809.replyassistant.chatcontext.buildCapturedChatSnapshot
import io.github.a982246809.replyassistant.chatcontext.ConversationContextAssembler
import io.github.a982246809.replyassistant.chatcontext.ChatContextSession
import io.github.a982246809.replyassistant.chatcontext.ChatSessionIdentity
import io.github.a982246809.replyassistant.chatcontext.ChatSessionToken
import io.github.a982246809.replyassistant.chatcontext.ChatTextNormalizer
import io.github.a982246809.replyassistant.getOrNullAt
import io.github.a982246809.replyassistant.floatingBallStatusDurationMs
import io.github.a982246809.replyassistant.isWeChatChatInterfaceTitle
import io.github.a982246809.replyassistant.isWeChatGroupChatTitle
import io.github.a982246809.replyassistant.isWeChatDirectChatTitle
import io.github.a982246809.replyassistant.isWeChatNavigationBoundaryEvent
import io.github.a982246809.replyassistant.isSuggestionOriginCurrent
import io.github.a982246809.replyassistant.isSuggestionFrameCurrent
import io.github.a982246809.replyassistant.shouldShowFirstUseActivationSuccess
import io.github.a982246809.replyassistant.pickSuggestion
import io.github.a982246809.replyassistant.shouldShowFloatingBallLongPressHint
import io.github.a982246809.replyassistant.shouldRefreshAccessibilityState
import io.github.a982246809.replyassistant.toSuggestionOrigin
import io.github.a982246809.replyassistant.toPixels
import io.github.a982246809.replyassistant.ui.main.FloatAction
import io.github.a982246809.replyassistant.ui.main.mapper
import io.github.a982246809.replyassistant.util.Debounce
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

internal fun manualAiFailureMessage(error: Throwable): String {
    return error.message?.trim()?.takeIf { it.isNotEmpty() }
        ?: "请求失败，请稍后重试"
}

internal fun buildSuggestionTouchableRects(
    panelBounds: Rect,
    closeButtonBounds: Rect
): List<Rect> {
    return listOf(Rect(panelBounds), Rect(closeButtonBounds))
}

private data class CapturedWeChatPage(
    val snapshot: CapturedChatSnapshot,
    val inputText: String,
    val inputBounds: Rect?,
    val hasEditableInput: Boolean,
    val frameKey: WeChatVoiceFrameKey
)

private data class CapturedWeChatVisionPage(
    val snapshot: CapturedChatSnapshot,
    val inputText: String,
    val frameKey: WeChatVoiceFrameKey,
    val windowId: Int,
    val viewportEpoch: Long
)

private sealed interface CapturedWeChatPageRead {
    data class TextReady(val page: CapturedWeChatPage) : CapturedWeChatPageRead
    data class VisionRequired(val page: CapturedWeChatVisionPage) : CapturedWeChatPageRead
    data class Blocked(
        val reason: WeChatChatMessagesBlockedReason,
        val frameKey: WeChatVoiceFrameKey? = null
    ) : CapturedWeChatPageRead
}

private data class SnapshotFrameReference(
    val sessionGeneration: Long,
    val normalizedTitle: String,
    val messages: List<Msg>
)

private data class SnapshotFrameValidation(
    val frameKey: WeChatVoiceFrameKey,
    val visibleMessages: List<Msg>
)

internal enum class SuggestionSource(
    val selectable: Boolean = true
) {
    AUTOMATIC_LIST,
    AUTOMATIC_BUBBLE,
    MANUAL_LIST,
    SYSTEM_MESSAGE(selectable = false)
}

internal fun shouldCompleteFirstUseGuideForSuggestionList(
    windowShown: Boolean,
    source: SuggestionSource,
    suggestionCount: Int,
    opensProviderSetup: Boolean
): Boolean {
    return windowShown && source.selectable && suggestionCount > 0 && !opensProviderSetup
}

class ReplyAssistantService : AccessibilityService() {

    // Hilt EntryPoint 用于获取依赖
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReplyAssistantServiceEntryPoint {
        fun userPreferences(): UserPreferences
        fun aiRepository(): AiRepository
        fun conversationContextAssembler(): ConversationContextAssembler
        fun assistantChatPolicy(): AssistantChatPolicy
    }

    private lateinit var userPreferences: UserPreferences
    private lateinit var aiRepository: AiRepository
    private lateinit var conversationContextAssembler: ConversationContextAssembler
    private lateinit var assistantChatPolicy: AssistantChatPolicy
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val suggestionSubmitCoordinator = SuggestionSubmitCoordinator()
    val TAG = javaClass.simpleName

    private companion object {
        const val WECHAT_PACKAGE = WECHAT_SOURCE_PACKAGE
        const val WECHAT_INPUT_VIEW_ID = "com.tencent.mm:id/bkk"
        const val WECHAT_SEND_BUTTON_VIEW_ID = "com.tencent.mm:id/bql"
        const val WECHAT_MESSAGE_CONTAINER_VIEW_ID = "com.tencent.mm:id/bn1"
        const val AUTOMATIC_LIST_REPLY_COUNT = 2
        const val MANUAL_LIST_REPLY_COUNT = 5
        const val MANUAL_AI_REQUEST_TIMEOUT_MS = 60_000L
        const val FLOATING_BALL_SNAP_DURATION_MS = 200L
        const val FLOATING_BALL_COLLAPSE_DELAY_MS = 1000L
        const val OVERLAY_GEOMETRY_RETRY_DELAY_MS = 16L
        const val SNAPSHOT_FRAME_REFERENCE_LIMIT = 64
    }

    private lateinit var windowManager: WindowManager
    private var firstUseActivationSuccessOverlay: FirstUseActivationSuccessOverlay? = null
    private lateinit var floatingBallView: View
    private lateinit var floatingWindowView: View
    private lateinit var realtimeSuggestionView: View
    private lateinit var ballParams: WindowManager.LayoutParams
    private lateinit var windowParams: WindowManager.LayoutParams
    private lateinit var realtimeSuggestionParams: WindowManager.LayoutParams
    private lateinit var ballView: CardView
    private lateinit var floatingBallVisual: View
    private lateinit var floatingBallIcon: ImageView
    private lateinit var floatingBallTouchHandler: FloatingBallTouchHandler
    private lateinit var progressBar: ProgressBar
    private lateinit var listView: ListView
    private lateinit var loadingTextView: TextView
    private lateinit var emptyStateView: TextView
    private lateinit var suggestionPanel: View
    private lateinit var closeBar: View
    private lateinit var suggestionToolbar: View
    private lateinit var suggestionToolbarDivider: View
    private lateinit var gestureHintText: TextView
    private lateinit var refreshButton: ImageView
    private lateinit var closeButton: ImageButton
    private lateinit var realtimeSuggestionTextView: TextView

    private var pendingSuggestionTouchableRegionPreDrawListener:
        ViewTreeObserver.OnPreDrawListener? = null
    private val suggestionWindowLayoutChangeListener = View.OnLayoutChangeListener {
            _, _, _, _, _, _, _, _, _ ->
        updateSuggestionTouchableRegion()
    }
    private val suggestionWindowAttachStateChangeListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                scheduleSuggestionTouchableRegionUpdate(view)
            }

            override fun onViewDetachedFromWindow(view: View) {
                clearPendingSuggestionTouchableRegionUpdate(view)
            }
        }

    private var isBallAdded = false
    private var isWindowShowing = false
    private var isRealtimeSuggestionShowing = false
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private lateinit var overlayDimensions: OverlayPixelDimensions
    private var overlayFontScale: Float = 1f
    private var loadingText: String = ""
    private var realtimeSuggestionText: String? = null
    private var realtimeSuggestionOrigin: SuggestionOrigin? = null
    private var pendingRealtimeSuggestionKey: String? = null
    private var realtimeSuggestionJob: Job? = null
    private var listRequestJob: Job? = null
    private var floatingBallEnabledJob: Job? = null
    private var firstUseGuideCompletionJob: Job? = null
    private var assistantChatPolicyJob: Job? = null
    private var providerStateJob: Job? = null
    private var assistantChatPolicyState: AssistantChatPolicyState =
        AssistantChatPolicyState.Loading
    private var listRequestVersion: Long = 0
    private var pendingAutomaticListChatKey: String? = null
    private var automaticSuggestionsBlockedForSession = false
    private var floatingBallDockPosition = FloatingBallDockPosition()
    private var floatingBallInteractionState = FloatingBallInteractionState.COLLAPSED
    private var floatingBallOperationState = FloatingBallOperationState.IDLE
    private var activeFloatingBallActionId: Long? = null
    private var nextFloatingBallActionId: Long = 0L
    private var activeFloatingBallWasNormalTap = false
    private var hasShownFloatingBallLongPressHint = false
    private val firstUseGuideCompletionTracker = FirstUseGuideCompletionTracker()
    private var floatingBallWidthAnimator: ValueAnimator? = null
    private var floatingBallSnapAnimator: ValueAnimator? = null
    private var floatingBallPulseAnimator: ObjectAnimator? = null
    private var refreshAnimator: ObjectAnimator? = null
    private var suggestionListAdapter: SuggestionListAdapter? = null
    private var floatingBallCollapseRunnable: Runnable? = null
    private var floatingBallStatusResetRunnable: Runnable? = null
    private var floatingBallDockCorrectionGeneration: Long = 0L
    private var overlayGeometryRetryGeneration: Long = 0L
    private var overlayGeometryRetryRunnable: Runnable? = null
    private var overlayGeometryRetryAvailable = false
    private var floatingBallGeometryDirty = false
    private var suggestionListGeometryDirty = false
    private var realtimeSuggestionGeometryDirty = false
    private var holdToScrollChatHistoryController: HoldToScrollChatHistoryController? = null
    private lateinit var weChatPageReader: WeChatPageReader
    private lateinit var weChatWindowImageCapture: WeChatWindowImageCapture
    private var visualReplyJob: Job? = null
    private var weChatViewportInvalidationPending = false
    private var weChatViewportEpoch = 0L
    private val snapshotFrameReferences =
        object : LinkedHashMap<SnapshotFrameReference, SnapshotFrameValidation>(
            SNAPSHOT_FRAME_REFERENCE_LIMIT,
            0.75f,
            true
        ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<SnapshotFrameReference, SnapshotFrameValidation>?
            ): Boolean = size > SNAPSHOT_FRAME_REFERENCE_LIMIT
        }
    private var chatContextSession: ChatContextSession? = null
    private var floatingBallEnabled = false
    private var watchdogObservedGroupChat = false
    private var watchdogObservedDirectChatKey: AssistantChatKey? = null
    private var assistantPolicySuppressedChatKey: AssistantChatKey? = null
    private var assistantPolicyFailClosedSuppressed = false
    private var assistantPolicyUnavailableNoticeShown = false
    private var isServiceDestroying = false
    private val realtimeSuggestionGate = RealtimeSuggestionRequestGate()
    private val suggestionTriggerState = SuggestionTriggerState()
    
    // 存储已收集的聊天记录
    // private var cachedChatHistory = emptyArray<Msg>()

    // 添加广播接收器实例变量
    private lateinit var ballClickReceiver: BallClickReceiver
    private lateinit var setEditTextReceiver: SetEditTextReceiver
    private lateinit var renderListReceiver: RenderListReceiver

    private val debouncedRealtimeSuggestionRequest = Debounce.debounce<RealtimeSuggestionContext>(1000) { context ->
        requestRealtimeSuggestion(context)
    }
    
    //    接收到系统发送AccessibilityEvent时回调，
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return

            val eventPackageName = event.packageName?.toString()
            if (!shouldRefreshAccessibilityState(
                    eventType = event.eventType,
                    eventPackageName = eventPackageName,
                    ownPackageName = packageName
                )) {
                return
            }
            val forceNavigationBoundary = if (holdToScrollChatHistoryController != null) {
                isWeChatNavigationBoundaryEvent(event.eventType, eventPackageName)
            } else {
                isWeChatWindowNavigationEvent(event.eventType, eventPackageName)
            }

            if (shouldInvalidateWeChatViewport(
                    eventType = event.eventType,
                    eventPackageName = eventPackageName
                )) {
                invalidateWeChatViewport()
            }
            accessibilityRefreshCoordinator.request(forceNavigationBoundary)
        } catch (e: Exception) {
            Log.e(TAG, "处理无障碍事件异常", e)
        }
    }

    private fun isWeChatWindowNavigationEvent(
        eventType: Int,
        eventPackageName: String?
    ): Boolean {
        if (eventPackageName != WECHAT_PACKAGE) return false
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
    }

    private fun shouldInvalidateWeChatViewport(
        eventType: Int,
        eventPackageName: String?
    ): Boolean {
        if (eventPackageName != WECHAT_PACKAGE) return false
        return eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
    }

    private fun invalidateWeChatViewport() {
        if (weChatViewportInvalidationPending) return
        weChatViewportInvalidationPending = true
        weChatViewportEpoch += 1
        snapshotFrameReferences.clear()
    }

    override fun onInterrupt() {
        accessibilityRefreshCoordinator.cancel()
        invalidateChatContextSession()
        Log.d(TAG, "onInterrupt")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshOverlayGeometry(newConfig.fontScale)
    }

    // 用于定时检查微信聊天界面状态的Handler
    private val handler = Handler(Looper.getMainLooper())

    private val accessibilityRefreshCoordinator = AccessibilityRefreshCoordinator(
        scheduler = object : AccessibilityRefreshScheduler {
            override fun postDelayed(delayMs: Long, action: Runnable) {
                handler.postDelayed(action, delayMs)
            }

            override fun removeCallbacks(action: Runnable) {
                handler.removeCallbacks(action)
            }
        },
        onRefresh = ::refreshAccessibilityState
    )
    
    // 定时检查微信聊天界面的Runnable
    private val runnable = object : Runnable {
        override fun run() {
            try {
                val root = rootInActiveWindow
                val sourcePackage = root?.packageName?.toString()
                val title = root
                    ?.takeIf { sourcePackage == WECHAT_PACKAGE }
                    ?.let(::readChatTitle)
                val isGroupChat = title?.let(::isWeChatGroupChatTitle) == true
                val directChatKey = assistantChatKey(sourcePackage, title)
                val isAssistantAllowed = isAssistantChatAllowed(directChatKey)
                if (isGroupChat) {
                    if (!watchdogObservedGroupChat) {
                        suppressSuggestionsForGroupChat()
                    }
                } else {
                    if (directChatKey != watchdogObservedDirectChatKey) {
                        if (directChatKey != null && !isAssistantAllowed) {
                            suppressAssistantForIneligibleChat(directChatKey)
                        } else {
                            if (isAssistantAllowed) {
                                assistantPolicySuppressedChatKey = null
                            }
                            invalidateSuggestionsForChatIdentityChange()
                            accessibilityRefreshCoordinator.request(
                                forceNavigationBoundary = false
                            )
                        }
                    } else if (directChatKey != null && !isAssistantAllowed) {
                        suppressAssistantForIneligibleChat(directChatKey)
                    }
                    updateFloatingBallVisibility(
                        visible = floatingBallEnabled && isAssistantAllowed
                    )
                }
                watchdogObservedGroupChat = isGroupChat
                watchdogObservedDirectChatKey = directChatKey
            } catch (e: Exception) {
                Log.e(TAG, "定时检查微信聊天界面状态异常", e)
            } finally {
                // 每秒执行一次
                handler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * 检查用户是否启用了悬浮球
     * @return 悬浮球是否启用
     */
    private fun isFloatingBallEnabled(): Boolean {
        return floatingBallEnabled
    }

    /**
     * 发送AI请求的公共方法
     */
    private fun sendAiRequest(
        snapshot: CapturedChatSnapshot,
        inputText: String,
        floatingBallActionId: Long? = null
    ) {
        if (!canUseSuggestionsInCurrentChat() ||
            !isAssistantChatAllowed(snapshotAssistantChatKey(snapshot))
        ) {
            cancelFloatingBallAction(floatingBallActionId)
            return
        }
        if (snapshot.messages.isEmpty()) {
            renderResponseList(
                listOf("聊天记录为空"),
                source = SuggestionSource.SYSTEM_MESSAGE,
                floatingBallActionId = floatingBallActionId
            )
            return
        }

        val requestVersion = beginListRequest()

        // 使用协程发送 AI 请求
        listRequestJob = serviceScope.launch {
            try {
                val result = withTimeout(MANUAL_AI_REQUEST_TIMEOUT_MS) {
                    val aiContext = conversationContextAssembler.assemble(
                        snapshot = snapshot,
                        inputText = inputText
                    )
                    if (requestVersion != listRequestVersion || !isSnapshotCurrent(snapshot)) {
                        return@withTimeout null
                    }
                    aiRepository.generateReplies(
                        context = aiContext,
                        replyCountOverride = MANUAL_LIST_REPLY_COUNT
                    )
                }

                if (result == null) {
                    finishManualRequestWithError(
                        requestVersion = requestVersion,
                        snapshot = snapshot,
                        floatingBallActionId = floatingBallActionId,
                        message = "读取聊天记录失败，请重试"
                    )
                    return@launch
                }

                if (requestVersion != listRequestVersion || !isSnapshotCurrent(snapshot)) {
                    finishManualRequestWithError(
                        requestVersion = requestVersion,
                        snapshot = snapshot,
                        floatingBallActionId = floatingBallActionId,
                        message = "读取聊天记录失败，请重试"
                    )
                    return@launch
                }

                result.fold(
                    onSuccess = success@{ choices ->
                        if (requestVersion != listRequestVersion || !isSnapshotCurrent(snapshot)) {
                            finishManualRequestWithError(
                                requestVersion = requestVersion,
                                snapshot = snapshot,
                                floatingBallActionId = floatingBallActionId,
                                message = "读取聊天记录失败，请重试"
                            )
                            return@success
                        }
                        Log.i(TAG, "AI 回复请求完成: count=${choices.size}")
                        renderResponseList(
                            choices.take(MANUAL_LIST_REPLY_COUNT),
                            source = SuggestionSource.MANUAL_LIST,
                            floatingBallActionId = floatingBallActionId,
                            origin = snapshot.toSuggestionOrigin()
                        )
                    },
                    onFailure = failure@{ error ->
                        if (requestVersion != listRequestVersion || !isSnapshotCurrent(snapshot)) {
                            finishManualRequestWithError(
                                requestVersion = requestVersion,
                                snapshot = snapshot,
                                floatingBallActionId = floatingBallActionId,
                                message = "读取聊天记录失败，请重试"
                            )
                            return@failure
                        }
                        if (error is AiProviderFailure.NotConfigured) {
                            renderResponseList(
                                listOf("请先配置 AI 服务，点击这里打开设置"),
                                "PROVIDER_NOT_CONFIGURED",
                                SuggestionSource.SYSTEM_MESSAGE,
                                floatingBallActionId
                            )
                        } else {
                            Log.w(TAG, "AI 回复请求失败: ${error.javaClass.simpleName}")
                            renderResponseList(
                                listOf(manualAiFailureMessage(error)),
                                source = SuggestionSource.SYSTEM_MESSAGE,
                                floatingBallActionId = floatingBallActionId
                            )
                        }
                    }
                )
            } catch (error: TimeoutCancellationException) {
                finishManualRequestWithError(
                    requestVersion = requestVersion,
                    snapshot = snapshot,
                    floatingBallActionId = floatingBallActionId,
                    message = "生成回复超时，请重试"
                )
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                if (requestVersion != listRequestVersion || !isSnapshotCurrent(snapshot)) {
                    finishManualRequestWithError(
                        requestVersion = requestVersion,
                        snapshot = snapshot,
                        floatingBallActionId = floatingBallActionId,
                        message = "读取聊天记录失败，请重试"
                    )
                } else {
                    Log.e(TAG, "AI 请求异常", e)
                    renderResponseList(
                        listOf("请求失败: ${e.message}"),
                        source = SuggestionSource.SYSTEM_MESSAGE,
                        floatingBallActionId = floatingBallActionId
                    )
                }
            }
        }
    }

    private fun finishManualRequestWithError(
        requestVersion: Long,
        snapshot: CapturedChatSnapshot,
        floatingBallActionId: Long?,
        message: String
    ) {
        if (requestVersion != listRequestVersion) {
            cancelFloatingBallAction(floatingBallActionId)
            return
        }

        val isSameEligibleChat = canUseSuggestionsInCurrentChat() &&
            currentAssistantChatKey() == snapshotAssistantChatKey(snapshot)
        if (!isSameEligibleChat) {
            hideFloatingWindow()
            clearRenderedSuggestionList()
            cancelFloatingBallAction(floatingBallActionId)
            return
        }

        renderResponseList(
            listOf(message),
            source = SuggestionSource.SYSTEM_MESSAGE,
            floatingBallActionId = floatingBallActionId
        )
    }

    private fun requestAutomaticResponseList(snapshot: CapturedChatSnapshot): Boolean {
        if (!canUseSuggestionsInCurrentChat() ||
            !isAssistantChatAllowed(snapshotAssistantChatKey(snapshot))
        ) return false
        if (automaticSuggestionsBlockedForSession) return false
        if (floatingBallOperationState == FloatingBallOperationState.LOADING) return false
        if (snapshot.messages.isEmpty()) return false

        val requestVersion = beginListRequest()
        listRequestJob = serviceScope.launch {
            try {
                val aiContext = conversationContextAssembler.assemble(
                    snapshot = snapshot,
                    inputText = null
                )
                if (!isCurrentListRequest(requestVersion, snapshot)) return@launch
                val result = aiRepository.generateReplies(
                    context = aiContext,
                    replyCountOverride = AUTOMATIC_LIST_REPLY_COUNT,
                    requestScene = AiRequestScene.REALTIME_SUGGESTION
                )

                if (!isCurrentListRequest(requestVersion, snapshot)) return@launch
                result.fold(
                    onSuccess = success@{ choices ->
                        if (automaticSuggestionsBlockedForSession ||
                            !isCurrentListRequest(requestVersion, snapshot)
                        ) return@success
                        val visibleChoices = choices
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .take(AUTOMATIC_LIST_REPLY_COUNT)
                        if (visibleChoices.isNotEmpty()) {
                            renderResponseList(
                                visibleChoices,
                                source = SuggestionSource.AUTOMATIC_LIST,
                                origin = snapshot.toSuggestionOrigin()
                            )
                        }
                    },
                    onFailure = { error ->
                        if (isCurrentListRequest(requestVersion, snapshot)) {
                            if (error is AiProviderFailure.NotConfigured) {
                                pauseAutomaticSuggestionsForSession("请先配置 AI 服务")
                            }
                            Log.w(TAG, "自动普通建议请求失败: ${error.javaClass.simpleName}")
                        }
                    }
                )
            } catch (e: Exception) {
                if (isCurrentListRequest(requestVersion, snapshot)) {
                    Log.e(TAG, "自动普通建议请求异常", e)
                }
            }
        }
        return true
    }

    private fun beginListRequest(): Long {
        invalidateListRequest()
        return listRequestVersion
    }

    private fun invalidateListRequest() {
        listRequestVersion += 1
        listRequestJob?.cancel()
        listRequestJob = null
        visualReplyJob?.cancel()
        visualReplyJob = null
    }

    private fun isCurrentListRequest(
        requestVersion: Long,
        snapshot: CapturedChatSnapshot
    ): Boolean {
        return requestVersion == listRequestVersion &&
            canUseSuggestionsInCurrentChat() &&
            isSnapshotCurrent(snapshot)
    }

    private fun isInputMethodVisible(): Boolean {
        return windows.any { window ->
            window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }
    }

    private fun handleRealtimeSuggestionContextChanged(page: CapturedWeChatPage) {
        if (!::userPreferences.isInitialized || !::aiRepository.isInitialized) {
            return
        }

        if (!canUseSuggestionsInCurrentChat() ||
            !isAssistantChatAllowed(snapshotAssistantChatKey(page.snapshot))
        ) {
            resetRealtimeSuggestionState()
            return
        }

        if (automaticSuggestionsBlockedForSession) {
            resetRealtimeSuggestionState()
            return
        }

        val context = buildRealtimeSuggestionContext(page.snapshot, page.inputText)
        if (context == null) {
            resetRealtimeSuggestionState()
            return
        }

        if (!realtimeSuggestionGate.shouldRequest(context)) {
            return
        }

        pendingRealtimeSuggestionKey = context.requestKey
        realtimeSuggestionJob?.cancel()
        realtimeSuggestionJob = null
        hideRealtimeSuggestion()
        debouncedRealtimeSuggestionRequest(context)
    }

    private fun requestRealtimeSuggestion(context: RealtimeSuggestionContext) {
        if (context.requestKey != pendingRealtimeSuggestionKey) return

        if (automaticSuggestionsBlockedForSession || !canUseSuggestionsInCurrentChat()) {
            resetRealtimeSuggestionState()
            return
        }

        realtimeSuggestionJob = serviceScope.launch {
            try {
                val aiContext = conversationContextAssembler.assemble(
                    snapshot = context.snapshot,
                    inputText = context.inputText
                )
                if (context.requestKey != pendingRealtimeSuggestionKey ||
                    !isSnapshotCurrent(context.snapshot)
                ) {
                    return@launch
                }
                val result = aiRepository.generateReplies(
                    context = aiContext,
                    requestScene = AiRequestScene.REALTIME_SUGGESTION
                )

                if (context.requestKey != pendingRealtimeSuggestionKey ||
                    !isSnapshotCurrent(context.snapshot)
                ) return@launch

                result.fold(
                    onSuccess = success@{ choices ->
                        if (automaticSuggestionsBlockedForSession ||
                            context.requestKey != pendingRealtimeSuggestionKey ||
                            !isSnapshotCurrent(context.snapshot)
                        ) return@success
                        val suggestion = pickSuggestion(choices)
                        if (suggestion == null) {
                            hideRealtimeSuggestion()
                        } else {
                            renderRealtimeSuggestion(
                                suggestion,
                                origin = context.snapshot.toSuggestionOrigin()
                            )
                        }
                    },
                    onFailure = failure@{ error ->
                        if (context.requestKey != pendingRealtimeSuggestionKey ||
                            !isSnapshotCurrent(context.snapshot)
                        ) return@failure
                        if (error is AiProviderFailure.NotConfigured) {
                            pauseAutomaticSuggestionsForSession("请先配置 AI 服务")
                        }
                        Log.w(TAG, "实时建议请求失败: ${error.javaClass.simpleName}")
                        hideRealtimeSuggestion()
                    }
                )
            } catch (e: Exception) {
                if (context.requestKey != pendingRealtimeSuggestionKey ||
                    !isSnapshotCurrent(context.snapshot)
                ) return@launch
                Log.e(TAG, "实时建议请求异常", e)
                hideRealtimeSuggestion()
            }
        }
    }

    private fun initOverlayWindows() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        refreshOverlayGeometry(resources.configuration.fontScale)

        initFloatingBall()
        initFloatingWindow()
        initRealtimeSuggestionWindow()
    }

    private fun resolveOverlayViewport(): Pair<Int, Int> {
        val fallback = resources.displayMetrics.let { metrics ->
            metrics.widthPixels.coerceAtLeast(1) to metrics.heightPixels.coerceAtLeast(1)
        }
        if (!::windowManager.isInitialized || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return fallback
        }
        return try {
            val bounds = windowManager.currentWindowMetrics.bounds
            val width = bounds.width()
            val height = bounds.height()
            if (width > 0 && height > 0) width to height else fallback
        } catch (error: Exception) {
            Log.e(TAG, "读取悬浮窗窗口尺寸异常", error)
            fallback
        }
    }

    private fun resolveOverlayDimensions(widthPx: Int): OverlayPixelDimensions {
        val validDensity = resources.displayMetrics.density
            .takeIf { it.isFinite() && it > 0f }
        val widthDp = if (widthPx > 1 && validDensity != null) {
            widthPx / validDensity
        } else {
            Float.NaN
        }
        return OverlaySizePolicy.resolve(widthDp).toPixels(validDensity ?: 1f)
    }

    private fun ensureOverlayDimensions(): OverlayPixelDimensions {
        if (!::overlayDimensions.isInitialized) {
            val resolvedWidth = screenWidth.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels.coerceAtLeast(1)
            overlayDimensions = resolveOverlayDimensions(resolvedWidth)
        }
        return overlayDimensions
    }

    private fun refreshOverlayGeometry(newFontScale: Float, allowRetry: Boolean = true) {
        if (isServiceDestroying || !::windowManager.isInitialized) return
        if (allowRetry) {
            cancelOverlayGeometryRetry()
            overlayGeometryRetryAvailable = true
        }

        val viewport = resolveOverlayViewport()
        val newDimensions = resolveOverlayDimensions(viewport.first)
        val dimensionsChanged = !::overlayDimensions.isInitialized ||
            overlayDimensions != newDimensions
        val resolvedFontScale = newFontScale
            .takeIf { it.isFinite() && it > 0f }
            ?: 1f
        val fontScaleChanged = overlayFontScale != resolvedFontScale
        val viewportChanged = screenWidth != viewport.first || screenHeight != viewport.second
        val hasDirtyGeometry = floatingBallGeometryDirty ||
            suggestionListGeometryDirty ||
            realtimeSuggestionGeometryDirty
        val shouldRefreshBall = dimensionsChanged || viewportChanged ||
            floatingBallGeometryDirty
        if (
            !dimensionsChanged &&
            !fontScaleChanged &&
            !viewportChanged &&
            !hasDirtyGeometry
        ) {
            return
        }

        if (shouldRefreshBall) {
            if (::floatingBallTouchHandler.isInitialized) {
                floatingBallTouchHandler.cancelGesture()
            }
            if (
                floatingBallInteractionState == FloatingBallInteractionState.PRESSED ||
                floatingBallInteractionState == FloatingBallInteractionState.DRAGGING
            ) {
                floatingBallInteractionState = FloatingBallInteractionState.EXPANDED
            }
            floatingBallSnapAnimator?.cancel()
            floatingBallSnapAnimator = null
            floatingBallWidthAnimator?.cancel()
            floatingBallWidthAnimator = null
        }

        screenWidth = viewport.first
        screenHeight = viewport.second
        overlayDimensions = newDimensions
        overlayFontScale = resolvedFontScale

        if (shouldRefreshBall) {
            refreshFloatingBallGeometry(newDimensions)
            scheduleFloatingBallDockCorrection()
            if (
                floatingBallInteractionState == FloatingBallInteractionState.EXPANDED &&
                floatingBallOperationState == FloatingBallOperationState.IDLE
            ) {
                scheduleFloatingBallCollapse()
            }
        }
        if (dimensionsChanged || fontScaleChanged || suggestionListGeometryDirty) {
            refreshSuggestionListGeometry(
                newDimensions,
                forceRowRebind = fontScaleChanged
            )
        }
        if (
            dimensionsChanged ||
            fontScaleChanged ||
            viewportChanged ||
            realtimeSuggestionGeometryDirty
        ) {
            refreshRealtimeSuggestionGeometry(newDimensions)
        }
        if (
            allowRetry &&
            (floatingBallGeometryDirty ||
                suggestionListGeometryDirty ||
                realtimeSuggestionGeometryDirty)
        ) {
            scheduleOverlayGeometryRetry()
        }
    }

    private fun cancelOverlayGeometryRetry() {
        overlayGeometryRetryGeneration += 1
        overlayGeometryRetryRunnable?.let(handler::removeCallbacks)
        overlayGeometryRetryRunnable = null
        overlayGeometryRetryAvailable = false
    }

    private fun scheduleOverlayGeometryRetry() {
        if (
            overlayGeometryRetryRunnable != null ||
            !overlayGeometryRetryAvailable ||
            isServiceDestroying
        ) {
            return
        }
        overlayGeometryRetryAvailable = false
        val generation = overlayGeometryRetryGeneration
        val retry = Runnable {
            if (generation != overlayGeometryRetryGeneration || isServiceDestroying) {
                return@Runnable
            }
            overlayGeometryRetryRunnable = null
            refreshOverlayGeometry(overlayFontScale, allowRetry = false)
        }
        overlayGeometryRetryRunnable = retry
        handler.postDelayed(retry, OVERLAY_GEOMETRY_RETRY_DELAY_MS)
    }

    private fun refreshFloatingBallGeometry(dimensions: OverlayPixelDimensions) {
        if (!::floatingBallView.isInitialized || !::ballParams.isInitialized) return
        try {
            ballParams.width = dimensions.ball.rootSizePx
            ballParams.height = dimensions.ball.rootSizePx
            val restored = calculateFloatingBallPositionFromDock(
                floatingBallDockPosition,
                floatingBallBounds()
            )
            ballParams.x = restored.first
            ballParams.y = restored.second
            renderFloatingBallPresentation(animateWidth = false)
            floatingBallGeometryDirty = isBallAdded &&
                !updateOverlayViewLayout(floatingBallView, ballParams)
        } catch (error: Exception) {
            floatingBallGeometryDirty = true
            Log.e(TAG, "刷新悬浮球尺寸异常", error)
        }
    }

    private fun refreshSuggestionListGeometry(
        dimensions: OverlayPixelDimensions,
        forceRowRebind: Boolean
    ) {
        if (!::floatingWindowView.isInitialized || !::windowParams.isInitialized) return
        try {
            windowParams.width = dimensions.list.windowWidthPx
            OverlayGeometryApplier.applySuggestionList(floatingWindowView, dimensions.list)
            suggestionListAdapter?.updateDimensions(
                dimensions.list,
                forceRebind = forceRowRebind
            )
            if (::listView.isInitialized && suggestionListAdapter != null) {
                listView.invalidateViews()
            }
            if (isWindowShowing) {
                suggestionListGeometryDirty = !updateOverlayViewLayout(
                    floatingWindowView,
                    windowParams
                )
                floatingWindowView.post {
                    scheduleSuggestionTouchableRegionUpdate(floatingWindowView)
                }
            } else {
                suggestionListGeometryDirty = false
            }
        } catch (error: Exception) {
            suggestionListGeometryDirty = true
            Log.e(TAG, "刷新建议列表尺寸异常", error)
        }
    }

    private fun refreshRealtimeSuggestionGeometry(dimensions: OverlayPixelDimensions) {
        if (!::realtimeSuggestionView.isInitialized ||
            !::realtimeSuggestionParams.isInitialized
        ) {
            return
        }
        try {
            OverlayGeometryApplier.applyRealtimeSuggestion(
                realtimeSuggestionView,
                dimensions.realtime
            )
            realtimeSuggestionParams.x = dimensions.realtime.horizontalInsetPx
            updateRealtimeSuggestionPosition()
            if (isRealtimeSuggestionShowing) {
                realtimeSuggestionGeometryDirty = !updateOverlayViewLayout(
                    realtimeSuggestionView,
                    realtimeSuggestionParams
                )
            } else {
                realtimeSuggestionGeometryDirty = false
            }
        } catch (error: Exception) {
            realtimeSuggestionGeometryDirty = true
            Log.e(TAG, "刷新实时建议尺寸异常", error)
        }
    }

    private fun initFloatingBall() {
        val dimensions = ensureOverlayDimensions().ball
        floatingBallView = LayoutInflater.from(this).inflate(R.layout.floating_ball, null)
        ballView = floatingBallView.findViewById(R.id.ballView)
        floatingBallVisual = floatingBallView.findViewById(R.id.floatingBallVisual)
        floatingBallIcon = floatingBallView.findViewById(R.id.floatingBallIcon)
        floatingBallDockPosition = userPreferences.getFloatingBallDockPositionBlocking()
        hasShownFloatingBallLongPressHint =
            userPreferences.hasShownFloatingBallLongPressHintBlocking()
        firstUseGuideCompletionTracker.update(
            userPreferences.hasCompletedFirstUseGuideBlocking()
        )

        ballParams = WindowManager.LayoutParams(
            dimensions.rootSizePx,
            dimensions.rootSizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val restored = calculateFloatingBallPositionFromDock(
                floatingBallDockPosition,
                floatingBallBounds()
            )
            x = restored.first
            y = restored.second
        }
        windowManager.addView(floatingBallView, ballParams)
        isBallAdded = true
        updateFloatingBallVisibility(visible = false)

        ballView.isHapticFeedbackEnabled = true
        renderFloatingBallPresentation(animateWidth = false)
        floatingBallTouchHandler = FloatingBallTouchHandler(
            touchSlopPx = ViewConfiguration.get(this).scaledTouchSlop,
            positionProvider = { ballParams.x to ballParams.y },
            onPressedChanged = ::handleFloatingBallPressed,
            onPositionChanged = ::handleFloatingBallDragged,
            onDragFinished = ::handleFloatingBallDragFinished,
            onTap = { onClickBallView(enableScrollCollection = false) },
            onLongPress = { onClickBallView(enableScrollCollection = true) },
            onLongPressReleased = ::stopHoldToScrollChatHistory
        )
        ballView.setOnTouchListener(floatingBallTouchHandler)
        scheduleFloatingBallDockCorrection()
    }

    private fun scheduleFloatingBallDockCorrection() {
        if (!::floatingBallView.isInitialized || !::ballParams.isInitialized || !isBallAdded) {
            return
        }
        val generation = ++floatingBallDockCorrectionGeneration
        floatingBallView.requestApplyInsets()
        floatingBallView.doOnPreDraw {
            if (
                generation != floatingBallDockCorrectionGeneration ||
                isServiceDestroying ||
                !isBallAdded ||
                floatingBallInteractionState == FloatingBallInteractionState.PRESSED ||
                floatingBallInteractionState == FloatingBallInteractionState.DRAGGING
            ) {
                return@doOnPreDraw
            }
            try {
                val restored = calculateFloatingBallPositionFromDock(
                    floatingBallDockPosition,
                    floatingBallBounds()
                )
                if (
                    ballParams.x == restored.first &&
                    ballParams.y == restored.second &&
                    !floatingBallGeometryDirty
                ) {
                    return@doOnPreDraw
                }
                ballParams.x = restored.first
                ballParams.y = restored.second
                renderFloatingBallPresentation(animateWidth = false)
                floatingBallGeometryDirty = !updateOverlayViewLayout(
                    floatingBallView,
                    ballParams
                )
                if (floatingBallGeometryDirty) {
                    scheduleOverlayGeometryRetry()
                }
            } catch (error: Exception) {
                floatingBallGeometryDirty = true
                scheduleOverlayGeometryRetry()
                Log.e(TAG, "二次校正悬浮球位置异常", error)
            }
        }
    }

    private fun handleFloatingBallPressed(isPressed: Boolean) {
        cancelFloatingBallCollapse()
        if (isPressed) {
            floatingBallInteractionState = FloatingBallInteractionState.PRESSED
            renderFloatingBallPresentation()
        } else if (floatingBallInteractionState != FloatingBallInteractionState.DRAGGING) {
            floatingBallInteractionState = FloatingBallInteractionState.EXPANDED
            renderFloatingBallPresentation()
            scheduleFloatingBallCollapse()
        }
    }

    private fun handleFloatingBallDragged(nextX: Int, nextY: Int) {
        floatingBallInteractionState = FloatingBallInteractionState.DRAGGING
        val clamped = clampFloatingBallPosition(nextX, nextY, floatingBallBounds())
        ballParams.x = clamped.first
        ballParams.y = clamped.second
        renderFloatingBallPresentation()
        updateOverlayViewLayout(floatingBallView, ballParams)
    }

    private fun handleFloatingBallDragFinished() {
        val target = calculateFloatingBallDockTarget(
            ballParams.x,
            ballParams.y,
            floatingBallBounds()
        )
        floatingBallDockPosition = target.position
        serviceScope.launch {
            userPreferences.setFloatingBallDockPosition(target.position)
        }
        floatingBallInteractionState = FloatingBallInteractionState.EXPANDED
        animateFloatingBallTo(target.x, target.y) {
            renderFloatingBallPresentation()
            scheduleFloatingBallCollapse()
        }
    }

    private fun floatingBallBounds(): FloatingBallBounds {
        val dimensions = ensureOverlayDimensions().ball
        val rootSize = dimensions.rootSizePx
        val verticalMargin = dimensions.verticalMarginPx
        val displayFrame = Rect(0, 0, screenWidth, screenHeight)
        var resolvedFromInsets = false
        if (::floatingBallView.isInitialized) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                floatingBallView.rootWindowInsets?.let { rootInsets ->
                    val insets = rootInsets.getInsets(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.ime()
                    )
                    displayFrame.set(
                        insets.left,
                        insets.top,
                        screenWidth - insets.right,
                        screenHeight - insets.bottom
                    )
                    resolvedFromInsets = true
                }
            }
            if (!resolvedFromInsets) {
                val visibleFrame = Rect()
                floatingBallView.getWindowVisibleDisplayFrame(visibleFrame)
                if (visibleFrame.width() > 0 && visibleFrame.height() > 0) {
                    displayFrame.set(visibleFrame)
                }
            }
        }
        val minX = displayFrame.left.coerceAtLeast(0)
        val maxX = (displayFrame.right - rootSize).coerceAtLeast(minX)
        val minY = (displayFrame.top + verticalMargin).coerceAtLeast(0)
        val maxY = (displayFrame.bottom - rootSize - verticalMargin).coerceAtLeast(minY)
        return FloatingBallBounds(minX, maxX, minY, maxY)
    }

    private fun animateFloatingBallTo(targetX: Int, targetY: Int, onEnd: () -> Unit) {
        floatingBallSnapAnimator?.cancel()
        val startX = ballParams.x
        val startY = ballParams.y
        floatingBallSnapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FLOATING_BALL_SNAP_DURATION_MS
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                ballParams.x = startX + ((targetX - startX) * fraction).toInt()
                ballParams.y = startY + ((targetY - startY) * fraction).toInt()
                updateOverlayViewLayout(floatingBallView, ballParams)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var wasCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    wasCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (wasCancelled) return
                    ballParams.x = targetX
                    ballParams.y = targetY
                    onEnd()
                }
            })
            start()
        }
    }

    private fun scheduleFloatingBallCollapse() {
        cancelFloatingBallCollapse()
        if (floatingBallOperationState != FloatingBallOperationState.IDLE) return
        val action = Runnable {
            if (floatingBallOperationState == FloatingBallOperationState.IDLE) {
                floatingBallInteractionState = FloatingBallInteractionState.COLLAPSED
                renderFloatingBallPresentation()
            }
        }
        floatingBallCollapseRunnable = action
        handler.postDelayed(action, FLOATING_BALL_COLLAPSE_DELAY_MS)
    }

    private fun cancelFloatingBallCollapse() {
        floatingBallCollapseRunnable?.let(handler::removeCallbacks)
        floatingBallCollapseRunnable = null
    }

    private fun renderFloatingBallPresentation(animateWidth: Boolean = true) {
        if (!::floatingBallVisual.isInitialized || !::floatingBallIcon.isInitialized) return
        val presentation = buildFloatingBallPresentation(
            floatingBallInteractionState,
            floatingBallOperationState
        )
        val side = if (::ballParams.isInitialized && ballParams.x <= screenWidth / 2) {
            FloatingBallDockSide.LEFT
        } else {
            FloatingBallDockSide.RIGHT
        }
        val dimensions = ensureOverlayDimensions().ball
        OverlayGeometryApplier.applyFloatingBall(
            root = floatingBallView,
            dimensions = dimensions,
            expanded = presentation.expanded,
            dockSide = side,
            applyVisualWidth = false
        )
        val targetWidth = if (presentation.expanded) {
            dimensions.expandedWidthPx
        } else {
            dimensions.collapsedWidthPx
        }
        animateFloatingBallWidth(targetWidth, animateWidth)
        floatingBallIcon.setImageResource(
            when (floatingBallOperationState) {
                FloatingBallOperationState.SUCCESS -> R.drawable.ic_floating_success
                FloatingBallOperationState.ERROR -> R.drawable.ic_floating_error
                else -> R.drawable.ic_floating_ai_sparkle
            }
        )
        if (floatingBallOperationState == FloatingBallOperationState.LOADING) {
            startFloatingBallPulse()
        } else {
            stopFloatingBallPulse()
            floatingBallVisual.alpha = presentation.alpha
        }
    }

    private fun animateFloatingBallWidth(targetWidth: Int, animate: Boolean) {
        floatingBallWidthAnimator?.cancel()
        val startWidth = floatingBallVisual.layoutParams.width
        if (!animate || startWidth <= 0 || startWidth == targetWidth) {
            floatingBallVisual.layoutParams = floatingBallVisual.layoutParams.apply {
                width = targetWidth
            }
            return
        }
        floatingBallWidthAnimator = ValueAnimator.ofInt(startWidth, targetWidth).apply {
            duration = FLOATING_BALL_SNAP_DURATION_MS
            addUpdateListener { animator ->
                floatingBallVisual.layoutParams = floatingBallVisual.layoutParams.apply {
                    width = animator.animatedValue as Int
                }
            }
            start()
        }
    }

    private fun startFloatingBallPulse() {
        if (floatingBallPulseAnimator?.isRunning == true) return
        floatingBallPulseAnimator = ObjectAnimator.ofFloat(
            floatingBallVisual,
            View.ALPHA,
            1f,
            0.72f,
            1f
        ).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopFloatingBallPulse() {
        floatingBallPulseAnimator?.cancel()
        floatingBallPulseAnimator = null
    }

    private fun initFloatingWindow() {
        val dimensions = ensureOverlayDimensions().list
        floatingWindowView = LayoutInflater.from(this).inflate(R.layout.floating_list_window, null)
        progressBar = floatingWindowView.findViewById(R.id.progressBar)
        listView = floatingWindowView.findViewById(R.id.resPromptRecycler)
        loadingTextView = floatingWindowView.findViewById(R.id.loadingTextView)
        emptyStateView = floatingWindowView.findViewById(R.id.emptyStateView)
        suggestionPanel = floatingWindowView.findViewById(R.id.suggestionPanel)
        closeBar = floatingWindowView.findViewById(R.id.closeBar)
        suggestionToolbar = floatingWindowView.findViewById(R.id.suggestionToolbar)
        suggestionToolbarDivider = floatingWindowView.findViewById(R.id.suggestionToolbarDivider)
        gestureHintText = floatingWindowView.findViewById(R.id.gestureHintText)
        refreshButton = floatingWindowView.findViewById(R.id.refreshButton)
        closeButton = floatingWindowView.findViewById(R.id.closeButton)
        loadingText = getString(R.string.suggestion_loading_default)

        windowParams = WindowManager.LayoutParams(
            dimensions.windowWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        }

        updateFloatingWindowChrome(SuggestionSource.MANUAL_LIST)
        closeButton.setOnClickListener {
            hideFloatingWindow()
        }
        refreshButton.setOnClickListener {
            startChatCollection(consumer = WeChatReadConsumer.MANUAL_REFRESH)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            (closeBar.parent as? ViewGroup)?.removeView(closeBar)
            (suggestionPanel as ViewGroup).addView(closeBar)
        } else {
            floatingWindowView.addOnAttachStateChangeListener(
                suggestionWindowAttachStateChangeListener
            )
            floatingWindowView.addOnLayoutChangeListener(suggestionWindowLayoutChangeListener)
        }
        OverlayGeometryApplier.applySuggestionList(floatingWindowView, dimensions)
    }

    private fun scheduleSuggestionTouchableRegionUpdate(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        clearPendingSuggestionTouchableRegionUpdate(view)
        val observer = view.viewTreeObserver
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (observer.isAlive) {
                    observer.removeOnPreDrawListener(this)
                }
                if (pendingSuggestionTouchableRegionPreDrawListener === this) {
                    pendingSuggestionTouchableRegionPreDrawListener = null
                }
                updateSuggestionTouchableRegion()
                return true
            }
        }
        pendingSuggestionTouchableRegionPreDrawListener = listener
        observer.addOnPreDrawListener(listener)
    }

    private fun clearPendingSuggestionTouchableRegionUpdate(view: View) {
        val listener = pendingSuggestionTouchableRegionPreDrawListener ?: return
        val observer = view.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnPreDrawListener(listener)
        }
        pendingSuggestionTouchableRegionPreDrawListener = null
    }

    private fun updateSuggestionTouchableRegion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            !::floatingWindowView.isInitialized ||
            !floatingWindowView.isAttachedToWindow
        ) {
            return
        }
        val root = floatingWindowView as? ViewGroup ?: return
        if (suggestionPanel.width <= 0 || suggestionPanel.height <= 0 ||
            closeButton.width <= 0 || closeButton.height <= 0
        ) {
            return
        }

        val panelBounds = root.descendantBounds(suggestionPanel)
        val closeButtonBounds = root.descendantBounds(closeButton)
        val touchableRegion = Region().apply {
            buildSuggestionTouchableRects(panelBounds, closeButtonBounds).forEach(::union)
        }
        root.rootSurfaceControl?.setTouchableRegion(touchableRegion)
    }

    private fun ViewGroup.descendantBounds(descendant: View): Rect {
        return Rect(0, 0, descendant.width, descendant.height).also { bounds ->
            offsetDescendantRectToMyCoords(descendant, bounds)
        }
    }

    private fun updateFloatingWindowChrome(source: SuggestionSource) {
        val toolbarVisibility = if (source == SuggestionSource.AUTOMATIC_LIST) {
            View.GONE
        } else {
            View.VISIBLE
        }
        suggestionToolbar.visibility = toolbarVisibility
        suggestionToolbarDivider.visibility = toolbarVisibility
        if (toolbarVisibility == View.GONE) {
            gestureHintText.visibility = View.GONE
        }

        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        if (isWindowShowing) {
            updateOverlayViewLayout(floatingWindowView, windowParams)
        }
    }

    private fun initRealtimeSuggestionWindow() {
        val dimensions = ensureOverlayDimensions().realtime
        realtimeSuggestionView = LayoutInflater.from(this).inflate(R.layout.floating_realtime_suggestion, null)
        realtimeSuggestionTextView = realtimeSuggestionView.findViewById(R.id.realtimeSuggestionText)
        OverlayGeometryApplier.applyRealtimeSuggestion(realtimeSuggestionView, dimensions)

        realtimeSuggestionParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dimensions.horizontalInsetPx
            y = screenHeight - dimensions.fallbackBottomOffsetPx
        }

        realtimeSuggestionView.setOnClickListener {
            sendRealtimeSuggestion()
        }
        realtimeSuggestionView.setOnLongClickListener {
            insertRealtimeSuggestion()
            true
        }
        realtimeSuggestionView.addOnLayoutChangeListener {
                _, _, _, _, bottom, _, _, _, oldBottom ->
            if (
                isRealtimeSuggestionShowing &&
                bottom != oldBottom &&
                updateRealtimeSuggestionPosition()
            ) {
                realtimeSuggestionGeometryDirty = !updateOverlayViewLayout(
                    realtimeSuggestionView,
                    realtimeSuggestionParams
                )
                if (realtimeSuggestionGeometryDirty) {
                    scheduleOverlayGeometryRetry()
                }
            }
        }
    }

    private fun isCurrentWeChatGroupChat(): Boolean {
        val title = getChatTitle()
        return isWeChatChatInterfaceTitle(title) && isWeChatGroupChatTitle(title)
    }

    private fun handleAssistantChatPolicyState(newState: AssistantChatPolicyState) {
        val previousState = assistantChatPolicyState
        assistantChatPolicyState = newState

        when (newState) {
            is AssistantChatPolicyState.Unavailable -> {
                if (!assistantPolicyUnavailableNoticeShown) {
                    assistantPolicyUnavailableNoticeShown = true
                    Toast.makeText(
                        this,
                        "不启用的聊天名单暂不可用，请到本应用设置中重试",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            is AssistantChatPolicyState.Ready -> {
                assistantPolicyFailClosedSuppressed = false
                assistantPolicyUnavailableNoticeShown = false
            }
            AssistantChatPolicyState.Loading -> Unit
        }

        val currentKey = currentAssistantChatKey()
        if (newState !is AssistantChatPolicyState.Ready) {
            suppressAssistantForFailClosedState(currentKey)
            return
        }
        if (currentKey == null) {
            // 策略恢复时无障碍树可能正处于切换瞬间；清空 watchdog 身份，确保标题重新
            // 可读后同一个聊天也会被当作一次新观察并触发完整刷新。
            watchdogObservedDirectChatKey = null
            return
        }
        val wasAllowed = isAssistantChatAllowed(currentKey, previousState)
        val isAllowed = isAssistantChatAllowed(currentKey, newState)
        if (!isAllowed) {
            if (wasAllowed || assistantPolicySuppressedChatKey != currentKey) {
                suppressAssistantForIneligibleChat(currentKey)
            } else {
                updateFloatingBallVisibility(visible = false)
            }
            return
        }

        assistantPolicySuppressedChatKey = null
        if (!wasAllowed) {
            accessibilityRefreshCoordinator.request(forceNavigationBoundary = false)
        }
    }

    private fun isAssistantChatAllowed(
        key: AssistantChatKey?,
        stateOverride: AssistantChatPolicyState? = null
    ): Boolean {
        // 热路径直接读取 StateFlow.value，避免排除/故障已经发布、主线程 collector
        // 尚未执行时继续发起请求、填入或发送。collector 缓存只用于状态转换副作用。
        val state = stateOverride ?: if (::assistantChatPolicy.isInitialized) {
            assistantChatPolicy.state.value
        } else {
            AssistantChatPolicyState.Loading
        }
        return key != null &&
            state is AssistantChatPolicyState.Ready &&
            key !in state.excludedKeys
    }

    private fun currentAssistantChatKey(): AssistantChatKey? {
        return try {
            val root = rootInActiveWindow ?: return null
            assistantChatKey(
                sourcePackage = root.packageName?.toString(),
                rawTitle = readChatTitle(root)
            )
        } catch (error: Exception) {
            Log.w(TAG, "读取当前聊天窗口信息异常", error)
            null
        }
    }

    private fun assistantChatKey(
        sourcePackage: String?,
        rawTitle: String?
    ): AssistantChatKey? {
        if (sourcePackage != WECHAT_PACKAGE || !isWeChatDirectChatTitle(rawTitle)) return null
        return AssistantChatKey.from(sourcePackage, rawTitle.orEmpty())
    }

    private fun snapshotAssistantChatKey(snapshot: CapturedChatSnapshot): AssistantChatKey? =
        assistantChatKey(snapshot.sourcePackage, snapshot.rawTitle)

    private fun sessionIdentityAssistantChatKey(
        identity: ChatSessionIdentity?
    ): AssistantChatKey? = identity?.let {
        AssistantChatKey.from(it.sourcePackage, it.normalizedTitle)
    }

    private fun currentChatContextGeneration(): Long =
        chatContextSession?.generation ?: 0L

    private fun isWeChatFrameCurrent(
        frameKey: WeChatVoiceFrameKey,
        expectedViewportEpoch: Long? = null
    ): Boolean {
        if (isServiceDestroying || currentChatContextGeneration() != frameKey.generation) return false
        if (expectedViewportEpoch != null && weChatViewportEpoch != expectedViewportEpoch) return false
        return try {
            val root = rootInActiveWindow ?: return false
            if (root.packageName?.toString() != WECHAT_PACKAGE) return false
            val rawTitle = readChatTitle(root)
            if (WeChatNodeTextPolicy.normalize(rawTitle.orEmpty()) !=
                frameKey.normalizedChatTitle
            ) return false
            weChatPageReader.currentFrameKey(
                generation = frameKey.generation,
                chatTitle = rawTitle,
                root = root
            ) == frameKey
        } catch (_: Exception) {
            false
        }
    }

    private fun captureCurrentSuggestionOrigin(): SuggestionOrigin? {
        val root = rootInActiveWindow ?: return null
        val sourcePackage = root.packageName?.toString() ?: return null
        val rawTitle = readChatTitle(root)
        val key = assistantChatKey(sourcePackage, rawTitle)
        if (!isAssistantChatAllowed(key)) return null
        return SuggestionOrigin(
            sessionGeneration = currentChatContextGeneration(),
            sourcePackage = sourcePackage,
            normalizedTitle = ChatTextNormalizer.normalizeTitle(rawTitle.orEmpty())
        )
    }

    private fun snapshotIdentity(snapshot: CapturedChatSnapshot) = ChatSessionIdentity(
        sourcePackage = snapshot.sourcePackage,
        normalizedTitle = snapshot.normalizedTitle
    )

    private fun snapshotFrameReference(snapshot: CapturedChatSnapshot) = SnapshotFrameReference(
        sessionGeneration = snapshot.sessionGeneration,
        normalizedTitle = snapshot.normalizedTitle,
        messages = snapshot.messages
    )

    private fun rememberSnapshotFrame(
        snapshot: CapturedChatSnapshot,
        frameKey: WeChatVoiceFrameKey,
        visibleMessages: List<Msg> = snapshot.messages
    ) {
        snapshotFrameReferences[snapshotFrameReference(snapshot)] = SnapshotFrameValidation(
            frameKey = frameKey,
            visibleMessages = visibleMessages.map { it.copy() }
        )
    }

    private fun isSnapshotCurrent(snapshot: CapturedChatSnapshot): Boolean {
        val session = chatContextSession ?: return false
        val expectedIdentity = snapshotIdentity(snapshot)
        if (!session.isCurrent(snapshot.sessionGeneration, expectedIdentity)) return false
        val expectedFrame = snapshotFrameReferences[snapshotFrameReference(snapshot)] ?: return false
        if (!::weChatPageReader.isInitialized) return false
        val root = rootInActiveWindow ?: return false
        val sourcePackage = root.packageName?.toString()
        val rawTitle = root.takeIf { sourcePackage == WECHAT_PACKAGE }?.let(::readChatTitle)
        if (!isAssistantChatAllowed(assistantChatKey(sourcePackage, rawTitle))) return false
        val currentPage = readCurrentWeChatPageResult(
            generation = snapshot.sessionGeneration,
            root = root,
            rawTitle = rawTitle
        ) as? CapturedWeChatPageRead.TextReady ?: return false
        return currentPage.page.frameKey == expectedFrame.frameKey &&
            currentPage.page.snapshot.messages == expectedFrame.visibleMessages
    }

    private fun freezeCurrentSessionToken(
        session: ChatContextSession,
        snapshot: CapturedChatSnapshot
    ): ChatSessionToken? {
        if (snapshot.messages.isEmpty()) return null
        if (!isAssistantChatAllowed(snapshotAssistantChatKey(snapshot))) return null
        if (!session.isCurrent(snapshot.sessionGeneration, snapshotIdentity(snapshot))) return null
        return session.tokenFor(snapshot)
    }

    private fun refreshAccessibilityState(forceNavigationBoundary: Boolean) {
        try {
            val root = rootInActiveWindow
            val sourcePackage = root?.packageName?.toString()
            val rawTitle = root
                ?.takeIf { sourcePackage == WECHAT_PACKAGE }
                ?.let(::readChatTitle)
            val assistantChatKey = assistantChatKey(sourcePackage, rawTitle)
            val isAssistantAllowed = isAssistantChatAllowed(assistantChatKey)
            if (assistantChatKey != null && !isAssistantAllowed) {
                suppressAssistantForIneligibleChat(assistantChatKey)
            } else if (isAssistantAllowed) {
                assistantPolicySuppressedChatKey = null
            }
            val generation = synchronizeChatIdentity(
                rawTitle = rawTitle,
                forceNavigationBoundary = forceNavigationBoundary
            )
            val pageRead = if (root != null && isAssistantAllowed) {
                readCurrentWeChatPageResult(
                    generation = generation,
                    root = root,
                    rawTitle = rawTitle
                )
            } else {
                CapturedWeChatPageRead.Blocked(WeChatChatMessagesBlockedReason.READ_FAILED)
            }
            watchdogObservedGroupChat = rawTitle?.let(::isWeChatGroupChatTitle) == true
            watchdogObservedDirectChatKey = assistantChatKey
            val currentPage = when (pageRead) {
                is CapturedWeChatPageRead.TextReady -> refreshChatContext(pageRead.page)
                is CapturedWeChatPageRead.VisionRequired -> {
                    blockSuggestionsForUnreadyPage()
                    null
                }
                is CapturedWeChatPageRead.Blocked -> {
                    blockSuggestionsForUnreadyPage()
                    null
                }
            }
            refreshSuggestionTriggers(root, rawTitle, currentPage)
        } catch (e: Exception) {
            Log.e(TAG, "刷新微信聊天状态异常", e)
        }
    }

    private fun synchronizeChatIdentity(
        rawTitle: String?,
        forceNavigationBoundary: Boolean = false,
        preserveFloatingBallAction: Boolean = false
    ): Long {
        val session = chatContextSession ?: return 0L
        val previousGeneration = session.generation
        val identity = rawTitle
            ?.takeIf {
                isAssistantChatAllowed(
                    assistantChatKey(WECHAT_PACKAGE, it)
                )
            }
            ?.let {
                ChatSessionIdentity(
                    WECHAT_PACKAGE,
                    ChatTextNormalizer.normalizeTitle(it)
                )
            }
        session.synchronizeIdentity(identity, forceNavigationBoundary)
        if (session.generation != previousGeneration) {
            cancelHoldToScrollChatHistory()
            snapshotFrameReferences.clear()
            pendingAutomaticListChatKey = null
            invalidateListRequest()
            hideFloatingWindow()
            resetRealtimeSuggestionState()
            suggestionTriggerState.reset()
            if (!preserveFloatingBallAction) {
                cancelFloatingBallAction(activeFloatingBallActionId)
            }
        }
        return session.generation
    }

    private fun refreshChatContext(page: CapturedWeChatPage): CapturedWeChatPage? {
        val session = chatContextSession ?: return null
        val currentPage = page.copy(
            snapshot = page.snapshot.copy(sessionGeneration = session.generation)
        )
        if (holdToScrollChatHistoryController == null) {
            session.observe(currentPage.snapshot)
        }
        rememberSnapshotFrame(currentPage.snapshot, currentPage.frameKey)
        return currentPage
    }

    private fun invalidateChatContextSession() {
        chatContextSession?.invalidate()
        snapshotFrameReferences.clear()
        clearAssistantRuntimeState()
    }

    private fun canUseSuggestionsInCurrentChat(): Boolean {
        return ::userPreferences.isInitialized &&
            isFloatingBallEnabled() &&
            isAssistantChatAllowed(currentAssistantChatKey())
    }

    private fun suppressAssistantForIneligibleChat(key: AssistantChatKey) {
        updateFloatingBallVisibility(visible = false)
        if (assistantPolicySuppressedChatKey == key) return
        assistantPolicySuppressedChatKey = key

        val session = chatContextSession
        if (sessionIdentityAssistantChatKey(session?.activeIdentity) == key) {
            session?.invalidateAndDiscardPending()
        } else if (session?.activeIdentity != null) {
            session.observe(identity = null)
        } else {
            session?.invalidateAndDiscardPending()
        }
        clearAssistantRuntimeState()
    }

    private fun suppressAssistantForFailClosedState(currentKey: AssistantChatKey?) {
        updateFloatingBallVisibility(visible = false)
        if (assistantPolicyFailClosedSuppressed) return
        assistantPolicyFailClosedSuppressed = true
        assistantPolicySuppressedChatKey = currentKey ?: sessionIdentityAssistantChatKey(
            chatContextSession?.activeIdentity
        )
        chatContextSession?.invalidateAndDiscardPending()
        clearAssistantRuntimeState()
    }

    private fun clearAssistantRuntimeState() {
        cancelHoldToScrollChatHistory()
        pendingAutomaticListChatKey = null
        invalidateListRequest()
        hideFloatingWindow()
        resetRealtimeSuggestionState()
        clearRenderedSuggestionList()
        resetFloatingBallOperation()
        suggestionTriggerState.reset()
    }

    private fun suppressSuggestionsForGroupChat() {
        chatContextSession?.invalidate()
        updateFloatingBallVisibility(visible = false)
        pendingAutomaticListChatKey = null
        invalidateListRequest()
        hideFloatingWindow()
        resetRealtimeSuggestionState()
        clearRenderedSuggestionList()
        resetFloatingBallOperation()
        suggestionTriggerState.reset()
    }

    private fun clearRenderedSuggestionList() {
        suggestionListAdapter = null
        if (::listView.isInitialized) {
            listView.adapter = null
            listView.onItemClickListener = null
            listView.onItemLongClickListener = null
        }
    }

    private fun invalidateSuggestionsForChatIdentityChange() {
        pendingAutomaticListChatKey = null
        invalidateListRequest()
        hideFloatingWindow()
        resetRealtimeSuggestionState()
        resetFloatingBallOperation()
        suggestionTriggerState.reset()
    }

    private fun isManualListGenerationInProgress(): Boolean =
        activeFloatingBallActionId != null && listRequestJob?.isActive == true

    private fun isInteractiveSuggestionWorkInProgress(): Boolean =
        visualReplyJob?.isActive == true ||
            holdToScrollChatHistoryController != null ||
            isManualListGenerationInProgress()

    private fun blockSuggestionsForUnreadyPage(
        preserveInteractiveLoading: Boolean = isInteractiveSuggestionWorkInProgress()
    ) {
        pendingAutomaticListChatKey = null
        if (!isManualListGenerationInProgress() && visualReplyJob?.isActive != true) {
            invalidateListRequest()
        }
        resetRealtimeSuggestionState()
        clearRenderedSuggestionList()

        if (!preserveInteractiveLoading) {
            suggestionTriggerState.reset()
            hideFloatingWindow()
            cancelFloatingBallAction(activeFloatingBallActionId)
        }
    }

    private fun refreshSuggestionTriggers(
        root: AccessibilityNodeInfo?,
        rawTitle: String?,
        page: CapturedWeChatPage?
    ) {
        if (!::userPreferences.isInitialized || !::floatingBallView.isInitialized) {
            return
        }

        if (rawTitle?.let(::isWeChatGroupChatTitle) == true) {
            suppressSuggestionsForGroupChat()
            return
        }

        val canUseSuggestions =
            root?.packageName?.toString() == WECHAT_PACKAGE &&
                isFloatingBallEnabled() &&
                isAssistantChatAllowed(
                    assistantChatKey(root.packageName?.toString(), rawTitle)
                )
        val interactiveCaptureInProgress = isInteractiveSuggestionWorkInProgress()
        if (interactiveCaptureInProgress) {
            updateFloatingBallVisibility(visible = canUseSuggestions)
            pendingAutomaticListChatKey = null
            if (!isManualListGenerationInProgress() && visualReplyJob?.isActive != true) {
                invalidateListRequest()
            }
            resetRealtimeSuggestionState()
            page?.snapshot?.normalizedTitle
                ?.takeIf { it.isNotEmpty() }
                ?.let { suggestionTriggerState.synchronizeChat(it) }
            return
        }
        val eligiblePage = page.takeIf { canUseSuggestions }
        val chatKey = eligiblePage?.snapshot?.normalizedTitle?.takeIf { it.isNotEmpty() }
        val keyboardVisible = canUseSuggestions && isInputMethodVisible()
        val hasEditableInput = eligiblePage?.hasEditableInput == true
        val realtimeContextAvailable =
            canUseSuggestions &&
                !automaticSuggestionsBlockedForSession &&
                keyboardVisible &&
                hasEditableInput
        val actions = suggestionTriggerState.update(
            chatKey = chatKey,
            keyboardVisible = keyboardVisible,
            hasEditableInput = hasEditableInput,
            messages = eligiblePage?.snapshot?.messages.orEmpty()
        )

        updateFloatingBallVisibility(visible = canUseSuggestions)

        if (actions.chatExited) {
            pendingAutomaticListChatKey = null
            invalidateListRequest()
            hideFloatingWindow()
            resetRealtimeSuggestionState()
            return
        }

        if ((actions.chatEntered || actions.incomingMessageReceived) && chatKey != null) {
            pendingAutomaticListChatKey = chatKey
            invalidateListRequest()
            hideFloatingWindow()
            resetRealtimeSuggestionState()
        }

        if (!automaticSuggestionsBlockedForSession &&
            eligiblePage != null &&
            chatKey != null &&
            pendingAutomaticListChatKey == chatKey
        ) {
            val snapshot = eligiblePage.snapshot
            if (requestAutomaticResponseList(snapshot)) {
                pendingAutomaticListChatKey = null
            }
        }

        if (actions.keyboardOpened) {
            realtimeSuggestionGate.reset()
        }
        val realtimePage = eligiblePage.takeIf { realtimeContextAvailable }
        if (realtimePage != null) {
            handleRealtimeSuggestionContextChanged(realtimePage)
        } else {
            resetRealtimeSuggestionState()
        }

        if (isRealtimeSuggestionShowing) {
            repositionRealtimeSuggestionIfShowing(
                inputBounds = page?.inputBounds,
                canUseSuggestions = canUseSuggestions
            )
        }
    }

    private fun repositionRealtimeSuggestionIfShowing(
        inputBounds: Rect?,
        canUseSuggestions: Boolean
    ) {
        if (!isRealtimeSuggestionShowing ||
            !::realtimeSuggestionView.isInitialized ||
            !::realtimeSuggestionParams.isInitialized
        ) {
            return
        }

        val positionChanged = updateRealtimeSuggestionPosition(inputBounds)
        if (positionChanged) {
            updateOverlayViewLayout(realtimeSuggestionView, realtimeSuggestionParams)
        }
        showFloatingBallWithRealtimeSuggestion(canUseSuggestions)
    }

    private fun updateFloatingBallVisibility(visible: Boolean) {
        if (!isBallAdded || !::floatingBallView.isInitialized || !::ballView.isInitialized) {
            return
        }

        val state = buildFloatingBallState(ballParams.flags, visible)
        val flagsChanged = ballParams.flags != state.flags
        if (floatingBallView.alpha != state.alpha) {
            floatingBallView.alpha = state.alpha
        }
        if (floatingBallView.isEnabled != state.isEnabled) {
            floatingBallView.isEnabled = state.isEnabled
        }
        if (ballView.isEnabled != state.isEnabled) {
            ballView.isEnabled = state.isEnabled
        }
        ballParams.flags = state.flags
        if (flagsChanged) {
            updateOverlayViewLayout(floatingBallView, ballParams)
        }
    }

    private fun showFloatingBallWithRealtimeSuggestion(
        canUseSuggestions: Boolean = canUseSuggestionsInCurrentChat()
    ) {
        if (!canUseSuggestions) {
            updateFloatingBallVisibility(visible = false)
            return
        }
        updateFloatingBallVisibility(visible = true)
    }

    private fun updateOverlayViewLayout(
        view: View,
        params: WindowManager.LayoutParams
    ): Boolean {
        return try {
            windowManager.updateViewLayout(view, params)
            true
        } catch (e: Exception) {
            Log.e(TAG, "更新悬浮窗布局异常", e)
            false
        }
    }

    private fun showFloatingWindow(): Boolean {
        if (!::floatingWindowView.isInitialized) {
            return false
        }
        if (isWindowShowing) {
            return true
        }

        return try {
            windowManager.addView(floatingWindowView, windowParams)
            isWindowShowing = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "显示悬浮回复窗口异常", e)
            false
        }
    }

    private fun hideFloatingWindow() {
        setRefreshLoading(false)
        if (::floatingWindowView.isInitialized && isWindowShowing) {
            try {
                windowManager.removeView(floatingWindowView)
            } catch (e: Exception) {
                Log.e(TAG, "隐藏悬浮回复窗口异常", e)
            } finally {
                isWindowShowing = false
            }
        }

        if (::ballView.isInitialized) {
            ballView.visibility = View.VISIBLE
            renderFloatingBallPresentation(animateWidth = false)
        }
    }

    private fun renderRealtimeSuggestion(
        suggestion: String,
        origin: SuggestionOrigin
    ) {
        if (automaticSuggestionsBlockedForSession) {
            hideRealtimeSuggestion()
            return
        }
        if (isCurrentWeChatGroupChat()) {
            suppressSuggestionsForGroupChat()
            return
        }
        if (!canUseSuggestionsInCurrentChat()) {
            hideRealtimeSuggestion()
            updateFloatingBallVisibility(visible = false)
            return
        }

        if (!::realtimeSuggestionView.isInitialized || suggestion.isBlank()) {
            hideRealtimeSuggestion()
            return
        }

        realtimeSuggestionText = suggestion
        realtimeSuggestionOrigin = origin
        realtimeSuggestionTextView.text = suggestion
        val positionChanged = updateRealtimeSuggestionPosition()

        try {
            if (isRealtimeSuggestionShowing) {
                if (positionChanged) {
                    updateOverlayViewLayout(realtimeSuggestionView, realtimeSuggestionParams)
                }
            } else {
                windowManager.addView(realtimeSuggestionView, realtimeSuggestionParams)
                isRealtimeSuggestionShowing = true
            }
            showFloatingBallWithRealtimeSuggestion()
            markFirstActionableSuggestionShown()
        } catch (e: Exception) {
            Log.e(TAG, "显示实时建议胶囊异常", e)
            isRealtimeSuggestionShowing = false
            realtimeSuggestionOrigin = null
            updateFloatingBallVisibility(
                visible = canUseSuggestionsInCurrentChat()
            )
        }
    }

    private fun sendRealtimeSuggestion() {
        val suggestion = realtimeSuggestionText ?: return
        val origin = realtimeSuggestionOrigin ?: return
        submitSelectedSuggestion(
            suggestion,
            send = true,
            segmented = true,
            source = SuggestionSource.AUTOMATIC_BUBBLE,
            origin = origin
        )
    }

    private fun insertRealtimeSuggestion() {
        val suggestion = realtimeSuggestionText ?: return
        val origin = realtimeSuggestionOrigin ?: return
        submitSelectedSuggestion(
            suggestion,
            send = false,
            segmented = false,
            source = SuggestionSource.AUTOMATIC_BUBBLE,
            origin = origin
        )
    }

    private fun hideRealtimeSuggestion() {
        if (::realtimeSuggestionView.isInitialized && isRealtimeSuggestionShowing) {
            try {
                windowManager.removeView(realtimeSuggestionView)
            } catch (e: Exception) {
                Log.e(TAG, "隐藏实时建议胶囊异常", e)
            } finally {
                isRealtimeSuggestionShowing = false
            }
        }
        realtimeSuggestionText = null
        realtimeSuggestionOrigin = null
    }

    private fun resetRealtimeSuggestionState() {
        pendingRealtimeSuggestionKey = null
        realtimeSuggestionGate.reset()
        realtimeSuggestionJob?.cancel()
        realtimeSuggestionJob = null
        hideRealtimeSuggestion()
    }

    private fun updateRealtimeSuggestionPosition(
        inputBounds: Rect? = getInputBoundsInScreen()
    ): Boolean {
        val dimensions = ensureOverlayDimensions().realtime
        val targetY = if (inputBounds != null) {
            val renderedHeight = if (::realtimeSuggestionView.isInitialized) {
                maxOf(
                    realtimeSuggestionView.height,
                    realtimeSuggestionView.measuredHeight,
                    dimensions.touchMinHeightPx
                )
            } else {
                dimensions.touchMinHeightPx
            }
            val inputGap = dimensions.inputTopOffsetPx - dimensions.touchMinHeightPx
            (inputBounds.top - renderedHeight - inputGap)
                .coerceAtLeast(dimensions.minTopPx)
        } else {
            screenHeight - dimensions.fallbackBottomOffsetPx
        }
        if (realtimeSuggestionParams.y == targetY) return false
        realtimeSuggestionParams.y = targetY
        return true
    }

    private fun getInputBoundsInScreen(): Rect? {
        return try {
            val editText = findWeChatInputNode() ?: return null
            readBoundsInScreen(editText)
        } catch (e: Exception) {
            Log.e(TAG, "获取输入框位置异常", e)
            null
        }
    }

    private fun findWeChatInputNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findWeChatInputNode(root)
    }

    private fun findWeChatInputNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByViewId(WECHAT_INPUT_VIEW_ID)

        return nodes.firstOrNull { it.isEditable && it.isEnabled }
            ?: nodes.firstOrNull { it.isFocused && it.isEnabled }
            ?: nodes.firstOrNull { it.isEnabled }
            ?: nodes.firstOrNull()
    }

    private fun readBoundsInScreen(node: AccessibilityNodeInfo): Rect =
        Rect().also(node::getBoundsInScreen)

    private fun findWeChatSendButton(): AccessibilityNodeInfo? {
        val nodes = rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId(WECHAT_SEND_BUTTON_VIEW_ID)
            .orEmpty()

        return nodes.firstOrNull { it.isClickable && it.isEnabled }
            ?: nodes.firstOrNull { it.isEnabled }
            ?: nodes.firstOrNull()
    }

    private fun showLoading(isLoading: Boolean) {
        val hasItems = listView.adapter?.count?.let { it > 0 } == true
        val shouldShowGestureHint = suggestionToolbar.visibility == View.VISIBLE &&
            hasItems &&
            suggestionListAdapter?.showsGestureHint == true

        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        setRefreshLoading(isLoading)

        if (isLoading) {
            listView.visibility = View.GONE
            emptyStateView.visibility = View.GONE
            gestureHintText.visibility = View.GONE
            updateLoadingText()
        } else {
            loadingTextView.visibility = View.GONE
            listView.visibility = if (hasItems) View.VISIBLE else View.GONE
            emptyStateView.visibility = if (hasItems) View.GONE else View.VISIBLE
            gestureHintText.visibility = if (shouldShowGestureHint) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT

        if (isWindowShowing) {
            updateOverlayViewLayout(floatingWindowView, windowParams)
        }
    }

    private fun setRefreshLoading(isLoading: Boolean) {
        if (!::refreshButton.isInitialized) return

        refreshButton.isEnabled = !isLoading
        refreshButton.alpha = if (isLoading) 0.72f else 1f
        if (isLoading) {
            if (refreshAnimator?.isRunning == true) return
            refreshAnimator = ObjectAnimator.ofFloat(
                refreshButton,
                View.ROTATION,
                0f,
                360f
            ).apply {
                duration = 800L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        } else {
            refreshAnimator?.cancel()
            refreshAnimator = null
            refreshButton.rotation = 0f
        }
    }

    private fun updateLoadingText() {
        loadingTextView.text = loadingText
        loadingTextView.visibility = View.VISIBLE
    }

    private fun startChatCollection(
        consumer: WeChatReadConsumer,
        floatingBallActionId: Long? = null
    ) {
        val root = rootInActiveWindow
        val rawTitle = root
            ?.takeIf { it.packageName?.toString() == WECHAT_PACKAGE }
            ?.let(::readChatTitle)
        if (rawTitle?.let(::isWeChatGroupChatTitle) == true) {
            suppressSuggestionsForGroupChat()
            return
        }

        val assistantChatKey = assistantChatKey(root?.packageName?.toString(), rawTitle)
        if (root == null || !isAssistantChatAllowed(assistantChatKey)) {
            if (assistantChatKey != null) {
                suppressAssistantForIneligibleChat(assistantChatKey)
            }
            renderResponseList(
                listOf("请在微信聊天页使用"),
                source = SuggestionSource.SYSTEM_MESSAGE,
                floatingBallActionId = floatingBallActionId
            )
            return
        }

        val generation = synchronizeChatIdentity(
            rawTitle = rawTitle,
            preserveFloatingBallAction = true
        )
        suggestionTriggerState.synchronizeChat(
            ChatTextNormalizer.normalizeTitle(rawTitle.orEmpty())
        )
        val pageRead = readCurrentWeChatPageResult(
            generation = generation,
            root = root,
            rawTitle = rawTitle
        )
        when (decideWeChatReadAction(pageRead.toMessagesResult(), consumer)) {
            WeChatReadAction.USE_TEXT -> continueChatCollection(
                capturedPage = (pageRead as CapturedWeChatPageRead.TextReady).page,
                enableScrollCollection = consumer == WeChatReadConsumer.LONG_PRESS,
                floatingBallActionId = floatingBallActionId
            )
            WeChatReadAction.CAPTURE_WINDOW -> startVisualReply(
                page = (pageRead as CapturedWeChatPageRead.VisionRequired).page,
                floatingBallActionId = floatingBallActionId
            )
            WeChatReadAction.PROMPT_CONVERSION ->
                renderVoiceConversionRequired(floatingBallActionId)
            WeChatReadAction.PROMPT_SHORT_TAP ->
                renderVisionShortTapRequired(floatingBallActionId)
            WeChatReadAction.PLATFORM_UNSUPPORTED ->
                renderVisionPlatformUnsupported(floatingBallActionId)
            WeChatReadAction.FAIL -> renderWeChatReadFailure(floatingBallActionId)
            WeChatReadAction.SKIP_SILENTLY ->
                cancelFloatingBallAction(floatingBallActionId)
        }
    }

    private fun CapturedWeChatPageRead.toMessagesResult(): WeChatChatMessagesResult = when (this) {
        is CapturedWeChatPageRead.TextReady -> WeChatChatMessagesResult.TextReady(
            page.frameKey,
            page.snapshot.messages
        )
        is CapturedWeChatPageRead.VisionRequired -> WeChatChatMessagesResult.VisionRequired(
            page.frameKey,
            page.snapshot.messages,
            page.windowId
        )
        is CapturedWeChatPageRead.Blocked -> WeChatChatMessagesResult.Blocked(frameKey, reason)
    }

    private fun startVisualReply(
        page: CapturedWeChatVisionPage,
        floatingBallActionId: Long?
    ) {
        if (!isWeChatFrameCurrent(page.frameKey, page.viewportEpoch)) {
            cancelFloatingBallAction(floatingBallActionId)
            return
        }
        val requestVersion = beginListRequest()
        pendingAutomaticListChatKey = null
        loadingText = getString(R.string.suggestion_loading_generating)
        updateFloatingWindowChrome(SuggestionSource.MANUAL_LIST)
        showFloatingWindow()
        showLoading(true)

        visualReplyJob = serviceScope.launch {
            try {
                val aiContext = conversationContextAssembler.assemble(
                    snapshot = page.snapshot,
                    inputText = page.inputText
                )
                if (!isVisualRequestCurrent(requestVersion, page)) {
                    cancelFloatingBallAction(floatingBallActionId)
                    return@launch
                }
                val screenshot = weChatWindowImageCapture.capture(page.windowId)
                if (!isVisualRequestCurrent(requestVersion, page)) {
                    cancelFloatingBallAction(floatingBallActionId)
                    return@launch
                }
                val result = aiRepository.generateVisualReplies(
                    context = aiContext,
                    screenshotJpeg = screenshot.jpegBytes,
                    replyCountOverride = MANUAL_LIST_REPLY_COUNT
                )
                if (!isVisualRequestCurrent(requestVersion, page)) {
                    cancelFloatingBallAction(floatingBallActionId)
                    return@launch
                }
                result.fold(
                    onSuccess = { choices ->
                        if (!isVisualRequestCurrent(requestVersion, page)) return@fold
                        renderResponseList(
                            choices.take(MANUAL_LIST_REPLY_COUNT),
                            source = SuggestionSource.MANUAL_LIST,
                            floatingBallActionId = floatingBallActionId,
                            origin = page.snapshot.toSuggestionOrigin().copy(
                                frame = SuggestionFrameOrigin(
                                    windowId = page.frameKey.windowId,
                                    fingerprint = page.frameKey.fingerprint,
                                    viewportEpoch = page.viewportEpoch
                                )
                            )
                        )
                    },
                    onFailure = { error ->
                        if (!isVisualRequestCurrent(requestVersion, page)) return@fold
                        renderVisualReplyFailure(error, floatingBallActionId)
                    }
                )
            } catch (error: Exception) {
                if (isVisualRequestCurrent(requestVersion, page)) {
                    Log.w(TAG, "视觉回复请求失败: ${error.javaClass.simpleName}")
                    renderResponseList(
                        listOf("请求失败，请稍后重试"),
                        source = SuggestionSource.SYSTEM_MESSAGE,
                        floatingBallActionId = floatingBallActionId
                    )
                }
            } finally {
                if (requestVersion == listRequestVersion) {
                    visualReplyJob = null
                }
            }
        }
    }

    private fun isVisualRequestCurrent(
        requestVersion: Long,
        page: CapturedWeChatVisionPage
    ): Boolean = requestVersion == listRequestVersion &&
        canUseSuggestionsInCurrentChat() &&
        isAssistantChatAllowed(snapshotAssistantChatKey(page.snapshot)) &&
        isWeChatFrameCurrent(page.frameKey, page.viewportEpoch)

    private fun renderVisualReplyFailure(
        error: Throwable,
        floatingBallActionId: Long?
    ) {
        val notConfigured = error is AiProviderFailure.NotConfigured
        val message = if (notConfigured) {
            "请先配置 AI 服务，点击这里打开设置"
        } else {
            manualAiFailureMessage(error)
        }
        renderResponseList(
            listOf(message),
            bizCode = if (notConfigured) "PROVIDER_NOT_CONFIGURED" else null,
            source = SuggestionSource.SYSTEM_MESSAGE,
            floatingBallActionId = floatingBallActionId
        )
    }

    private fun renderVoiceConversionRequired(floatingBallActionId: Long?) {
        blockSuggestionsForUnreadyPage(preserveInteractiveLoading = true)
        renderResponseList(
            listOf("请先将语音转成文字"),
            source = SuggestionSource.SYSTEM_MESSAGE,
            floatingBallActionId = floatingBallActionId
        )
    }

    private fun renderVisionShortTapRequired(floatingBallActionId: Long?) {
        blockSuggestionsForUnreadyPage(preserveInteractiveLoading = true)
        renderResponseList(
            listOf("当前页面含语音文字，请短按悬浮球生成回复"),
            source = SuggestionSource.SYSTEM_MESSAGE,
            floatingBallActionId = floatingBallActionId
        )
    }

    private fun renderVisionPlatformUnsupported(floatingBallActionId: Long?) {
        blockSuggestionsForUnreadyPage(preserveInteractiveLoading = true)
        renderResponseList(
            listOf("当前系统版本暂不支持读取语音文字"),
            source = SuggestionSource.SYSTEM_MESSAGE,
            floatingBallActionId = floatingBallActionId
        )
    }

    private fun renderWeChatReadFailure(floatingBallActionId: Long?) {
        blockSuggestionsForUnreadyPage(preserveInteractiveLoading = true)
        renderResponseList(
            listOf("读取聊天记录失败，请稍后重试"),
            source = SuggestionSource.SYSTEM_MESSAGE,
            floatingBallActionId = floatingBallActionId
        )
    }

    private fun continueChatCollection(
        capturedPage: CapturedWeChatPage,
        enableScrollCollection: Boolean,
        floatingBallActionId: Long?
    ) {
        val session = chatContextSession
        val frozenPage = refreshChatContext(capturedPage)
        val frozenToken = if (session != null && frozenPage != null) {
            freezeCurrentSessionToken(session, frozenPage.snapshot)
        } else {
            null
        }
        if (session == null || frozenPage == null || frozenToken == null) {
            completeFloatingBallAction(
                floatingBallActionId,
                FloatingBallOperationState.ERROR
            )
            return
        }

        invalidateListRequest()
        pendingAutomaticListChatKey = null

        loadingText = if (enableScrollCollection) {
            getString(R.string.suggestion_loading_chat_scroll)
        } else {
            getString(R.string.suggestion_loading_chat)
        }

        updateFloatingWindowChrome(SuggestionSource.MANUAL_LIST)
        showFloatingWindow()
        showLoading(true)

        val sendCollectedSnapshot: (CapturedChatSnapshot) -> Unit = { snapshot ->
            loadingText = getString(R.string.suggestion_loading_generating)
            updateLoadingText()
            sendAiRequest(
                snapshot = snapshot,
                inputText = frozenPage.inputText,
                floatingBallActionId = floatingBallActionId
            )
        }

        if (enableScrollCollection) {
            startHoldToScrollChatHistory(
                frozenPage = frozenPage,
                session = session,
                frozenToken = frozenToken,
                floatingBallActionId = floatingBallActionId,
                onComplete = sendCollectedSnapshot
            )
        } else {
            sendCollectedSnapshot(frozenPage.snapshot)
        }
    }

    private fun startHoldToScrollChatHistory(
        frozenPage: CapturedWeChatPage,
        session: ChatContextSession,
        frozenToken: ChatSessionToken,
        floatingBallActionId: Long?,
        onComplete: (CapturedChatSnapshot) -> Unit
    ) {
        cancelHoldToScrollChatHistory()
        val frozenSnapshot = frozenPage.snapshot
        val frozenAssistantChatKey = snapshotAssistantChatKey(frozenSnapshot) ?: return
        val isFrozenSessionValid: () -> Boolean = {
            val root = rootInActiveWindow
            val currentKey = root?.let {
                assistantChatKey(it.packageName?.toString(), readChatTitle(it))
            }
            session.isTokenCurrent(frozenToken) &&
                currentKey == frozenAssistantChatKey &&
                isAssistantChatAllowed(frozenAssistantChatKey)
        }
        session.pauseCapture()
        var unavailableAction = WeChatReadAction.FAIL
        lateinit var controller: HoldToScrollChatHistoryController
        controller = HoldToScrollChatHistoryController(
            scheduler = object : HoldScrollScheduler {
                override fun postDelayed(delayMs: Long, action: Runnable) {
                    handler.postDelayed(action, delayMs)
                }

                override fun removeCallbacks(action: Runnable) {
                    handler.removeCallbacks(action)
                }
            },
            readVisibleMessages = {
                val root = rootInActiveWindow
                if (root == null) {
                    VisibleChatMessagesRead.Blocked
                } else {
                    val title = readChatTitle(root)
                    when (val messagesRead = readChatMessages(
                        generation = frozenToken.generation,
                        rawTitle = title,
                        root = root
                    )) {
                        is WeChatChatMessagesResult.TextReady ->
                            VisibleChatMessagesRead.Ready(
                                messagesRead.messages.toTypedArray()
                            )
                        is WeChatChatMessagesResult.VisionRequired -> {
                            unavailableAction = WeChatReadAction.PROMPT_SHORT_TAP
                            VisibleChatMessagesRead.Blocked
                        }
                        is WeChatChatMessagesResult.Blocked -> {
                            unavailableAction = if (
                                messagesRead.reason ==
                                WeChatChatMessagesBlockedReason.VOICE_CONVERSION_REQUIRED
                            ) {
                                WeChatReadAction.PROMPT_CONVERSION
                            } else {
                                WeChatReadAction.FAIL
                            }
                            VisibleChatMessagesRead.Blocked
                        }
                    }
                }
            },
            scrollTowardHistory = {
                performChatHistoryScrollAndInvalidateViewport(
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                )
            },
            scrollTowardLatest = {
                performChatHistoryScrollAndInvalidateViewport(
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                )
            },
            isSessionValid = {
                isFrozenSessionValid()
            },
            requireContinuousWindows = true,
            onCancelled = cancelled@{
                if (holdToScrollChatHistoryController !== controller) return@cancelled
                holdToScrollChatHistoryController = null
                session.resumeCapture()
                cancelFloatingBallAction(floatingBallActionId)
            },
            onUnavailable = unavailable@{
                if (holdToScrollChatHistoryController !== controller) return@unavailable
                holdToScrollChatHistoryController = null
                session.resumeCapture()
                if (isFrozenSessionValid()) {
                    when (unavailableAction) {
                        WeChatReadAction.PROMPT_SHORT_TAP ->
                            renderVisionShortTapRequired(floatingBallActionId)
                        WeChatReadAction.PROMPT_CONVERSION ->
                            renderVoiceConversionRequired(floatingBallActionId)
                        else -> renderWeChatReadFailure(floatingBallActionId)
                    }
                } else {
                    cancelFloatingBallAction(floatingBallActionId)
                }
            },
            onCompleted = completed@{ chatHistory ->
                if (holdToScrollChatHistoryController !== controller) return@completed
                holdToScrollChatHistoryController = null
                session.resumeCapture()
                if (!isFrozenSessionValid()) {
                    cancelFloatingBallAction(floatingBallActionId)
                    return@completed
                }
                val completedSnapshot = frozenSnapshot.copy(
                    messages = chatHistory.connectedMessages.toList(),
                    capturedAtMillis = System.currentTimeMillis()
                )
                rememberSnapshotFrame(
                    snapshot = completedSnapshot,
                    frameKey = frozenPage.frameKey,
                    visibleMessages = frozenPage.snapshot.messages
                )
                val isCompletedSnapshotCurrent = {
                    isFrozenSessionValid() && isSnapshotCurrent(completedSnapshot)
                }
                val completedSnapshotGuard: suspend () -> Boolean = {
                    withContext(Dispatchers.Main.immediate) {
                        isCompletedSnapshotCurrent()
                    }
                }
                if (!isCompletedSnapshotCurrent()) {
                    cancelFloatingBallAction(floatingBallActionId)
                    return@completed
                }
                if (!isCompletedSnapshotCurrent()) return@completed
                onComplete(completedSnapshot)
            }
        )
        holdToScrollChatHistoryController = controller
        controller.start()
    }

    private fun stopHoldToScrollChatHistory() {
        holdToScrollChatHistoryController?.stop()
    }

    private fun cancelHoldToScrollChatHistory() {
        holdToScrollChatHistoryController?.cancel()
        holdToScrollChatHistoryController = null
    }

    private fun beginFloatingBallAction(completedNormalTap: Boolean): Long {
        floatingBallStatusResetRunnable?.let(handler::removeCallbacks)
        floatingBallStatusResetRunnable = null
        cancelFloatingBallCollapse()
        nextFloatingBallActionId += 1
        activeFloatingBallActionId = nextFloatingBallActionId
        activeFloatingBallWasNormalTap = completedNormalTap
        floatingBallOperationState = FloatingBallOperationState.LOADING
        floatingBallInteractionState = FloatingBallInteractionState.EXPANDED
        renderFloatingBallPresentation()
        return nextFloatingBallActionId
    }

    private fun completeFloatingBallAction(
        actionId: Long?,
        state: FloatingBallOperationState
    ) {
        if (actionId == null || activeFloatingBallActionId != actionId) return
        val completedNormalTap = activeFloatingBallWasNormalTap
        activeFloatingBallActionId = null
        activeFloatingBallWasNormalTap = false
        floatingBallOperationState = state
        floatingBallInteractionState = FloatingBallInteractionState.EXPANDED
        renderFloatingBallPresentation()

        if (
            state == FloatingBallOperationState.SUCCESS &&
            shouldShowFloatingBallLongPressHint(
                alreadyShown = hasShownFloatingBallLongPressHint,
                completedNormalTap = completedNormalTap
            )
        ) {
            hasShownFloatingBallLongPressHint = true
            Toast.makeText(this, "长按可读取更多聊天记录", Toast.LENGTH_SHORT).show()
            serviceScope.launch {
                userPreferences.markFloatingBallLongPressHintShown()
            }
        }

        val duration = floatingBallStatusDurationMs(state) ?: return
        val resetAction = Runnable {
            if (activeFloatingBallActionId == null && floatingBallOperationState == state) {
                floatingBallOperationState = FloatingBallOperationState.IDLE
                renderFloatingBallPresentation()
                scheduleFloatingBallCollapse()
            }
        }
        floatingBallStatusResetRunnable = resetAction
        handler.postDelayed(resetAction, duration)
    }

    private fun markFirstActionableSuggestionShown() {
        if (!firstUseGuideCompletionTracker.tryMarkCompleted()) return

        serviceScope.launch {
            userPreferences.markFirstUseGuideCompleted()
        }
    }

    private fun cancelFloatingBallAction(actionId: Long?) {
        if (actionId == null || activeFloatingBallActionId != actionId) return
        activeFloatingBallActionId = null
        activeFloatingBallWasNormalTap = false
        floatingBallOperationState = FloatingBallOperationState.IDLE
        floatingBallInteractionState = FloatingBallInteractionState.EXPANDED
        renderFloatingBallPresentation()
        scheduleFloatingBallCollapse()
    }

    private fun resetFloatingBallOperation() {
        cancelHoldToScrollChatHistory()
        invalidateListRequest()
        activeFloatingBallActionId = null
        activeFloatingBallWasNormalTap = false
        floatingBallOperationState = FloatingBallOperationState.IDLE
        floatingBallInteractionState = FloatingBallInteractionState.COLLAPSED
        stopFloatingBallPulse()
        if (::floatingBallVisual.isInitialized) {
            renderFloatingBallPresentation(animateWidth = false)
        }
    }

    private fun onClickBallView(enableScrollCollection: Boolean) {
        if (!canUseSuggestionsInCurrentChat()) {
            updateFloatingBallVisibility(visible = false)
            return
        }
        val presentation = buildFloatingBallPresentation(
            floatingBallInteractionState,
            floatingBallOperationState
        )
        if (!presentation.acceptsBusinessAction) return
        if (enableScrollCollection) {
            ballView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        val actionId = beginFloatingBallAction(
            completedNormalTap = !enableScrollCollection
        )
        startChatCollection(
            consumer = if (enableScrollCollection) {
                WeChatReadConsumer.LONG_PRESS
            } else {
                WeChatReadConsumer.SHORT_TAP
            },
            floatingBallActionId = actionId
        )
    }

    private fun renderResponseList(
        resList: List<String>,
        bizCode: String? = null,
        source: SuggestionSource = SuggestionSource.MANUAL_LIST,
        floatingBallActionId: Long? = null,
        origin: SuggestionOrigin? = null
    ) {
        if (isCurrentWeChatGroupChat()) {
            suppressSuggestionsForGroupChat()
            return
        }
        if (!canUseSuggestionsInCurrentChat()) {
            hideFloatingWindow()
            clearRenderedSuggestionList()
            return
        }

        val renderedOrigin = if (source.selectable) {
            origin ?: captureCurrentSuggestionOrigin()
        } else {
            null
        }
        if (source.selectable && renderedOrigin == null) {
            hideFloatingWindow()
            return
        }

        Log.i(TAG, "渲染悬浮回复列表: count=${resList.size}, bizCode=${bizCode ?: "none"}")
        updateFloatingWindowChrome(source)
        val suggestionWindowShown = showFloatingWindow()

        val opensProviderSetup = mapper(bizCode) == FloatAction.OpenProviderSetup
        val rowKind = resolveSuggestionRowKind(
            selectable = source.selectable,
            opensProviderSetup = opensProviderSetup
        )
        val rows = resList.map { text ->
            SuggestionRowUiModel(text = text, kind = rowKind)
        }
        val adapter = SuggestionListAdapter(this, rows, ensureOverlayDimensions().list)
        suggestionListAdapter = adapter
        listView.adapter = adapter
        showLoading(false)

        if (
            shouldCompleteFirstUseGuideForSuggestionList(
                windowShown = suggestionWindowShown,
                source = source,
                suggestionCount = rows.size,
                opensProviderSetup = opensProviderSetup
            )
        ) {
            markFirstActionableSuggestionShown()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            if (opensProviderSetup) {
                openProviderSetupActivity()
                hideFloatingWindow()
                return@setOnItemClickListener
            }
            if (!source.selectable) return@setOnItemClickListener

            submitSelectedSuggestion(
                adapter.getItem(position).text,
                send = true,
                segmented = true,
                source = source,
                origin = renderedOrigin ?: return@setOnItemClickListener
            )
        }

        listView.setOnItemLongClickListener { _, itemView, position, _ ->
            if (opensProviderSetup) {
                openProviderSetupActivity()
                hideFloatingWindow()
                return@setOnItemLongClickListener true
            }
            if (!source.selectable) return@setOnItemLongClickListener true

            itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            submitSelectedSuggestion(
                adapter.getItem(position).text,
                send = false,
                segmented = false,
                source = source,
                origin = renderedOrigin ?: return@setOnItemLongClickListener true
            )
            true
        }

        completeFloatingBallAction(
            floatingBallActionId,
            if (source == SuggestionSource.MANUAL_LIST && resList.isNotEmpty()) {
                FloatingBallOperationState.SUCCESS
            } else {
                FloatingBallOperationState.ERROR
            }
        )
    }

    private fun submitSelectedSuggestion(
        text: String,
        send: Boolean,
        segmented: Boolean,
        source: SuggestionSource,
        origin: SuggestionOrigin
    ) {
        if (text.isBlank()) {
            Log.d(TAG, "忽略空回复选项")
            return
        }

        val root = rootInActiveWindow
        val currentPackageName = root?.packageName?.toString()
        val expectedChatTitle = root
            ?.takeIf { it.packageName?.toString() == WECHAT_PACKAGE }
            ?.let(::readChatTitle)
            .orEmpty()
        val currentAssistantChatKey = assistantChatKey(
            currentPackageName,
            expectedChatTitle
        )
        val eligibleAssistantChatKey = currentAssistantChatKey
            ?.takeIf(::isAssistantChatAllowed)
        if (eligibleAssistantChatKey == null ||
            !canUseSuggestionsInCurrentChat()
        ) {
            if (isWeChatGroupChatTitle(expectedChatTitle)) {
                suppressSuggestionsForGroupChat()
            } else if (currentAssistantChatKey != null) {
                suppressAssistantForIneligibleChat(currentAssistantChatKey)
            }
            return
        }
        if (!isSuggestionOriginCurrent(
                origin = origin,
                currentGeneration = currentChatContextGeneration(),
                currentPackageName = currentPackageName,
                currentRawTitle = expectedChatTitle
            )) {
            invalidateSuggestionsForChatIdentityChange()
            accessibilityRefreshCoordinator.request(forceNavigationBoundary = false)
            return
        }
        val currentPage = readCurrentWeChatPageResult(
            generation = currentChatContextGeneration(),
            root = root,
            rawTitle = expectedChatTitle
        )
        val currentFrameKey = when (currentPage) {
            is CapturedWeChatPageRead.TextReady -> currentPage.page.frameKey
            is CapturedWeChatPageRead.VisionRequired -> currentPage.page.frameKey
            is CapturedWeChatPageRead.Blocked -> null
        }
        val isPageCurrent = isSuggestionFrameCurrent(
            origin = origin,
            currentWindowId = currentFrameKey?.windowId,
            currentFingerprint = currentFrameKey?.fingerprint,
            currentViewportEpoch = weChatViewportEpoch,
            currentPageIsTextReady = currentPage is CapturedWeChatPageRead.TextReady
        )
        if (!isPageCurrent) {
            blockSuggestionsForUnreadyPage()
            accessibilityRefreshCoordinator.request(forceNavigationBoundary = false)
            return
        }
        val expectedInputText = root?.let(::readInputText).orEmpty()

        performSelectedSuggestion(
            text = text,
            send = send,
            segmented = segmented,
            source = source,
            expectedInputText = expectedInputText,
            expectedChatTitle = expectedChatTitle,
            expectedAssistantChatKey = eligibleAssistantChatKey
        )
    }

    private fun performSelectedSuggestion(
        text: String,
        send: Boolean,
        segmented: Boolean,
        source: SuggestionSource,
        expectedInputText: String,
        expectedChatTitle: String,
        expectedAssistantChatKey: AssistantChatKey
    ) {
        val expectedGeneration = currentChatContextGeneration()
        hideFloatingWindow()
        hideRealtimeSuggestion()
        submitSelectedText(
            text = text,
            isSend = send,
            isSegmented = segmented,
            expectedInputText = expectedInputText,
            expectedGeneration = expectedGeneration,
            expectedChatTitle = expectedChatTitle,
            expectedAssistantChatKey = expectedAssistantChatKey
        )
    }

    private fun pauseAutomaticSuggestionsForSession(message: String) {
        automaticSuggestionsBlockedForSession = true
        pendingAutomaticListChatKey = null
        resetRealtimeSuggestionState()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun submitSelectedText(
        text: String,
        isSend: Boolean,
        isSegmented: Boolean,
        expectedInputText: String = getCurrentInputText(),
        expectedGeneration: Long = currentChatContextGeneration(),
        expectedChatTitle: String = getChatTitle(),
        expectedAssistantChatKey: AssistantChatKey? = currentAssistantChatKey()
    ) {
        if (text.isBlank()) {
            Log.d(TAG, "忽略空文本提交")
            return
        }
        if (!canUseSuggestionsInCurrentChat() ||
            !isAssistantChatAllowed(expectedAssistantChatKey)
        ) {
            Log.d(TAG, "当前聊天未启用 AI，忽略文本提交")
            return
        }

        val segments = if (isSend && isSegmented) {
            splitTextByPunctuation(text)
        } else {
            listOf(text)
        }
        serviceScope.launch {
            accessibilityRefreshCoordinator.pause()
            try {
                val result = suggestionSubmitCoordinator.submit(
                    request = SuggestionSubmitRequest(
                        segments = segments,
                        send = isSend,
                        expectedInputText = expectedInputText
                    ),
                    port = object : SuggestionSubmitPort {
                        override fun isContextCurrent(): Boolean {
                            val root = rootInActiveWindow ?: return false
                            val currentPackageName = root.packageName?.toString()
                            val currentChatTitle = readChatTitle(root).orEmpty()
                            return currentChatContextGeneration() == expectedGeneration &&
                                isFloatingBallEnabled() &&
                                currentPackageName == expectedAssistantChatKey?.sourcePackage &&
                                currentChatTitle == expectedChatTitle &&
                                isAssistantChatAllowed(expectedAssistantChatKey)
                        }

                        override fun readInputText(): String? {
                            val input = findWeChatInputNode() ?: return null
                            return input.text?.toString().orEmpty()
                        }

                        override fun setInputText(text: String): Boolean {
                            val input = findWeChatInputNode() ?: return false
                            return setWeChatInputText(input, text)
                        }

                        override fun isSendButtonReady(): Boolean {
                            val button = findWeChatSendButton() ?: return false
                            return button.isClickable && button.isEnabled
                        }

                        override fun clickSendButton(): Boolean = clickWeChatSendButton()
                    }
                )
                renderSuggestionSubmitResult(result)
            } finally {
                if (!isServiceDestroying) {
                    accessibilityRefreshCoordinator.request(forceNavigationBoundary = false)
                    accessibilityRefreshCoordinator.resume()
                } else {
                    accessibilityRefreshCoordinator.cancel()
                }
            }
        }
    }

    private fun renderSuggestionSubmitResult(result: SuggestionSubmitResult) {
        when (result) {
            SuggestionSubmitResult.Completed -> Unit
            SuggestionSubmitResult.Busy -> Log.d(TAG, "上一条建议仍在提交，忽略重复选择")
            SuggestionSubmitResult.Cancelled -> Log.d(TAG, "聊天上下文已变化，取消建议提交")
            is SuggestionSubmitResult.Failed -> {
                val message = when {
                    result.reason == SuggestionSubmitFailure.INPUT_CHANGED -> {
                        "检测到输入内容已变化，已停止自动发送"
                    }
                    result.reason == SuggestionSubmitFailure.SEND_COMPLETION_NOT_CONFIRMED -> {
                        "未确认上一段已发送，已停止后续分段"
                    }
                    result.textWritten -> {
                        "发送按钮未就绪，文本已保留，请手动发送"
                    }
                    else -> "未找到可用的微信输入框，请重试"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clickWeChatSendButton(): Boolean {
        val button = findWeChatSendButton()
        if (button == null) {
            Log.d("SetEditTextReceiver", "发送按钮不存在")
            return false
        }

        return button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun setWeChatInputText(editText: AccessibilityNodeInfo, text: String?): Boolean {
        return try {
            editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val bundle = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text.orEmpty()
                )
            }
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle) ||
                editText.paste(text)
        } catch (e: Exception) {
            Log.e(TAG, "设置输入框文本异常", e)
            false
        }
    }

    private fun openProviderSetupActivity() {
        val intent = Intent(this, io.github.a982246809.replyassistant.AiProviderSetupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun removeOverlayWindows() {
        cancelOverlayGeometryRetry()
        floatingBallDockCorrectionGeneration += 1
        firstUseActivationSuccessOverlay?.hide()
        firstUseActivationSuccessOverlay = null
        hideFloatingWindow()
        hideRealtimeSuggestion()
        floatingBallSnapAnimator?.cancel()
        floatingBallWidthAnimator?.cancel()
        stopFloatingBallPulse()
        if (::floatingBallView.isInitialized && isBallAdded) {
            try {
                windowManager.removeView(floatingBallView)
            } catch (e: Exception) {
                Log.e(TAG, "移除悬浮球异常", e)
            } finally {
                isBallAdded = false
            }
        }
    }

    private fun registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
    }

    private fun unregisterReceiverSafely(receiver: BroadcastReceiver) {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "注销广播接收器异常", e)
        }
    }

    inner class RenderListReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.example.SWITCH_BALL" -> {
                    val canShowBall = intent.getBooleanExtra("isShowBall", true) &&
                        canUseSuggestionsInCurrentChat()
                    updateFloatingBallVisibility(visible = canShowBall)
                    if (canShowBall && isRealtimeSuggestionShowing) {
                        showFloatingBallWithRealtimeSuggestion()
                    }
                    if (!canShowBall) {
                        resetFloatingBallOperation()
                        suggestionTriggerState.reset()
                        pendingAutomaticListChatKey = null
                        invalidateListRequest()
                        hideFloatingWindow()
                        resetRealtimeSuggestionState()
                    }
                }

                "com.example.ACTION_RES_LIST" -> {
                    val message = intent.getStringExtra("data")
                    val bizCode = intent.getStringExtra("biz_code")
                    val resList = try {
                        Json.decodeFromString<List<String>>(message.toString())
                    } catch (e: Exception) {
                        listOf(message.toString())
                    }

                    val isFromInput = intent.getBooleanExtra("from_input", false)
                    if (!isFromInput && canUseSuggestionsInCurrentChat()) {
                        renderResponseList(resList, bizCode)
                    } else if (!canUseSuggestionsInCurrentChat()) {
                        hideFloatingWindow()
                        clearRenderedSuggestionList()
                    }
                }

                "com.example.ACTION_LOADING_STATE" -> {
                    val newLoadingText = intent.getStringExtra("loading_text")
                    if (newLoadingText != null) {
                        loadingText = newLoadingText
                        updateLoadingText()
                    }
                }
            }
        }
    }

    /**
     * 获取当前输入框的文本内容
     * @return 输入框的文本内容，如果获取失败则返回空字符串
     */
    private fun getCurrentInputText(): String {
        return try {
            val root = rootInActiveWindow ?: return ""
            readInputText(root)
        } catch (e: Exception) {
            Log.e(TAG, "获取输入框文本异常", e)
            ""
        }
    }

    /**
     * 检查是否在微信聊天界面
     * @return 是否在微信聊天界面
     */
    private fun isInWeChatChatInterface(): Boolean {
        return try {
            val root = rootInActiveWindow ?: return false
            isWeChatChatInterfaceTitle(readChatTitle(root))
        } catch (e: Exception) {
            Log.e(TAG, "检查微信聊天界面异常", e)
            false
        }
    }

    /**
     * 获取聊天界面标题
     * @return 聊天界面标题，如果不在聊天界面则返回空字符串
     */
    private fun getChatTitle(): String {
        return try {
            val root = rootInActiveWindow ?: return ""
            readChatTitle(root).orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "获取聊天界面标题异常", e)
            ""
        }
    }

    /**
     * 获取聊天记录
     * @return 包含聊天记录的消息数组
     */
    fun getChatHistory(): Array<Msg> {
        return try {
            if (!isAssistantChatAllowed(currentAssistantChatKey())) return emptyArray()
            val root = rootInActiveWindow ?: return emptyArray()
            val rawTitle = readChatTitle(root)
            when (val messagesRead = readChatMessages(
                generation = currentChatContextGeneration(),
                rawTitle = rawTitle,
                root = root
            )) {
                is WeChatChatMessagesResult.TextReady -> messagesRead.messages.toTypedArray()
                is WeChatChatMessagesResult.VisionRequired,
                is WeChatChatMessagesResult.Blocked -> emptyArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取聊天记录异常", e)
            emptyArray<Msg>()
        }
    }

    private fun readCurrentWeChatPage(
        generation: Long,
        root: AccessibilityNodeInfo? = rootInActiveWindow,
        rawTitle: String? = root?.let(::readChatTitle)
    ): CapturedWeChatPage? = when (val pageRead = readCurrentWeChatPageResult(
        generation = generation,
        root = root,
        rawTitle = rawTitle
    )) {
        is CapturedWeChatPageRead.TextReady -> pageRead.page
        is CapturedWeChatPageRead.VisionRequired,
        is CapturedWeChatPageRead.Blocked -> null
    }

    private fun readCurrentWeChatPageResult(
        generation: Long,
        root: AccessibilityNodeInfo? = rootInActiveWindow,
        rawTitle: String? = root?.let(::readChatTitle)
    ): CapturedWeChatPageRead {
        val activeRoot = root
            ?: return CapturedWeChatPageRead.Blocked(
                WeChatChatMessagesBlockedReason.READ_FAILED
            )
        return try {
            val sourcePackage = activeRoot.packageName?.toString()
            val assistantChatKey = assistantChatKey(sourcePackage, rawTitle)
            if (!isAssistantChatAllowed(assistantChatKey)) {
                return CapturedWeChatPageRead.Blocked(
                    WeChatChatMessagesBlockedReason.READ_FAILED
                )
            }
            val messagesRead = readChatMessages(generation, rawTitle, activeRoot)
            if (messagesRead is WeChatChatMessagesResult.Blocked) {
                return CapturedWeChatPageRead.Blocked(
                    reason = messagesRead.reason,
                    frameKey = messagesRead.frameKey
                )
            }
            val messages = when (messagesRead) {
                is WeChatChatMessagesResult.TextReady -> messagesRead.messages
                is WeChatChatMessagesResult.VisionRequired -> messagesRead.accessibleMessages
                is WeChatChatMessagesResult.Blocked -> error("blocked result already handled")
            }
            val snapshot = buildCapturedChatSnapshot(
                generation = generation,
                sourcePackage = sourcePackage,
                rawTitle = rawTitle,
                messages = messages,
                capturedAtMillis = System.currentTimeMillis()
            ) ?: run {
                return CapturedWeChatPageRead.Blocked(
                    WeChatChatMessagesBlockedReason.READ_FAILED,
                    messagesRead.frameKey
                )
            }
            val inputNode = findWeChatInputNode(activeRoot)
            when (messagesRead) {
                is WeChatChatMessagesResult.TextReady -> CapturedWeChatPageRead.TextReady(
                    CapturedWeChatPage(
                        snapshot = snapshot,
                        inputText = inputNode?.text?.toString().orEmpty(),
                        inputBounds = inputNode?.let(::readBoundsInScreen),
                        hasEditableInput = inputNode?.isEditable == true && inputNode.isEnabled,
                        frameKey = messagesRead.frameKey
                    )
                )
                is WeChatChatMessagesResult.VisionRequired ->
                    CapturedWeChatPageRead.VisionRequired(
                        CapturedWeChatVisionPage(
                            snapshot = snapshot,
                            inputText = inputNode?.text?.toString().orEmpty(),
                            frameKey = messagesRead.frameKey,
                            windowId = messagesRead.windowId,
                            viewportEpoch = weChatViewportEpoch
                        )
                    )
                is WeChatChatMessagesResult.Blocked -> error("blocked result already handled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取微信原子快照异常", e)
            CapturedWeChatPageRead.Blocked(WeChatChatMessagesBlockedReason.READ_FAILED)
        }
    }

    private fun readChatTitle(root: AccessibilityNodeInfo): String? {
        return root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/obn")
            .getOrNullAt(0)
            ?.text
            ?.toString()
    }

    private fun readChatMessages(
        generation: Long,
        rawTitle: String?,
        root: AccessibilityNodeInfo
    ): WeChatChatMessagesResult {
        weChatViewportInvalidationPending = false
        return weChatPageReader.read(
            generation = generation,
            chatTitle = rawTitle,
            root = root
        )
    }

    private fun readInputText(root: AccessibilityNodeInfo): String {
        return findWeChatInputNode(root)?.text?.toString().orEmpty()
    }

    private fun performChatHistoryScrollAndInvalidateViewport(action: Int): Boolean {
        val didScroll = performChatHistoryScroll(action)
        if (didScroll) {
            invalidateWeChatViewport()
        }
        return didScroll
    }

    private fun performChatHistoryScroll(action: Int): Boolean {
        return try {
            findChatHistoryScrollNode(action)?.performAction(action) == true
        } catch (e: Exception) {
            Log.w(TAG, "执行聊天列表滚动失败: action=$action", e)
            false
        }
    }

    private fun findChatHistoryScrollNode(action: Int): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        var current = root
            .findAccessibilityNodeInfosByViewId(WECHAT_MESSAGE_CONTAINER_VIEW_ID)
            .firstOrNull()
            ?: return null

        while (true) {
            val supportsAction = current.actionList.any { it.id == action }
            if (current.isScrollable || supportsAction) {
                return current
            }
            current = current.parent ?: return null
        }
    }

    /**
     * 服务连接时的初始化
     */
    override fun onServiceConnected() {
        isServiceDestroying = false
        watchdogObservedGroupChat = false
        watchdogObservedDirectChatKey = null
        firstUseGuideCompletionJob?.cancel()
        firstUseGuideCompletionJob = null
        assistantChatPolicyJob?.cancel()
        assistantChatPolicyJob = null
        providerStateJob?.cancel()
        providerStateJob = null
        assistantChatPolicyState = AssistantChatPolicyState.Loading
        assistantPolicySuppressedChatKey = null
        assistantPolicyFailClosedSuppressed = false
        assistantPolicyUnavailableNoticeShown = false
        accessibilityRefreshCoordinator.cancel()
        invalidateChatContextSession()
        automaticSuggestionsBlockedForSession = false
        try {
            // 初始化 Hilt EntryPoint
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ReplyAssistantServiceEntryPoint::class.java
            )
            userPreferences = entryPoint.userPreferences()
            aiRepository = entryPoint.aiRepository()
            conversationContextAssembler = entryPoint.conversationContextAssembler()
            assistantChatPolicy = entryPoint.assistantChatPolicy()
            if (::weChatWindowImageCapture.isInitialized) {
                weChatWindowImageCapture.close()
            }
            weChatPageReader = WeChatPageReader()
            weChatWindowImageCapture = AndroidWeChatWindowImageCapture(this)
            floatingBallEnabledJob?.cancel()
            firstUseGuideCompletionJob?.cancel()
            chatContextSession = ChatContextSession()
            floatingBallEnabledJob = serviceScope.launch {
                userPreferences.floatingBallEnabled.collect { enabled ->
                    floatingBallEnabled = enabled
                    accessibilityRefreshCoordinator.request()
                }
            }
            firstUseGuideCompletionJob = firstUseGuideCompletionTracker.observe(
                scope = serviceScope,
                preferences = userPreferences.firstUseGuidePreferences
            )

            initOverlayWindows()

            firstUseActivationSuccessOverlay?.hide()
            firstUseActivationSuccessOverlay = FirstUseActivationSuccessOverlay(
                host = AndroidFirstUseActivationSuccessOverlayHost(
                    context = this,
                    windowManager = windowManager,
                    onFailure = { error ->
                        Log.e(TAG, "首次启用成功提示异常", error)
                    }
                ),
                scheduler = HandlerFirstUseActivationSuccessOverlayScheduler(handler)
            )
            serviceScope.launch {
                val firstUseGuidePreferences =
                    userPreferences.firstUseGuidePreferences.first()
                if (shouldShowFirstUseActivationSuccess(firstUseGuidePreferences)) {
                    firstUseActivationSuccessOverlay?.show()
                }
            }

            assistantChatPolicyJob = serviceScope.launch {
                assistantChatPolicy.observeState().collect(::handleAssistantChatPolicyState)
            }

            providerStateJob = serviceScope.launch {
                aiRepository.providerState.collect { state ->
                    if (state is AiProviderState.Ready && automaticSuggestionsBlockedForSession) {
                        automaticSuggestionsBlockedForSession = false
                        pendingAutomaticListChatKey = null
                        accessibilityRefreshCoordinator.request(forceNavigationBoundary = false)
                    }
                }
            }

            // 注册广播接收器
            ballClickReceiver = BallClickReceiver()
            registerReceiverCompat(
                ballClickReceiver,
                IntentFilter("com.example.ACTION_TO_ACCESSIBILITY")
            )

            setEditTextReceiver = SetEditTextReceiver()
            registerReceiverCompat(
                setEditTextReceiver,
                IntentFilter("com.example.ACTION_SET_EDIT_TEXT")
            )

            renderListReceiver = RenderListReceiver()
            registerReceiverCompat(
                renderListReceiver,
                IntentFilter().apply {
                    addAction("com.example.SWITCH_BALL")
                    addAction("com.example.ACTION_RES_LIST")
                    addAction("com.example.ACTION_LOADING_STATE")
                }
            )

            // 配置无障碍服务
            val serviceInfo = AccessibilityServiceInfo().apply {
                eventTypes = accessibilityRefreshEventTypes
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 100
            }
            setServiceInfo(serviceInfo)
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 1000)
            accessibilityRefreshCoordinator.request()
        } catch (e: Exception) {
            Log.e(TAG, "服务连接时的初始化异常", e)
        }
    }

    /**
     * 服务销毁时的清理
     */
    override fun onDestroy() {
        isServiceDestroying = true
        accessibilityRefreshCoordinator.cancel()
        runBlocking {
            withTimeoutOrNull(1_500L) {
                chatContextSession?.shutdown()
            }
        }
        invalidateChatContextSession()
        floatingBallEnabledJob?.cancel()
        floatingBallEnabledJob = null
        firstUseGuideCompletionJob?.cancel()
        firstUseGuideCompletionJob = null
        assistantChatPolicyJob?.cancel()
        assistantChatPolicyJob = null
        providerStateJob?.cancel()
        providerStateJob = null
        handler.removeCallbacks(runnable)
        floatingBallCollapseRunnable?.let(handler::removeCallbacks)
        floatingBallStatusResetRunnable?.let(handler::removeCallbacks)
        floatingBallCollapseRunnable = null
        floatingBallStatusResetRunnable = null
        resetFloatingBallOperation()
        sendBroadcast(
            Intent("com.example.SWITCH_BALL").putExtra("isShowBall", false)
        )

        if (::ballClickReceiver.isInitialized) {
            unregisterReceiverSafely(ballClickReceiver)
        }
        if (::setEditTextReceiver.isInitialized) {
            unregisterReceiverSafely(setEditTextReceiver)
        }
        if (::renderListReceiver.isInitialized) {
            unregisterReceiverSafely(renderListReceiver)
        }

        if (::floatingWindowView.isInitialized &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            clearPendingSuggestionTouchableRegionUpdate(floatingWindowView)
            floatingWindowView.removeOnAttachStateChangeListener(
                suggestionWindowAttachStateChangeListener
            )
            floatingWindowView.removeOnLayoutChangeListener(
                suggestionWindowLayoutChangeListener
            )
        }

        removeOverlayWindows()
        if (::weChatWindowImageCapture.isInitialized) {
            weChatWindowImageCapture.close()
        }
        serviceScope.cancel()
        accessibilityRefreshCoordinator.cancel()
        super.onDestroy()
    }

    /**
     * 处理悬浮球点击事件的广播接收器
     */
    inner class BallClickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!canUseSuggestionsInCurrentChat()) {
                updateFloatingBallVisibility(visible = false)
                return
            }
            try {
                startChatCollection(consumer = WeChatReadConsumer.SHORT_TAP)
            } catch (e: Exception) {
                Log.e(TAG, "处理悬浮球点击事件的广播接收器异常", e)
                renderResponseList(
                    listOf("读取聊天记录失败，请稍后重试"),
                    source = SuggestionSource.SYSTEM_MESSAGE
                )
            }
        }
    }


    /**
     * 处理设置输入框文本的广播接收器
     */
    inner class SetEditTextReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val text = intent.getStringExtra("data")
                val isSend = intent.getBooleanExtra("is_send", false)
                val isSegmented = intent.getBooleanExtra("is_segmented", false)
                
                if (text != null && canUseSuggestionsInCurrentChat()) {
                    submitSelectedText(text, isSend, isSegmented)
                }
            } catch (e: Exception) {
                Log.d("SetEditTextReceiver", e.toString())
            }
        }
    }

    /**
     * 粘贴文本到当前元素
     * @param text 要粘贴的文本
     */
    private fun AccessibilityNodeInfo.paste(text: String?): Boolean {
        return try {
            performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("chat_text", text)
            clipboardManager.setPrimaryClip(clip)
            performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to paste text", e)
            false
        }
    }

    /**
     * 按标点符号分割文本
     * @param text 要分割的文本
     * @return 分割后的文本列表，去除标点符号但保留问号
     */
    private fun splitTextByPunctuation(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        
        // 定义分割用的标点符号（中文标点）
        val punctuationPattern = Regex("(?<=[。！？；、，])")
        
        val segments = text.split(punctuationPattern)
            .map { segment ->
                // 去除末尾标点符号，但保留问号
                segment.trim().replace(Regex("[。！；、，]$"), "")
            }
            .filter { it.isNotBlank() }
        
        // 如果没有分割出多个段落，返回原文本作为单个段落
        return if (segments.isEmpty()) listOf(text) else segments
    }
} 
