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

    init {
        //注入Ch34x
        Ch34xAction.setCh34x(ch34x)
    }
    /**
     * usb权限检查
     * @return 是否有usb权限
     */
    fun usbPermissionCheck() : Boolean {
        ch34x.myApp.driver = CH34xUARTDriver(
            MyApplication.context.getSystemService(AppCompatActivity.USB_SERVICE)  as UsbManager,
            MyApplication.context,
            "cn.wch.wchusbdriver.USB_PERMISSION")
        return ch34x.myApp.driver.UsbFeatureSupported()
    }

    /**
     * 注入Handler
     * @param handler Handler实例
     */
    fun setHandler(handler: Handler) {
        Ch34xAction.setHandler(handler)
    }

    /**
     * 通过向USB输出数据控制继电器的开关
     * @param switchId 继电器序号
     * @param isOn 开关状态
     * @return 执行结果String
     */
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

    /**
     * 打开设备
     * @return 执行结果String
     */
    fun open() : String {
        return try {
            Ch34xAction.open()
        } catch (e: Exception) {
            e.stackTraceToString()
        }
    }

    /**
     * 关闭设备
     * @return 执行结果String
     */
    fun close() : String {
        return Ch34xAction.close()
    }

    /**
     * 配置设备参数
     * @return 执行结果String
     */
    fun config() : String {
        return Ch34xAction.config()
    }

    /**
     * 发出设备状态请求
     * @return 执行结果String
     */
    fun requestState() : String {
        return Ch34xAction.write( "FF")
    }

    /**
     * 数据写入设备
     * @return 执行结果String
     */
    private fun write(outputString: String) : String {
        return Ch34xAction.write(outputString)
    }

}