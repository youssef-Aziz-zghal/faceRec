package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout

import androidx.appcompat.app.AppCompatActivity

import com.example.myapplication.R
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.terminate1
import com.example.myapplication.utilities.timeFinish



class IdleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_idle)
        val layout: RelativeLayout = findViewById(R.id.idle_layout)
        layout.setOnClickListener{val myintent = Intent(this, WelcomeActivity::class.java);this.startActivity(myintent) }
       //  setupInactivityTimeout(timeFinish){terminate1(true)}


    }
}