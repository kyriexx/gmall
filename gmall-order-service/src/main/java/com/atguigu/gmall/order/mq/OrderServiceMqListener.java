package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * 消息队列的消费端
 * @author Kyrie
 * @create 2019-11-26 16:14
 */
@Component
public class OrderServiceMqListener {

    @Autowired
    OrderService orderService;

    //消息监听器连接工厂，这里定义的是点对点模式的监听器连接工厂
    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no = mapMessage.getString("out_trade_no");
        // 更新订单状态业务
        System.out.println(out_trade_no);
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(out_trade_no);
        orderService.updateOrder(omsOrder);
    }
}
