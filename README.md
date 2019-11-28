#kyrie

#######gmall第一次在idea上进行git提交

gmall-user-service用户服务的service层是8070
gmall-user-web用户服务的web层是8080

gmall-manage-service用户服务的service层是8071
gmall-manage-web用户服务的web层是8081

gmall-item-service前台的商品详情服务 8072(调用gmall-manage-service)
gmall-item-web前台的商品详情展示 8082

gmall-search-service 搜索服务的后台 8073
gmall-search-web 搜索服务的前台 8083

gmall-cart-service 搜索服务的后台 8074
gmall-cart-web 搜索服务的前台 8084

gmall-user-service 用户服务的service层8070
gmall-passport-web 用户认证中心 8085

gmall-order-service 订单服务的后台 8076
gmall-order-web 订单服务的前台 8086

gmall-payment 支付服务 8087(将支付模块的service 和web工程使用一个模块)


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
SPU(Standard Product Unit)：标准化产品单元。是商品信息聚合的最小单位，是一组可复用、易检索的
    标准化信息的集合，该集合描述了一个产品的特性。
SKU=Stock Keeping Unit（库存量单位）。即库存进出计量的基本单元，可以是以件，盒，托盘等为单位。
    SKU这是对于大型连锁超市DC（配送中心）物流管理的一个必要的方法。现在已经被引申为产品统一编号的简称，
    每种产品均对应有唯一的SKU号。

比如，咱们购买一台iPhoneX手机，iPhoneX手机就是一个SPU，但是你购买的时候，
不可能是以iPhoneX手机为单位买的，商家也不可能以iPhoneX为单位记录库存。
必须要以什么颜色什么版本的iPhoneX为单位。比如，你购买的是一台银色、128G内存的、支持联通网络的iPhoneX ，
商家也会以这个单位来记录库存数。那这个更细致的单位就叫库存单元（SKU）。


2 manage系统前后端分离
1）、准备阶段,渲染、模板技术
2）、前后端分离
Jvm spring maven idea
Nodejs vue npm vscode
3）、安装nodejs
一直点下一步。。。
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
一般前端会用post向后端发送请求(把参数封装到json中)
请求格式@RequestBody、返回格式@ResponseBody

####四、商城的商品录入功能(manage)(10.17)
1）、三级分类的查询
2）、商品的平台属性列表的增删改查
3）、商品spu的添加
Spu列表查询，spu的销售属性、属性值、Fastdfs图片上传。
4）、商品sku的添加，sku信息、sku关联的销售属性、sku关联的平台属性、sku图片

1 sku的结构  pms_sku_
2 spu的结构  pms_spu_
3类目的结构 pms_catalog_
三级分类，一级二级三级
4属性的结构 pms_attr_
平台属性的外键是三级分类id，在使用平台属性功能之前必须选择三级分类

5 商品分类的查询
1）、新建gmall-manage-web项目
2）、配置gmall-manage-web
3）、写一个getCatalog1写给前端项目调用
4）、返回一个分类列表的集合(josn)
5）、新建一个catalogService的服务
6）、实现cagalogService的功能，新建mapper

6 前后端的跨域问题
1）、前端127.0.0.1:8888
2）、后端127.0.0.1:8080
前端和后端因为来自不同的网域，所以在http的安全协议策略下，不信任
3）、解决方案，在springmvc的控制层加入@CrossOrigin跨域访问的注解

####五、商城的商品录入功能(manage)(10.18)
1 商品的平台属性列表的增删改查
pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
    //通用mapper中insert insertSelective的区别 是否将null插入数据库
    //insertSelective只将非空的有值的插入数据库，而那些无值的为null的不插入
    //insert是都会插
    //一般用insertSelective
