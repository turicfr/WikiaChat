package com.oren.wikia_chat.client

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

class DelayAutoCompleteTextView(context: Context, attrs: AttributeSet) :
    AppCompatAutoCompleteTextView(context, attrs) {

    private companion object {
        const val MESSAGE_TEXT_CHANGED = 100
        const val AUTOCOMPLETE_DELAY = 750L
    }

    var loadingIndicator: ProgressBar? = null

    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super@DelayAutoCompleteTextView.performFiltering(msg.obj as CharSequence, msg.arg1)
        }
    }

    override fun performFiltering(text: CharSequence, keyCode: Int) {
        loadingIndicator?.visibility = View.VISIBLE
        mHandler.removeMessages(MESSAGE_TEXT_CHANGED)
        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(MESSAGE_TEXT_CHANGED, text),
            AUTOCOMPLETE_DELAY,
        )
    }

    override fun onFilterComplete(count: Int) {
        super.onFilterComplete(count)
        loadingIndicator?.visibility = View.GONE
    }
}
