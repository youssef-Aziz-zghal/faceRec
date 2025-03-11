package com.example.myapplication.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utilities.Statics
import java.util.Locale

class ResultActivity : AppCompatActivity() {
    lateinit var backHome:Button
    lateinit var msgTextview: TextView
    lateinit var successImage:ImageView
    lateinit var failureImage:ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        changeLanguage(Statics.currentLuanguage)
        backHome=findViewById(R.id.result_next)
        msgTextview= findViewById(R.id.result_title)
        successImage= findViewById(R.id.result_success)
        failureImage= findViewById(R.id.result_failure)
        backHome.setText(R.string.result_next)
        backHome.setOnClickListener { val myintent = Intent(this, IdleActivity::class.java);this.startActivity(myintent) }
        val result =intent.getSerializableExtra("RESULT")
        when (result) {
                ResultType.Success -> {
                    successImage.visibility = View.VISIBLE
                    failureImage.visibility = View.GONE
                    if (Statics.isEnrollement) {
                        msgTextview.setText(R.string.result_title_enrollment_success)
                    } else {
                        msgTextview.setText(R.string.result_title_un_enrollment_success)
                    }
                }

                ResultType.Duplicate -> {
                    successImage.visibility = View.GONE
                    failureImage.visibility = View.VISIBLE
                    msgTextview.setText(R.string.result_title_enrollment_duplicate)
                }

                ResultType.Canceled -> {
                    successImage.visibility = View.GONE
                    failureImage.visibility = View.VISIBLE
                    if (Statics.isEnrollement) {
                        msgTextview.setText(R.string.result_title_enrollment_canceled)
                    } else {
                        msgTextview.setText(R.string.result_title_un_enrollment_canceled)
                    }
                }
                ResultType.Unrecognized -> {
                    successImage.visibility = View.GONE
                    failureImage.visibility = View.VISIBLE
                    msgTextview.setText(R.string.result_title_un_enrollment_failure)
                }
            }




    }


    private fun changeLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

    }

    enum class ResultType {
        Success,
        Duplicate,
        Canceled,
        Unrecognized
    }

}