在PmsBaseAttrInfo中设置主键返回策略，和@Options(useGeneratedKeys = true,keyProperty = "id")
public class PmsBaseAttrInfo implements Serializable {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    private String id;
    
（//使用自动生成的主键，并且告诉mybatis Department的id属性用来封装主键
     //插入完Department后，主键会封装到Department对象，返回
     @Options(useGeneratedKeys = true,keyProperty = "id")
     @Insert("insert into department(departmentName) values(#{departmentName})")
     public int insertDept(Department department);）

2 平台属性的修改操作
1）、根据平台属性id判断，有id是修改操作，没有id是添加操作
2）、修改操作
A 先修改平台属性
// 属性修改
Example example = new Example(PmsBaseAttrInfo.class);
example.createCriteria().andEqualTo("id",pmsBaseAttrInfo.getId());
pmsBaseAttrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo,example);
B 修改平台属性值
// 属性值修改
// 按照属性id删除所有属性值
PmsBaseAttrValue pmsBaseAttrValueDel = new PmsBaseAttrValue();
pmsBaseAttrValueDel.setAttrId(pmsBaseAttrInfo.getId());
pmsBaseAttrValueMapper.delete(pmsBaseAttrValueDel);

// 删除后，将新的属性值插入
List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
    pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
}
3）、进入修改页面，调用http://127.0.0.1:8081/getAttrValueList?attrId=43
实现getAttrValueList的方法，为修改页面查询一个平台属性值的集合

3 spu的功能
1）、http://127.0.0.1:8081/spuList?catalog3Id=61
2）、spu数据列表的查询
3）、spu的添加功能

4 spu 的添加功能

####六、图片存储服务fastDFS(10.19)
1 安装libfastcommon（/opt目录下）
1.1 上传压缩包文件libfastcommonV1.0.7.tar.gz 到 /opt目录下，并解压。
1.2 tar -zxvf libfastcommonV1.0.7.tar.gz

1.3 进入到解压后的文件夹中,cd libfastcommon-1.0.7/
1.4	进行编译 ./make.sh
1.4.1	如果出现编译perl 不识别 运行下面这段命令
1.4.2	# yum -y install zlib zlib-devel pcre pcre-devel gcc gcc-c++ openssl openssl-devel libevent libevent-devel perl unzip net-tools wget
1.5	安装 ./make.sh install
1.6	注意：libfastcommon安装好后会自动将库文件拷贝至/usr/lib64下，
         由于FastDFS程序引用usr/lib目录所以需要将/usr/lib64下的库文件拷贝至/usr/lib下。
         cp /usr/lib64/libfastcommon.so /usr/lib/


Spring 事务管理

事务是指访问并可能更新数据库中各种数据项的一个程序执行单元。事务可以是一条 SQL 语句或者一组 SQL 语句。

事务具有四个基本特性（ACID）：

原子性（Atomicity）:一个事务是一个不可分割的工作单位，事务中包括的操作要么一起成功，要么一起失败；

一致性（Consistency）：事务必须使数据库从一个一致性状态变换到另一个一致性状态，
    也就是说一个事务执行之前和执行之后都必须处于一致性状态；

隔离性（Isolation）：一个事务的执行不能被其他事务干扰。比如多个用户操作同一张表时，数据库为每一个用户开启的事务，
    不能被其他事务的操作所干扰，多个并发事务之间要相互隔离；

持久性（Durability）：持久性是指一个事务一旦被提交了，那么对数据库中的数据的改变就是永久性的，
    即便是在数据库系统遇到故障的情况下也不会丢失提交事务的操作。

关于事务的更多详细内容请查看相关文档，这里主要说下使用方法。
使用方法
1.之前在 spring-mybatis.xml 配置文件中已配置事务管理，如下：
<!-- 事务管理 -->
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource"/>
</bean>

<tx:annotation-driven transaction-manager="transactionManager"/>
代码解读如下：
（1）将 DataSourceTransactionManager 交给 Spring 管理，注入数据源 dataSource；
（2）开启事务注解支持。

2.在 service.impl 包下的实现类的增、删、改方法上添加 @Transactional 注解，表示开启事务，注意导入的包是下面这个：
import org.springframework.transaction.annotation.Transactional;

