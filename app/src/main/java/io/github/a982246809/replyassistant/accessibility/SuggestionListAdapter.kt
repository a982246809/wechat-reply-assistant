package io.github.a982246809.replyassistant.accessibility

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.a982246809.replyassistant.OverlaySizePolicy
import io.github.a982246809.replyassistant.R
import io.github.a982246809.replyassistant.SuggestionListPixelDimensions
import io.github.a982246809.replyassistant.toPixels

internal enum class SuggestionRowKind {
    REPLY,
    MESSAGE,
    ACTION
}

internal data class SuggestionRowUiModel(
    val text: String,
    val kind: SuggestionRowKind
)

internal fun resolveSuggestionRowKind(
    selectable: Boolean,
    opensProviderSetup: Boolean
): SuggestionRowKind {
    return when {
        opensProviderSetup -> SuggestionRowKind.ACTION
        selectable -> SuggestionRowKind.REPLY
        else -> SuggestionRowKind.MESSAGE
    }
}

internal class SuggestionListAdapter(
    private val context: Context,
    private val items: List<SuggestionRowUiModel>,
    initialDimensions: SuggestionListPixelDimensions? = null
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)
    private var interactionsEnabled = true
    private var dimensions = initialDimensions ?: defaultDimensions(context)

    val showsGestureHint: Boolean
        get() = items.any { it.kind == SuggestionRowKind.REPLY }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): SuggestionRowUiModel = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun areAllItemsEnabled(): Boolean = false

    override fun isEnabled(position: Int): Boolean {
        return interactionsEnabled && getItem(position).kind != SuggestionRowKind.MESSAGE
    }

    fun setInteractionsEnabled(enabled: Boolean) {
        if (interactionsEnabled == enabled) return
        interactionsEnabled = enabled
        notifyDataSetChanged()
    }

    fun updateDimensions(
        dimensions: SuggestionListPixelDimensions,
        forceRebind: Boolean = false
    ) {
        if (this.dimensions == dimensions && !forceRebind) return
        this.dimensions = dimensions
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = convertView
            ?: inflater.inflate(R.layout.item_suggestion_reply, parent, false)
        val item = getItem(position)
        val textView = rowView.findViewById<TextView>(R.id.suggestionText)
        val actionHintView = rowView.findViewById<TextView>(R.id.suggestionActionHint)
        val dividerView = rowView.findViewById<View>(R.id.suggestionDivider)

        OverlayGeometryApplier.applySuggestionRow(rowView, dimensions)

        textView.text = item.text
        textView.setTextColor(
            ContextCompat.getColor(
                context,
                if (item.kind == SuggestionRowKind.MESSAGE) {
                    R.color.suggestion_text_muted
                } else {
                    R.color.suggestion_text_primary
                }
            )
        )
        actionHintView.visibility = if (item.kind == SuggestionRowKind.ACTION) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val showDivider = position < items.lastIndex
        dividerView.visibility = if (showDivider) View.VISIBLE else View.GONE

        val enabled = isEnabled(position)
        rowView.isEnabled = enabled
        rowView.alpha = if (interactionsEnabled) 1f else 0.55f
        rowView.contentDescription = when (item.kind) {
            SuggestionRowKind.REPLY -> context.getString(
                R.string.suggestion_reply_content_description,
                item.text
            )
            SuggestionRowKind.ACTION -> context.getString(
                R.string.suggestion_action_content_description,
                item.text
            )
            SuggestionRowKind.MESSAGE -> item.text
        }
        return rowView
    }

    private companion object {
        fun defaultDimensions(context: Context): SuggestionListPixelDimensions {
            val metrics = context.resources.displayMetrics
            val validDensity = metrics.density.takeIf { it.isFinite() && it > 0f }
            val widthDp = if (metrics.widthPixels > 1 && validDensity != null) {
                metrics.widthPixels / validDensity
            } else {
                Float.NaN
            }
            return OverlaySizePolicy.resolve(widthDp)
                .toPixels(validDensity ?: 1f)
                .list
        }
    }
}
