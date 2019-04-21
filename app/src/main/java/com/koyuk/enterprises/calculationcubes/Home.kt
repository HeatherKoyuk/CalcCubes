package com.koyuk.enterprises.calculationcubes

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.koyuk.enterprises.calculationcubes.popups.Rules
import kotlinx.android.synthetic.main.activity_home.*


class Home : AppCompatActivity() {

    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        pBar.visibility = View.GONE

        play.setOnClickListener {
            pBar.visibility = View.VISIBLE
            val intent = Intent(this, Roll::class.java)
            startActivity(intent)
        }

        rules.setOnClickListener {
            pBar.visibility = View.VISIBLE
            val intent = Intent(this, Rules::class.java)
            startActivity(intent)
        }
//        billingManager = BillingManager(this)

//        pro.setOnClickListener{
//            billingManager.upgrade(this)
//        }
    }

    override fun onResume() {
        super.onResume()
        pBar.visibility = View.GONE
    }
}
