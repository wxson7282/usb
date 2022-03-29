package com.wxson.switch4

import android.os.Handler
import androidx.lifecycle.ViewModel
import com.wxson.commlib.Ch34x
import com.wxson.commlib.Ch34xAction


class MainViewModel : ViewModel() {
    private val ch34x = Ch34x()

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
        return Ch34xAction.open(ch34x)
    }

    fun close() : String {
        return Ch34xAction.close(ch34x)
    }

    fun config() : String {
        return Ch34xAction.config(ch34x)
    }

    fun requestState() : String {
        return Ch34xAction.write(ch34x, "FF")
    }

    private fun write(outputString: String) : String {
        return Ch34xAction.write(ch34x, outputString)
    }

}