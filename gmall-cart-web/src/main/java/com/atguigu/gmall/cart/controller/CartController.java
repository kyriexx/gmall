package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-29 15:43
 */

    // 建议cookie只保存英文数字，否则需要进行编码、解码
    /*
    cookie和session的区别：
                session		cookie
    保存的位置	服务端		客户端
    安全性		较安全		较不安全
    保存的内容	Object		String

    Cookie：	name=value
    javax.servlet.http.Cookie
    public Cookie(String name,String value)
    String getName()：获取name
    String getValue():获取value
    void setMaxAge(int expiry);最大有效期 （秒）

    服务端准备 Cookie：response.addCookie(Cookie cookie)
    页面跳转（转发，重定向）:客户端获取cookie:  request.getCookies();
    a.服务端增加cookie :response对象；客户端获取对象：request对象
    b.不能直接获取某一个单独对象，只能一次性将 全部的cookie拿到

    session方法：
    String getId() :获取sessionId
    boolean isNew() :判断是否是 新用户（第一次访问）
    void invalidate():使session失效  （退出登录、注销）
    void setAttribute()
    Object getAttribute();
    void setMaxInactiveInterval(秒) ：设置最大有效 非活动时间
    int getMaxInactiveInterval():获取最大有效 非活动时间
    */
@Controller
public class CartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;

    //@LoginRequired(loginSuccess = false)表示该方法百分百可以访问
    //@LoginRequired(loginSuccess = true)表示该方法只有判定通过（用户登录了）才可以访问
    @RequestMapping(value = "addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response) {

        // 调用商品服务查询商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId, "");

        List<OmsCartItem> omsCartItemList = new ArrayList<>();

        // 将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("11111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));

        // 判断用户是否登录，如果判断已登录，则memberId是有值的，如果没登录，则memberId是没有值的
        //String memberId = "1";//"1";
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        if (StringUtils.isBlank(memberId)) {
            // 用户没有登录
            // cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isBlank(cartListCookie)) {
                // cookie为空
                omsCartItemList.add(omsCartItem);
            } else {
                // cookie不为空
                omsCartItemList = JSON.parseArray(cartListCookie, OmsCartItem.class);
                // 判断添加的购物车数据在cookie中是否存在
                boolean exist = if_cart_exist(omsCartItemList, omsCartItem);
                if (exist) {
                    // 之前添加过，更新购物车添加数量
                    for (OmsCartItem cartItem : omsCartItemList) {
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())) {
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                        }
                    }
                } else {
                    // 之前没有添加，新增当前的购物车
                    omsCartItemList.add(omsCartItem);
                }
            }

            // 更新cookie
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(omsCartItemList), 60 * 60 * 72, true);

        } else {
            // 用户已登录
            // 从db中查出购物车数据,判断当前用户有没有添加当前商品
            OmsCartItem omsCartItemFromDb = cartService.ifCartExistByUser(memberId, skuId);

            if (omsCartItemFromDb == null) {
                // 该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname("test小明");
                omsCartItem.setQuantity(new BigDecimal(quantity));
                cartService.addCart(omsCartItem);

            } else {
                // 该用户添加过当前商品
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);
            }

            // 同步缓存
            cartService.flushCartCache(memberId);
        }

        return "redirect:/success.html";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItemList, OmsCartItem omsCartItem) {

        boolean b = false;

        for (OmsCartItem cartItem : omsCartItemList) {
            String productSkuId = cartItem.getProductSkuId();
            if (productSkuId.equals(omsCartItem.getProductSkuId())) {
                b = true;
            }
        }
        return b;
    }

    //购物车列表
    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        List<OmsCartItem> omsCartItemList = new ArrayList<>();

        //String memberId = "1";
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        if(StringUtils.isNotBlank(memberId)){
            // 已经登录查询db
            omsCartItemList = cartService.cartList(memberId);
        }else {
            // 没有登录查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isNotBlank(cartListCookie)){
                omsCartItemList = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }

        for (OmsCartItem omsCartItem : omsCartItemList) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
        }

        modelMap.put("cartList",omsCartItemList);

        // 被勾选商品的总额
        BigDecimal totalAmount =getTotalAmount(omsCartItemList);
        modelMap.put("totalAmount",totalAmount);

        return "cartList";
    }

    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String skuId,String isChecked,ModelMap modelMap,HttpServletRequest request){

        //String memberId = "1";
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        // 调用服务，修改状态
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        cartService.checkCart(omsCartItem);

        // 将最新的数据从缓存中查出，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        modelMap.put("cartList",omsCartItems);

        // 被勾选商品的总额
        BigDecimal totalAmount =getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);

        // 返回一个内嵌页面
        return "cartListInner";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");

        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();

            if(omsCartItem.getIsChecked().equals("1")){
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }

//    @RequestMapping("toTrade")
//    @LoginRequired(loginSuccess = true)
//    public String toTrade(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
//
//        String memberId = (String)request.getAttribute("memberId");
//        String nickname = (String)request.getAttribute("nickname");
//
//        return "toTrade";
//    }
}
