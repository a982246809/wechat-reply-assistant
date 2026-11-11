package io.github.a982246809.replyassistant.util

import android.os.Handler
import android.os.Looper

/**
 * 防抖动工具类
 * 用于处理连续快速触发的事件，只响应最后一次
 */
object Debounce {
    /**
     * 防抖函数，延迟指定时间后执行操作，如果在延迟时间内再次调用则取消上一次操作
     *
     * @param delayMillis 延迟执行的时间(毫秒)
     * @param action 要执行的操作
     * @return 返回一个可以触发操作的函数
     */
    fun <T> debounce(delayMillis: Long = 500, action: (T) -> Unit): (T) -> Unit {
        var debounceJob: Runnable? = null
        val handler = Handler(Looper.getMainLooper())

        return { param: T ->
            // 取消之前的延迟执行
            debounceJob?.let { handler.removeCallbacks(it) }

            // 创建新的延迟执行
            debounceJob = Runnable {
                action(param)
            }

            // 延迟执行
            handler.postDelayed(debounceJob!!, delayMillis)
        }
    }

    /**
     * 防抖函数，无参数版本
     *
     * @param delayMillis 延迟执行的时间(毫秒)
     * @param action 要执行的操作
     * @return 返回一个可以触发操作的函数
     */
    fun debounce(delayMillis: Long = 500, action: () -> Unit): () -> Unit {
        var debounceJob: Runnable? = null
        val handler = Handler(Looper.getMainLooper())

        return {
            // 取消之前的延迟执行
            debounceJob?.let { handler.removeCallbacks(it) }

            // 创建新的延迟执行
            debounceJob = Runnable {
                action()
            }

            // 延迟执行
            handler.postDelayed(debounceJob!!, delayMillis)
        }
    }
} 
