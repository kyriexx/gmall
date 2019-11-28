package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-29 22:54
 */
public interface CartService {
    OmsCartItem ifCartExistByUser(String memberId, String skuId);

    void addCart(OmsCartItem omsCartItem);

    void updateCart(OmsCartItem omsCartItemFromDb);

    void flushCartCache(String memberId);

    List<OmsCartItem> cartList(String memberId);

    void checkCart(OmsCartItem omsCartItem);
}
