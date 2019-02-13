package com.koyuk.enterprises.calculationcubes

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.TextView.BufferType
import com.koyuk.enterprises.calculationcubes.models.Answer


class TwoColumnListAdapter(list: MutableList<Answer>) : RecyclerView.Adapter<ViewHolder>() {

    private var list = mutableListOf<Answer>()

    init {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //Inflate the layout, initialize the View Holder
        val v = LayoutInflater.from(parent.context).inflate(R.layout.two_column_list_view, parent, false)
        return ViewHolder(v)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        val ans = list[position]
        if (ans.display.indexOf("<sup>") > -1) {
            // TODO
            @Suppress("DEPRECATION")
            holder.solution.setText(Html.fromHtml(ans.display), BufferType.SPANNABLE)
        } else {
            holder.solution.text = ans.display
        }
        val diffNone = holder.diff.findViewById(R.id.textAbsoluteDifferenceNone) as TextView
        val diffMinus = holder.diff.findViewById(R.id.textAbsoluteDifferenceMinus) as TextView
        val diffPlus = holder.diff.findViewById(R.id.textAbsoluteDifferencePlus) as TextView
        diffNone.text = ""
        diffMinus.text = ""
        diffPlus.text = ""
        diffNone.visibility = View.INVISIBLE
        diffMinus.visibility = View.INVISIBLE
        diffPlus.visibility = View.INVISIBLE
        when {
            ans.plusMinus == 0 -> {
                diffNone.visibility = View.VISIBLE
                diffNone.text = ans.absoluteDiffDisplay
            }
            ans.plusMinus < 0 -> {
                diffMinus.visibility = View.VISIBLE
                diffMinus.text = ans.absoluteDiffDisplay
            }
            else -> {
                diffPlus.visibility = View.VISIBLE
                diffPlus.text = ans.absoluteDiffDisplay
            }
        }
        diffNone.invalidate()
        diffMinus.invalidate()
        diffPlus.invalidate()
        holder.diff.invalidate()
        //animate(holder);

    }

    override fun getItemCount(): Int {
        //returns the number of elements the RecyclerView will display
        return list.size
    }

    // Insert a new item to the RecyclerView on a predefined position
    fun insert(position: Int, data: Answer) {
        list.add(position, data)
        notifyItemInserted(position)
    }

    // Remove a RecyclerView item containing a specified Data object
    fun remove(data: Answer) {
        val position = list.indexOf(data)
        list.removeAt(position)
        notifyItemRemoved(position)
    }

}