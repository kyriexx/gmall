package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kyrie
 * @create 2019-10-21 17:37
 */
@Controller
public class ItemController {

    @Reference
    SkuService skuService;

    @Reference
    SpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap map, HttpServletRequest request) {

        String remoteAddr = request.getRemoteAddr();//获取的是直接ip，如果nginx反向代理获取的是nginx的ip

        // request.getHeader("x-forwarded-for");// nginx负载均衡
        /*
        String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();// 从request中获取ip
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.11";
            }
        }
        */

        // sku对象
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId,remoteAddr);
        map.put("skuInfo", pmsSkuInfo);

        // 销售属性列表
        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(), pmsSkuInfo.getId());
        map.put("spuSaleAttrListCheckBySku", pmsProductSaleAttrs);

        // 查询当前sku的spu的其他sku的集合的hash表
        Map<String, String> skuSaleAttrHash = new HashMap<>();
        List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());

        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k = "";
            String v = skuInfo.getId();

            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                k += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";
            }

            skuSaleAttrHash.put(k,v);
        }

        // 将sku的销售属性hash表放到页面
        // 如果使用ModelMap.put()的话，是放在了request域中，将来在页面上从request中取的时候
        // 是一个Java对象，而我们要完全的客户端工作（把这个当作一个js，或者当作客户端里面的一个隐藏
        // 的json字符串，直接放到页面上，不是放在域中，因为将来客户端不同sku的切换和后台没有关系）

        // 后台要生成一个“属性值1|属性值2|属性值3：skuId”的一个json串以提供页面进行匹配
        // 如 skuSaleAttrHashJsonStr:{"266|269":"107","265|270":"108","267|271":"109"}

        String skuSaleAttrHashJsonStr = JSON.toJSONString(skuSaleAttrHash);
        map.put("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonStr);
        // 页面上有一个隐藏域专门接收这个
        // <input type="hidden" th:value="${skuSaleAttrHashJsonStr}" id="valuesSku"/>

        return "item";
    }
}
