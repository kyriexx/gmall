package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.service.PmsBaseCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-16 23:48
 */
@CrossOrigin
@RestController
public class CatalogController {

    @Reference
    PmsBaseCatalogService pmsBaseCatalogService;

    //获取一级分类
    @RequestMapping("getCatalog1")
    public List<PmsBaseCatalog1> getCatalog1(){
        List<PmsBaseCatalog1> catalog1s = pmsBaseCatalogService.getCatalog1();
        return catalog1s;
    }


    /*
    @RequestMapping("/doLogin")
    public String doLogin(Model model,
          @RequestParam(value = "username",required = false) String email,
          @RequestParam(value = "password",required = false) String password) {
    */
    //@RequestBody 接收前端传的post请求的json格式的数据
    //public List<PmsBaseCatalog2> getCatalog2(@RequestBody String catalog1Id){

    //获取二级分类
    @RequestMapping("getCatalog2")
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id){
        List<PmsBaseCatalog2> catalog2s = pmsBaseCatalogService.getCatalog2(catalog1Id);
        return catalog2s;
    }

    //获取三级分类
    @RequestMapping("getCatalog3")
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id){
        List<PmsBaseCatalog3> catalog3s = pmsBaseCatalogService.getCatalog3(catalog2Id);
        return catalog3s;
    }
}
