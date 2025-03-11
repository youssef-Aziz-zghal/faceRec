package com.example.myapplication.emv

import android.os.Bundle
import com.usdk.apiservice.aidl.emv.EMVData


class CardOption private constructor() {
    private val option = Bundle()
    fun supportMagCard(supportMagCard: Boolean): CardOption {
        option.putBoolean(EMVData.SUPPORT_MAG_CARD, supportMagCard)
        return this
    }

    fun supportICCard(supportICCard: Boolean): CardOption {
        option.putBoolean(EMVData.SUPPORT_IC_CARD, supportICCard)
        return this
    }

    fun supportRFCard(supportRFCard: Boolean): CardOption {
        option.putBoolean(EMVData.SUPPORT_RF_CARD, supportRFCard)
        return this
    }

    fun rfDeviceName(rfDeviceName: String?): CardOption {
        option.putString(EMVData.RF_DEVICE_NAME, rfDeviceName)
        return this
    }

    fun trackCheckEnabled(trkDataCheck: Boolean): CardOption {
        option.putBoolean(EMVData.TRACK_CHECK_ENABLED, trkDataCheck)
        return this
    }

    fun trkIdWithWholeData(trkIdWithWholeData: Int): CardOption {
        option.putInt(EMVData.TRKID_WITH_WHOLE_DATA, trkIdWithWholeData)
        return this
    }

    fun supportAllRFCardTypes(supportAllRFCardTypes: Boolean): CardOption {
        option.putBoolean(EMVData.SUPPORT_ALL_RF_CARD_TYPES, supportAllRFCardTypes)
        return this
    }

    fun loopSearchRFCard(loopSearchRFCard: Boolean): CardOption {
        option.putBoolean(EMVData.LOOP_SEARCH_RF_CARD, loopSearchRFCard)
        return this
    }

    fun toBundle(): Bundle {
        return option
    }

    companion object {
        fun create(): CardOption {
            return CardOption()
        }
    }
}

