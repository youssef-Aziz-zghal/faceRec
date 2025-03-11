package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle


import android.view.MenuItem
import android.widget.Button
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utilities.goToTdle
import com.example.myapplication.utilities.palmLib
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPApplicationEvent


class TermConditionActivity : AppCompatActivity() {

    lateinit var button:Button




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_term_condition)
        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)

        button=findViewById(R.id.terms_next)

        if(testTimeFinish){ setupInactivityTimeout(timeFinish) { goToTdle()}}


        button.setOnClickListener {
            // It is considered here that the enrollment process is started when the T&C are accepted.
            palmLib.pvpHelper.recordEvent(PVPApplicationEvent.ENROLLMENT_PROCESS_STARTED)
            Toast.makeText(this,"record event succes",Toast.LENGTH_SHORT).show()
            val myintent = Intent(this, PersonalDataInputActivity::class.java);this.startActivity(myintent) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {android.R.id.home->{ val myintent = Intent(this, IdleActivity::class.java);this.startActivity(myintent) }
        }
        return super.onOptionsItemSelected(item)
    }




}