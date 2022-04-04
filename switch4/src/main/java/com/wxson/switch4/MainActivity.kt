package com.wxson.switch4

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.wxson.switch4.databinding.ActivityMainBinding
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var handler: Handler
    private var stateMsg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        if (viewModel.usbPermissionCheck()) {
            showMsg("完美支持USB HOST")
        } else {
            showMsg("你的手机不支持USB HOST，请换手机")
            exitProcess(0)
        }

        viewModel.setCh34x()

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                stateMsg += msg.obj as String
                if (stateMsg.length >= 40) {    //状态信息是分段返回，全部返回后再行处理
                    binding.textView.text = stateMsg
                    binding.switch1.isChecked = stateMsg[6] == 'N'
                    binding.switch2.isChecked = stateMsg[16] == 'N'
                    binding.switch3.isChecked = stateMsg[26] == 'N'
                    binding.switch4.isChecked = stateMsg[36] == 'N'
                    stateMsg = ""
                }
            }
        }
        viewModel.setHandler(handler)

        binding.toggleBtnOpen.setOnCheckedChangeListener { _, isChecked ->
            val returnMsg: String
            if (isChecked) {
                returnMsg = viewModel.open()
                if (returnMsg != "Device opened") {
                    binding.toggleBtnOpen.isChecked = false
                } else {
                    binding.btnConfig.isEnabled = true
                    binding.btnGetState.isEnabled = true
                    binding.toggleBtn1.isEnabled = true
                    binding.toggleBtn2.isEnabled = true
                    binding.toggleBtn3.isEnabled = true
                    binding.toggleBtn4.isEnabled = true
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

        binding.btnConfig.setOnClickListener {
            showMsg(viewModel.config())
        }

        binding.btnGetState.setOnClickListener {
            showMsg(viewModel.requestState())
        }

        binding.btnClear.setOnClickListener {
            binding.textView.text = ""
        }

        setOnCheckedChangeListener(binding.toggleBtn1, 1)
        setOnCheckedChangeListener(binding.toggleBtn2, 2)
        setOnCheckedChangeListener(binding.toggleBtn3, 3)
        setOnCheckedChangeListener(binding.toggleBtn4, 4)
    }

    private fun setOnCheckedChangeListener(toggleButton: ToggleButton, ToggleButtonId: Int) {
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            showMsg(viewModel.setSwitch(ToggleButtonId, isChecked))
        }
    }

    private fun showMsg(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}