package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

/**
 * @author Kyrie
 * @create 2019-11-20 11:36
 */
public interface OrderService {
    String genTradeCode(String memberId);

    String checkTradeCode(String memberId, String tradeCode);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);

    void updateOrder(OmsOrder omsOrder);
}
