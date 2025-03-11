package com.example.myapplication.emv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import com.usdk.apiservice.aidl.DeviceServiceData
import com.usdk.apiservice.aidl.UDeviceService
import com.usdk.apiservice.aidl.beeper.UBeeper
import com.usdk.apiservice.aidl.emv.UEMV
import com.usdk.apiservice.aidl.system.USystem
import com.usdk.apiservice.aidl.system.application.UApplication
import com.usdk.apiservice.aidl.system.keyboard.UKeyboard
import com.usdk.apiservice.aidl.system.statusbar.UStatusBar
import kotlin.concurrent.Volatile


class DeviceHelper : ServiceConnection {
    private var context: Context? = null
    private var serviceListener: ServiceReadyListener? = null
    private var retry = 0

    @Volatile
    private var isBinded = false
    private var deviceService: UDeviceService? = null
    fun init(context: Context?) {
        this.context = context
    }

    fun setServiceListener(listener: ServiceReadyListener?) {
        Log.d("setServiceListener ", "setServiceListener")
        serviceListener = listener
        if (isBinded) {
            notifyReady()
        }
    }

    fun bindService() {
        if (isBinded) {
            return
        }
        val service = Intent("com.usdk.apiservice")
        service.setPackage("com.usdk.apiservice")
        val bindSucc = context!!.bindService(service, me, Context.BIND_AUTO_CREATE)

        // If the binding fails, it is rebinded
        if (!bindSucc && retry++ < MAX_RETRY_COUNT) {
            Log.e(TAG, "=> bind fail, rebind ($retry)")
            Handler().postDelayed({ bindService() }, RETRY_INTERVALS)
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        Log.d(TAG, "=> onServiceConnected")
        Toast.makeText(context, "serviceConnected", Toast.LENGTH_SHORT).show()
        retry = 0
        isBinded = true
        deviceService = UDeviceService.Stub.asInterface(service)
        debugLog(true)
        notifyReady()
    }

    private fun notifyReady() {
        if (serviceListener != null) {
            try {
                serviceListener!!.onReady(deviceService!!.version)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Log.e(TAG, "=> onServiceDisconnected")
        deviceService = null
        isBinded = false
        bindService()
    }

    fun unbindService() {
        if (isBinded) {
            Log.e(TAG, "=> unbindService")
            context!!.unbindService(this)
            isBinded = false
        }
    }

    @Throws(IllegalStateException::class)
    fun register() {
        try {
            val param = Bundle()
            // This app only demonstrates EMV payment
            param.putBoolean(DeviceServiceData.USE_EPAY_MODULE, true)
            deviceService!!.register(param, Binder())
        } catch (e: RemoteException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        } catch (e: SecurityException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        }
    }

    @Throws(IllegalStateException::class)
    fun unregister() {
        try {
            deviceService!!.unregister(null)
        } catch (e: RemoteException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        }
    }

    fun debugLog(open: Boolean) {
        try {
            val logOption = Bundle()
            logOption.putBoolean(DeviceServiceData.COMMON_LOG, open)
            logOption.putBoolean(DeviceServiceData.MASTERCONTROL_LOG, open)
            deviceService!!.debugLog(logOption)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    @get:Throws(IllegalStateException::class)
    val beeper: UBeeper
        get() {
            val iBinder = object : IBinderCreator() {
                @Throws(RemoteException::class)
                override fun create(): IBinder {
                    return deviceService!!.beeper
                }
            }.start()
            return UBeeper.Stub.asInterface(iBinder)
        }

    @get:Throws(IllegalStateException::class)
    val eMV: UEMV
        get() {
            val iBinder = object : IBinderCreator() {
                @Throws(RemoteException::class)
                override fun create(): IBinder {
                    return deviceService!!.emv
                }
            }.start()
            return UEMV.Stub.asInterface(iBinder)
        }

    @get:Throws(IllegalStateException::class)
    val system: USystem
        get() {
            val iBinder = object : IBinderCreator() {
                @Throws(RemoteException::class)
                override fun create(): IBinder {
                    return deviceService!!.system
                }
            }.start()
            return USystem.Stub.asInterface(iBinder)
        }

    @get:Throws(IllegalStateException::class)
    val application: UApplication
        get() {
            val iBinder = object : IBinderCreator() {
                @Throws(RemoteException::class)
                override fun create(): IBinder {
                    return system.getApplication()
                }
            }.start()
            return UApplication.Stub.asInterface(iBinder)
        }

    @get:Throws(IllegalStateException::class)
    val keyboard: UKeyboard
        get() {
            val iBinder = object : IBinderCreator() {
                @Throws(RemoteException::class)
                override fun create(): IBinder {
                    return system.getKeyboard()
                }
            }.start()
            return UKeyboard.Stub.asInterface(iBinder)
        }

    @get:Throws(IllegalStateException::class)
    val statusBar: UStatusBar
        get() {
            val iBinder = object : IBinderCreator() {
                @Throws(RemoteException::class)
                override fun create(): IBinder {
                    return system.getStatusBar()
                }
            }.start()
            return UStatusBar.Stub.asInterface(iBinder)
        }

    // Re-enable the navigation bar and notification menu
    fun enableHomeButton() {
        if (!isBinded) {
            return
        }
        try {
            context!!.sendBroadcast(Intent(HOME_KEY_ENABLE))
            context!!.sendBroadcast(Intent(STATUS_BAR_EXPAND_ENABLE))
            keyboard.setHomeKeyEnabled(true)
            keyboard.setNavigationBarEnabled(true)
            statusBar.setPanelExpandEnabled(true)
        } catch (ex: Exception) {
            Log.e(TAG, "Update UI error")
            ex.printStackTrace()
        }
    }

    // Disable the navigation bar and notification menu
    fun disableHomeButton() {
        try {
            context!!.sendBroadcast(Intent(HOME_KEY_DISABLE))
            context!!.sendBroadcast(Intent(STATUS_BAR_EXPAND_DISABLE))
            keyboard.setHomeKeyEnabled(false)
            keyboard.setNavigationBarEnabled(false)
            statusBar.setPanelExpandEnabled(false)
        } catch (ex: Exception) {
            Log.e(TAG, "Update UI error.")
            ex.printStackTrace()
        }
    }

    interface ServiceReadyListener {
        fun onReady(version: String?)
    }

    internal abstract inner class IBinderCreator {
        @Throws(IllegalStateException::class)
        fun start(): IBinder {
            if (deviceService == null) {
                bindService()
                throw IllegalStateException("Servic unbound,please retry latter!")

            }
            return try {
                create()
            } catch (e: DeadObjectException) {
                deviceService = null
                throw IllegalStateException("Service process has stopped,please retry latter!")
            } catch (e: RemoteException) {
                throw IllegalStateException(e.message, e)
            } catch (e: SecurityException) {
                throw IllegalStateException(e.message, e)
            }
        }

        @Throws(RemoteException::class)
        abstract fun create(): IBinder
    }

    companion object {
        private const val TAG = "DeviceHelper"

        // The maximum number of rebinds
        private const val MAX_RETRY_COUNT = 3

        // Rebinding interval time
        private const val RETRY_INTERVALS: Long = 3000
        private const val HOME_KEY_ENABLE = "android.intent.action.HOME_KEY_ENABLE"
        private const val HOME_KEY_DISABLE = "android.intent.action.HOME_KEY_DISABLE"
        private const val STATUS_BAR_EXPAND_ENABLE = "android.intent.action.STATUSBAR_ENABLE"
        private const val STATUS_BAR_EXPAND_DISABLE = "android.intent.action.STATUSBAR_DISABLE"
        private val me = DeviceHelper()
        fun me(): DeviceHelper {
            return me
        }
    }
}