3.使用的数据库存储引擎要支持事务。
查看当前 MySQL 当前存储引擎，打开 Navicat -> 选择数据库 -> 点击查询 -> 新建查询 -> 输入如下查询语句 -> 运行：
show variables like '%storage_engine%'
-- 如果是 InnoDB 说明是支持事务的。如果不是，需要配置下，在配置文件 my.ini 中的 [mysqld] 下面加上如下配置：
-- default-storage-engine=INNODB

-- 如果不知道 my.ini 在哪，可以按下 WIN+R 组合键，在运行窗口中输入 services.msc，打开系统服务，找到 MySQL 服务，然后右键属性，可查看 my.ini 的路径

-- MySQL 事务默认是自动提交的（Autocommit），需要将自动提交关闭。
-- 同样方法输入 SQL 语句后运行，关闭自动提交：set autocommit=0;  

-- 查看自动提交是否已经关闭：select @@autocommit;
-- 如果查询结果是0，说明自动提交已经关闭

SELECT
	sa.id AS sa_id,
	sav.id AS sav_id,
	sa.*,
	sav.*,
IF ( ssav.sku_id, 1, 0 ) AS isChecked 
FROM
	pms_product_sale_attr sa
	INNER JOIN pms_product_sale_attr_value sav ON sa.product_id = sav.product_id 
	AND sa.sale_attr_id = sav.sale_attr_id 
	AND sa.product_id = 68
	LEFT JOIN pms_sku_sale_attr_value ssav ON sav.id = ssav.sale_attr_value_id 
	AND ssav.sku_id = 106


1、缓存穿透：
缓存穿透是指查询一个一定不存在的数据，由于缓存是不命中，将去查询数据库，但是数据库也无此记录，并且处于容错考虑，
我们没有将这次查询的null写入缓存，这将导致这个不存在的数据每次请求都要到存储层去查询，失去了缓存的意义。
在流量大时，可能DB就挂掉了，要是有人利用不存在的key频繁攻击我们的应用，这就是漏洞。

解决：
空结果进行缓存，但它的过期时间会很短，最长不超过五分钟。
jedis.setex("sku:" + skuId + ":info", 60 * 3, JSON.toJSONString(""));

2、缓存雪崩：
缓存雪崩是指在我们设置缓存时采用了相同的过期时间，导致缓存在某一时刻同时失效，请求全部转发到DB，DB瞬时压力过重雪崩。

解决：
原有的失效时间基础上增加一个随机值，比如1-5分钟随机，这样每一个缓存的过期时间的重复率就会降低，就很难引发集体失效的事件。

3、缓存击穿
对于一些设置了过期时间的key，如果这些key可能会在某些时间点被超高并发地访问，是一种非常“热点”的数据。
这个时候，需要考虑一个问题：如果这个key在大量请求同时进来前正好失效，那么所有对这个key的数据查询都落到db，我们称为缓存击穿。

和缓存雪崩的区别：
	击穿是一个热点key失效
	雪崩是很多key集体失效
缓存在某个时间点过期的时候，恰好在这个时间点对这个Key有大量的并发请求过来，这些请求发现缓存过期一般都会从后端DB加载数据并回设到缓存，
这个时候大并发的请求可能会瞬间把后端DB压垮。
解决：
分布式锁

redisson：java用来控制redis的一个各种各样工具的集合，其中包括分布式锁和多线程


Windows中查看80端口有没有被占用，netstat -ano | findstr "80"


	设置IP地址
vi /etc/sysconfig/network-scripts/ifcfg-ens33
ONBOOT=yes
IPADDR=192.168.2.125
GATEWAY=192.168.2.1
BROADCAST=192.168.2.255
DNS1=8.8.8.8
DNS2=114.114.114.114

Ik(中英文分词器)有两个：
1 ik_smart（简易分词）
2 ik_max_word（尽最大可能分词）

GET _analyze
{
  "analyzer": "ik_smart", 
  "text": "我是中国人"
}

GET _analyze
{
  "analyzer": "ik_max_word", 
  "text": "我是中国人"
}


