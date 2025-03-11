package com.example.myapplication.utilities

import android.app.Activity
import android.app.AlertDialog

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver

import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.ui.CaptureActivity
import com.example.myapplication.ui.MainActivity
import com.example.myapplication.R
import com.example.myapplication.ui.IdleActivity
import com.example.myapplication.ui.ResultActivity
import com.ingenico.palmvein.helperlibrary.api.PVPHelper
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPPaymentInformation

import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPResponse

//palm lib declaration as Singleton
object palmLib {
    val pvpHelper: PVPHelper by lazy { PVPHelper() }
}

val REG = "^(?!\\b(0)\\1+\\b)(\\+?\\d{1,3}[. -]?)?\\(?\\d{3}\\)?([. -]?)\\d{3}\\3\\d{4}$"

val first_name_list= listOf("John","Jane","Sam","Alice","Bob")
val last_name_list= listOf("Doe","Smith","Johnson","Wilson","Brown")

const val testTimeFinish=false
const val timeFinish:Long=30000
const val timeCaptureFinish:Long=15000

fun randomNumber(len :Int)=('0'..'9').toList().shuffled().take(len).joinToString("")
fun randomName(nameList:List<String>)= nameList.shuffled()[0]
fun String.isPhoneNumber()=this.matches(REG.toRegex())
fun hideKeyboard(context: Context, view: View) {
    val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

//utils functions as extention functions
fun AppCompatActivity.setupActionBar(toolbarId: Int,homeIcon:Int, title: String,titleVisible:Boolean,homeVisible:Boolean) {
    setSupportActionBar(findViewById(toolbarId))
    supportActionBar?.title = title
    supportActionBar?.setDisplayShowTitleEnabled(titleVisible)
    supportActionBar?.setHomeAsUpIndicator(homeIcon)
    supportActionBar?.setDisplayHomeAsUpEnabled(homeVisible)

}


fun showConfirmationDialog(context: Context, title:String,message: String,confirmText:String,cancelText:String, onConfirm: () -> Unit,onCancel:( () -> Unit)?=null) {

    val dialogView: View = LayoutInflater.from(context).inflate(R.layout.confirm_dialog, null)
    dialogView.findViewById<TextView>(R.id.dialog_title).text=title
    dialogView.findViewById<TextView>(R.id.dialog_message).text=message
    val confirmButton=dialogView.findViewById<Button>(R.id.dialog_button_confirm)
    val cancelButton=dialogView.findViewById<Button>(R.id.dialog_button_cancel)

    cancelButton.text=cancelText
    confirmButton.text=confirmText
    // Build the AlertDialog
    val builder = AlertDialog.Builder(context, R.style.ConfirmDialogTheme)
    builder.setView(dialogView)
    val dialog = builder.create()
    dialog.show()
    confirmButton.setOnClickListener { onConfirm.invoke();dialog.dismiss() }
    cancelButton.setOnClickListener { onCancel?.invoke();dialog.dismiss() }

}







object Statics
{
    var userPhone:String=""
    var userFirstName:String=""
    var userLastName:String=""
    var isEnrollement=true
    var isAutoStart=false
    var currentLuanguage="en"

}

//After calling `terminate`, if you wish to continue using the library in your application,
// you must follow it  with a call to the init API to reinitialize the library.
fun AppCompatActivity.terminate1(killApplication: Boolean) {

    println(1)
    palmLib.pvpHelper.terminate { terminateCallback1(it) }
    println(3)
   // if(killApplication){ finishAffinity();return}
    val myintent = Intent(this, MainActivity::class.java);this.startActivity(myintent)
}

private fun AppCompatActivity.terminateCallback1(pvpResponse: PVPResponse) {
    println(2)
    if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
        Log.d("terminateResult", "Library terminated with success")
        runOnUiThread {
            Toast.makeText(applicationContext,"Library terminated with success", Toast.LENGTH_LONG).show()
        }

    } else {
        Log.e("terminateResult", "The library failed to terminate")
        runOnUiThread { Toast.makeText(applicationContext,"The library failed to terminate", Toast.LENGTH_LONG).show()} }
}

fun AppCompatActivity.goToInit() { terminate1(false) }

//At any step during the un-enrollment process The `cancelUnEnrollment` API allows you to initiate the process
//of canceling your un-enrollment in a program.
fun AppCompatActivity.cancelUnEnrollment() { palmLib.pvpHelper.cancelUnEnrollment { cancelCallback(it) } }

//At any step during the enrollment process The `CancelEnrollment` API allows you to initiate the process of canceling
// your enrollment in a program.
fun AppCompatActivity.cancelEnrollment() { palmLib.pvpHelper.cancelEnrollment { cancelCallback(it) } }

