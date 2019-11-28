package com.atguigu.gmall.passport.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpclientUtil;

import java.util.HashMap;
import java.util.Map;

public class TestOauth2 {

    public static String getCode() {

        // 1 获得授权码
        // App Key：93671618
        // 授权回调页: http://passport.gmall.com:8085/vlogin

        //地址一：https://api.weibo.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&response_type=code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI
        //引导用户与第三方平台进行授权交互地址（第三方平台地址，页面上有我们自己的授权应用的信息）
        String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=93671618&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");

        System.out.println(s1);

        // 在第一步和第二部返回回调地址之间,有一个用户操作授权的过程

        // 2 返回授权码到回调地址
        //地址二：http://passport.gmall.com:8085/vlogin?code=fef987b3f9ad1169955840b467bfc661
        //授权成功后，第三方平台将授权码回调给我们的地址(我们自己的地址)

        return null;
    }

    public static String getAccess_token() {
        // 换取access_token
        // App Key：93671618
        // App Secret：626ebac0f2a966d1c31608f65ba7db81

        //地址三：https://api.weibo.com/oauth2/access_token?client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&grant_type=authorization_code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
        //使用授权码code交换access_token的地址，需要加入client_secret应用密钥，并且使用post请求进行交换，保证access_token的安全性

        String s3 = "https://api.weibo.com/oauth2/access_token?";//?client_id=187638711&client_secret=a79777bba04ac70d973ee002d27ed58c&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=CODE";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("client_id", "93671618");
        paramMap.put("client_secret", "626ebac0f2a966d1c31608f65ba7db81");
        paramMap.put("grant_type", "authorization_code");
        paramMap.put("redirect_uri", "http://passport.gmall.com:8085/vlogin");
        paramMap.put("code", "b882d988548ed2b9174af641d20f0dc1");// 授权有效期内可以使用，每新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授权码全部过期
        String access_token_json = HttpclientUtil.doPost(s3, paramMap);

        Map<String, String> access_map = JSON.parseObject(access_token_json, Map.class);

        System.out.println(access_map.get("access_token"));
        System.out.println(access_map.get("uid"));

        return access_map.get("access_token");
    }

    public static Map<String, String> getUser_info() {

        // 4 用access_token查询用户信息

        //地址四：https://api.weibo.com/2/users/show.json?access_token=2.00HMAs7H0p5_hMdbefcb34140Lydjf&uid=6809985023
        //使用access_token换取用户信息的地址

        String s4 = "https://api.weibo.com/2/users/show.json?access_token=2.00HMAs7H0p5_hMdbefcb34140Lydjf&uid=6809985023";
        String user_json = HttpclientUtil.doGet(s4);
        Map<String, String> user_map = JSON.parseObject(user_json, Map.class);

        System.out.println(user_map.get("1"));

        return user_map;
    }


    public static void main(String[] args) {

        getUser_info();

    }
}
