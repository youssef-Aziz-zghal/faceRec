package com.example.myapplication.emv

import com.usdk.apiservice.aidl.constants.RFDeviceName


object EmvConfig {
    var RF_DEVICE_NAME = RFDeviceName.INNER

    var CARD_READ_TIMEOUT_SEC = 15
}
