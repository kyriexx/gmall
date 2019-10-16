#kyrie

#######gmall第一次在idea上进行git提交

gmall-user-service用户服务的service层是8070
gmall-user-web用户服务的web层是8080

gmall-manage-service用户服务的service层是8071
gmall-manage-web用户服务的web层是8081

####一、gmall-user demo阶段（10.15）
1 新建module（gmall-user用户服务是8080），选择springweb、mysql驱动、jdbc、mybatis
springboot配置文件：
    # 服务端口
    server.port=8080
    # jdbc
    spring.datasource.password=123456
    spring.datasource.username=root
    spring.datasource.url=jdbc:mysql://localhost:3306
    # mybtais配置
    mybatis.mapper-locations=classpath:mapper/*Mapper.xml
    mybatis.configuration.map-underscore-to-camel-case=true
    # 日志级别，如果想查查看执行的sql，可以将日志级别改为debug，打印sql语句
    logging.level.root=debug


2 配置域名
1）、配置本机的dns
C:\Windows\System32\drivers\etc\hosts
127.0.0.1 localhost user.gmall.com cart.gmall.com manage.gmall.com www.gmall.com
可以直接访问http://user.gmall.com:8080/index 相当于访问了http://localhost:8080/index
2）、可以通过nginx去代理端口号

upstream user.gmall.com{
       server 127.0.0.1:8080;
    }
   server {
     listen 80;
     server_name user.gmall.com;
     location / {
        proxy_pass http://user.gmall.com;
        proxy_set_header X-forwarded-for $proxy_add_x_forwarded_for;
     }
}

3 通用mapper的整合(可以将单表的增删改查操作省去)
1）、在pom.xml文件中，加入
<!-- 通用mapper -->
<dependency>
    <groupId>tk.mybatis</groupId>
    <artifactId>mapper-spring-boot-starter</artifactId>
<version>1.2.3</version>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </exclusion>
    </exclusions>
</dependency>

2）、配置mapper，继承通用mapper
   public interface UserMapper extends Mapper<UmsMember>
   
3）、 配置通用mapper的主键和主键返回策略
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   
4）、 配置启动类扫描器MapperScan，使用通用mapper的tk……..MapperScan
   tk.mybatis.spring.annotation.MapperScan
   
####二、商城的架构（10.16）
1 gmall-parent父依赖的创建
1）、用maven创建一个gmall-parent的工程
2）、新建其他项目模块(子项目)的时候，继承自gmall-parent
3）、gmall-parent中父依赖使用springboot1.5
  <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>1.5.21.RELEASE</version>
      <relativePath/> <!-- lookup parent from repository -->
  </parent>
  
  <groupId>com.atguigu.gmall</groupId>
  <artifactId>gmall-parent</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
4）、在gmall-parent中定义好项目的技术框架各种版本

2 抽取api工程（负责管理项目中所有的接口和bean）
1）、首先用maven创建一个gmall-api的工程
2）、引入tk通用mapper(映射类)
3）、将XXXService接口和所有的bean类都放到api中
4）、service、service实现、controller、mapper、mapper.xml中所有的bean的引入全部修改引入路径

3 抽取util工程
1）、项目中的通用框架，是所有应用工程需要引入的包
例如：springboot、common-langs、common-beanutils

<dependencies>
    <!--测试(springboot有默认版本号)-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
    </dependency>
    <!--内含tomcat容器、HttpSevrletRequest等(springboot有默认版本号)-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!--json工具-->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
    </dependency>
    <!--restful调用客户端-->
    <dependency>
        <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
    </dependency>
    <!--方便好用的apache工具库-->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
    <!--方便好用的apache处理实体bean工具库-->
    <dependency>
        <groupId>commons-beanutils</groupId>
        <artifactId>commons-beanutils</artifactId>
    </dependency>
    <!--方便好用的apache解码工具库-->
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
    </dependency>
</dependencies>

2）、基于soa的架构理念，项目分为web前端controller(webUtil)
Jsp、thymeleaf、cookie工具类
加入commonUtil
<dependencies>
    <dependency>
        <groupId>com.atguigu.gmall</groupId>
        <artifactId>gmall-common-util</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>

3）、基于soa的架构理念，项目分为web后端service(serviceUtil)
Mybatis、mysql、redis
加入commonUtil
<dependencies>
    <dependency>
        <groupId>com.atguigu.gmall</groupId>
        <artifactId>gmall-common-util</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
    </dependency>
</dependencies>

即：新建一个web的前端controller模块的项目
Controller = parent + api + webUtil(commonUtil+Jsp、thymeleaf、cookie)
    新建一个web的后端service模块的项目
service = parent + api + serviceUtil(commonUtil+Mybatis、jdbc、mysql、redis)

4 soa面向服务(以dubbo为基础，dubbo框架是通过dubbo协议利用注册中心的客户端通过dubbo协议访问服务的，
注册中心的客户端负责实时的同步注册中心服务信息，dubbo框架是把这个服务发布成dubbo协议互相访问)
1）、dubbo的soa的工作原理，和springcloud类似
2）、dubbo和springcloud的区别在于dubbo由自己的dubbo协议通讯，springcloud是由http协议(rest风格)
3）、dubbo有一个注册中心的客户端在时时同步注册中心的服务信息
4）、dubbo有一个javaweb的监控中心，负责监控服务的注册信息，甚至可以配置负载均衡

补充Linux命令：
    ls:查看目录（如果显示为红色表示没有权限）
    ls -lrt:查看目录以及权限时间信息
        -a 显示所有文件及目录 (ls内定将文件名或目录名称开头为"."的视为隐藏档，不会列出) 
        -l 除文件名称外，亦将文件型态、权限、拥有者、文件大小等资讯详细列出 
        -r 将文件以相反次序显示(原定依英文字母次序) 
        -t 将文件依建立时间之先后次序列出 
        -A 同 -a ，但不列出 "." (目前目录) 及 ".." (父目录) 
        -F 在列出的文件名称后加一符号；例如可执行档则加 "*", 目录则加 "/" 
        -R 若目录下有文件，则以下之文件亦皆依序列出
    chmod 777 文件名:打开文件的所有（777）权限
    vi命令：
        shift+g 来到文件最后一行

5 将dubbo框架引入到项目中
  启动监控中心
1）、将dubbo监控中心（dubbo-admin-2.6.0.war）和tomct上传只linux服务器/opt目录
2）、用unzip命令解压dubbo-admin.war监控中心
  Unzip file文件名 -d 解压路径（unzip dubbo-admin-2.6.0.war -d dubbo）
3）、配置tomcat的server.xml配置文件，访问192.168.2.125:8080/dubbo就是访问/opt/dubbo项目
  <Context path="/dubbo" docBase="/opt/dubbo" debug="0" privileged="true" />
4）、启动tomcat，打开监控中心，访问192.168.2.125:8080/dubbo，用户名和密码都是root

6 centos7的/opt目录下安装
安装jdk
解压：tar -zxvf jdk-8u231-linux-x64.tar.gz
设置环境变量：vim /etc/profile，在最前面加上
    export JAVA_HOME=/opt/jdk1.8.0_231
export JRE_HOME=${JAVA_HOME}/jre  
export CLASSPATH=.:${JAVA_HOME}/lib:${JRE_HOME}/lib  
export  PATH=${JAVA_HOME}/bin:$PATH
JAVA_HOME的值为jdk的解压目录
执行profile文件：source /etc/profile

安装tomcat 
解压：tar -zxvf apache-tomcat-8.5.46.tar.gz
启动：cd apache-tomcat-8.5.46/bin/ 目录下执行./start
查看tomcat日志：cd /opt/apache-tomcat-8.5.46/logs/ 目录下执行vi catalina.out

安装zookeeper
解压：tar -zxvf zookeeper-3.4.14.tar.gz
配置：cd /opt/zookeeper-3.4.14/conf
      cp zoo_sample.cfg zoo.cfg（复制一份）
      在zookeeper安装目录下新建data数据目录，mkdir data即/opt/zookeeper-3.4.14/data
      修改zoo.cfg：dataDir=/opt/zookeeper-3.4.14/data
启动zookeeper：在/opt/zookeeper-3.4.14/bin目录下：
               ./zkServer.sh start 启动主进程
               ./zkServer.sh status 启动status进程


设置监控中心（tomcat）的开启自启动：
    来到/etc/init.d目录，这是Linux开机需要扫描的服务脚本目录，开机后扫描这些脚本，按脚本的指示去启动服务
    vim /etc/init.d/dubbo-admin，写入如下脚本：
        #!/bin/bash
        #chkconfig:2345 20 90
        #description:dubbo-admin
        #processname:dubbo-admin
        CATALANA_HOME=/opt/apache-tomcat-8.5.46
        export JAVA_HOME=/opt/jdk1.8.0_231
        case $1 in
        start)  
            echo "Starting Tomcat..."  
            $CATALANA_HOME/bin/startup.sh  
            ;;   
        stop)  
            echo "Stopping Tomcat..."  
            $CATALANA_HOME/bin/shutdown.sh  
            ;;   
        restart)  
            echo "Stopping Tomcat..."  
            $CATALANA_HOME/bin/shutdown.sh  
            sleep 2  
            echo  
            echo "Starting Tomcat..."  
            $CATALANA_HOME/bin/startup.sh  
            ;;  
        *)  
            echo "Usage: tomcat {start|stop|restart}"  
            ;; esac
然后注册进入到服务中，chkconfig --add dubbo-admin
加入权限：chmod 777 dubbo-admin
启动服务：service dubbo-admin start

设置zookeeper的开启自启动：
   来到/etc/init.d目录，这是Linux开机需要扫描的服务脚本目录，开机后扫描这些脚本，按脚本的指示去启动服务
    vim /etc/init.d/dubbo-admin，写入如下脚本：
        #!/bin/bash
        #chkconfig:2345 20 90
        #description:zookeeper
        #processname:zookeeper
        ZK_PATH=/opt/zookeeper-3.4.14
        export JAVA_HOME=/opt/jdk1.8.0_231
        case $1 in
                 start) sh  $ZK_PATH/bin/zkServer.sh start;;
                 stop)  sh  $ZK_PATH/bin/zkServer.sh stop;;
                 status) sh  $ZK_PATH/bin/zkServer.sh status;;
                 restart) sh $ZK_PATH/bin/zkServer.sh restart;;
                 *)  echo "require start|stop|status|restart"  ;;
        esac
然后注册进入到服务中，chkconfig --add zookeeper
加入权限：chmod 777 zookeeper
启动服务：service zookeeper start

7 将项目改造为dubbo的分布式架构
1）、将user项目拆分成user-service和user-web
2）、 引入dubbo框架
将dubbo框架引入到common-util中(因为web层和service层将来都需要使用dubbo进行通讯，依赖版本均在parent统一设置)
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dubbo</artifactId>
</dependency>

<dependency>
    <groupId>com.101tec</groupId>
    <artifactId>zkclient</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>com.gitee.reger</groupId>
    <artifactId>spring-boot-starter-dubbo</artifactId>
</dependency>
3）、刷新maven依赖
4）、配置consumer和service
服务端增加@Service（dubbo的）
#服务端口
server.port=8070

#######jdbc
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.url=jdbc:mysql://localhost:3306/gmall?characterEncoding=UTF-8

#######mybtais配置
mybatis.mapper-locations=classpath:mapper/*Mapper.xml
mybatis.configuration.map-underscore-to-camel-case=true

#######日志级别
logging.level.root=debug

#######dubbo的配置
#######dubbo中的服务名称
spring.dubbo.application=user-service
#######dubbo的通讯协议名称
spring.dubbo.protocol.name=dubbo
#######zookeeper注册中心的地址
spring.dubbo.registry.address=192.168.2.125:2181
#######zookeeper的通讯协议的名称
spring.dubbo.registry.protocol=zookeeper
#######dubbo的服务的扫描路径
spring.dubbo.base-package=com.atguigu.gmall

客户端增加@Reference代替@Autowired
#######服务端口
server.port=8080

#######日志级别
logging.level.root=debug

#######dubbo的配置
#######dubbo中的服务名称
spring.dubbo.application=user-web
#######dubbo的通讯协议名称
spring.dubbo.protocol.name=dubbo
#######zookeeper注册中心的地址
spring.dubbo.registry.address=192.168.2.125:2181
#######zookeeper的通讯协议的名称
spring.dubbo.registry.protocol=zookeeper
#######dubbo的服务的扫描路径
spring.dubbo.base-package=com.atguigu.gmall
#######设置超时时间，便于调试
spring.dubbo.consumer.timeout=600000
#######设置是否检查服务存在
spring.dubbo.consumer.check=false


8 dubbo配置的注意事项
1）、spring的@Service改为dubbo的@Service
2）、将@Autowired改为@Reference
3）、dubbo在进行dubbo协议通讯时，需要实现序列化接口(封装的数据对象bean)
4）、dubbo的consumer在三秒钟之内每间隔一秒进行一次重新访问，默认一秒钟超时，三次访问之后会直接抛超时异常，所以我们在开发阶段，可以将consumer设置的超时时间延长，方便断点调试
#######设置超时时间
spring.dubbo.consumer.timeout=600000
#######设置是否检查服务存在
spring.dubbo.consumer.check=false


####三、商城manage和前后端分离(10.16晚)
1 pms商品管理系统的介绍
1）、系统名称
Gmall-Manage
2）、数据结构
pms
sku + spu
sku : stock keeping unit 库存存储单元(一般只一个具体的库存商品，单位时台、部、件)
spu : standard product unit 标准的商品单元(一般一个商品XXX系列，就是一个spu)

2 manage系统前后端分离
1）、准备阶段
渲染、模板技术
2）、前后端分离
Jvm spring maven idea
Nodejs vue npm vscode
3）、安装nodejs
下一步。。。
Node -v查看安装结果
4）、用npm install命令安装npm
5）、解压前端项目(gmall-admin)
config目录下
配置前端服务的ip和前端访问数据的后端的服务的ip地址
dev.env.js 前端访问后端的数据服务的地址
index.js 前端的服务器端口
6）、用npm命令编译和启动前端项目
在gmall-admin目录下运行
npm run dev
7）、前后端请求格式
一般前端会用post想后端发送请求(把参数封装到json中)
请求格式@RequestBody、返回格式@ResponseBody
































