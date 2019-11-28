package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-15 21:32
 */
@RestController
public class UserController {

    //远程引用（按照TicketService全类名进行匹配，看谁给注册中心注册了这个全类名的服务，
    // 而在服务提供方发布的时候就是按这个接口的全类名发布的
    /*
        @Service //将服务发布出去
        public class UserServiceImpl implements UserService）
    */
    @Reference
    UserService userService;

    @RequestMapping("/getAllUser")
    public List<UmsMember> getAllUser(){
        return userService.getAllUser();
    }

    @RequestMapping("/hello")
    public String hello(){
        return "hello user";
    }
}
