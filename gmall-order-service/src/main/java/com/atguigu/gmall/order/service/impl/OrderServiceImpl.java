package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Kyrie
 * @create 2019-11-20 11:33
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String genTradeCode(String memberId) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";
            String tradeCode = UUID.randomUUID().toString();
            jedis.setex(tradeKey, 60 * 15, tradeCode);
            return tradeCode;
        } finally {
            jedis.close();
        }
    }

    //jedis.eval("lua");可以用lua脚本，在查询到key的同时立即删除该key，防止高并发下的意外的发生
    //意外的发生（是指查询到key后，在删除之前，锁过期了，而此时有其他线程生成锁，再删除锁的话，就删除的不是自己的锁了）
    //String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    //jedis.eval(script, Collections.singletonList("lock"),Collections.singletonList(token));
    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";

            //String tradeCodeFromCache = jedis.get(tradeKey);// 使用lua脚本在发现key的同时将key删除，防止并发订单攻击
            //对比防重删令牌，防止并发的时候一单多提，在redis中发现交易码就删除
            //KEYS[1]是请求携带的key（tradeKey），ARGV[1]是redis中存储的value（tradeCode）

            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

            //eval != null && eval = 0表示tradeCode与redis中的不一样
            //eval != null && eval != 0表示tradeCode与redis中的一样，并且已经将redis中的删除了

            if (eval != null && eval != 0) {
                jedis.del(tradeKey);
                return "success";
            } else {
                return "fail";
            }
            /*
            String tradeCodeFromCache = jedis.get(tradeKey);在redis中发现交易码，但是是回来在这个服务中进行删除，中间有漏洞
            if (StringUtils.isNotBlank(tradeCodeFromCache) && tradeCode.equals(tradeCodeFromCache)) {
                jedis.del(tradeKey);
                return "success";
            } else {
                return "fail";
            }
            */
        } finally {
            jedis.close();
        }
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {

        //保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        //保存订单详情表
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder omsOrder1 = omsOrderMapper.selectOne(omsOrder);
        return omsOrder1;
    }

    @Override
    public void updateOrder(OmsOrder omsOrder) {
        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn", omsOrder.getOrderSn());
        OmsOrder omsOrderUpdate = new OmsOrder();
        omsOrderUpdate.setStatus("1");// 订单状态为1表示已支付

        // 发送一个订单已支付的队列，提供给库存消费
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_pay_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(order_pay_queue);
            TextMessage textMessage=new ActiveMQTextMessage();//字符串文本
            //MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

            // 查询订单的对象，转化成json字符串，存入ORDER_PAY_QUEUE的消息队列
            OmsOrder omsOrderParam = new OmsOrder();
            omsOrderParam.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrderResponse = omsOrderMapper.selectOne(omsOrderParam);
            OmsOrderItem omsOrderItemParam = new OmsOrderItem();
            omsOrderItemParam.setOrderSn(omsOrderParam.getOrderSn());
            List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItemParam);
            omsOrderResponse.setOmsOrderItems(omsOrderItems);

            textMessage.setText(JSON.toJSONString(omsOrderResponse));

            omsOrderMapper.updateByExampleSelective(omsOrderUpdate, example);
            producer.send(textMessage);
            session.commit();
        } catch (Exception ex) {
            try {
                session.rollback();// 消息回滚
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }
    }
}
