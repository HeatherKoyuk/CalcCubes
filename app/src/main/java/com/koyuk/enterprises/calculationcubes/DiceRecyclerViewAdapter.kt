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
import android.content.Context
import android.graphics.Color
import java.util.Collections.swap
import android.view.MotionEvent
import android.support.v4.view.MotionEventCompat
import android.util.Log


class DiceRecyclerViewAdapter(list: MutableList<Die>, context: Context,
                              dragLlistener: OnStartDragListener,
                              listChangedListener: OnDiceListChangedListener)  : RecyclerView.Adapter<DieViewHolder>(), ItemTouchHelperAdapter {

    var list = mutableListOf<Die>()
    private var mContext: Context? = null
    private var mDragStartListener: OnStartDragListener
    private var mListChangedListener: OnDiceListChangedListener

    init {
        this.list = list
        this.mContext = context
        this.mDragStartListener = dragLlistener
        this.mListChangedListener = listChangedListener
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DieViewHolder {
        //Inflate the layout, initialize the View Holder
        val v = LayoutInflater.from(parent.context).inflate(R.layout.dice_view, parent, false)
        return DieViewHolder(v)
    }

    fun setDie(list: MutableList<Die>){
        this.list = list
    }

    override fun onBindViewHolder(holder: DieViewHolder, position: Int) {

        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        val pip = list.get(position)
        var num = pip.pip

        setDieImage(holder.image, num)
        holder.text.text = if (num == 0) "" else (num).toString()

        holder.image.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder)
            }
            false
        }

    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        //mListChangedListener!!.onNoteListChanged(list)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun getItemCount(): Int {
        //returns the number of elements the RecyclerView will display
        return list.size
    }

    override fun onItemDismiss(position: Int) {

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
interface ItemTouchHelperAdapter {
    /**
     * Called when an item has been dragged far enough to trigger a move. This is called every time
     * an item is shifted, and not at the end of a "drop" event.
     *
     * @param fromPosition The start position of the moved item.
     * @param toPosition   Then end position of the moved item.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int)


    /**
     * Called when an item has been dismissed by a swipe.
     *
     * @param position The position of the item dismissed.
     */
    fun onItemDismiss(position: Int)
}