es的集群
新建一个索引：
    索引中的数据分别放在不同机器中的不同分片上，尽量分散放；
    每个机器上的片在其他机器上都至少有2到3个复制片。提高集群的容错率。

1 节点
一个节点就是一个es的服务器，es集群中，主节点负责集群的管理和任务的分发，一般不负责文档的增删改查
2 分片
分片是es的实际物理存储单元(一个lucene的实例)
3 索引
索引是es的逻辑单元，一个索引一般建立在多个不同机器的分片上
4 复制片
每个机器的分片一般在其他机器上会有两到三个复制片(目的是提高数据的容错率)
5 容错
一旦集群中的某些机器发生故障，那么剩余的机器会在主机点的管理下，重新分配资源(分片)
6 分片的路由
写操作(新建、删除)只在主分片上进行，然后将结果同步给复制分片
sync 主分片同步给复制成功后，才返回结果给客户端
async 主分片在操作成功后，在同步复制分片的同时返回成功结果给客户端
读操作(查询)可以在主分片或者复制分片上进行


当索引创建完成的时候，主分片的数量就固定了，但是复制分片的数量可以随时调整。

创建分片：
PUT /blogs
{
   "settings" : {
      "number_of_shards" : 3,
      "number_of_replicas" : 1
   }
}

增加副分片：
PUT /blogs/_settings
{
   "number_of_replicas" : 2
}
主分片设置后不能进行修改，只能修改副本分片

集群的健康状态yellow表示所有的主分片(primary shards)启动并且正常运行了——
集群已经可以正常处理任何请求——但是复制分片(replica shards)还没有全部可用。
事实上所有的三个复制分片现在都是unassigned状态——它们还未被分配给节点。
在同一个节点上保存相同的数据副本是没有必要的，如果这个节点故障了，那所有的数据副本也会丢失。



路由
当你索引一个文档，它被存储在单独一个主分片上。Elasticsearch是如何知道文档属于哪个分片的呢？
当你创建一个新文档，它是如何知道是应该存储在分片1还是分片2上的呢？
进程不能是随机的，因为我们将来要检索文档。
算法决定：shard = hash(routing) % number_of_primary_shards

routing值是一个任意字符串，它默认是_id但也可以自定义。

为什么主分片的数量只能在创建索引时定义且不能修改？
如果主分片的数量在未来改变了，所有先前的路由值就失效了，文档也就永远找不到了。

所有的文档API（get、index、delete、bulk、update、mget）都接收一个routing参数，
它用来自定义文档到分片的映射。自定义路由值可以确保所有相关文档——
例如属于同一个人的文档——被保存在同一分片上。


Mapping
	作用：
定义数据库中的表的结构的定义，通过mapping来控制索引存储数据的设置
a.	定义Index下的字段名（Field Name）
b.	定义字段的类型，比如数值型、字符串型、布尔型等
c.	定义倒排索引相关的配置，比如documentId、记录position、打分等
	获取索引mapping
不进行配置时，自动创建的mapping

//对应数据库中的pms_sku_info表

PUT gmall                   //新建索引（库），索引名必须小写
{
 "mappings": {              //关键字
   "PmsSkuInfo":{           //类型（表）
     "properties": {        //关键字，属性和字段
       "id":{               //主键id
        "type": "keyword",  //字段类型，es的默认数据类型之一，而且是字符串类型的一种，（text+keyword）,keyword是不可拆分，text是可拆分的
        "index": true       //新建索引，默认就会创建
      },
      "skuName":{           //商品名称
        "type": "text",     //可拆分的字符串（先分词，后建立索引）
        "analyzer": "ik_max_word"   //指定拆分的分词器
      },
      "skuDesc":{           //商品描述
        "type": "text"      //进行拆词（默认会新建索引"index": true）
        , "analyzer": "ik_smart"
      },
      "catalog3Id":{        //三级分类id（默认会新建索引"index": true）
        "type": "keyword"   //对应java中的String
      },
      "price":{             //商品价格（默认会新建索引"index": true）
        "type": "double"    //价格一定不进行分词，double，long,boolean默认不会进行分词
      },
      "skuDefaultImg":{     //商品的默认图片
        "type": "keyword",  //默认不分词
        "index": false      //不要新建索引
      },
      "hotScore":{          //商品热度值
        "type": "double"    //会新建索引
      },
      "productId":{         //商品id       
        "type": "keyword"   //不分词，会新建索引（有可能用商品id进行搜索）
      },
      "skuAttrValueList":{  //根据平台属性过滤商品
        "properties": {
          "attrId":{        //属性id
            "type":"keyword"
          },
          "valueId":{       //有属性值id就可以了
            "type":"keyword"
          }
        }
      }
     } 
   }
 } 
}

