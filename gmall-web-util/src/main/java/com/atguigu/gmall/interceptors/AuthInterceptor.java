package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kyrie
 * @create 2019-10-31 23:39
 * <p>
 * preHandle: 在请求处理之前进行调用（Controller方法调用之前）
 * 　　返回值：true表示继续流程；false表示流程中断，不会继续调用其他的拦截器或处理器
 * postHandle: 在请求处理之后进行调用（Controller方法调用之后）
 * afterCompletion: 整个请求处理完毕回调方法，即在视图渲染完毕时回调
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    // request表示当前请求，里面既有cookie也有token
    // response表示响应
    // handler表示被拦截的请求的访问的方法
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截代码
        // 判断被拦截的请求的访问的方法的注解(是否是需要拦截的)
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

        StringBuffer url = request.getRequestURL();
        System.out.println(url);

        // 是否拦截
        // 若方法上没有@LoginRequired注解，表示不用拦截（不用登录），直接放行
        if (methodAnnotation == null) {
            return true;
        }

        // 是否必须登录
        // @LoginRequired(loginSuccess = false)表示该方法百分百可以访问，表示该方法必须要进行用户登录判定，
        // 但如果判定没通过（购物车走cookie的分支）也允许访问
        // @LoginRequired(loginSuccess = true)表示该方法只有判定通过（用户登录了）才可以访问，表示该方法
        // 必须要进行用户登录判定，并且必须要判定通过（购物车走数据库的分支）
        boolean loginSuccess = methodAnnotation.loginSuccess();// 获得该请求是否必须要登录成功

        // token为空表示从未登录过
        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        // 调用认证中心进行验证
        String success = "fail";
        Map<String, String> successMap = new HashMap<>();
        if (StringUtils.isNotBlank(token)) {

            String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
            if (StringUtils.isBlank(ip)) {
                ip = request.getRemoteAddr();// 从request中获取ip
                if (StringUtils.isBlank(ip)) {
                    ip = "127.0.0.11";
                }
            }
            String successJson = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token=" + token + "&currentIp=" + ip);
            successMap = JSON.parseObject(successJson, Map.class);
            success = successMap.get("status");
        }

        if (loginSuccess) {
            // 必须登录成功才能使用
            if (!success.equals("success")) {
                //重定向会passport登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl=" + requestURL);
                return false;
            }

            // 需要将token携带的用户信息写入
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));
            //验证通过，覆盖cookie中的token
            if (StringUtils.isNotBlank(token)) {
                CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
            }
        } else {
            // 没有登录也能用，但是必须验证，此时还验证通过了
            if (success.equals("success")) {
                // 需要将token携带的用户信息写入
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));
                //验证通过，覆盖cookie中的token
                if (StringUtils.isNotBlank(token)) {
                    CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
                }
            }
        }

        return true;
    }
}
