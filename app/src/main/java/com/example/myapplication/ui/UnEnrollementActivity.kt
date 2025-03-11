package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utilities.Statics
import com.example.myapplication.utilities.goToTdle
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish

class UnEnrollementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_un_enrollement)

        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)
       Statics.isEnrollement=false
        findViewById<Button>(R.id.un_enroll_phone_auth).setOnClickListener{val myintent = Intent(this, PersonalDataInputActivity::class.java);this.startActivity(myintent)}

        // UnEnroll with Palm Vein
        findViewById<Button>(R.id.un_enroll_pvp_auth).setOnClickListener{val myintent = Intent(this, CaptureActivity::class.java);this.startActivity(myintent)}

        if(testTimeFinish){setupInactivityTimeout(timeFinish){goToTdle()}}

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {android.R.id.home->{ val myintent = Intent(this, IdleActivity::class.java);this.startActivity(myintent) }
        }
        return super.onOptionsItemSelected(item)
    }
}