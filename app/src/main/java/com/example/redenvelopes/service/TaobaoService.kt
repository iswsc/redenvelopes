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
import com.example.redenvelopes.TaobaoConstants
import com.example.redenvelopes.MyApplication
import com.example.redenvelopes.R
import com.example.redenvelopes.activity.MainActivity
import com.example.redenvelopes.utils.AccessibilityHelper
import com.example.redenvelopes.utils.AccessibilityServiceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class TaobaoService : AccessibilityService() {
    private val TAG = "wsc"
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
            if (TaobaoConstants.HUAWEI_PACKAGE != event.packageName) return
//            if (event.className.toString().startsWith(TaobaoConstants.HUAWEI_PACKAGE)) {
                currentClassName = event.className.toString()
//            }
//            Log.i("wsc", "currentClassName = $currentClassName")

            when (event.eventType) {
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    Log.d(TAG, "通知改变" + event.text)
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d(TAG, "界面改变${event.className}")
                    monitorBuy()
//                    monitorSettingNotify()
                    monitorOrderPage()
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    if (rootInActiveWindow == null)
                        return

                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    Log.d(TAG, "点击")

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun monitorOrderPage() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    if (TaobaoConstants.HUAWEI_SUBMIT_ORDER_ACTIVITY != currentClassName) return@launch

                    var result = false
                    rootInActiveWindow?.let {
                        for (i in 0 until it.childCount) {
                            val child = it.getChild(i)
                            result = getAllNodeName(child)
//                            if (TaobaoConstants.HUAWEI_SUBMIT_ORDER_ACTIVITY != currentClassName) {
//                                return@launch
//                            }
                            if (result) {
                                return@launch
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getAllNodeName(child: AccessibilityNodeInfo?): Boolean {
        try {
            child?.let {
                if (it.childCount > 0) {
                    for (i in 0 until it.childCount) {
                        val result = getAllNodeName(child.getChild(i))
                        if (result) {
                            return true
                        }
                    }
                } else {
                    Log.i(TAG, " text = ${child.text} id = ${child.viewIdResourceName}")
                    if (child.text?.contains("抱歉，没有抢到") == true) {
                        //没抢到 后退再抢
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        isHasReceived = false
                        return true
                    } else if ("immediatePay" == child.viewIdResourceName) {
                        //提交订单 抢到了
                        AccessibilityHelper.performClick(child)
                        Log.i(TAG, " 找到提交订单按钮了")
                        return true
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
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
     * 监控立即申购页面
     */
    private fun monitorBuy() {
        if (TaobaoConstants.HUAWEI_PRODUCT_DETAIL_ACTIVITY != currentClassName) return
        Log.d(TAG, "monitorBuy")
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                if (TaobaoConstants.HUAWEI_PRODUCT_DETAIL_ACTIVITY != currentClassName) return@launch

                val lists = AccessibilityServiceUtils.getElementsById(
                    TaobaoConstants.HUAWEI_SINGLE_BUY,
                    rootInActiveWindow
                ) ?: return@launch
                Log.d(TAG, "lists = " + lists.size)
                for (envelope in lists) {
                    Log.d(TAG, "文字--${envelope.text} enable = ${envelope.isEnabled}")
                    AccessibilityHelper.performClick(envelope)
                    Log.i(TAG, "点击 ${envelope.text}")
                    isHasReceived = true
                    if(envelope.isEnabled){
                        return@launch
                    }
                }
            }
        }
    }

    /**
     * 监控立即申购页面
     */
    private fun monitorSettingNotify() {
        if (TaobaoConstants.HUAWEI_PRODUCT_DETAIL_ACTIVITY != currentClassName) return
        Log.d(TAG, "monitorSettingNotify")
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                if (TaobaoConstants.HUAWEI_PRODUCT_DETAIL_ACTIVITY != currentClassName) return@launch

                val lists = AccessibilityServiceUtils.getElementsById(
                    TaobaoConstants.HUAWEI_SETTING_NOTIFY,
                    rootInActiveWindow
                ) ?: return@launch
                Log.d(TAG, "lists = " + lists.size)
                for (envelope in lists) {
                    Log.d(TAG, "文字--${envelope.text} enable = ${envelope.isEnabled}")
                    AccessibilityHelper.performClick(envelope)
                    isHasReceived = true
                    return@launch
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
            TaobaoConstants.HUAWEI_SINGLE_BUY,
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
