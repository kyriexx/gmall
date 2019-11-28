package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证中心：负责颁发和验证通行证
 *
 * @author Kyrie
 * @create 2019-10-31 15:11
 */
@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap modelMap) {
        modelMap.put("ReturnUrl", ReturnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request) {
        // 调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);

        String token = "";

        if (umsMemberLogin != null) {
            // 登录成功
            // 用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("memberId", memberId);
            userMap.put("nickname", nickname);

            String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
            if (StringUtils.isBlank(ip)) {
                ip = request.getRemoteAddr();// 从request中获取ip
                if (StringUtils.isBlank(ip)) {
                    ip = "127.0.0.11";
                }
            }

            // 按照设计的算法对参数进行加密后，生成token
            token = JwtUtil.encode("2019gmall", userMap, ip);

            // 将token存入redis一份
            userService.addUserToken(token, memberId);
        } else {
            // 登录失败
            token = "fail";
        }

        return token;
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token, String currentIp) {
        // 通过jwt校验token真假
        Map<String, String> map = new HashMap<>();
        // token中的用户信息
        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall", currentIp);
        if (decode != null) {
            map.put("status", "success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        } else {
            map.put("status", "fail");
        }

        String successJson = JSON.toJSONString(map);
        return successJson;
    }

    @RequestMapping("vlogin")
    //@ResponseBody
    public String vlogin(String code,HttpServletRequest request) {

        // 授权码换取access_token
        // (App Secret)client_secret=626ebac0f2a966d1c31608f65ba7db81,(App Key)client_id=93671618
        String s3 = "https://api.weibo.com/oauth2/access_token?";
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","93671618");
        paramMap.put("client_secret","626ebac0f2a966d1c31608f65ba7db81");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code",code);// 授权有效期内可以使用，没新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授权码全部过期
        String access_token_json = HttpclientUtil.doPost(s3, paramMap);
        Map<String,Object> access_map = JSON.parseObject(access_token_json,Map.class);

        // access_token换取用户信息
        String uid = (String)access_map.get("uid");
        String access_token = (String)access_map.get("access_token");
        String show_user_url = "https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;
        String user_json = HttpclientUtil.doGet(show_user_url);
        Map<String,Object> user_map = JSON.parseObject(user_json,Map.class);

        // 将用户信息保存数据库，用户类型设置为微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setSourceUid((String)user_map.get("idstr"));
        umsMember.setCity((String)user_map.get("location"));
        umsMember.setNickname((String)user_map.get("screen_name"));
        String g = "0";
        String gender = (String)user_map.get("gender");
        if(gender.equals("m")){
            g = "1";
        }
        umsMember.setGender(g);

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheck);

        if(umsMemberCheck==null){
            umsMember = userService.addOauthUser(umsMember);//这里的umsMember是没有主键的，mybatis的主键返回策略只针对dao层有用
            //之前由于controller层和dao层在一个虚拟机里，根据地址可以直接封装上，现在是通过dubbo的rpc远程访问，
            //service层封装的数据在经过dubbo传输后，到不了controller层
            //主键返回策略没有办法跨越rpc
            /*
            注意mybatis的主键返回策略不能跨rpc使用(要在控制层得到生成的主键，需要将保存db的对象返回给控制层)
            */
        }else{
            umsMember = umsMemberCheck;
        }

        // 生成jwt的token，并且重定向到首页，携带该token
        String token = null;
        String memberId = umsMember.getId();
        String nickname = umsMember.getNickname();
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("memberId",memberId);
        userMap.put("nickname",nickname);

        String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();// 从request中获取ip
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.11";
            }
        }

        // 按照设计的算法对参数进行加密后，生成token
        token = JwtUtil.encode("2019gmall", userMap, ip);

        // 将token存入redis一份
        userService.addUserToken(token,memberId);

        return "redirect:http://search.gmall.com:8083/index?token="+token;
    }
}
