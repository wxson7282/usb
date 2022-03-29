package com.wxson.switch4

import android.hardware.usb.UsbManager
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import cn.wch.ch34xuartdriver.CH34xUARTDriver
import com.wxson.commlib.Ch34x
import com.wxson.commlib.Ch34xAction


class MainViewModel : ViewModel() {
    private val ch34x = Ch34x()

    fun usbPermissionCheck() : Boolean {
        ch34x.myApp.driver = CH34xUARTDriver(
            MyApplication.context.getSystemService(AppCompatActivity.USB_SERVICE)  as UsbManager,
            MyApplication.context,
            "cn.wch.wchusbdriver.USB_PERMISSION")
        return ch34x.myApp.driver.UsbFeatureSupported()
    }

    fun setCh34x() {
        Ch34xAction.setCh34x(ch34x)
    }

    fun setHandler(handler: Handler) {
        Ch34xAction.setHandler(handler)
    }

    fun setSwitch(switchId: Int, isOn: Boolean) : String {
        val result = with(StringBuilder()) {
            append("A0 ")
            when (switchId) {
                1 -> {
                    append("01 ")
                    append(if (isOn) "01 A2" else "00 A1")
                }
                2 -> {
                    append("02 ")
                    append(if (isOn) "01 A3" else "00 A2")
                }
                3 -> {
                    append("03 ")
                    append(if (isOn) "01 A4" else "00 A3")
                }
                4 -> {
                    append("04 ")
                    append(if (isOn) "01 A5" else "00 A4")
                }
                else -> {}
            }
            toString()
        }
        return write(result)
    }

    fun open() : String {
        return Ch34xAction.open()
    }

    fun close() : String {
        return Ch34xAction.close()
    }

    fun config() : String {
        return Ch34xAction.config()
    }

    fun requestState() : String {
        return Ch34xAction.write( "FF")
    }

    private fun write(outputString: String) : String {
        return Ch34xAction.write(outputString)
    }

}