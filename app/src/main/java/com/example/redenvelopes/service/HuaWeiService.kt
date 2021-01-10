package com.example.redenvelopes.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.example.redenvelopes.HuaWeiConstants
import com.example.redenvelopes.HuaWeiConstants.RED_ENVELOPE_ID
import com.example.redenvelopes.HuaWeiConstants.RED_ENVELOPE_OPEN_ID
import com.example.redenvelopes.HuaWeiConstants.RED_ENVELOPE_TITLE
import com.example.redenvelopes.HuaWeiConstants.WECHAT_LUCKYMONEYDETAILUI_ACTIVITY
import com.example.redenvelopes.HuaWeiConstants.WECHAT_LUCKYMONEY_ACTIVITY
import com.example.redenvelopes.HuaWeiConstants.WECHAT_PACKAGE
import com.example.redenvelopes.MyApplication
import com.example.redenvelopes.R
import com.example.redenvelopes.activity.MainActivity
import com.example.redenvelopes.data.RedEnvelopePreferences
import com.example.redenvelopes.data.RedEnvelopePreferences.daleyTime
import com.example.redenvelopes.utils.AccessibilityHelper
import com.example.redenvelopes.utils.AccessibilityServiceUtils
import com.example.redenvelopes.utils.AppUtils
import com.example.redenvelopes.utils.WakeupTools
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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


        if (WECHAT_PACKAGE != event.packageName) return
        if (event.className.toString().startsWith(WECHAT_PACKAGE)) {
            currentClassName = event.className.toString()
        }

