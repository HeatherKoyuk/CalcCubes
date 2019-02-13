package com.koyuk.enterprises.calculationcubes.popups

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.koyuk.enterprises.calculationcubes.R
import kotlinx.android.synthetic.main.edit_mode_info_popup.*


class HideSolutions : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hide_solutions_info_popup)

        fab.setOnClickListener {
            finish()
        }
    }
}
