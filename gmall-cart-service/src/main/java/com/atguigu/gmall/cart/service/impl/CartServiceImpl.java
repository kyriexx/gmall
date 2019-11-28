package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kyrie
 * @create 2019-10-29 22:56
 */
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public OmsCartItem ifCartExistByUser(String memberId, String skuId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        OmsCartItem cartItem = omsCartItemMapper.selectOne(omsCartItem);
        return cartItem;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        if (StringUtils.isNotBlank(omsCartItem.getMemberId())) {
            omsCartItemMapper.insertSelective(omsCartItem);
        }
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        //createCriteria()修改条件正则表达式
        //andEqualTo根据什么(id)修改
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id", omsCartItemFromDb.getId());
        //Selective表示空值不修改，不空的才会修改
        //(omsCartItemFromDb,example)表示根据example的条件修改成omsCartItemFromDb的样子
        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb, example);
    }

    /*
    mapkey
        key value
        key value

    hset user:1:cart sku1 cartinfo1
    hset user:1:cart sku2 cartinfo2
    hset user:1:cart sku3 cartinfo3

    获取用户1的所有购物车信息 hvals user:1:cart
    获取用户1的某一个商品的购物车信息 hget user:1:cart sku1[/sku2/sku3]
    */
    @Override
    public void flushCartCache(String memberId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);

        // 同步到redis缓存中
        Jedis jedis = redisUtil.getJedis();

        // List集合转map
        Map<String, String> map = new HashMap<>();
        for (OmsCartItem cartItem : omsCartItems) {
            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
        }

        jedis.del("user:" + memberId + ":cart");
        jedis.hmset("user:" + memberId + ":cart", map);

        jedis.close();
    }

    @Override
    public List<OmsCartItem> cartList(String memberId) {
        Jedis jedis = null;
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        try {
            jedis = redisUtil.getJedis();

            List<String> hvals = jedis.hvals("user:" + memberId + ":cart");

            for (String hval : hvals) {
                OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }

        } catch (Exception e) {
            // 处理异常，记录系统日志
            e.printStackTrace();
            //String message = e.getMessage();
            //logService.addErrLog(message);
            return null;
        } finally {
            jedis.close();
        }

        return omsCartItems;
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem) {

        Example e = new Example(OmsCartItem.class);
        // 根据memberId和productSkuId两个条件更新
        e.createCriteria().andEqualTo("memberId", omsCartItem.getMemberId()).andEqualTo("productSkuId", omsCartItem.getProductSkuId());
        //updateByExampleSelective 只更新有值的，不会更新无值的
        omsCartItemMapper.updateByExampleSelective(omsCartItem, e);

        // 缓存同步
        flushCartCache(omsCartItem.getMemberId());
    }
}
