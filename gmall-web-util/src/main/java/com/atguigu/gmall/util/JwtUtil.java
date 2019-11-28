package com.atguigu.gmall.util;

import io.jsonwebtoken.*;

import java.util.Map;

public class JwtUtil {

    //key 是服务器公共key，只有服务器有，代表服务器的身份
    //param 用户的基本信息
    //salt 盐值，使不同的浏览器在不同的时间访问的时候生成的jwt token都不一样，是在浏览器上的
    public static String encode(String key, Map<String, Object> param, String salt) {
        if (salt != null) {
            key += salt;
        }
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256, key);

        jwtBuilder = jwtBuilder.setClaims(param);

        String token = jwtBuilder.compact();

        return token;
    }


    public static Map<String, Object> decode(String token, String key, String salt) {
        Claims claims = null;
        if (salt != null) {
            key += salt;
        }
        try {
            claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch (JwtException e) {
            return null;
        }
        return claims;
    }
}
