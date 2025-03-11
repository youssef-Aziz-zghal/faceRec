package com.example.myapplication.ui

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.utilities.Statics
import com.example.myapplication.utilities.goToTdle
import com.example.myapplication.utilities.palmLib
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.showConfirmationDialog
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPResponse
import java.util.Locale
import kotlin.system.exitProcess


class WelcomeActivity : AppCompatActivity() {
    lateinit var validate:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        validate=findViewById(R.id.welcome_validate_button)
        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)

       if(testTimeFinish){setupInactivityTimeout(timeFinish) { goToTdle()}}


        validate.setOnClickListener { Statics.isEnrollement=true; val myintent = Intent(this, TermConditionActivity::class.java);this.startActivity(myintent)}


    }










   // After completing your application's workflow involving the PVP Helper Library API, you have the option
    // to terminate the library initialization using the `terminate`. This is useful when you want to release
    // resources or conclude the interaction with the library.
    fun terminate(killApplication: Boolean) {
        println(1)
        palmLib.pvpHelper.terminate { terminateCallback(it) }
        println(3)
       val myintent = Intent(this, MainActivity::class.java);this.startActivity(myintent)
        //finishAffinity() //exitProcess(0)
    }

    private fun terminateCallback(pvpResponse: PVPResponse) {
        println(2)
        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            Log.d("terminateResult", "Library terminated with success")
            runOnUiThread { Toast.makeText(applicationContext,"Library terminated with success", Toast.LENGTH_LONG).show()
            }


        } else {
            Log.e("terminateResult", "The library failed to terminate")
            runOnUiThread {
                Toast.makeText(applicationContext,"The library failed to terminate", Toast.LENGTH_LONG).show()}
        }


    }







    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        menu?.findItem(R.id.action_auto_start)?.isChecked=Statics.isAutoStart

        menu?.findItem(R.id.action_auto_start)?.isVisible=true
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_version)
            .setTitle(getString(R.string.menu_version) + " " + /*BuildConfig.VERSION_NAME*/"1.1")
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {
            R.id.action_quit ->{ showConfirmationDialog(this,getString(R.string.dialog_title_terminate),getString(
                R.string.dialog_message_terminate
            ),
                getString(R.string.confirm),getString(R.string.dialog_cancel) , {terminate(false)})
            }
            R.id.action_version ->{ Toast.makeText(this,"clckedversion", Toast.LENGTH_SHORT).show() }
            R.id.action_unEnroll ->{ val myintent = Intent(this, UnEnrollementActivity::class.java);this.startActivity(myintent)}

            android.R.id.home->{ val myintent = Intent(this, IdleActivity::class.java);this.startActivity(myintent) }
            R.id.action_auto_start ->{ Statics.isAutoStart=!item.isChecked;item.isChecked=Statics.isAutoStart }

        }

        return super.onOptionsItemSelected(item)
    }


}