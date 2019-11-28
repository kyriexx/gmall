package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 * @author Kyrie
 * @create 2019-10-26 21:18
 */
@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index() {
        return "index";
    }

    @RequestMapping("list.html")
    // PmsSearchParam封装三级分类id、关键字、
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {

        // 调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfoList);

        // 抽取检索结果所包含的平台属性集合
        Set<String> valueIdSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }
        // 根据valueId将属性列表查询出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfoList = attrService.getAttrValueListByValueId(valueIdSet);
        modelMap.put("attrList", pmsBaseAttrInfoList);

        // 对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组
        String[] delValueIds = pmsSearchParam.getValueId();
        if (delValueIds != null) {
            // 面包屑
            // pmsSearchParam
            // delValueIds

            //Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfoList.iterator(); 错误的
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            for (String delValueId : delValueIds) {
                //iterator只是一个独立的迭代器，它只负责迭代，它并不是集合本身的内容，它只负责用游标指向集合中的某一个元素
                //iterator最适合做检查式的删除
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfoList.iterator();
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                // 生成面包屑的参数
                pmsSearchCrumb.setValueId(delValueId);
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, delValueId));
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String valueId = pmsBaseAttrValue.getId();
                        if (delValueId.equals(valueId)) {
                            // 查找面包屑的属性值名称
                            //删除的该属性值对应的属性名称就是要生成的面包屑的名称
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            //删除该属性值所在的属性组
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            modelMap.put("attrValueSelectedList", pmsSearchCrumbs);
        }

        String urlParam = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam", urlParam);

        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }

        return "list";
    }

//    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String... delValueId) {
//        return "";
//    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] valueIds = pmsSearchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }

        if (valueIds != null) {
            for (String valueId : valueIds) {
                if (!valueId.equals(delValueId)) {
                    urlParam = urlParam + "&valueId=" + valueId;
                }
            }
        }

        return urlParam;
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] valueIds = pmsSearchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }

        if (valueIds != null) {
            for (String valueId : valueIds) {
                urlParam = urlParam + "&valueId=" + valueId;
            }
        }

        return urlParam;
    }
}
/*
String str = "xxx";

isNotEmpty(str)等价于 str != null && str.length > 0（可以理解为 str !=null && str != ""）

isNotBlank(str) 等价于 str != null && str.length > 0 && str.trim().length > 0
（可以理解为 str !=null && str != "" && str != "n个空格"）

同理
isEmpty 等价于 str == null || str.length == 0
isBlank  等价于 str == null || str.length == 0 || str.trim().length == 0

str.length > 0 && str.trim().length > 0  --->   str.length > 0

其中，强调一下可能存在的坑：
trim()方法，是去除字符串首尾的空格，字符串中间的空格仍然存在。比如 ” abc  efg “经过trim()后变成“abc efg"

总结：
综上所述，某些场景下，isNotBlank()使用要比isNotEmpty()好。
*/
