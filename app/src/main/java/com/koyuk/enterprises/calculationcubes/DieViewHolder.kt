package com.koyuk.enterprises.calculationcubes

import android.widget.TextView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView


class DieViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {

    internal var image: ImageView = view.findViewById(R.id.dieImage)
    internal var text: TextView = view.findViewById(R.id.dieText)
}