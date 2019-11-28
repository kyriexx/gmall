package com.atguigu.gmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

// 在整合redis的时候，由于需要在spring容器启动的时候扫描
// service-util工程的com.atguigu.gmall下的util包和conf包中的组件，所以需要将主配置类从
// com.atguigu.gmall.manage下移动到com.atguigu.gmall下
@MapperScan("com.atguigu.gmall.manage.mapper")
@SpringBootApplication
public class GmallManageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallManageServiceApplication.class, args);
    }

}
