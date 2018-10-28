package com.koyuk.enterprises.calculationcubes

import android.widget.TextView
import android.support.v7.widget.RecyclerView
import android.view.View


class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal var solution: TextView
    internal var diff: TextView

    init {
        solution = itemView.findViewById(R.id.textSolution)
        diff = itemView.findViewById(R.id.textAbsoluteDifference)
    }

    interface OnStartDragListener {

        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
}