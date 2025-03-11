package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.example.myapplication.R
import com.example.myapplication.utilities.cancel
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.showConfirmationDialog
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish


//you need to interact with your Payment Service Provider (PSP) server to tokenize the user's card. For demonstration purposes, three demo payment means are implemented in this example:
/*- **EMV:** Represents an EMV payment method for demonstration.
- **Fake Tokenization:** Represents a fake tokenization method for testing.
- **Fidelity Tokenization:** Represents a fidelity tokenization method for demonstration.*/



class TokenizationMethodActivity : AppCompatActivity() {
    lateinit var radioTokenMeth: RadioGroup
    lateinit var errorTextView: TextView
    lateinit var confirm:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tokenization_method)
        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)
        radioTokenMeth=findViewById(R.id.tokenization_method_radio_group)
        errorTextView=findViewById(R.id.tokenization_method_error)
        confirm=findViewById(R.id.tokenization_method_next)
        radioTokenMeth.setOnCheckedChangeListener { _: RadioGroup?, _: Int ->errorTextView.text = "" }
        confirm.setOnClickListener {  handleChoice()  }
        if(testTimeFinish){ setupInactivityTimeout(timeFinish, resetOnTouch = true,radioTokenMeth[0],radioTokenMeth[1]){cancel()}}
    }
    fun handleChoice(){
        when (radioTokenMeth.checkedRadioButtonId) {
            R.id.tokenization_method_radio_button_fidelity -> {val myintent = Intent(this, FidelityAccountActivity::class.java);this.startActivity(myintent)}
            R.id.tokenization_method_radio_button_emv_card -> {val myintent = Intent(this, EMVActivity::class.java);this.startActivity(myintent)}
            else -> errorTextView.setText(R.string.toast_select_tokenization_method)
        }
    }


    enum class TokenizationMethod { EMV_CARD_TOKENIZATION, FIDELITY_ACCOUNT }

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