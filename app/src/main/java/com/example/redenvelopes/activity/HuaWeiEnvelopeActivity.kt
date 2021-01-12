package com.example.redenvelopes.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Toast
import com.example.redenvelopes.R
import com.example.redenvelopes.base.BaseActivity
import kotlinx.android.synthetic.main.include_title.*


class HuaWeiEnvelopeActivity : BaseActivity(), SeekBar.OnSeekBarChangeListener {

    private val WECHAT_SERVICE_NAME = "com.example.redenvelopes/.service.HuaWeiService"

    private lateinit var mCbWechatControl: CheckBox

    private var t_putong: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_huawei_envelope)

        back()

        setMenuTitle("华为设置")

        initView()

        addListener()
    }

    private fun setMenuTitle(s: String) {
        tv_title.text = s
    }

    private fun back() {
        ib_back.setOnClickListener {
            finish()
        }
    }

    private fun initView() {
        mCbWechatControl = findViewById(R.id.cb_huawei_control)

        mCbWechatControl.setOnCheckedChangeListener { buttonView, isChecked ->
            mCbWechatControl.isChecked = !isChecked
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this@HuaWeiEnvelopeActivity, "辅助功能找到（华为手机抢购）开启或关闭。", Toast.LENGTH_SHORT)
                .show()
        }

    }

    private fun addListener() {
        addAccessibilityServiceListener(object : AccessibilityServiceListeners {
            override fun updateStatus(boolean: Boolean) {
                updateControlView(boolean)
            }
        }, WECHAT_SERVICE_NAME)
        updateControlView(checkStatus())
    }

    private fun updateControlView(boolean: Boolean) {
        if (boolean) mCbWechatControl.setButtonDrawable(R.mipmap.switch_on)
        else mCbWechatControl.setButtonDrawable(R.mipmap.switch_off)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        when (seekBar.id) {
            R.id.sb_qq_putong -> {
                t_putong = progress
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

    }
}
