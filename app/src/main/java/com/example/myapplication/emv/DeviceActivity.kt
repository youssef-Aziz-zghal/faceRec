package com.example.myapplication.emv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.myapplication.ui.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.Utilisateur
import com.example.myapplication.database.ApiService
import com.example.myapplication.utilities.Statics
import com.example.myapplication.utilities.getAutoStart
import com.example.myapplication.utilities.saveAutoStart
import com.facebook.soloader.nativeloader.NativeLoader
import com.facebook.soloader.nativeloader.SystemDelegate
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DeviceActivity : AppCompatActivity(), DeviceHelper.ServiceReadyListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The DeviceHelper is initiated here in order to setup the navigation bar
        // notification menu (from within the "onReady" method of this class).

        Statics.isAutoStart=getAutoStart()
      /*  val currentLanguage = resources.configuration.locales.get(0).language
        val defaultLanguage= Locale.getDefault().language
        Toast.makeText(this, "Default=$defaultLanguage,Current=$currentLanguage", Toast.LENGTH_SHORT).show()*/

        if (!NativeLoader.isInitialized()) {
            NativeLoader.init(SystemDelegate())
        }
        NativeLoader.loadLibrary("pytorch_jni")
        NativeLoader.loadLibrary("torchvision_ops")
        NativeLoader.loadLibrary("opencv_java4")
        NativeLoader.loadLibrary("fr")


        //lifecycleScope.launch(Dispatchers.IO) { ApiService(applicationContext).persitUtilisateur(Utilisateur(name = "")) }

        try {
            DeviceHelper.me().init(applicationContext)
            DeviceHelper.me().bindService()
            DeviceHelper.me().setServiceListener(this)
            setContentView(R.layout.activity_device)
            val myintent = Intent(this, MainActivity::class.java);this.startActivity(myintent)
        }
        catch (e:Exception)
        {
            Toast.makeText(this,"failed",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onReady(version: String?) {
        Toast.makeText(this, "onReady", Toast.LENGTH_SHORT).show()
        DeviceHelper.me().register()
        DeviceHelper.me().disableHomeButton()


    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this,"devicedestroyedSaveauto",Toast.LENGTH_SHORT).show()
        saveAutoStart(value=Statics.isAutoStart)
    }
}