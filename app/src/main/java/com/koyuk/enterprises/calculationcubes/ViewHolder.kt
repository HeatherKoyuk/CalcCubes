package com.koyuk.enterprises.calculationcubes

import android.widget.TextView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.LinearLayout


class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal var solution: TextView
    internal var diff: LinearLayout

    init {
        solution = itemView.findViewById(R.id.textSolution)
        diff = itemView.findViewById(R.id.absoluteDiff)
    }
}