//        HuaWeiConstants.setVersion(AppUtils.getVersionName(baseContext, WECHAT_PACKAGE))

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                Log.d(TAG, "通知改变" + event.text)
                monitorNotification(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "界面改变$event")
                openRedEnvelope(event)
                quitEnvelope(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (rootInActiveWindow == null)
                    return
                Log.d(TAG, "内容改变")
                monitorChat()
                getAllIdName()

            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "点击")
                getClickContent(event)
                if (rootInActiveWindow == null) {
                    return
                }

                getAllIdName()
                grabRedEnvelope()

            }
        }
    }

    private fun getAllIdName() {
        try {

            for (i in 0 until rootInActiveWindow.childCount) {
                val child = rootInActiveWindow.getChild(i)
                Log.i("wsc", " text = ${child.text} id = ${child.viewIdResourceName}")

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
     * 监控通知是否有红包
     */
    private fun monitorNotification(event: AccessibilityEvent) {
        if (!RedEnvelopePreferences.wechatControl.isMonitorNotification) return
        if (isHasReceived) return
        val texts = event.text
        Log.d(TAG, "检测到微信通知，文本为------------>$texts")
        if (texts.isEmpty())
            return
        if (texts.toString().contains(RED_ENVELOPE_TITLE)) {
            Log.d(TAG, "monitorNotification:红包")
            WakeupTools.wakeUpAndUnlock(applicationContext)
            //以下是精华，将QQ的通知栏消息打开
            val notification = event.parcelableData as Notification
            val pendingIntent = notification.contentIntent
            try {
                Log.d(TAG, "准备打开通知栏")
                pendingIntent.send()
                isHasReceived = true
            } catch (e: PendingIntent.CanceledException) {
                Log.d(TAG, "error:$e")
            }
        }

    }

    /**
     * 监控微信聊天列表页面是否有红包，经测试若聊天页面与通知同时开启聊天页面快
     */
    private fun monitorChat() {
        Log.d(TAG, "monitorChat")
        if (!RedEnvelopePreferences.wechatControl.isMonitorChat) return
        val lists = AccessibilityServiceUtils.getElementsByText(
            "立即购买",
            rootInActiveWindow
        ) ?: return
        Log.d(TAG, "lists" + lists.toString())
        for (envelope in lists) {
            Log.d(TAG, "文字--" + envelope.text)
            if (!envelope.text.isNullOrEmpty()) {
                if (envelope.text.contains("立即购买")) {
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

//        /* 发现红包点击进入领取红包页面 */
//        GlobalScope.launch {
//            val delayTime = 100L + daleyTime//小米10测试数据
//            delay(delayTime)
//                Log.d(TAG, "发现确定按钮：$envelopes")
//
//                AccessibilityHelper.performClick(envelopes)
//                isHasClicked = true
//
////            break
//        }

        isHasReceived = false
    }

    /**
     * 拆开红包
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun openRedEnvelope(event: AccessibilityEvent) {
        if (event.className != WECHAT_LUCKYMONEY_ACTIVITY) return
        GlobalScope.launch {

            val delayTime = 100L + daleyTime//小米10测试数据
            Log.d(TAG, "延时开红包:$daleyTime")
            delay(delayTime)
            var envelopes = AccessibilityServiceUtils.getElementsById(
                RED_ENVELOPE_OPEN_ID,
                rootInActiveWindow
            )
            Log.d(TAG, "拆红包页面:$envelopes")
            if (envelopes != null) {
                if (envelopes.isEmpty()) {
                    openRedEnvelopeNew(event)
                    //            envelopes = rootInActiveWindow.findAccessibilityNodeInfosByViewId(RED_ENVELOPE_CLOSE_ID)
                    //            Log.d(TAG, "拆红包页面1111:$envelopes")
                    //            /* 进入红包页面点击退出按钮 */
                    //            for (envelope in envelopes.reversed()) {
                    //                AccessibilityHelper.performClick(envelope)
                    //            }
                } else {
                    Log.d(TAG, "拆红包页面2222:$envelopes")
                    /* 进入红包页面点击开按钮 */
                    for (envelope in envelopes.reversed()) {
                        GlobalScope.launch {
                            val delayTime =
                                1000L * RedEnvelopePreferences.wechatControl.delayOpenTime
                            Log.d(TAG, "delay open time:$delayTime")
                            delay(delayTime)
                            AccessibilityHelper.performClick(envelope)
                            isHasOpened = true
                            isHasClicked = false
                        }
                    }
                }
            }
        }

    }


    /**
     * 退出红包详情页
     */
    private fun quitEnvelope(event: AccessibilityEvent) {

        Log.d(TAG, "quitEnvelope")
        if (event.className != WECHAT_LUCKYMONEYDETAILUI_ACTIVITY) return
        if (!isHasOpened) return //如果不是点击进来的则不退出

        GlobalScope.launch {
            val delayTime = 1000L * RedEnvelopePreferences.wechatControl.delayCloseTime
            Log.d(TAG, "delay close time:$delayTime")
            if (delayTime != 11000L) {
                delay(delayTime)
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
        isHasOpened = false
    }

    private fun openRedEnvelopeNew(event: AccessibilityEvent) {
        Log.d(TAG, "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
        if (!isHasClicked) return
        if (WECHAT_LUCKYMONEY_ACTIVITY != currentClassName) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "sdk:" + Build.VERSION.SDK_INT)
            val metrics = resources.displayMetrics
            val dpi = metrics.densityDpi
            val path = Path()
            Log.d(TAG, "dpi---:$dpi")
            when (dpi) {
                640 -> //1440
                    path.moveTo(720f, 1575f)
                320 -> //720p
                    path.moveTo(360f, 780f)
                480 -> //1080p
                    path.moveTo(540f, 1465f) //oppo r15,android 9, 小米8 android 9
//                  path.moveTo(540f, 1210f) //小米mix5
                440 -> //1080*2160
                    path.moveTo(540f, 1210f)
                420 -> //420一加5T
                    path.moveTo(540f, 1213f)
                400 ->
                    path.moveTo(550f, 1200f) //华为mate9
                else ->
                    path.moveTo(550f, 1200f)
            }
            val build = GestureDescription.Builder()
            val gestureDescription =
                build.addStroke(GestureDescription.StrokeDescription(path, 500, 100)).build()

            dispatchGesture(gestureDescription, object : GestureResultCallback() {

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "onCompleted")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.d(TAG, "onCancelled")
                }

            }, null)
        }
        isHasOpened = true
        isHasClicked = false
    }
}
