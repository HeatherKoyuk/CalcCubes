package com.koyuk.enterprises.calculationcubes

import android.content.Context
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import java.lang.Integer.parseInt
import java.util.*


class DiceRecyclerViewAdapter(val roll: Roll, list: MutableList<Die>, context: Context,
                              dragListener: OnStartDragListener,
                              listChangedListener: OnDiceListChangedListener,
                              var editMode: Boolean) : RecyclerView.Adapter<DieViewHolder>(), ItemTouchHelperAdapter {

    private var list = mutableListOf<Die>()
    var values = ArrayList<RetItem>()
    private var mContext: Context? = null
    private var mDragStartListener: OnStartDragListener
    private var mListChangedListener: OnDiceListChangedListener

    init {
        this.list = list
        values = arrayListOf()
        for (i in 0 until list.size) {
            values.add(RetItem())
        }
        this.mContext = context
        this.mDragStartListener = dragListener
        this.mListChangedListener = listChangedListener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DieViewHolder {
        //Inflate the layout, initialize the View Holder
        val v = LayoutInflater.from(parent.context).inflate(R.layout.dice_view, parent, false)
        return DieViewHolder(v)
    }

    fun setDie(list: MutableList<Die>) {
        this.list = list
        values = arrayListOf()
        for (i in 0 until list.size) {
            values.add(RetItem(list[i].pip))
        }
    }

    fun clearDie(list: MutableList<Die>) {
        this.list = list
        values = arrayListOf()
        for (i in 0 until list.size) {
            values.add(RetItem())
        }
    }

    fun setEdit(em: Boolean) {
        editMode = em
    }

    override fun onBindViewHolder(holder: DieViewHolder, position: Int) {

        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        val pip = list[position]
        val num = pip.pip

        setDieImage(holder.image, num)
        if (editMode) {
            holder.text.visibility = View.GONE
            holder.editText.visibility = View.VISIBLE
            holder.editText.text = if (num == 0) "" else (num).toString()
            holder.editText.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(s: CharSequence, start: Int,
                                               count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    roll.clearAnswers()
                    values[position].value = s.toString()
                    try {
                        setDieImage(holder.image, parseInt(s.toString()))
                    } catch (e: Exception) {
                    }
                }
            })
        } else {
            holder.editText.visibility = View.GONE
            holder.text.visibility = View.VISIBLE
            val stringValue = if (num == 0) "" else (num).toString()
            holder.text.text = stringValue
            values[position].value = stringValue
        }

        holder.image.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder)
            }
            false
        }

    }

    fun retrieveData(): List<RetItem> {
        return values
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

    private fun setDieImage(img: ImageView, random: Int) {

        when (random) {
            0 -> img.setImageResource(R.drawable.dice3droll)
            1 -> img.setImageResource(R.drawable.one)
            2 -> img.setImageResource(R.drawable.two)
            3 -> img.setImageResource(R.drawable.three)
            4 -> img.setImageResource(R.drawable.four)
            5 -> img.setImageResource(R.drawable.five)
            6 -> img.setImageResource(R.drawable.six)
            7 -> img.setImageResource(R.drawable.seven)
            8 -> img.setImageResource(R.drawable.eight)
            9 -> img.setImageResource(R.drawable.nine)
            10 -> img.setImageResource(R.drawable.ten)
            11 -> img.setImageResource(R.drawable.eleven)
            12 -> img.setImageResource(R.drawable.twelve)
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

class RetItem {
    var value: String? = null

    constructor()
    constructor(i: Int) {
        value = i.toString()
    }
}