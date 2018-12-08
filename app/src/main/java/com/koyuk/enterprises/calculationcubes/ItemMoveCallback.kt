package com.koyuk.enterprises.calculationcubes


import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

class ItemMoveCallback(private val mAdapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

    var moreThanOneRow = false

    fun setMoreThanOneRow(){
        moreThanOneRow = true
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }


    override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        mAdapter.onItemMove(source.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        mAdapter.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            val itemViewHolder = viewHolder as ItemTouchHelperViewHolder?
            itemViewHolder!!.onItemSelected()
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        val itemViewHolder = viewHolder as ItemTouchHelperViewHolder
        itemViewHolder.onItemClear()
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        var dragFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        if(moreThanOneRow){
            dragFlags = dragFlags or ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
        return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, 0)
    }

}