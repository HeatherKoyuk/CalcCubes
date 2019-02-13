package com.koyuk.enterprises.calculationcubes

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView


class DieViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), ItemTouchHelperViewHolder {

    internal var image: ImageView = view.findViewById(R.id.dieImage)
    internal var text: TextView = view.findViewById(R.id.dieText)
    internal var editText: TextView = view.findViewById(R.id.dieTextEdit)
    // var rowView: View? = view

    override fun onItemSelected() {
    }

    override fun onItemClear() {
    }

}

interface ItemTouchHelperViewHolder {
    /**
     * Implementations should update the item view to indicate it's active state.
     */
    fun onItemSelected()

    /**
     * state should be cleared.
     */
    fun onItemClear()
}