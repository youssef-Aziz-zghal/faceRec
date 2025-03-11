package com.example.myapplication.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle

import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.example.myapplication.R
import com.example.myapplication.emv.DeviceHelper
import com.example.myapplication.utilities.Statics
import com.example.myapplication.utilities.maplanguages

import com.example.myapplication.utilities.palmLib
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.showConfirmationDialog
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPApplicationMode
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPResponse
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPTimeouts
import java.util.Locale


class MainActivity : AppCompatActivity() {

    lateinit var  initButton: Button
    lateinit var progressBarInit:ProgressBar
    lateinit var texte: TextView
    lateinit var currentLanguage:String
    var languageId:Int=R.id.menu_english
    lateinit var imageLanguage:ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,false)

        currentLanguage = resources.configuration.locales.get(0).language
        Statics.currentLuanguage=currentLanguage
        languageId=when(currentLanguage){"en"->R.id.menu_english;"es"->R.id.menu_spanish;"fr"->R.id.menu_french;else ->R.id.menu_english }
        imageLanguage=findViewById(R.id.imageView)
        imageLanguage.setImageResource(maplanguages[currentLanguage] ?:R.drawable.en)
        initButton=findViewById(R.id.button)
        texte=findViewById(R.id.textView)
        progressBarInit=findViewById(R.id.init_progress_bar)
        initButton.setOnClickListener { init() }


    }

   // Initialize the library in your application. This should be done during the initialization phase of your app,
   // such as in the onCreate method of your main activity
    private fun init() {
        progressBarInit.visibility = View.VISIBLE
        initButton.isEnabled=false
        palmLib.pvpHelper.init(
            PVPApplicationMode.PVP_APPLICATION_MODE_ENROLLMENT, PVPTimeouts(),
            {  initializeCallback(it) }, applicationContext)
    }

    private fun initializeCallback(returnStatus: PVPResponse) {
        if (returnStatus == PVPResponse.PVP_RESPONSE_SUCCESS) {
            Log.d("initResult", "Library initialized with success")
            val myintent = Intent(this, IdleActivity::class.java);this.startActivity(myintent)
            runOnUiThread {
                initButton.isEnabled = true
                progressBarInit.visibility = View.GONE
                Toast.makeText(this,"${returnStatus.name} Library initialized with success",Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("initResult", "Library Initialization Failed")
            Log.e("initStatus",returnStatus.name )
            runOnUiThread {
                initButton.isEnabled = true
                progressBarInit.visibility = View.GONE
                texte.text=resources.getString(R.string.toast_init_failed)
                Toast.makeText(applicationContext, getString(R.string.toast_init_failed)+returnStatus.name, Toast.LENGTH_LONG).show()

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        menu?.setGroupVisible(R.id.grpUnEnroll,false)
       menu?.setGroupVisible(R.id.grpLanguage,true)
        menu?.findItem(R.id.action_auto_start)?.isChecked=Statics.isAutoStart
        menu?.findItem(R.id.action_auto_start)?.isVisible=true
        menu?.findItem(languageId)?.isChecked=true



        return super.onCreateOptionsMenu(menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_version)
            .setTitle(getString(R.string.menu_version) + " " + /*BuildConfig.VERSION_NAME+*/"1.1")
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {
            R.id.action_quit ->{ showConfirmationDialog(this,getString(R.string.dialog_title_terminate),getString(
                R.string.dialog_message_terminate
            ), getString(R.string.confirm),getString(R.string.dialog_cancel) , { finishAffinity();DeviceHelper.me().enableHomeButton();/*DeviceHelper.me().unregister();DeviceHelper.me().unbindService()*//*exitProcess(0)*/ }) }
            R.id.action_version ->{ Toast.makeText(this,"clckedversion",Toast.LENGTH_SHORT).show() }
            R.id.action_unEnroll ->{ Toast.makeText(this,"clckedunenroll",Toast.LENGTH_SHORT).show() }
            R.id.action_auto_start ->{ Statics.isAutoStart=!item.isChecked;item.isChecked=Statics.isAutoStart }
           android.R.id.home->{ Toast.makeText(this,"clckedhome",Toast.LENGTH_SHORT).show() }
            R.id.menu_english->{ changeLanguage("en")}
            R.id.menu_spanish->{changeLanguage("es")  }
            R.id.menu_french-> {changeLanguage("fr")}

        }

        return super.onOptionsItemSelected(item)
    }



    private fun changeLanguage(languageCode: String) {
       val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
         val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)








        //createConfigurationContext(config) // Appliquer la nouvelle langue

       // baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        // Redémarrer l'activité pour appliquer les changements de langue

       //recreate()

        // val myintent = Intent(this, MainActivity2::class.java);this.startActivity(myintent)


       /* val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK//Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)*/


    }



}