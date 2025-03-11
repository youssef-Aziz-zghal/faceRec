package com.example.myapplication.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.myapplication.emv.DeviceActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

      if(context.getAutoStart()) {
          val i = Intent(context, DeviceActivity::class.java)
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(i)
          Toast.makeText(context, "BOOTSTART", Toast.LENGTH_SHORT).show()
      }
    }
}