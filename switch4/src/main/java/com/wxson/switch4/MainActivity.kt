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

    lateinit var viewModel: MainViewModel
    lateinit var handler: Handler

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
                binding.textView.append(msg.obj as String)
            }
        }
        viewModel.setHandler(handler)

        binding.toggleBtnOpen.setOnCheckedChangeListener { _, isChecked ->
            val returnMsg: String
            if (isChecked) {
                returnMsg = viewModel.open()
                if (returnMsg != "Device opened") {
                    binding.toggleBtnOpen.isChecked = false
                }
            } else {
                returnMsg = viewModel.close()
                if (returnMsg != "Device closed") {
                    binding.toggleBtnOpen.isChecked = true
                }
            }
            showMsg(returnMsg)
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