过滤--查询前过滤（推荐）
GET 库名/表名/_search{
"query":{
        "bool":{                // bool是联合查询，先过滤，后查询
        "filter":{"term":{},    //filter是过滤，是前端传来的条件，一般都是主外键id
                  "term":{}
                 }
        "must":{"match":{}      //must是搜索，根据输入的关键字搜索
               }
            }
        }
    }

查询所有商品名称(skuName)带Apple的手机，并且valueId=53(内存-n3),valueId=42(屏幕尺寸-5.6寸以上)
GET gmall/PmsSkuInfo/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "skuAttrValueList.valueId": "53"
          }
        },
        {
          "term": {
            "skuAttrValueList.valueId":"42"
          }
        }
      ],
      "must": {
        "match": {
          "skuName": "Apple"
        }
      }
    }
  }
}

IntelliJ Idea 常用快捷键列表:

Ctrl+Alt+h：显示调用当前方法的所有位置
Ctrl+E，最近的文件
Ctrl+Shift+E，最近更改的文件
Ctrl+Alt+L，格式化代码



${} 拼接sql字符串  #{} 赋值


在for循环数组做删除时，会出现下标越界，因为在每删除一个元素后，数组会重新组合，
索引会变更，比如原来时0，1，2，将第二个元素删除后，数组索引变为0，1，但是之前的循环还是会循环3次，
变更后的数组将会出现下标越界。
Iterator最适合做检查式的删除


es是以倒排索引算法为基础，nosql，操作关于文本的内存资源基于lucene的搜索引擎库

建议cookie只保存英文数字，否则需要进行编码、解码
/*
cookie和session的区别：
            session		cookie
保存的位置	服务端		客户端
安全性		较安全		较不安全
保存的内容	Object		String

Cookie：	name=value
javax.servlet.http.Cookie
public Cookie(String name,String value)
String getName()：获取name
String getValue():获取value
void setMaxAge(int expiry);最大有效期 （秒）

服务端准备 Cookie：response.addCookie(Cookie cookie)
页面跳转（转发，重定向）:客户端获取cookie:  request.getCookies();
a.服务端增加cookie :response对象；客户端获取对象：request对象
b.不能直接获取某一个单独对象，只能一次性将 全部的cookie拿到

session方法：
String getId() :获取sessionId
boolean isNew() :判断是否是 新用户（第一次访问）
void invalidate():使session失效  （退出登录、注销）
void setAttribute()
Object getAttribute();
void setMaxInactiveInterval(秒) ：设置最大有效 非活动时间
int getMaxInactiveInterval():获取最大有效 非活动时间
*/

/*
String str = "xxx";

isNotEmpty(str)等价于 str != null && str.length > 0（可以理解为 str !=null && str != ""）

isNotBlank(str) 等价于 str != null && str.length > 0 && str.trim().length > 0
（可以理解为 str !=null && str != "" && str != "n个空格"）

同理
isEmpty 等价于 str == null || str.length == 0
isBlank  等价于 str == null || str.length == 0 || str.trim().length == 0

str.length > 0 && str.trim().length > 0  --->   str.length > 0

其中，强调一下可能存在的坑：
trim()方法，是去除字符串首尾的空格，字符串中间的空格仍然存在。比如 ” abc  efg “经过trim()后变成“abc efg"

总结：
综上所述，某些场景下，isNotBlank()使用要比isNotEmpty()好。
*/

