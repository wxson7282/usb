package com.wxson.commlib

import android.os.Handler
import android.os.Message
import kotlin.concurrent.thread

object Ch34xAction {
    private lateinit var handler: Handler
    private var ch34x: Ch34x? = null

    fun setHandler(handler: Handler) {
        this.handler = handler
    }

    fun open(ch34: Ch34x): String {
        ch34x = ch34
        return with(ch34) {
            if (!isOpen) {
                if (myApp.driver.ResumeUsbPermission() == 0) {
                    //Resume usb device list
                    //ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                    when (myApp.driver.ResumeUsbList()) {
                        -1 -> {
                            myApp.driver.CloseDevice()
                            "Open failed! ResumeUsbList()=-1"
                        }
                        0 -> {
                            if (myApp.driver.mDeviceConnection != null) {
                                //对串口设备进行初始化操作
                                if (!myApp.driver.UartInit()) {
                                    "Initialization failed!"
                                } else {
                                    isOpen = true
                                    //开启读线程读取串口接收的数据
                                    readThread.start()
                                    "Device opened"
                                }
                            } else {
                                "Open failed! mDeviceConnection is null"
                            }
                        }
                        else -> {
                            "未授权限!"
                        }
                    }
                } else {
                    "ResumeUsbPermission() != 0"
                }
            } else {
                "no action executed"
            }
        }
    }

    fun close(ch34: Ch34x): String {
        return with(ch34) {
            if (isOpen) {
                isOpen = false
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
                myApp.driver.CloseDevice()
                "Device closed"
            } else {
                "no action executed"
            }
        }
    }

    fun config(ch34: Ch34x): String {
        return with(ch34) {
            if (myApp.driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl)) {
                "Config successfully"
            } else {
                "Config failed!"
            }
        }
    }

    fun write(ch34: Ch34x, outputString: String): String {
        return with(ch34.myApp.driver) {
            val toSend: ByteArray = toByteArray(outputString)
            if (WriteData(toSend, toSend.size) < 0) {
                "Write failed!"
            } else {
                "Write successfully"
            }
        }
    }

    private val readThread = thread(false){
        ch34x?.let {
            val buffer = ByteArray(4096)
            while (true) {
                val msg = Message.obtain()
                val length: Int = it.myApp.driver.ReadData(buffer, 4096)
                if (length > 0) {
                    val inputStr: String = toHexString(buffer) //以16进制字符串输出
                    //					String recv = new String(buffer, 0, length);		//以字符串形式输出
                    msg.obj = inputStr
                    if (this::handler.isInitialized){
                        handler.sendMessage(msg)
                    }
                }
            }
        }
    }

    /**
     * 将ByteArray转化为十六进制String
     * @param bytes     需要转换的byte[]数组
     * @param length  需要转换的数组长度
     * @return 转换后的String
     */
    private fun toHexString(bytes: ByteArray?): String {
        var result = String()
        var hexString: String
        bytes?.let {
            for (bt in it) {
                hexString = Integer.toHexString((if(bt < 0) bt + 256 else bt).toInt())
                result += if (hexString.length == 1)  "0$hexString" else "$hexString "
            }
//            it.forEach { it1 ->
//                hexString = Integer.toHexString((if(it1 < 0) it1 + 256 else it1).toInt())
//                result += if (hexString.length == 1)  "0$hexString" else "$hexString "
//            }
        }
        return result
    }

    /**
     * 将String转化为byte[]数组
     * @param str 需要转换的String对象
     * @return 转换后的byte[]数组
     */
    private fun toByteArray(str: String): ByteArray {
        /* 1.先去除String中的' '，然后将String转换为char数组 */
        val charArray = str.toCharArray().filterNot { it.isWhitespace()}.toCharArray()
        val length = charArray.size
        /* 将char数组中的值转成一个实际的十进制数组 */
        val evenLength = if (length % 2 == 0) length else length + 1 //如果长度未基数则加一
        if (evenLength != 0) {
            val data = IntArray(evenLength)
            data[evenLength - 1] = 0
            for (i in 0 until length) {
                when (val char: Char = charArray[i]) {
                    in '0'..'9' -> {
                        data[i] = char - '0'
                    }
                    in 'a'..'f' -> {
                        data[i] = char - 'a' + 10
                    }
                    in 'A'..'F' -> {
                        data[i] = char - 'A' + 10
                    }
                }
            }
            /* 将每个char的值(十进制)每两个组成一个16进制数据 */
            val byteArray = ByteArray(evenLength / 2)
            for (i in 0 until evenLength / 2) {
                byteArray[i] = (data[i * 2] * 16 + data[i * 2 + 1]).toByte()
            }
            return byteArray
        }
        return byteArrayOf()
    }

}