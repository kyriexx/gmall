package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsSkuInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-21 12:05
 */
public interface PmsSkuInfoMapper extends Mapper<PmsSkuInfo> {
    List<PmsSkuInfo> selectSkuSaleAttrValueListBySpu(String productId);
}