示例一:
function switchSkuId() {
/*
<input type="text" th:value="${skuSaleAttrHashJsonStr}" id="valuesSku"/>
*/
// $("#valuesSku") jquery 的id选择器，获取的是整个input对象
// $("#valuesSku").val() 获取的是里面的值（json字符串）
var skuSaleAttrValueJsonStr = $("#valuesSku").val();

/*
<div class="box-attr-2 clear"  th:each="spuSaleAttr:${spuSaleAttrListCheckBySku}">
    <dl>
        <dt th:text="${spuSaleAttr.saleAttrName}">选择颜色</dt>
        <dd th:class="(${saleAttrValue.isChecked}=='1'?'redborder':'')" 
                th:each="saleAttrValue:${spuSaleAttr.spuSaleAttrValueList}">
            <div th:value="${saleAttrValue.id}" th:text="${saleAttrValue.saleAttrValueName}">
                  摩卡金
            </div>
        </dd>
    </dl>
</div>
*/
// 获取当前被选中的销售属性值（红色框住的）
// $(".redborder") jquery 的样式选择器
// $(".redborder div") 样式选择器空格后，表示层级的下一级

//saleAttrValueIds 代表的是被选中div数组
var saleAttrValueIds = $(".redborder div");

var k = "";

// saleAttrValueId 代表每一个被选中的div
// $(saleAttrValueId).attr("value") 获取被选中的div的value属性
$(saleAttrValueIds).each(function (i, saleAttrValueId) {
    k = k + $(saleAttrValueId).attr("value") + "|";
});

// skuSaleAttrValueJsonStr是一个字符串，直接用skuSaleAttrValueJsonStr[k] 会报错
// 需要先将skuSaleAttrValueJsonStr字符串转换为json
var kuSaleAttrValueJson = JSON.parse(skuSaleAttrValueJsonStr);
var v_skuId = kuSaleAttrValueJson[k];

// 当v_skuId不是undefined时跳转，当前页面直接跳转下面的链接
if (v_skuId) {
    window.location.href = "http://127.0.0.1:8082/" + v_skuId + ".html";
}
}

示例二:
/*
<input type="checkbox" class="check" th:value="${cartInfo.productSkuId}"
    onchange="checkSku(this)" th:checked="(${cartInfo.isChecked}==1)?'true':'false'"/>
onchange="checkSku(this)"事件，当输入框发生变化的时候，包括值发生变化也包括选中状态发生变化，会调用checkSku()方法
*/
function checkSku(chkbox){
    //当前商品的skuId
   var skuId= $(chkbox).attr("value");
   // 当前商品的选中状态，true或者false
   var checked=$(chkbox).prop("checked");
   var isCheckedFlag="0";
   if(checked){
       isCheckedFlag="1";
   }

   // param是ajax异步请求字符串，是Ajax默认向后台传递数据的格式（也可以是 {skuId:109,isChecked:1} 这种格式，
    // 并不是json格式，只是一种类似json字符串的表达式，最终也会转换成param样的格式）
    var param="isChecked="+isCheckedFlag+"&"+"skuId="+skuId;
    $.post("checkCart",param,function (data) {
        // data就是返回结果
        // 服务会返回一个inner内嵌页给ajax,我们把返回的新的页面刷新替换掉原来的老的页面
        // <div id="cartListInner" th:include="cartListInner"></div>
        // $("#cartListInner") id选择器获取整个对象
        // $("#cartListInner").html(data) 将内部元素替换成服务返回的
        $("#cartListInner").html(data);
    });

}

ajax异步请求返回的不一定都是json，异步请求也可以返回页面，一般可以用来做列表页的刷新（p164）