fun AppCompatActivity.cancel()= if(Statics.isEnrollement) cancelEnrollment() else cancelUnEnrollment()

 fun AppCompatActivity.cancelCallback(pvpResponse: PVPResponse) {

    if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
        Log.d("cancelResult", "successful cancellation")
        val myintent = Intent(this, ResultActivity::class.java);myintent.putExtra("RESULT",
            ResultActivity.ResultType.Canceled
        )
        this.startActivity(myintent)
        runOnUiThread { Toast.makeText(applicationContext,"Successful Cancellation${pvpResponse.name}", Toast.LENGTH_LONG).show()}
    } else if (pvpResponse.name.startsWith("FATAL")) { goToInit() }
    else { applicationFailure("Failure Cancellation${pvpResponse.name}") }
}


fun AppCompatActivity.applicationFailure(msg:String)
{
    runOnUiThread{Toast.makeText(applicationContext,msg,Toast.LENGTH_LONG).show()}
    goToInit()
}

fun AppCompatActivity.tokenize(paymentInformation: PVPPaymentInformation, cardBin: String?) {
    palmLib.pvpHelper.storePaymentInformation(paymentInformation, cardBin) {storePaymentInformationCallback(it)
    }
}

//After obtaining the `paymentMeansAlias` from your PSP server, you should call the `storePaymentInformation`
// API to save the data returned by the PSP.
 fun AppCompatActivity.storePaymentInformationCallback(pvpResponse: PVPResponse) {
     Log.d("storePaymentInformationResult", "Store payment information ${pvpResponse.name}.")
    if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
        println("storePaymentInformationResult, Store payment information successfully")
        Log.d("storePaymentInformationResult", "Store payment information successfully.")
        runOnUiThread{ Toast.makeText(this," Store payment information Tokenize successfully ${pvpResponse.name}", Toast.LENGTH_SHORT).show()}
       val myintent = Intent(this, CaptureActivity::class.java);this.startActivity(myintent)
    } else if (pvpResponse.name.startsWith("FATAL")) { goToInit() }
     else { applicationFailure("store payment information failure ${pvpResponse.name}")}
}

fun AppCompatActivity.goToTdle()=startActivity(Intent(this, IdleActivity::class.java))











class InactivityTimeout(private val timeoutMillis: Long, private val onTimeout: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { onTimeout() }

    // Démarrer le timer
    fun start() {
        reset()
    }

    // Arrêter le timer
    fun stop() {
        handler.removeCallbacks(timeoutRunnable)
    }

    // Réinitialiser le timer
    fun reset() {
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, timeoutMillis)
    }

    // Attacher l'écouteur pour détecter les touches
    fun attachToView(view: View, vararg views: View?) {
        // Écouteur global (écran entier)
        view.setOnTouchListener { _, _ ->
            reset()
         //   Toast.makeText(view.context, "resetTouch", Toast.LENGTH_SHORT).show()
            false
        }

        // Écouteurs spécifiques sur les vues fournies
        views.forEach { v ->
            v?.setOnTouchListener { _, _ ->
                reset()
               // Toast.makeText(view.context, "resetSpecificTouch", Toast.LENGTH_SHORT).show()
                false
            }

        }
    }




}

// Extension function pour simplifier l'utilisation dans AppCompatActivity
fun AppCompatActivity.setupInactivityTimeout(
    timeout: Long,
    resetOnTouch: Boolean = true,
    /*view1: View?=null*/vararg views: View?,
    onTimeout: () -> Unit
) {
    val inactivityTimeout = InactivityTimeout(timeout, onTimeout)

    if (resetOnTouch) {
        val rootView = findViewById<View>(android.R.id.content)

        //inactivityTimeout.attachToView(rootView,view1)

        inactivityTimeout.attachToView(rootView,*views)


    }



    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
         // Toast.makeText(this@setupInactivityTimeout,"start",Toast.LENGTH_SHORT).show()

            inactivityTimeout.start()
        }

        override fun onPause(owner: LifecycleOwner) {
         //  Toast.makeText(this@setupInactivityTimeout,"stop",Toast.LENGTH_SHORT).show()
            inactivityTimeout.stop()
        }


    })


}


fun Context.saveAutoStart(prefsName:String="Auto",key:String="autoStart",value:Boolean)
{getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putBoolean(key,value).apply()}



fun Context.getAutoStart(prefsName:String="Auto",key:String="autoStart",defaultValue:Boolean=false):Boolean
{
   return  getSharedPreferences(prefsName, Context.MODE_PRIVATE).getBoolean(key,defaultValue)
}



val maplanguages= mapOf("en" to R.drawable.en,"fr" to R.drawable.fr,"es" to R.drawable.es)