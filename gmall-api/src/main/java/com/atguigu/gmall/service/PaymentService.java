package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

/**
 * @author Kyrie
 * @create 2019-11-24 16:38
 */
public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);

    void sendDelayPaymentResultCheckQueue(String outTradeNo, int checkCount);

    Map<String, Object> checkAlipayPayment(String out_trade_no);
}
