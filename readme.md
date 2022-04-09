手机本身就是一台功能强大的计算机，通过其USB接口可以方便地控制外部设备，前提是外部设备也要有USB接口。
本实例用到一个四路USB智能继电器接口模块，价格便宜，网上可以买到。
模块上有单片微控制器、USB转串口控制芯片CH340 、四路继电器和指示灯，细节请参考[说明书。](http://www.lctech-inc.com/cpzx/2/jdqmk/2021/0825/507.html)
### 为了使用接口模块，必须在build.gradle中导入厂商提供的驱动库。
```
dependencies {
	...
	api files('libs/CH34xUARTDrive_no_toast.jar')
	}
```
### 权限等设置
获取USB HOST权限
```xml
<uses-feature android:name="android.hardware.usb.host" />
```
USB设备插入时启动相关ACTIVITY或SERVICE
```xml
<intent-filter>
   <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>
```
USB设备插入时检索需要过滤的设备列表
```xml
<meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" android:resource="@xml/device_filter" />
```
需要过滤的设备列表
```xml
<resource>
   <usb-device product-id="29987" vendor-id="6790" />
   <usb-device product-id="21795" vendor-id="6790" />
   <usb-device product-id="21778" vendor-id="6790" />
</resource>
```

### 在公共库commlib里有三个类：

1. MyApp
   需要将芯片CH340的驱动类CH34xUARTDriver的对象创建在 Application 类下，以保证在应用有多个Activity 切换时均能进行串口收发。
```kotlin
class MyApp : Application() {
    lateinit var driver: CH34xUARTDriver
}
```


2. Ch34x
   这里包括CH34x驱动和相关参数。
```kotlin
class Ch34x(
    val myApp: MyApp = MyApp(),
    var isOpen: Boolean = false,
    var baudRate: Int = 9600,
    var stopBit: Byte = 1,
    var dataBit: Byte = 8,
    var parity: Byte = 0,
    var flowControl: Byte = 0
) {}
```
4. Ch34xAction
   这里包括若干操作USB接口的静态方法。
```kotlin
object Ch34xAction {
    private lateinit var handler: Handler
    private lateinit var ch34x: Ch34x

    /**
     * 注入Ch34x
     * @param ch34 Ch34x的实例
     * @return Unit
     */
    fun setCh34x(ch34: Ch34x) {
        ch34x = ch34
    }

    /**
     * 注入Handler
     *  @param handler Handler的实例
     *  @return Unit
     */
    fun setHandler(handler: Handler) {
        this.handler = handler
    }

    /**
     * 打开设备
     * @return 执行结果String
     */
    fun open(): String {
        return with(ch34x) {
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
//                                    startReadThread()
                                    readDataCoroutine()
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

    /**
     * 关闭设备
     * @return 执行结果String
     */
    fun close(): String {
        return with(ch34x) {
            if (isOpen) {
                isOpen = false
//                try {
//                    Thread.sleep(200)
//                } catch (e: InterruptedException) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace()
//                }
                job.cancel()
                myApp.driver.CloseDevice()
                "Device closed"
            } else {
                "no action executed"
            }
        }
    }

    /**
     * 配置设备参数
     * @return 执行结果String
     */
    fun config(): String {
        return with(ch34x) {
            if (myApp.driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl)) {
                "Config successfully"
            } else {
                "Config failed!"
            }
        }
    }

    /**
     * 数据写入设备
     * @param outputString 十六进制字符形式的数据
     * @return 执行结果String
     */
    fun write(outputString: String): String {
        return with(ch34x.myApp.driver) {
            val toSend: ByteArray = toByteArray(outputString)
            if (WriteData(toSend, toSend.size) < 0) {
                "Write failed!"
            } else {
                "Write successfully"
            }
        }
    }

    /**
     * 判断设备是否连接
     * @return 连接状态
     */
    fun isConnected(): Boolean {
        return ch34x.myApp.driver.isConnected
    }

    /**
     * 启动读数据线程
     */
    private fun startReadThread() {
        thread {
            val buffer = ByteArray(4096)
            while (true) {
                val msg = Message.obtain()
                val length: Int = ch34x.myApp.driver.ReadData(buffer, 4096)
                if (length > 0) {
//                    val inputStr = String(buffer, 0, length)    //以字符串形式输出
                    val inputStr = toHexString(buffer)
                    msg.obj = inputStr
                    if (::handler.isInitialized){
                        handler.sendMessage(msg)
                    }
                }
            }
        }
    }

    private val job = Job()     // background job for coroutine
    private val scope = CoroutineScope(job) // coroutine scope

    /**
     * 启动读数据协程
     */
    private fun readDataCoroutine() {
        scope.launch {
            val buffer = ByteArray(4096)
            while (true) {
                val msg = Message.obtain()
                val length: Int = ch34x.myApp.driver.ReadData(buffer, 4096)
                if (length > 0) {
                    val inputStr = String(buffer, 0, length)    //以字符串形式输出
//                    val inputStr = toHexString(buffer)
                    msg.obj = inputStr
                    if (::handler.isInitialized){
                        handler.sendMessage(msg)
                    }
                }
            }
        }
    }

    /**
     * 将ByteArray转化为十六进制String
     * @param bytes     需要转换的byte[]数组
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
```
需要说明的是上面的读数据线程并没有使用，是个备用方案。这里用协程来读取USB设备的数据。
### 应用APP
#### activity
![页面LAYOUT](https://img-blog.csdnimg.cn/b4db16058df141d99d2afdc829f3acdd.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAd3hzb243Mjgy,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)
只有一个简单ACTIVITY，通过页面开关经USB控制四个继电器。

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var handler: Handler
    private var stateMsg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //获取ViewModel实例
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        //USB权限检查
        if (viewModel.usbPermissionCheck()) {
            showMsg("完美支持USB HOST")
        } else {
            showMsg("你的手机不支持USB HOST，请换手机")
            exitProcess(0)
        }
        //设备返回信息处理Handler定义
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                stateMsg += msg.obj as String
                if (stateMsg.length >= 40) {    //状态信息是分段返回，全部返回后再行处理
                    binding.textView.text = stateMsg
                    binding.switch1.isChecked = stateMsg[6] == 'N'
                    binding.switch2.isChecked = stateMsg[16] == 'N'
                    binding.switch3.isChecked = stateMsg[26] == 'N'
                    binding.switch4.isChecked = stateMsg[36] == 'N'
                    binding.toggleBtn1.isChecked = stateMsg[6] == 'N'
                    binding.toggleBtn2.isChecked = stateMsg[16] == 'N'
                    binding.toggleBtn3.isChecked = stateMsg[26] == 'N'
                    binding.toggleBtn4.isChecked = stateMsg[36] == 'N'
                    stateMsg = ""
                }
            }
        }
        //注入Handler
        viewModel.setHandler(handler)
        //toggleBtnOpen Listener
        binding.toggleBtnOpen.setOnCheckedChangeListener { _, isChecked ->
            val returnMsg: String
            if (isChecked) {
                returnMsg = viewModel.open()
                if (returnMsg != "Device opened") {
                    binding.toggleBtnOpen.isChecked = false
                } else {
                    binding.btnConfig.isEnabled = true
                }
            } else {
                returnMsg = viewModel.close()
                if (returnMsg != "Device closed") {
                    binding.toggleBtnOpen.isChecked = true
                } else {
                    binding.btnConfig.isEnabled = false
                    binding.btnGetState.isEnabled = false
                    binding.toggleBtn1.isEnabled = false
                    binding.toggleBtn2.isEnabled = false
                    binding.toggleBtn3.isEnabled = false
                    binding.toggleBtn4.isEnabled = false
                }
            }
            showMsg(returnMsg)
//            binding.textView.text = returnMsg
        }
        //btnConfig Listener
        binding.btnConfig.setOnClickListener {
            val returnMsg = viewModel.config()
            if (returnMsg == "Config successfully") {
                binding.btnGetState.isEnabled = true
                binding.toggleBtn1.isEnabled = true
                binding.toggleBtn2.isEnabled = true
                binding.toggleBtn3.isEnabled = true
                binding.toggleBtn4.isEnabled = true
                Thread.sleep(200)
                viewModel.requestState()
            }
            showMsg(viewModel.config())
        }
        //btnGetState Listener
        binding.btnGetState.setOnClickListener {
            showMsg(viewModel.requestState())
        }
        //btnClear Listener
        binding.btnClear.setOnClickListener {
            binding.textView.text = ""
        }
        //set toggleBtn1..4 Listener
        setOnCheckedChangeListener(binding.toggleBtn1, 1)
        setOnCheckedChangeListener(binding.toggleBtn2, 2)
        setOnCheckedChangeListener(binding.toggleBtn3, 3)
        setOnCheckedChangeListener(binding.toggleBtn4, 4)
    }
    //toggleBtn1..4 Listener
    private fun setOnCheckedChangeListener(toggleButton: ToggleButton, ToggleButtonId: Int) {
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            showMsg(viewModel.setSwitch(ToggleButtonId, isChecked))
            Thread.sleep(200)   //给外部设备一些处理时间
            viewModel.requestState()
        }
    }

    private fun showMsg(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}
```
#### MainViewModel
对数据的访问和控制都在这里实现。

```kotlin
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
```
如果有任何指摘、问题、BUG，可以联系我 wxson@126.com 。
