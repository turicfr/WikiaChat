package com.oren.wikia_chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.runBlocking

class SwipeToDeleteCallback(
    private val context: Context,
    private val wikis: MutableList<Wiki>,
    private val adapter: WikiAdapter,
) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_clear_24)!!
    private val background = ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete))

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        runBlocking {
            (context.applicationContext as ChatApplication).database.wikiDao()
                .delete(adapter.getItem(viewHolder.adapterPosition))
        }
        wikis.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        val itemView = viewHolder.itemView

        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconLeft = itemView.left + iconMargin
        val iconTop = itemView.top + iconMargin
        val iconRight = itemView.right - iconMargin
        val iconBottom = itemView.bottom - iconMargin

        if (dX > 0) {
            background.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
            icon.setBounds(iconLeft, iconTop, iconLeft + icon.intrinsicWidth, iconBottom)
            c.clipRect(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
        } else {
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom,
            )
            icon.setBounds(iconRight - icon.intrinsicWidth, iconTop, iconRight, iconBottom)
            c.clipRect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        }

        background.draw(c)
        icon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
