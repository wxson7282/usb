package com.wxson.commlib

import android.app.Application
import cn.wch.ch34xuartdriver.CH34xUARTDriver

class MyApp : Application() {
    // 需要将CH34x的驱动类CH34xUARTDriver类的对象创建在 Application 类下，以保证在应用有多个
    //Activity 切换时均能进行串口收发
    lateinit var driver: CH34xUARTDriver
}
