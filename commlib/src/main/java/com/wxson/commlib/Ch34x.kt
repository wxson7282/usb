package com.wxson.commlib

class Ch34x(
    val myApp: MyApp = MyApp(),
    var isOpen: Boolean = false,
    var baudRate: Int = 9600,
    var stopBit: Byte = 1,
    var dataBit: Byte = 8,
    var parity: Byte = 0,
    var flowControl: Byte = 0
) {}