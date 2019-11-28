package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-27 0:18
 */
public interface SearchService {
    List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam);
}
