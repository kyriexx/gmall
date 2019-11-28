package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class TestMqQueue {

    public static void main(String[] args) {

        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try {
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);// 开启事务
            Queue testqueue = session.createQueue("drink");// 队列模式的消息

            //Topic t = session.createTopic("");// 话题模式的消息

            MessageProducer producer = session.createProducer(testqueue);
            TextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText("我渴了，谁能帮我打一杯水！");
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(textMessage);
            session.commit();// 提交事务
            connection.close();//关闭链接

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}

/*
关于事务控制

//第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
    Session session = connection.createSession(true, Session.SESSION_TRANSACTED);// 开启事务

producer 提交时的事务
    事务开启	只执行send并不会提交到队列中，只有当执行session.commit()时，消息才被真正的提交到队列中。

	事务不开启，只要执行send，就进入到队列中。

consumer 接收时的事务
    事务开启，签收必须写Session.SESSION_TRANSACTED，收到消息后，消息并没有真正的被消费。消息只是被锁住。
               一旦出现该线程死掉、抛异常，或者程序执行了session.rollback()那么消息会释放，重新回到队列中被别的消费端再次消费。

    事务不开启，签收方式选择Session.AUTO_ACKNOWLEDGE，只要调用comsumer.receive方法，自动确认。

    事务不开启，签收方式选择Session.CLIENT_ACKNOWLEDGE，需要客户端执行 message.acknowledge(),否则视为未提交状态，线程结束后，
                其他线程还可以接收到。这种方式跟事务模式很像，区别是不能手动回滚,而且可以单独确认某个消息。

    事务不开启，签收方式选择Session.DUPS_OK_ACKNOWLEDGE，在Topic模式下做批量签收时用的，可以提高性能。
                但是某些情况消息可能会被重复提交，使用这种模式的consumer要可以处理重复提交的问题。

    int AUTO_ACKNOWLEDGE = 1;
    int CLIENT_ACKNOWLEDGE = 2;
    int DUPS_OK_ACKNOWLEDGE = 3;
    int SESSION_TRANSACTED = 0;
*/
