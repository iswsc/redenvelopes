package com.example.redenvelopes


object HuaWeiConstants {
    val Tag = "======="
    val WECHAT_PACKAGE = "com.vmall.client"
    val HUAWEI_PRODUCT_DETAIL_ACTIVITY =
        "$WECHAT_PACKAGE.product.fragment.ProductDetailActivity" //华为产品详情页面
    val HUAWEI_SUBMIT_ORDER_ACTIVITY =
        "$WECHAT_PACKAGE.base.fragment.SinglePageActivity" //提交订单页面


    var RED_ENVELOPE_ID = "com.vmall.client:id/buy_single" //确定购买按钮
    var RED_ENVELOPE_OPEN_ID = "com.tencent.mm:id/cyf" //抢红包页面点开控件id

    var RED_ENVELOPE_TITLE = "[微信红包]" //红包文字
    var DELAYTIME = 80f

}
