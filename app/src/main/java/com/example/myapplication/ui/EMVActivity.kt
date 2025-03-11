package com.example.myapplication.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.emv.CardOption
import com.example.myapplication.emv.DeviceHelper
import com.example.myapplication.emv.EmvConfig
import com.example.myapplication.utilities.cancel
import com.example.myapplication.utilities.goToInit
import com.example.myapplication.utilities.goToTdle
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.showConfirmationDialog
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeFinish
import com.example.myapplication.utilities.tokenize
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPPaymentInformation
import com.usdk.apiservice.aidl.beeper.UBeeper
import com.usdk.apiservice.aidl.emv.CardRecord
import com.usdk.apiservice.aidl.emv.SearchCardListener
import com.usdk.apiservice.aidl.emv.UEMV
import java.util.concurrent.atomic.AtomicBoolean

class EMVActivity : AppCompatActivity() {

    lateinit var errorTextView: TextView
    val STUBBED_TOKEN_ID = "STUBBED_TOKEN_ID"
    val STUBBED_CARD_BIN = "STUBBED_CARD_BIN"
    private val cardOption: CardOption = CardOption.create()
    private val cancellationPossible = AtomicBoolean(true)
    lateinit var cardBin: String
    lateinit var tapCardView: View
    lateinit var retryButton: Button
    lateinit var progressBar: ProgressBar
    lateinit var  emv: UEMV
    lateinit var  beeper: UBeeper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emvactivity)
        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)
        errorTextView=findViewById(R.id.emv_error)
        tapCardView = findViewById(R.id.emv_tap_view)
        retryButton = findViewById(R.id.emv_retry)
        progressBar = findViewById(R.id.emv_progress_bar)


        try{ emv= DeviceHelper.me().eMV} catch (e:Exception)
        { Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show();goToInit()
            return }

        try{ beeper= DeviceHelper.me().beeper}
        catch (e:Exception) { Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show();goToInit()
            return }


        retryButton.setOnClickListener { showTapCardView()
            startSearch() }

        initCardOption()
        startSearch()

        if(testTimeFinish){ setupInactivityTimeout(timeFinish, resetOnTouch = true,  retryButton) {confirmPressed() }}
    }




    override fun onDestroy() {
        stopSearch()
        super.onDestroy()
    }

    private fun initCardOption() {
        cardOption.supportAllRFCardTypes(true)
        cardOption.supportMagCard(true)
        cardOption.supportRFCard(true)
        cardOption.supportICCard(true)
        cardOption.loopSearchRFCard(false)
        cardOption.rfDeviceName(EmvConfig.RF_DEVICE_NAME)
        cardOption.trackCheckEnabled(false)
    }

    private fun handleException(e: java.lang.Exception) {
        e.printStackTrace()
        beepWhenError()
        showRetryLayout(e.message)
    }

    private fun showTapCardView() {
        runOnUiThread {
            errorTextView.setText("")
            progressBar.visibility = View.INVISIBLE
            tapCardView.visibility = View.VISIBLE
            retryButton.visibility = View.INVISIBLE
        }
    }

    private fun showRetryLayout(error: String?) {
        runOnUiThread {
            progressBar.visibility = View.INVISIBLE
            errorTextView.text = error
            retryButton.visibility = View.VISIBLE
            tapCardView.visibility = View.INVISIBLE
        }
    }

    fun startSearch() {
        try {
            emv.searchCard(
                cardOption.toBundle(),
                EmvConfig.CARD_READ_TIMEOUT_SEC,
                object : SearchCardListener.Stub() {
                    override fun onCardPass(cardType: Int) {
                        if (cancellationPossible.compareAndSet(true, false)) {

                            startEMV()

                        }
                    }

                    override fun onCardInsert() {
                        if (cancellationPossible.compareAndSet(true, false)) {
                            startEMV()
                        }
                    }

                    override fun onCardSwiped(track: Bundle) {
                        if (cancellationPossible.compareAndSet(true, false)) {
                            //TODO retrieve card data from track
                            beepWhenNormal()

                            // In this code, the card BIN is stubbed
                            cardBin = STUBBED_CARD_BIN
                            stopSearch()
                            tokenizeCard()
                        }
                    }

                    override fun onTimeout() {
                        beepWhenError()
                        stopSearch()
                        showRetryLayout("onTimeout")
                    }

                    override fun onError(code: Int, message: String) {
                        beepWhenError()
                        stopSearch()
                        showRetryLayout(String.format("=> onError | %s[0x%02X]", message, code))
                    }
                })
        } catch (e: java.lang.Exception) {
            handleException(e)
        }
    }

    private fun stopSearch() {
        try {
            emv.stopSearch()
            emv.halt()
        } catch (e: java.lang.Exception) {
            handleException(e)
        }
    }

    private fun startEMV( /* TODO add correctly instantiated EMVOption */) {
        runOnUiThread { progressBar.visibility = View.VISIBLE }
        //TODO Here should be that starts the EMV and retrieve the cardRecord
        // In this code, this step is stubbed with an empty cardRecord object.
        val cardRecord = CardRecord()
        readEMVCardData(cardRecord)
    }

    fun readEMVCardData(record: CardRecord?) {
        //TODO Here should be the code extract required data from the cardRecord
        beepWhenNormal()
        // In this code, the card BIN is stubbed
        cardBin = STUBBED_CARD_BIN
        stopSearch()
        tokenizeCard()
    }

    private fun tokenizeCard( /* TODO add required card data */) {
        // The required card data depends on the chosen tokenization service.
        //TODO If another data is needed - e.g., the CVV - add those steps here

        //TODO Here should be the code to contact your tokenization service
        // The tokenization service is to use card data to produce a token ID
        // In this code, this step is stubbed with a hardcoded token ID
        val tokenId: String = STUBBED_TOKEN_ID
        returnTokenInfo(tokenId)
    }

    private fun returnTokenInfo(tokenId: String) {
        tokenize(
            PVPPaymentInformation(
                tokenId, TokenizationMethodActivity.TokenizationMethod.EMV_CARD_TOKENIZATION.name
            ), cardBin
        )
    }

    fun beepWhenNormal() {
        try {
            beeper.startBeep(500)
        } catch (e: java.lang.Exception) {
            handleException(e)
        }
    }

    fun beepWhenError() {
        try {
            beeper.startBeep(1000)
        } catch (e: java.lang.Exception) {
            handleException(e)
        }
    }


    fun confirmPressed()
    { errorTextView.setText(R.string.dialog_message_cancellation_ongoing)
        cancel()
    }
    fun homePressed ()
    {
        if (cancellationPossible.compareAndSet(true, false)) {
            stopSearch()
            cancellationPossible.set(true)
            showRetryLayout("Canceled")
            showConfirmationDialog(this,getString(R.string.dialog_title_cancel), getString(R.string.dialog_message_cancel),"Confirm","Cancel",{confirmPressed()})

        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {android.R.id.home->{ homePressed() } }
        return super.onOptionsItemSelected(item)
    }
}