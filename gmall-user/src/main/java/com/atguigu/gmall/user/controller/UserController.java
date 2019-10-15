package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.user.bean.UmsMember;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-15 21:32
 */
@RestController
public class UserController {

    @Autowired
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
