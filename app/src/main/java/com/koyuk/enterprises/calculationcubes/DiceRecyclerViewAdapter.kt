package com.koyuk.enterprises.calculationcubes

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.view.LayoutInflater
import android.view.ViewGroup
import java.util.*
import android.widget.TextView.BufferType
import android.text.Spannable
import android.text.style.SuperscriptSpan
import android.text.SpannableStringBuilder
import android.widget.ImageView
import android.widget.TextView
import android.support.v7.widget.helper.ItemTouchHelper.Callback.makeMovementFlags
import android.support.v7.widget.helper.ItemTouchHelper
import android.R.attr.data
import android.graphics.Color
import java.util.Collections.swap






class DiceRecyclerViewAdapter(list: MutableList<Die>)  : RecyclerView.Adapter<DieViewHolder>(), ItemMoveCallback.ItemTouchHelperContract {

    var list = mutableListOf<Die>()
    init {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DieViewHolder {
        //Inflate the layout, initialize the View Holder
        val v = LayoutInflater.from(parent.context).inflate(R.layout.dice_view, parent, false)
        return DieViewHolder(v)
    }

    override fun onBindViewHolder(holder: DieViewHolder, position: Int) {

        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        var pip = list.get(position)
        var num = pip.pip

        setDieImage(holder.image, num)
        holder.text.text = if (num == 0) "" else (num).toString()

        //animate(holder);

    }

    override fun getItemCount(): Int {
        //returns the number of elements the RecyclerView will display
        return list.size
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
    }

    fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    // Insert a new item to the RecyclerView on a predefined position
    fun insert(position: Int, data: Die) {
        list.add(position, data)
        notifyItemInserted(position)
    }

    // Remove a RecyclerView item containing a specified Data object
    fun remove(data: Die) {
        val position = list.indexOf(data)
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(list, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(list, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(myViewHolder: DieViewHolder) {
    }

    override fun onRowClear(myViewHolder: DieViewHolder) {
    }

    private fun setDieImage(img: ImageView, random: Int){
        when (random) {
            0 -> img.setImageResource(R.drawable.dice3droll)
            1 -> img.setImageResource(R.drawable.one)
            2 -> img.setImageResource(R.drawable.two)
            3 -> img.setImageResource(R.drawable.three)
            4 -> img.setImageResource(R.drawable.four)
            5 -> img.setImageResource(R.drawable.five)
            6 -> img.setImageResource(R.drawable.six)
        }
    }
}