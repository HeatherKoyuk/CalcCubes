package com.koyuk.enterprises.calculationcubes


import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

class ItemMoveCallback(private val mAdapter: ItemTouchHelperContract) : ItemTouchHelper.Callback() {

    var moreThanOneRow = false

    fun setMoreThanOneRow(){
        moreThanOneRow = true
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {

    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        var dragFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        if(moreThanOneRow){
            dragFlags = dragFlags or ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
        return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        mAdapter.onRowMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?,
                                   actionState: Int) {


        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is DieViewHolder) {
                val myViewHolder = viewHolder as DieViewHolder
                mAdapter.onRowSelected(myViewHolder)
            }

        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView,
                           viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (viewHolder is DieViewHolder) {
            val myViewHolder = viewHolder as DieViewHolder
            mAdapter.onRowClear(myViewHolder)
        }
    }

    interface ItemTouchHelperContract {

        fun onRowMoved(fromPosition: Int, toPosition: Int)
        fun onRowSelected(myViewHolder: DieViewHolder)
        fun onRowClear(myViewHolder: DieViewHolder)

    }

}