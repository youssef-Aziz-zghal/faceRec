package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utilities.Statics
import com.example.myapplication.utilities.applicationFailure
import com.example.myapplication.utilities.first_name_list
import com.example.myapplication.utilities.goToInit
import com.example.myapplication.utilities.goToTdle
import com.example.myapplication.utilities.isPhoneNumber
import com.example.myapplication.utilities.last_name_list
import com.example.myapplication.utilities.palmLib
import com.example.myapplication.utilities.randomName
import com.example.myapplication.utilities.randomNumber
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPCustomerIdentityInformation
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPCustomerInformation
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPResponse

class PersonalDataInputActivity : AppCompatActivity() {
    val DEV_MAGIC_NUMBER_PREFIX="+0"
    lateinit var skipButton:Button
    lateinit var errorTextView:TextView
    lateinit var confirmButton: Button
    lateinit var phoneInput:TextInputEditText
    lateinit var firstNameInput:TextInputEditText
    lateinit var firstNameInputLayout:TextInputLayout
    lateinit var lastNameInput:TextInputEditText
    lateinit var lastNameInputLayout:TextInputLayout
    lateinit var progressBar:ProgressBar
    lateinit var phoneInputLayout:TextInputLayout
    var isSkip=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_data_input)
        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)
        skipButton=findViewById(R.id.data_input_skip)
        errorTextView=findViewById(R.id.data_input_error)
        confirmButton=findViewById(R.id.data_input_next)
        phoneInput = findViewById(R.id.data_input_phone_number_input)
        firstNameInput=findViewById(R.id.data_input_first_name_input)
        firstNameInputLayout=findViewById(R.id.data_input_first_name_input_layout)
        lastNameInput=findViewById(R.id.data_input_last_name_input)
        lastNameInputLayout=findViewById(R.id.data_input_last_name_input_layout)
        progressBar = findViewById(R.id.data_input_progress_bar)
        phoneInputLayout = findViewById(R.id.data_input_phone_number_input_layout)

       // setupInactivityTimeout(120000, view1 = confirmButton) { goToTdle()}
        if(testTimeFinish) {
            setupInactivityTimeout(timeFinish, resetOnTouch = true, confirmButton, phoneInput, firstNameInput, lastNameInput, skipButton)
            { goToTdle() }
        }
        if(Statics.isEnrollement){
        Statics.userPhone="$DEV_MAGIC_NUMBER_PREFIX${randomNumber(7)}"
        Statics.userFirstName= randomName(first_name_list)
        Statics.userLastName= randomName(last_name_list)
        }
        else{
            findViewById<TextView>(R.id.data_input_step).setText(R.string.un_enroll_step)
            if (Statics.userPhone.isEmpty()) {
                skipButton.visibility = View.GONE
            }
        }

        skipButton.setOnClickListener { skipButton.visibility=View.GONE;isSkip=true;phoneInput.isEnabled = false; phoneInput.setText(
            Statics.userPhone)}
        confirmButton.setOnClickListener {handleClick()}

    }



    fun handleClick()
    {
        if(!(Statics.isEnrollement))
        {
                sendOTPForUnEnrollment()
            return
        }

        errorTextView.text=""
        if(isSkip) { firstNameInput.setText(Statics.userFirstName) ;lastNameInput.setText(Statics.userLastName) }

         val phoneNbr=phoneInput.text.toString();val firstName=firstNameInput.text.toString();val lastName=lastNameInput.text.toString()

        if (!(phoneNbr.startsWith(DEV_MAGIC_NUMBER_PREFIX) ||phoneNbr.isPhoneNumber())) {
            errorTextView.setText(R.string.data_input_error_phone_number_invalid)
            return
        }

        if(firstNameInputLayout.visibility==View.INVISIBLE) {
            firstNameInputLayout.visibility = View.VISIBLE

        }else if(lastNameInputLayout.visibility==View.INVISIBLE)
        {
            if(firstName.isEmpty()){
                errorTextView.setText(R.string.error_field_not_filled)
                return
            }

            lastNameInputLayout.visibility = View.VISIBLE
        }else
        {
            if(lastName.isEmpty()){
                errorTextView.setText(R.string.error_field_not_filled)
                return
            }
            sendOTPGenerationRequest()

        }
    }

