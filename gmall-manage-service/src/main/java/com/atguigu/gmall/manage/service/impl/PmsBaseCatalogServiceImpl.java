package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog1mapper;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog2mapper;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog3mapper;
import com.atguigu.gmall.service.PmsBaseCatalogService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-17 9:05
 */
@Service
public class PmsBaseCatalogServiceImpl implements PmsBaseCatalogService {

    @Autowired
    PmsBaseCatalog1mapper pmsBaseCatalog1mapper;

    @Autowired
    PmsBaseCatalog2mapper pmsBaseCatalog2mapper;

    @Autowired
    PmsBaseCatalog3mapper pmsBaseCatalog3mapper;

    @Override
    public List<PmsBaseCatalog1> getCatalog1() {
        return pmsBaseCatalog1mapper.selectAll();
    }

    @Override
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id) {
        PmsBaseCatalog2 pmsBaseCatalog2 = new PmsBaseCatalog2();
        pmsBaseCatalog2.setCatalog1Id(catalog1Id);
        return pmsBaseCatalog2mapper.select(pmsBaseCatalog2);
    }

    @Override
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id) {
        PmsBaseCatalog3 pmsBaseCatalog3 = new PmsBaseCatalog3();
        pmsBaseCatalog3.setCatalog2Id(catalog2Id);
        return pmsBaseCatalog3mapper.select(pmsBaseCatalog3);
    }
}
