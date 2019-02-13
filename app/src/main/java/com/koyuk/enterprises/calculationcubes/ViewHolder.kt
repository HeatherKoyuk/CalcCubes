package com.koyuk.enterprises.calculationcubes

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView


class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal var solution: TextView = itemView.findViewById(R.id.textSolution)
    internal var diff: LinearLayout = itemView.findViewById(R.id.absoluteDiff)

}