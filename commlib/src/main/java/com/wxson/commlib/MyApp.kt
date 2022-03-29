package com.wxson.commlib

import android.app.Application
import cn.wch.ch34xuartdriver.CH34xUARTDriver

class MyApp : Application() {
    // 需要将CH34x的驱动类写在APP类下面，使得帮助类的生命周期与整个应用程序的生命周期是相同的
    lateinit var driver: CH34xUARTDriver
}
