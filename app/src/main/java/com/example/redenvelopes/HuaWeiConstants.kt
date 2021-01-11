package com.example.redenvelopes


object HuaWeiConstants {
    val Tag = "======="
    val WECHAT_PACKAGE = "com.vmall.client"
    val HUAWEI_PRODUCT_DETAIL_ACTIVITY =
        "$WECHAT_PACKAGE.product.fragment.ProductDetailActivity" //华为产品详情页面
    val WECHAT_LUCKYMONEYDETAILUI_ACTIVITY =
        "$WECHAT_PACKAGE.plugin.luckymoney.ui.LuckyMoneyDetailUI" //微信红包详情页


    var RED_ENVELOPE_ID = "com.vmall.client:id/buy_single" //确定购买按钮
    var RED_ENVELOPE_OPEN_ID = "com.tencent.mm:id/cyf" //抢红包页面点开控件id

    var RED_ENVELOPE_TITLE = "[微信红包]" //红包文字
    var DELAYTIME = 80f

}
