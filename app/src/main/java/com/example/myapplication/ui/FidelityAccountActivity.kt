package com.example.myapplication.ui


import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utilities.cancel
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.showConfirmationDialog
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish
import com.example.myapplication.utilities.tokenize
import com.google.android.material.textfield.TextInputEditText
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPPaymentInformation

import java.util.concurrent.atomic.AtomicBoolean

class FidelityAccountActivity : AppCompatActivity() {
    private val cancellationPossible = AtomicBoolean(true)
    lateinit var errorTextView: TextView
    lateinit var secretCodeInput:TextInputEditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fidelity_account)
        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)
         secretCodeInput = findViewById(R.id.fidelity_input)
        val sendButton: Button = findViewById(R.id.fidelity_next)
        errorTextView = findViewById(R.id.fidelity_error)
        sendButton.setOnClickListener {handleClick()}
        if(testTimeFinish){setupInactivityTimeout(timeFinish, resetOnTouch = true,secretCodeInput){cancel()}}
    }

    fun handleClick()
    {
        if (secretCodeInput.text.toString().isEmpty()) {
            errorTextView.setText(R.string.fidelity_fill_field)
            return }
        if ( cancellationPossible.compareAndSet(true, false))
      { errorTextView.text = ""
          tokenize(PVPPaymentInformation(secretCodeInput.text.toString(), TokenizationMethodActivity.TokenizationMethod.FIDELITY_ACCOUNT.name), "")
      }
    }


    fun confirmPressed()
    { errorTextView.setText(R.string.dialog_message_cancellation_ongoing)
        cancel()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {android.R.id.home->{ showConfirmationDialog(this,getString(R.string.dialog_title_cancel), getString(
            R.string.dialog_message_cancel
        ),"Confirm","Cancel",{confirmPressed()}) } }
        return super.onOptionsItemSelected(item)
    }

}