//use the `sendOTPForUnEnrollment` to send OTP to the customer's phone
    private fun sendOTPForUnEnrollment() {
        val phoneNbr = phoneInput.text.toString()
        if (phoneNbr.trim { it <= ' ' }.isEmpty()) {
            errorTextView.setText(R.string.error_field_not_filled)
            return
        }
        if (!(phoneNbr.startsWith(DEV_MAGIC_NUMBER_PREFIX) ||phoneNbr.isPhoneNumber())) {
            errorTextView.setText(R.string.data_input_error_phone_number_invalid)
            return
        }

        confirmButton.setEnabled(false)
        Statics.userPhone=(phoneInput.text.toString())
        progressBar.visibility = View.VISIBLE
        palmLib.pvpHelper.sendOTPForUnEnrollment(phoneInput.text.toString()) { sendOTPForUnEnrollmentCallback(it) }

    }

    private fun sendOTPForUnEnrollmentCallback(pvpResponse: PVPResponse) {
        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            val myintent = Intent(this, ValidateOtpActivity::class.java);myintent.putExtra("skip",isSkip);this.startActivity(myintent)
        } else if (pvpResponse.name.startsWith("FATAL")) {
            goToInit()
        } else if (pvpResponse == PVPResponse.PVP_RESPONSE_PHONE_NUMBER_UNKNOWN) {
            runOnUiThread {
                phoneInputLayout.setVisibility(View.VISIBLE)
                firstNameInputLayout.visibility = View.GONE
                lastNameInputLayout.visibility = View.GONE
                confirmButton.setEnabled(true)
                progressBar.visibility = View.INVISIBLE
                errorTextView.setText(R.string.error_unknown_phone_number)
            }
        } else {
            applicationFailure("sendOTPForUnEnrollmentCallback${pvpResponse.name}")
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {android.R.id.home->{ val myintent = Intent(this, IdleActivity::class.java);this.startActivity(myintent) }
        }
        return super.onOptionsItemSelected(item)
    }

    //Call the createAccount method to start the account creation process.
    private fun sendOTPGenerationRequest() {
        confirmButton.setEnabled(false)
        progressBar.setVisibility(View.VISIBLE)
        Statics.userLastName=lastNameInput.text.toString();Statics.userFirstName=firstNameInput.text.toString()
        Statics.userPhone=phoneInput.text.toString()
        palmLib.pvpHelper.createAccount(PVPCustomerInformation(PVPCustomerIdentityInformation(
            Statics.userLastName, Statics.userFirstName), Statics.userPhone)) { createAccountCallback(it) }
    }

    private fun createAccountCallback(pvpResponse: PVPResponse) {

        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            val myintent = Intent(this, ValidateOtpActivity::class.java);myintent.putExtra("skip",isSkip);this.startActivity(myintent)
            runOnUiThread {  Toast.makeText(this,"create Account ${pvpResponse.name},${Statics.userPhone},${Statics.userLastName} ",Toast.LENGTH_SHORT).show()}
        } else if (pvpResponse.name.startsWith("FATAL")) {
           goToInit()
        } else if (pvpResponse == PVPResponse.PVP_RESPONSE_PHONE_NUMBER_ALREADY_ASSOCIATED) {
            runOnUiThread {
                progressBar.visibility = View.INVISIBLE
                errorTextView.setText(R.string.error_phone_number_already_associated)
                phoneInputLayout.visibility = View.VISIBLE
                firstNameInputLayout.visibility = View.GONE
                lastNameInputLayout.visibility = View.GONE
                confirmButton.isEnabled=true
            }
        } else {applicationFailure("Failure ${pvpResponse.name}")
        }
    }




}