示例三:
/*
<!--地址-->
<div class="top-3">
    <ul>
        <li class=".address default selected" th:each="userAddress:${userAddressList}">
            <input name="deliveryAddress" type="radio" th:value="${userAddress.id}"
                   th:checked="${userAddress.defaultStatus}=='1'" onclick="changeAddress()">
        </li>
    </ul>
</div>

属性选择器
首先属性是元素的属性，元素选择器$("元素")即 $("input")
$("元素[属性='属性名']")即 $("input[name='deliveryAddress']")，此时获得是一个数组，需要从当中过滤出选中的radio
过滤出选中的radio即 $("input[name='deliveryAddress']:checked")
获得选中的radio的值即 $("input[name='deliveryAddress']:checked").val()
*/
function changeAddress() {
    var receiveAddressId = $("input[name='deliveryAddress']:checked").val();
    $("#receiveAddressId").val(receiveAddressId);
}

三种单点登录算法的对比：
    （1）session共享（多个模块之间）：只要浏览器中cookie的jsessionid与共享的session（放在redis中）的sessionid匹配上，并且能从当中
取出用户数据，就认为该用户是登录过的。所以jsessionid尤为重要，但是可以劫持cookie的jsessionid，便能以用户的身份访问网站的功能。
    （2）token+redis：用户在认证中心登录过后，会给用户颁发一个token，将来这个token会写在浏览器中，同时也会在redis中写一份，将来根据
浏览器中的token与redis中的比对，如果一致就认为用户已经登录过。但问题就是用户在登录之后每访问一次模块，都要开启一次redis数据库连接，
进行token的比对。
    （3）jwt(json web token),专门制作token的，但是它做的token有一个特点，就是有一个自定义的加密过程，它加密的token只要你的算法足够复杂，
就没人能够解开，可以保证token的安全性。这个token中有用户的信息，用户在认证中心登录过后，会给用户颁发一个token，将来这个token会写在
浏览器中，在用户登录后的每次访问时，解密这个token，就可以获取用户的信息。使之前的一次redis数据库连接成为一次加密解密的过程。


验证登录（token）功能，用jwt解析
1、浏览器请求业务模块，且业务模块发现浏览器没有携带token
2、此时没有token，业务模块不允许访问，将请求踢回去，让浏览器重定向到认证中心
3、浏览器重定向到认证中心，来到登录页面，输入用户名和密码
4、认证通过后，重新访问业务模块，并且此时认证中心返回了一个token（但并没有写入浏览器的cookie中，只是在地址栏的url中）
5、此时浏览器携带了token重新访问业务模块，仍然要被拦截器拦截，验证token通过后，由业务模块的拦截器将token写入浏览器的cookie中
6、下次再访问时，浏览器的cookie中就会有token了。


认证中心写成PassportController，是一个web controller，是一个rest风格的http请求可以访问的，而不是一个dubbo的服务，如果写成一个dubbo
的服务，那么只有商城内部的服务可以调用，外部的服务调用不了，要使能够社交登录，一定要是一个rest风格的http请求可以访问的，这样将来其他的
系统才可以切入进来。


(将alipay-sdk-java20180104135026.jar复制到E:\apache-maven-3.5.4\bin目录下,cmd打开命令行工具)
在jar包所在目录下，打开命令行工具,执行如下命令
mvn install:install-file -DgroupId=com.alipay -DartifactId=alipay-sdk -Dversion=3.0.0 -Dpackaging=jar -Dfile=alipay-sdk-java20180104135026.jar
这样在pom.xml中就可以引入依赖
<dependency>
   <groupId>com.alipay</groupId>
   <artifactId>alipay-sdk</artifactId>
   <version>3.0.0</version>
</dependency>


分布式事务：在分布式的环境下如何保持数据的一致性，就是分布式事务要解决的问题

激活成功后，发现数据库中 state 和 enable 字段并未更新，因为 User 实体类的 id 我们没有给它标识为主键，所以没有根据主键来更新字段。
在 id 字段上加上以下注解：
@Id//标识主键
@GeneratedValue(strategy = GenerationType.IDENTITY) //自增长策略















