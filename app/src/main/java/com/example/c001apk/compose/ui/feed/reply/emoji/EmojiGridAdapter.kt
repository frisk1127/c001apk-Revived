package com.example.c001apk.compose.ui.feed.reply.emoji

import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.example.c001apk.compose.R
import com.example.c001apk.compose.util.dp


class EmojiGridAdapter(
    private val emojiList: List<Pair<String, Int>>,
    private val onClickEmoji: (String) -> Unit,
    private val onCountStart: () -> Unit,
    private val onCountStop: () -> Unit
) : BaseAdapter() {

    override fun getCount() = 28

    override fun getItem(position: Int): Any {
        return 0
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    @SuppressLint("ViewHolder", "ClickableViewAccessibility")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val emoji = emojiList.getOrNull(position)
        return if (emoji != null || position == 27) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
            val imageView: ImageView = view.findViewById(R.id.imageView)

            imageView.setImageResource(emoji?.second ?: R.drawable.outline_backspace_24)
            emoji?.first?.let {
                view.background =
                    AppCompatResources.getDrawable(parent.context, R.drawable.selector_emoji)
                if (SDK_INT >= 26)
                    view.tooltipText = it.substring(1, it.lastIndex)
            }
            view.setOnClickListener {
                onClickEmoji(emoji?.first ?: "[c001apk]")
            }
            if (position == 27) {
                if (SDK_INT >= 26)
                    view.tooltipText = null
                view.setOnLongClickListener {
                    onCountStart()
                    false
                }
                view.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        onCountStop()
                    }
                    false
                }
            }
            view
        } else View(parent.context).apply {
            val cellHeight = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 40.dp else 48.dp
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeight)
            isEnabled = false
        }
    }

}
