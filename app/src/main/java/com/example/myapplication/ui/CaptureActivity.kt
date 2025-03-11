package com.example.myapplication.ui

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.myapplication.R
import com.example.myapplication.utilities.Statics
import com.example.myapplication.utilities.applicationFailure
import com.example.myapplication.utilities.cancel
import com.example.myapplication.utilities.cancelEnrollment
import com.example.myapplication.utilities.cancelUnEnrollment
import com.example.myapplication.utilities.goToInit
import com.example.myapplication.utilities.palmLib
import com.example.myapplication.utilities.setupActionBar
import com.example.myapplication.utilities.setupInactivityTimeout
import com.example.myapplication.utilities.showConfirmationDialog
import com.example.myapplication.utilities.testTimeFinish
import com.example.myapplication.utilities.timeCaptureFinish
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPGuidanceMessage
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPIdentificationResult
import com.ingenico.palmvein.helperlibrary.api.exposedInterface.PVPResponse

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class CaptureActivity : AppCompatActivity() {

     val UI_MESSAGE_BUFFER_TIME = 500L
     val SUCCESS_SCREEN_DURATION_MS = 2000L
     val TRANSITION_DURATION = 600L
     val pvpGuidanceToUIMessage: Map<PVPGuidanceMessage, Int> =
        object : HashMap<PVPGuidanceMessage, Int>() {
            init {
                put(PVPGuidanceMessage.PVP_GUIDANCE_START, R.string.guidance_place_hand)
                put(PVPGuidanceMessage.PVP_GUIDANCE_PLACE_HAND, R.string.guidance_place_hand)
                put(PVPGuidanceMessage.PVP_GUIDANCE_REMOVE_HAND, R.string.guidance_remove_hand)
                put(PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_DOWN, R.string.guidance_hand_down)
                put(PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_UP, R.string.guidance_hand_up)
                put(PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_LEFT, R.string.guidance_hand_left)
                put(PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_RIGHT, R.string.guidance_hand_right)
                put(
                    PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_FURTHER,
                    R.string.guidance_hand_further
                )
                put(PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_CLOSER, R.string.guidance_hand_closer)
                put(PVPGuidanceMessage.PVP_GUIDANCE_FLATTEN_HAND, R.string.guidance_flatten_hand)
                put(PVPGuidanceMessage.PVP_GUIDANCE_DONT_MOVE, R.string.guidance_dont_move)
                put(PVPGuidanceMessage.PVP_GUIDANCE_COMPLETE, R.string.guidance_complete)
            }
        }
     val pvpGuidanceWithStaticPrefix = listOf(PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_DOWN, PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_UP,
        PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_LEFT, PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_RIGHT,
        PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_FURTHER, PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_CLOSER, PVPGuidanceMessage.PVP_GUIDANCE_FLATTEN_HAND
    )
     val constraintSet = ConstraintSet()
     val shouldDisplayPostSuccessMessage = AtomicBoolean()
    val guidanceDisplayMutex = ReentrantLock()
    private val cancellationPossible = AtomicBoolean(true)
    private var guidanceTextview: TextView? = null
    private var guidanceStaticTextview: TextView? = null
    private var workMessageTextview: TextView? = null
    private var defaultHandView: ImageView? = null
    private var moveLeftView: ImageView? = null
    private var moveRightView: ImageView? = null
    private var moveAwayView: ImageView? = null
    private var moveUpView: ImageView? = null
    private var moveCloserView: ImageView? = null
    private var moveDownView: ImageView? = null
    private var placeHandView: ImageView? = null
    private var confirmView: ImageView? = null
    private var hourglassView: ImageView? = null
    private var relativeLayout: RelativeLayout? = null
    private var progressBarLayout: ConstraintLayout? = null
    private var bufferedMessagesThread = Thread {}
    private var lastUIMessageTime = -1000L
    private var currentStep = 0
    private var allImageViews: ArrayList<View?>? = null
    private var circleViews: ArrayList<ImageView>? = null
    private var currentSuccessThread: Thread? = null
    var timefinished:Boolean=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        setupActionBar(R.id.toolbar, R.drawable.ic_close,"test",false,true)
        initControls()

        if(testTimeFinish){ setupInactivityTimeout(timeCaptureFinish){timefinished=true;onCancele() }}

       // Toast.makeText(this,resources.configuration.locales.get(0).language,Toast.LENGTH_SHORT).show()



    }










    fun initControls() {
        guidanceTextview = findViewById(R.id.guidanceDynamic)
        guidanceStaticTextview = findViewById(R.id.guidanceStatic)
        workMessageTextview = findViewById(R.id.WorkMessage)
        defaultHandView = findViewById(R.id.defautHandView)
        moveLeftView = findViewById(R.id.moveLeftView)
        moveRightView = findViewById(R.id.moveRightView)
        moveAwayView = findViewById(R.id.moveAwayView)
        moveCloserView = findViewById(R.id.moveCloserView)
        moveDownView = findViewById(R.id.moveDownView)
        progressBarLayout = findViewById(R.id.progressBarsWrapperLayout)
        val progressBarGeneralLayout = findViewById<View>(R.id.progressBarLayout)
        moveUpView = findViewById(R.id.moveUpView)
        placeHandView = findViewById(R.id.placeHandView)
        confirmView = findViewById(R.id.dont_move_View)
        relativeLayout = findViewById(R.id.rl)
        hourglassView = findViewById(R.id.hourglass_view)
        constraintSet.clone(progressBarLayout)
        allImageViews = ArrayList(listOf(moveCloserView, moveDownView, moveUpView, moveLeftView, moveRightView, moveAwayView, defaultHandView,
                placeHandView, confirmView, hourglassView))
        circleViews = ArrayList(listOf(findViewById(R.id.circle_1), findViewById(R.id.circle_2), findViewById(
            R.id.circle_3
        ),
            findViewById(R.id.circle_4)))
        setProgressBar(0, 0)
        currentStep = 0
        if (Statics.isEnrollement) {
            enroll()
        } else {
            progressBarGeneralLayout.visibility = View.INVISIBLE
            identifyForUnEnrollment()
        }
    }

    // Authenticate using your palm vein for quick and secure un-enrollment
    private fun identifyForUnEnrollment() {
        //Authenticate using your palm vein for quick and secure un-enrollment
      Toast.makeText(this,"identify for unenrollement",Toast.LENGTH_SHORT).show()
        palmLib.pvpHelper.identifyForUnEnrollment({  guiCallback(it) }) { identifyForUnEnrollCallback(it) }
    }


    private fun identifyForUnEnrollCallback(identificationResult: PVPIdentificationResult) {
        shouldDisplayPostSuccessMessage.set(false)
        if (identificationResult.status == PVPResponse.PVP_RESPONSE_SUCCESS) {
            showDialogToConfirmUnEnroll(identificationResult.customerIdentityInformation.firstName, identificationResult.customerIdentityInformation.lastName)
        } else if (identificationResult.status.name.startsWith("FATAL")) {
            goToInit()
        } else if (identificationResult.status == PVPResponse.PVP_RESPONSE_CANCELED) {

            val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Canceled)
            this.startActivity(myintent)

        } else if (identificationResult.status == PVPResponse.PVP_RESPONSE_IDENTIFICATION_SUBJECT_NOT_FOUND) {

            val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Unrecognized)
            this.startActivity(myintent)

        } else {
           applicationFailure("Failure identifyForUnEnrollCallback ${identificationResult.status.name}")
        }
    }

    private fun showDialogToConfirmUnEnroll(userFirstName: String, userLastName: String) {
        val userName = "$userFirstName $userLastName"
        runOnUiThread {  showConfirmationDialog(this,getString(R.string.dialog_title_confirm_un_enroll),getString(
            R.string.dialog_message_confirm_un_enroll, userName),
            "Confirm","Cancel", { confirmUnEnroll() }, {cancelUnEnrollment()})
        }
        runOnUiThread { guidanceTextview!!.setText(R.string.capture_guidance_un_enroll_processing) }
    }

    //after a successful identification  Call the confirmUnEnrollment method.
    private fun confirmUnEnroll() {
        //Call the confirmUnEnrollment method to confirm un-enroll.
    palmLib.pvpHelper.confirmUnEnrollment { confirmUnEnrollCallback(it) }
    }

    private fun confirmUnEnrollCallback(pvpResponse: PVPResponse) {

        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
           /* val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Success)
            this.startActivity(myintent)*/

            val myintent = Intent(this, FacerecActivity::class.java)
            this.startActivity(myintent)

        } else if (pvpResponse.name.startsWith("FATAL")) {
            goToInit()
        } else {
           applicationFailure("Failure ConfirmUnEnrollCallback${pvpResponse.name}")
        }
    }

    /* The `enroll` API is used to enroll the user's palm vein pattern. It returns two callbacks:

    - **enrollCallback:** This callback is invoked to provide the result of the enrollment, indicating whether the enrollment was successful or failed.

    - **guiCallback:** This callback is used to return guidance messages during the palm vein template acquisition process. It provides messages to guide the user through the enrollment process.
*/

    private fun enroll() {
        currentStep = 0
        setProgressBar(0, 0)
        //Call the enroll method to initiate the  enrollment process.
        palmLib.pvpHelper.enroll({ guiCallback(it) }) { enrollCallback(it) }
    }

    @Synchronized
    private fun guiCallback(uiMessage: PVPGuidanceMessage) {
        try {
            guidanceDisplayMutex.lock()
            runOnUiThread {
                if (bufferedMessagesThread.isAlive) {
                    bufferedMessagesThread.interrupt()
                }
                if (System.currentTimeMillis() - lastUIMessageTime >= UI_MESSAGE_BUFFER_TIME || uiMessage == PVPGuidanceMessage.PVP_GUIDANCE_DONT_MOVE) {
                    lastUIMessageTime = System.currentTimeMillis()
                    displayMessage(uiMessage)
                } else {
                    if (!bufferedMessagesThread.isInterrupted) {
                        lastUIMessageTime = System.currentTimeMillis()
                    }
                    bufferedMessagesThread = Thread {
                        try {
                            val sleepTime =
                                UI_MESSAGE_BUFFER_TIME - (System.currentTimeMillis() - lastUIMessageTime)
                            if (sleepTime > 0) {
                                Thread.sleep(sleepTime)
                            }
                            lastUIMessageTime = System.currentTimeMillis()
                            runOnUiThread { displayMessage(uiMessage) }
                        } catch (ignored: InterruptedException) {
                        }
                    }
                    bufferedMessagesThread.start()
                }
            }
        } finally {
            guidanceDisplayMutex.unlock()
        }
    }

    private fun enrollCallback(pvpResponse: PVPResponse) {
        shouldDisplayPostSuccessMessage.set(false)
        // val pvpResponse=PVPResponse.PVP_RESPONSE_SUCCESS
        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            runOnUiThread {
                guidanceStaticTextview!!.visibility = View.GONE
                guidanceTextview!!.setText(R.string.EnrollOk)
            }

          /*  val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Success)
            this.startActivity(myintent)*/



            val myintent = Intent(this, FacerecActivity::class.java)
            this.startActivity(myintent)


        } else if (pvpResponse.name.startsWith("FATAL")) {
            applicationFailure("FATAL ${pvpResponse.name}")
        } else if (pvpResponse == PVPResponse.PVP_RESPONSE_ENROLLMENT_DUPLICATE) {

            val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Duplicate)
            this.startActivity(myintent)

        } else if (pvpResponse == PVPResponse.PVP_RESPONSE_ENROLLMENT_VERIFICATION_FAILURE) {
            showDialogToRetryEnroll(R.string.dialog_message_failed_enrollment_verification)
        } else if (pvpResponse == PVPResponse.PVP_RESPONSE_CANCELED) {
            showDialogToRetryEnroll(R.string.dialog_message_capture_canceled)
        } else {
            applicationFailure("enrollCallback${pvpResponse.name}")
        }
    }

    private fun displayMessage(message: PVPGuidanceMessage) {
        val uiMessageId = pvpGuidanceToUIMessage[message]!!
        val uiMessageText = resources.getString(uiMessageId)
        if (pvpGuidanceWithStaticPrefix.contains(message)) {
            if (guidanceStaticTextview!!.visibility == View.VISIBLE) {
                val fadeIn = AlphaAnimation(0.0f, 1.0f)
                val fadeOut = AlphaAnimation(1.0f, 0.0f)
                guidanceTextview!!.startAnimation(fadeIn)
                guidanceTextview!!.startAnimation(fadeOut)
                fadeIn.duration = TRANSITION_DURATION / 2
                fadeIn.fillAfter = true
                fadeOut.duration = TRANSITION_DURATION / 2
                fadeOut.fillAfter = false
                fadeIn.startOffset = TRANSITION_DURATION / 2
                Thread {
                    try {
                        Thread.sleep(TRANSITION_DURATION / 2)
                        runOnUiThread { guidanceTextview!!.text = uiMessageText }
                    } catch (ignored: InterruptedException) {
                    }
                }.start()
            } else {
                guidanceTextview!!.text = uiMessageText
            }
            guidanceStaticTextview!!.setText(R.string.guidance_static)
            guidanceStaticTextview!!.visibility = View.VISIBLE
            guidanceTextview!!.setTextColor(getColor(R.color.orange))
        } else {
            guidanceTextview!!.text = uiMessageText
            guidanceStaticTextview!!.text = ""
            guidanceStaticTextview!!.visibility = View.GONE
            guidanceTextview!!.setTextColor(getColor(R.color.text))
        }
        guidanceDrivenViewPosition(message)
    }



    private fun showDialogToRetryEnroll(reasonMessageId: Int) {

        runOnUiThread {
           // setupInactivityTimeout(30000){onCancellationConfirmed()}

            showConfirmationDialog(this, getString(R.string.dialog_title_retry_enrollment), getString(reasonMessageId),
            "Retry","Abort", {toggle(ArrayList());enroll()}, { onCancellationConfirmed() })
            if(timefinished){onCancellationConfirmed()}
        }
    }






    private fun onCancellationConfirmed() {
        guidanceTextview!!.setText(R.string.dialog_message_cancellation_ongoing)
       cancelEnrollment()
    }



    private fun setProgressBar(contentStep: Int, progressStep: Int) {
        constraintSet.constrainPercentWidth(R.id.progressBarContent, contentStep * 0.25f)
        constraintSet.constrainPercentWidth(R.id.progressBarProgress, progressStep * 0.25f)
        constraintSet.applyTo(progressBarLayout)
        for (i in circleViews!!.indices) {
            if (i + 1 <= contentStep) {
                circleViews!![i].setColorFilter(getColor(R.color.blue_normal))
            } else if (i + 1 <= progressStep) {
                circleViews!![i].setColorFilter(getColor(R.color.green_bright))
            } else {
                circleViews!![i].setColorFilter(getColor(R.color.blue_light))
            }
        }
        if (Statics.isEnrollement) {
            workMessageTextview!!.setText(R.string.WorkEnrollStart)
        } else {
            workMessageTextview!!.text = resources.getText(R.string.WorkUnEnrollStart)
        }
    }

    //This function displays the corresponding view for each guidance received.
    // If the message is guidance PVP_GUIDANCE_START , the function displays the view at the beginning with an image view called placeHandView.
    // Otherwise, the function displays the view corresponding to the message.
    // If the message is guidance PVP_GUIDANCE_COMPLETE, the user can proceed to the next step in the enrollment/un-enrollment process.
    private fun guidanceDrivenViewPosition(uiMessage: PVPGuidanceMessage) {
        shouldDisplayPostSuccessMessage.set(false)
        when (uiMessage) {
            PVPGuidanceMessage.PVP_GUIDANCE_START -> {
                toggle(ArrayList(listOf(placeHandView, defaultHandView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_PLACE_HAND, PVPGuidanceMessage.PVP_GUIDANCE_FLATTEN_HAND -> {
                toggle(ArrayList(listOf(defaultHandView, placeHandView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_UP -> {
                toggle(ArrayList(listOf(moveUpView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_DONT_MOVE -> {
                guidanceTextview!!.setTextColor(getColor(R.color.blue_normal))
                toggle(ArrayList(listOf(hourglassView)))
                setProgressBar(currentStep, currentStep + 1)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_REMOVE_HAND -> {
                currentStep += 1
                if (currentStep > 3) {
                    // Sometimes, a step is missed, but we can't know it from there. When this happens,
                    // the last quarter of the progress bar is reset until the enrollment is completed.
                    currentStep = 3
                }
                setProgressBar(currentStep, currentStep)
                displaySuccess(R.string.guidance_remove_hand)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_DOWN -> {
                toggle(ArrayList(listOf(moveDownView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_LEFT -> {
                toggle(ArrayList(listOf(moveLeftView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_RIGHT -> {
                toggle(ArrayList(listOf(moveRightView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_CLOSER -> {
                toggle(ArrayList(listOf(moveCloserView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_MOVE_HAND_FURTHER -> {
                toggle(ArrayList(listOf(moveAwayView)))
                setProgressBar(currentStep, currentStep)
            }

            PVPGuidanceMessage.PVP_GUIDANCE_COMPLETE -> {

                setProgressBar(4, 4)
                displaySuccess(R.string.guidance_complete)
            }
        }
    }

    private fun displaySuccess(postSuccessMessageId: Int) {
        toggle(ArrayList(listOf(confirmView)))
        guidanceTextview!!.setTextColor(getColor(R.color.green))
        guidanceStaticTextview!!.visibility = View.GONE
        guidanceTextview!!.setText(R.string.step_success)
        if (shouldDisplayPostSuccessMessage.get()) {
            currentSuccessThread!!.interrupt()
        } else {
            shouldDisplayPostSuccessMessage.set(true)
        }
        currentSuccessThread = Thread {
            try {
                Thread.sleep(SUCCESS_SCREEN_DURATION_MS)
                runOnUiThread {
                    guidanceDisplayMutex.lock()
                    if (shouldDisplayPostSuccessMessage.compareAndSet(true, false)) {
                        guidanceTextview!!.setTextColor(getColor(R.color.text))
                        guidanceStaticTextview!!.visibility = View.GONE
                        guidanceTextview!!.setText(postSuccessMessageId)
                        toggle(ArrayList())
                        playBeep()
                    }
                    guidanceDisplayMutex.unlock()
                }
            } catch (e: InterruptedException) {
                Log.d("CaptureActivity", "Success thread interrupted")
            }
        }
        currentSuccessThread!!.start()
    }

    private fun playBeep() {
        val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
    }

    private fun toggle(visibleImageView: ArrayList<View?>) {
        val transition: Transition = Fade()
        transition.setDuration(TRANSITION_DURATION)
        for (iv in allImageViews!!) {
            transition.addTarget(iv)
            iv!!.visibility = View.GONE
        }
        for (iv in visibleImageView) {
            iv!!.visibility = View.VISIBLE
        }
        TransitionManager.beginDelayedTransition(relativeLayout, transition)
    }


    //If the `guiCallback` returns a message that indicates the need to cancel the capture, you should call
    // the `cancelCapture`, to cancel the ongoing capture process.
    private fun onCancele() {
        guidanceTextview!!.setText(R.string.dialog_message_cancellation_ongoing)
            palmLib.pvpHelper.cancelCapture { cancelCaptureCallback(it) }
        //force error to test
       //cancel()

    }

    private fun cancelCaptureCallback(pvpResponse: PVPResponse) {
        if (pvpResponse == PVPResponse.PVP_RESPONSE_SUCCESS) {
            runOnUiThread { Toast.makeText(this, R.string.toast_cancel_capture_success, Toast.LENGTH_SHORT).show() }
        } else if (pvpResponse.name.startsWith("FATAL")) {
           goToInit()
        } else if (pvpResponse == PVPResponse.PVP_RESPONSE_CANCEL_FAILURE) {
            runOnUiThread { Toast.makeText(this, R.string.toast_cancel_capture_failed, Toast.LENGTH_SHORT).show() }
        } else { applicationFailure("failure cancelCaptureCallback ${pvpResponse.name}") }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {android.R.id.home->{ onCancele() } }
        return super.onOptionsItemSelected(item)
    }
}

