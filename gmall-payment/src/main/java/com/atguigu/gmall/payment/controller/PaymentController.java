package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kyrie
 * @create 2019-11-23 14:30
 */
@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        modelMap.put("nickname", nickname);
        modelMap.put("outTradeNo", outTradeNo);
        modelMap.put("totalAmount", totalAmount);
        return "index";
    }

    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap) {

        // alipay.trade.page.pay 统一收单下单并支付页面接口
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 回调函数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", outTradeNo);
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", 0.01);
        map.put("subject", "APPLE iPhone 11 Pro Max 1");
        String param = JSON.toJSONString(map);

        alipayRequest.setBizContent(param);

        // 获得一个支付宝请求的客户端(它并不是一个链接，而是一个封装好的http的表单请求)
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 生成并且保存用户的支付信息
        OmsOrder omsOrder = orderService.getOrderByOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject("谷粒商城商品一件");
        paymentInfo.setTotalAmount(totalAmount);
        paymentService.savePaymentInfo(paymentInfo);

        /*
        延迟队列的应用场景：
        当用户选择支付后，通常来说用户都会在支付宝正常支付，支付宝转账成功后，通过后台异步发送成功的请求到电商支付模块。
        但是如果用户点击支付后，支付模块可能会长时间没有收到支付宝的支付成功通知。这种情况会有两种可能性，
        一种是用户在弹出支付宝付款界面时没有继续支付，另一种就是用户支付成功了，但是因为网络等各种问题，支付模块没有收到通知。
        如果是上述第二种可能性，对于用户来说体验是非常糟糕的，甚至会怀疑平台的诚信。
        所以为了尽可能避免第二种情况，在用户点击支付后一段时间后，不管用户是否付款，都要去主动询问支付宝，该笔单据是否付款

        支付模块一旦帮助用户重定向到支付宝后，就要每隔一段时间询问支付宝用户是否支付成功，直到收到支付宝的回复，或者超过了询问次数

        实现思路
        首先，需要知道如何主动查询支付宝中某笔交易的状态。支付宝查询接口文档：https://docs.open.alipay.com/api_1/alipay.trade.query
        其次，利用延迟队列反复调用
        */

        // 向消息中间件发送一个检查支付状态(支付服务消费)的延迟消息队列
        paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);

        // 提交请求到支付宝
        return form;
    }

    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String aliPayCallBackReturn(HttpServletRequest request, ModelMap modelMap) {

        // 回调请求中获取支付宝参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String call_back_content = request.getQueryString();

        // 异步回调有两个重要的职责：确认并记录用户已付款，通知电商模块。
        // 新版本的支付接口已经取消了同步回调的支付结果传递。所以用户付款成功与否全看异步回调
        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if (StringUtils.isNotBlank(sign)) {
            // 验签成功
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);// 支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            // 更新用户的支付状态
            paymentService.updatePayment(paymentInfo);
        }

        return "finish";
    }

    @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    public String mx(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap) {

        return null;
    }
}
