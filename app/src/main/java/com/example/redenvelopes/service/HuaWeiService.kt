package com.example.redenvelopes.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.example.redenvelopes.HuaWeiConstants.HUAWEI_PRODUCT_DETAIL_ACTIVITY
import com.example.redenvelopes.HuaWeiConstants.HUAWEI_SUBMIT_ORDER_ACTIVITY
import com.example.redenvelopes.HuaWeiConstants.RED_ENVELOPE_ID
import com.example.redenvelopes.HuaWeiConstants.RED_ENVELOPE_TITLE
import com.example.redenvelopes.HuaWeiConstants.WECHAT_PACKAGE
import com.example.redenvelopes.MyApplication
import com.example.redenvelopes.R
import com.example.redenvelopes.activity.MainActivity
import com.example.redenvelopes.data.RedEnvelopePreferences
import com.example.redenvelopes.utils.AccessibilityHelper
import com.example.redenvelopes.utils.AccessibilityServiceUtils
import com.example.redenvelopes.utils.WakeupTools


class HuaWeiService : AccessibilityService() {
    private val TAG = "========"
    private var isHasReceived: Boolean = false//true已经通知或聊天列表页面收到红包
    private var isHasClicked: Boolean = false//true点击红包弹出红包框
    private var isHasOpened: Boolean = false//true点击了拆开红包按钮


    private val WECHAT_LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI"
    private var currentClassName = WECHAT_LAUNCHER_UI

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var flags = flags
        val builder = Notification.Builder(MyApplication.instance.applicationContext)
        val notificationIntent = Intent(this, MainActivity::class.java)

        builder.setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    R.mipmap.ic_launcher
                )
            ) // set the large icon in the drop down list.
            .setContentTitle("RedEnvelope") // set the caption in the drop down list.
            .setSmallIcon(R.mipmap.ic_launcher) // set the small icon in state.
            .setContentText("RedEnvelope") // set context content.
            .setWhen(System.currentTimeMillis()) // set the time for the notification to occur.

        val notification = builder.build()
        notification.defaults = Notification.DEFAULT_SOUND// set default sound.

        startForeground(110, notification)
        flags = Service.START_FLAG_REDELIVERY
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onInterrupt() {
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        try {
            if (WECHAT_PACKAGE != event.packageName) return
            if (event.className.toString().startsWith(WECHAT_PACKAGE)) {
                currentClassName = event.className.toString()
            }

            when (event.eventType) {
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    Log.d(TAG, "通知改变" + event.text)
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d(TAG, "界面改变$event")
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    if (rootInActiveWindow == null)
                        return
                    Log.d(TAG, "内容改变")
                    monitorBuy()
                    if (HUAWEI_SUBMIT_ORDER_ACTIVITY != currentClassName) return
                    //遍历h5页面
                    rootInActiveWindow?.let {
                        for (i in 0 until it.childCount) {
                            val child = it.getChild(i)
                            getAllNodeName(child)

                        }
                    }


                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    Log.d(TAG, "点击")
                    getClickContent(event)
                    if (rootInActiveWindow == null) {
                        return
                    }

                    grabRedEnvelope()

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAllNodeName(child: AccessibilityNodeInfo?) {
        try {
            child?.let {
                if (it.childCount > 0) {
                    for (i in 0 until it.childCount) {
                        getAllNodeName(child.getChild(i))
                    }
                } else {
                    Log.i("wsc", " text = ${child.text} id = ${child.viewIdResourceName}")
                    if (child.text.contains("抱歉，没有抢到")) {
                        //没抢到 后退再抢
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        return@let
                    } else if ("immediatePay".equals(child.viewIdResourceName)) {
                        //提交订单 抢到了
                        AccessibilityHelper.performClick(child)
                        return@let
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getClickContent(event: AccessibilityEvent) {
        try {
            Log.i("wsc", "event.className = " + event.className)
            Log.i("wsc", "event.viewIdResourceName = " + event.source?.viewIdResourceName)
            Log.i("wsc", "event 文本 = " + event.text)
            Log.i("wsc", "event source = " + event.getSource())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 监控微信聊天列表页面是否有红包，经测试若聊天页面与通知同时开启聊天页面快
     */
    private fun monitorBuy() {
        Log.d(TAG, "monitorChat")
        val lists = AccessibilityServiceUtils.getElementsById(
            RED_ENVELOPE_ID,
            rootInActiveWindow
        ) ?: return
        Log.d(TAG, "lists" + lists.toString())
        for (envelope in lists) {
            Log.d(TAG, "文字--" + envelope.text)
            if (!envelope.text.isNullOrEmpty()) {
                if (envelope.text.contains("立即申购")) {
                    Log.d(TAG, "monitorChat:红包")
                    AccessibilityHelper.performClick(envelope)
                    isHasReceived = true
                }
            }
        }

    }

    /**
     * 确定
     */
    private fun grabRedEnvelope() {
        Log.d(TAG, "grabRedEnvelope")

        val envelopes = AccessibilityServiceUtils.getElementsById(
            RED_ENVELOPE_ID,
            rootInActiveWindow
        ) ?: return

        for (envelope in envelopes) {
            if (!envelope.text.isNullOrEmpty()) {
                if (envelope.text.contains("确定")) {
                    AccessibilityHelper.performClick(envelope)
                    isHasClicked = true

                }
            }
        }
        isHasReceived = false
    }

}
