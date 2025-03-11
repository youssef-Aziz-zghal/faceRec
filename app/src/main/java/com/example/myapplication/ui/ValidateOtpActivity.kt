package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
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
import com.example.myapplication.utilities.cancel
import com.example.myapplication.utilities.cancelUnEnrollment
import com.example.myapplication.utilities.goToInit
import com.example.myapplication.utilities.hideKeyboard
import com.example.myapplication.utilities.palmLib
import com.example.myapplication.utilities.randomNumber
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.showConfirmationDialog
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPIdentificationResult
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPResponse
import `in`.aabhasjindal.otptextview.OTPListener
import `in`.aabhasjindal.otptextview.OtpTextView
import java.util.concurrent.atomic.AtomicBoolean

class ValidateOtpActivity : AppCompatActivity() {
     val RESEND_OTP_TIMEOUT_SECONDS = 100L
     val cancellationPossible = AtomicBoolean(true)
    lateinit var countDownTimer: CountDownTimer
    lateinit var resendButton: Button
    lateinit var otpInput: OtpTextView
    lateinit var otpError: TextView
    lateinit var progressBar: ProgressBar
    lateinit var phoneNumberView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validate_otp)
        phoneNumberView=findViewById(R.id.validate_otp_phone_number)
        otpInput = findViewById(R.id.validate_otp_view)
        resendButton = findViewById(R.id.validate_otp_resend)
        progressBar = findViewById(R.id.validate_otp_progress_bar)
        otpError = findViewById(R.id.validate_otp_error)
        resendButton.isEnabled = false
        phoneNumberView.text= Statics.userPhone

        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)
        if(testTimeFinish){ setupInactivityTimeout(timeFinish, resetOnTouch = true,resendButton,otpInput){cancel()}}
        countDownTimer = object : CountDownTimer(RESEND_OTP_TIMEOUT_SECONDS * 1000, 500) {
            override fun onTick(millisUntilFinished: Long) {
                resendButton.text = getString(R.string.validate_otp_resend_otp_unavailable,millisUntilFinished / 1000)
            }
            override fun onFinish() {
                resendButton.run { setText(R.string.validate_otp_resend_otp_available);isEnabled=true }
            }
        }
        countDownTimer.start()
        resendButton.setOnClickListener {
            countDownTimer.start()
            resendButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            if (Statics.isEnrollement) { resendOTP() } else { resendOTPtoUnEnroll() }
        }


        otpInput.otpListener = object : OTPListener {
            override fun onInteractionListener() {
                // fired when user types something in the Otpbox
                otpError.visibility = View.INVISIBLE
            }

            override fun onOTPComplete(otp: String) {
                // fired when user has entered the OTP fully.
               hideKeyboard(applicationContext,requireViewById(R.id.validate_otp_view))
                if(Statics.isEnrollement) validateOTP() else validateOTPtoUnEnroll()
            }
        }



    }


   // use the `validateOTPForUnEnrollment` API to validate the OTP received.
    private fun validateOTPtoUnEnroll() {
        val OTP = otpInput.otp.trim { it <= ' ' }
        if (OTP.isEmpty()) {
            otpError.setText(R.string.error_field_not_filled)
        } else {
            if (cancellationPossible.compareAndSet(true, false)) {
                progressBar.visibility = View.VISIBLE
                // Call the validateOTPForUnEnrollment method to check if the OTP is valid.
                palmLib.pvpHelper.validateOTPForUnEnrollment(OTP) { validateOTPForUnEnrollmentCallback(it) }
            }
        }
    }

   // once the OTP is validated, gather customer information, and ask the user if he confirm the un-enrollment
    private fun validateOTPForUnEnrollmentCallback(pvpIdentificationResult: PVPIdentificationResult) {
        if (pvpIdentificationResult.status == PVPResponse.PVP_RESPONSE_SUCCESS) {
            runOnUiThread { progressBar.visibility = View.INVISIBLE }
            showAlertToConfirmUnEnroll(pvpIdentificationResult.customerIdentityInformation.firstName, pvpIdentificationResult.customerIdentityInformation.lastName
            )
        } else if (pvpIdentificationResult.status.name.startsWith("FATAL")) {
           goToInit()
        } else if (pvpIdentificationResult.status == PVPResponse.PVP_RESPONSE_WRONG_OTP) {
            runOnUiThread {
                progressBar.visibility = View.INVISIBLE
                cancellationPossible.set(true)
                otpError.visibility = View.VISIBLE
            }
        } else { applicationFailure("failure validate otp unenrollement ${pvpIdentificationResult.status.name}") }
    }




    //Gather additional details and display confirmation message to the user
    private fun showAlertToConfirmUnEnroll(userFirstName: String, userLastName: String) {
        val userName = "$userFirstName $userLastName"
        runOnUiThread {  showConfirmationDialog(this,getString(R.string.dialog_title_confirm_un_enroll),getString(
            R.string.dialog_message_confirm_un_enroll, userName),
            "Confirm","Cancel", { confirmUnEnroll() }, {
                progressBar.visibility = View.VISIBLE ;cancelUnEnrollment()})
        }

    }

    // if he confirms the un-enrollment then call the `confirmUnEnrollment` method
    private fun confirmUnEnroll() {
        progressBar.visibility = View.VISIBLE
        palmLib.pvpHelper.confirmUnEnrollment { confirmUnEnrollCallback(it) }
    }

    private fun confirmUnEnrollCallback(pvpResponse: PVPResponse) {

        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Success)
            this.startActivity(myintent)

        } else if (pvpResponse.name.startsWith("FATAL")) {
            goToInit()
        } else {
           applicationFailure("Failure ConfirmUnEnrollCallback ${pvpResponse.name}")
        }
    }


    override fun onStart() {
        if(intent.getBooleanExtra("skip",false)) {otpInput.otp= randomNumber(6)
        }
        super.onStart()
    }

    private fun validateOTP() {
        val OTP = otpInput.otp.trim { it <= ' ' }
        if (OTP.isEmpty()) {
            otpError.setText(R.string.error_field_not_filled)
        } else {
            if (cancellationPossible.compareAndSet(true, false)) {
                progressBar.visibility = View.VISIBLE

                palmLib.pvpHelper.validateOTPForEnrollment(OTP) { validateOTPCallback(it) }
            }
        }
    }

    private fun validateOTPCallback(pvpResponse: PVPResponse) {
        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            runOnUiThread { Toast.makeText(this, "validate otp${pvpResponse.name}", Toast.LENGTH_SHORT).show() }
           val myintent = Intent(this, TokenizationMethodActivity::class.java);this.startActivity(myintent)

        } else if (pvpResponse.name.startsWith("FATAL")) {
            runOnUiThread {  Toast.makeText(this,pvpResponse.name,Toast.LENGTH_SHORT).show(); }
        } else if (pvpResponse == PVPResponse.PVP_RESPONSE_WRONG_OTP) {
            runOnUiThread {
                progressBar.visibility = View.INVISIBLE
                cancellationPossible.set(true)
                otpError.visibility = View.VISIBLE
                otpError.text=pvpResponse.name
                Toast.makeText(this,pvpResponse.name,Toast.LENGTH_SHORT).show();
            }
        } else {
           applicationFailure("failure ${pvpResponse.name}")
        }
    }



    fun confirmPressed()
    { otpError.run { visibility=View.VISIBLE;setText(R.string.dialog_message_cancellation_ongoing) }
        cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {android.R.id.home->{ showConfirmationDialog(this,getString(R.string.dialog_title_cancel), getString(
            R.string.dialog_message_cancel
        ),"Confirm","Cancel",{confirmPressed()}) } }
        return super.onOptionsItemSelected(item)
    }


    private fun resendOTP() { palmLib.pvpHelper.resendOTPForEnrollment { resendOTPCallback(it) } }

    private fun resendOTPCallback(pvpResponse: PVPResponse) {
        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {

            Log.d("resendOTpResult", "OTP sent successfully")
            runOnUiThread {
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(applicationContext, getString(R.string.toast_send_otp_success), Toast.LENGTH_SHORT).show()
            }
        } else if (pvpResponse.name.startsWith("FATAL")) {
            goToInit()
        } else { applicationFailure("resendOTPCallback ${pvpResponse.name}") }
    }

    private fun resendOTPtoUnEnroll() { palmLib.pvpHelper.resendOTPForUnEnrollment { resendOTPForUnEnrollCallback(it) } }

    private fun resendOTPForUnEnrollCallback(pvpResponse: PVPResponse) {
        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            Log.d("resendOTPForUnEnrollResult", "OTP sent successfully")
            runOnUiThread { progressBar.visibility = View.INVISIBLE
                Toast.makeText(this, "resendOTPForUnEnrollResult ${getString(R.string.toast_send_otp_success)}", Toast.LENGTH_SHORT).show()
            }
        } else if (pvpResponse.name.startsWith("FATAL")) { goToInit() }
        else { applicationFailure("resendOTPForUnEnrollCallback ${pvpResponse.name}") }
    }

    override fun onDestroy() {
        countDownTimer.cancel()
        super.onDestroy()
    }

}