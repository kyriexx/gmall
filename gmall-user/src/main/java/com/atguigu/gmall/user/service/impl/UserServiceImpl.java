package com.atguigu.gmall.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-15 21:31
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {
        return userMapper.selectAll();
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();

            if(jedis!=null){
                String umsMemberStr = jedis.get("user:" + umsMember.getPassword() + ":info");

                if (StringUtils.isNotBlank(umsMemberStr)) {
                    // 密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                }
            }
            // 链接redis失败，开启数据库
            UmsMember umsMemberFromDb =loginFromDb(umsMember);
            if(umsMemberFromDb!=null){
                jedis.setex("user:" + umsMember.getPassword() + ":info",60*60*24, JSON.toJSONString(umsMemberFromDb));
            }
            return umsMemberFromDb;
        }finally {
            jedis.close();
        }
    }

    @Override
    public void addUserToken(String token, String memberId) {

    }

    @Override
    public void addOauthUser(UmsMember umsMember) {

    }

    private UmsMember loginFromDb(UmsMember umsMember) {

        List<UmsMember> umsMembers = userMapper.select(umsMember);

        if(umsMembers!=null){
            return umsMembers.get(0);
        }

        return null;
    }
}
