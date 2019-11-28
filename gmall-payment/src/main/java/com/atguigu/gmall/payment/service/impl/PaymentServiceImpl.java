package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kyrie
 * @create 2019-11-24 16:39
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        //必须保证每个订单只有唯一的支付信息，所以如果之前已经有了该笔订单的支付信息，那么只更新时间
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(paymentInfo.getOrderId());

        PaymentInfo paymentInfoExists = paymentInfoMapper.selectOne(paymentInfoQuery);
        if (paymentInfoExists != null) {
            paymentInfoExists.setCreateTime(new Date());
            paymentInfoMapper.updateByPrimaryKey(paymentInfoExists);
            return;
        }

        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        // 幂等性检查
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfoParam);
        String paymentStatus = paymentInfoResult.getPaymentStatus();
        if (StringUtils.isNotBlank(paymentStatus) && paymentStatus.equals("已支付")) {
            return;
        } else {
            //createCriteria()修改条件正则表达式,andEqualTo根据什么(orderSn)修改
            Example example = new Example(PaymentInfo.class);
            example.createCriteria().andEqualTo("orderSn", paymentInfo.getOrderSn());

            // 支付成功后，引起的系统服务-》订单服务的更新-》库存服务-》物流服务

            //Selective表示空值不修改，不空的才会修改
            //(paymentInfo,example)表示根据example的条件修改成paymentInfo的样子
            paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
            // 调用mq发送支付成功的消息
            sendPaymentResult(paymentInfo.getOrderSn());
        }
    }

    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, int checkCount) {
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_check_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payment_check_queue);
            MapMessage mapMessage = new ActiveMQMapMessage();// hash结构
            mapMessage.setString("out_trade_no", outTradeNo);
            mapMessage.setInt("checkCount", checkCount);
            // 为消息加入延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000 * 60);
            producer.send(mapMessage);
            session.commit();
        } catch (JMSException e) {
            try {
                session.rollback();// 消息回滚
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, Object> checkAlipayPayment(String out_trade_no) {

        Map<String, Object> resultMap = new HashMap<>();

        //alipay.trade.query(统一收单线下交易查询)
        AlipayTradeQueryRequest queryrequest = new AlipayTradeQueryRequest();
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", out_trade_no);
        String param = JSON.toJSONString(map);
        queryrequest.setBizContent(param);
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(queryrequest);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("交易已创建，调用成功");
            resultMap.put("out_trade_no", response.getOutTradeNo());
            resultMap.put("trade_no", response.getTradeNo());
            resultMap.put("trade_status", response.getTradeStatus());
            resultMap.put("call_back_content", response.getMsg());
        } else {
            System.out.println("有可能交易未创建，调用失败");
        }
        return resultMap;
    }

    public void sendPaymentResult(String outTradeNo) {
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue paymentResultQueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no", outTradeNo);
            //mapMessage.setString("result",result);
            MessageProducer producer = session.createProducer(paymentResultQueue);
            producer.send(mapMessage);
            session.commit();
            //producer.close();
            //session.close();
        } catch (JMSException e) {
            try {
                session.rollback();// 消息回滚
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

//    @Transactional
//    @Override
//    public void updatePayment(PaymentInfo paymentInfo) {
//        //createCriteria()修改条件正则表达式
//        //andEqualTo根据什么(orderSn)修改
//        Example example = new Example(PaymentInfo.class);
//        example.createCriteria().andEqualTo("orderSn", paymentInfo.getOrderSn());
//
//        Connection connection = null;
//        Session session = null;
//        // 支付成功后，引起的系统服务-》订单服务的更新-》库存服务-》物流服务
//        // 调用mq发送支付成功的消息
//        try {
//            //Selective表示空值不修改，不空的才会修改
//            //(paymentInfo,example)表示根据example的条件修改成paymentInfo的样子
//            paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
//
//            connection = activeMQUtil.getConnectionFactory().createConnection();
//            connection.start();
//            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
//            session = connection.createSession(true, Session.SESSION_TRANSACTED);
//            Queue payment_success_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
//            MessageProducer producer = session.createProducer(payment_success_queue);
//            //TextMessage textMessage=new ActiveMQTextMessage();//字符串文本
//            MapMessage mapMessage = new ActiveMQMapMessage();// hash结构
//            mapMessage.setString("out_trade_no", paymentInfo.getOrderSn());
//            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
//            producer.send(mapMessage);
//            session.commit();// 提交事务
//            // producer.close();
//            // session.close();
//            // connection.close();
//        } catch (JMSException e) {
//            try {
//                // 消息回滚
//                session.rollback();
//            } catch (JMSException e1) {
//                e1.printStackTrace();
//            }
//            e.printStackTrace();
//        } finally {
//            try {
//                //关闭链接
//                connection.close();
//            } catch (JMSException e1) {
//                e1.printStackTrace();
//            }
//        }
//    }

}
