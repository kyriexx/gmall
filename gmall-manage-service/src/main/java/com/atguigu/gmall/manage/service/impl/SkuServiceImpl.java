package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author Kyrie
 * @create 2019-10-21 12:04
 */
@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        //添加PmsSkuInfo
        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);

        //添加平台属性关联 skuAttrValueList
        List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : pmsSkuAttrValueList) {
            pmsSkuAttrValue.setSkuId(pmsSkuInfo.getId());
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        //添加销售属性关联 skuSaleAttrValueList
        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : pmsSkuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(pmsSkuInfo.getId());
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        //添加图片 skuImageList
        List<PmsSkuImage> pmsSkuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : pmsSkuImageList) {
            pmsSkuImage.setSkuId(pmsSkuInfo.getId());
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }

    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId) {

        //sku信息
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        //图片信息
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setSkuImageList(pmsSkuImages);

        return skuInfo;
    }

    @Override
    public PmsSkuInfo getSkuById(String skuId, String ip) {
        System.out.println("ip为" + ip + "的同学:" + Thread.currentThread().getName() + "进入的商品详情的请求");

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        // 链接缓存
        Jedis jedis = redisUtil.getJedis();
        // 查询缓存
        String skuKey = "sku:" + skuId + ":info";
        String skuJson = jedis.get(skuKey);

        // if(skuJson!=null&&!skuJson.equals(""))
        if (StringUtils.isNotBlank(skuJson)) {
            System.out.println("ip为" + ip + "的同学:" + Thread.currentThread().getName() + "从缓存中获取商品详情");

            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        } else {
            // 如果缓存中没有，查询mysql
            System.out.println("ip为" + ip + "的同学:" + Thread.currentThread().getName() + "发现缓存中没有，申请缓存的分布式锁：" + "sku:" + skuId + ":lock");

            // 设置分布式锁
            String token = UUID.randomUUID().toString();
            // 拿到锁的线程有10秒的过期时间
            String OK = jedis.set("sku:" + skuId + ":lock", token, "nx", "px", 10 * 1000);
            if (StringUtils.isNotBlank(OK) && OK.equals("OK")) {
                // 设置成功，有权在10秒的过期时间内访问数据库
                System.out.println("ip为" + ip + "的同学:" + Thread.currentThread().getName() + "有权在10秒的过期时间内访问数据库：" + "sku:" + skuId + ":lock");

                pmsSkuInfo = getSkuByIdFromDb(skuId);

                if (pmsSkuInfo != null) {
                    // mysql查询结果存入redis
                    jedis.set("sku:" + skuId + ":info", JSON.toJSONString(pmsSkuInfo));
                } else {
                    // 数据库中不存在该sku
                    // 为了防止缓存穿透，将null或者空字符串值设置给redis
                    // 一旦某个sku在数据库中不存在，将一个空字符串设置给redis缓存，并且设置过期时间为3分钟
                    jedis.setex("sku:" + skuId + ":info", 60 * 3, JSON.toJSONString(""));
                }

                // 在访问mysql后，将mysql的分布锁释放
                System.out.println("ip为" + ip + "的同学:" + Thread.currentThread().getName() + "使用完毕，将锁归还：" + "sku:" + skuId + ":lock");

                String lockToken = jedis.get("sku:" + skuId + ":lock");
                if (StringUtils.isNotBlank(lockToken) && lockToken.equals(token)) {
                    //jedis.eval("lua");可以用lua脚本，在查询到key的同时立即删除该key，防止高并发下的意外的发生
                    //意外的发生（是指查询到key后，在删除之前，锁过期了，而此时有其他线程生成锁，
                    // 再删除锁的话，就删除的不是自己的锁了）
                    //String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    //jedis.eval(script, Collections.singletonList("lock"),Collections.singletonList(token));
                    jedis.del("sku:" + skuId + ":lock");// 用token确认删除的是自己的sku的锁
                }
            } else {
                // 设置失败，自旋（该线程在睡眠几秒后，重新尝试访问本方法）
                System.out.println("ip为" + ip + "的同学:" + Thread.currentThread().getName() + "没有拿到锁，开始自旋");
                // getSkuById(skuId, ip);错误的自旋，重新开了一个线程尝试访问本方法，与当前线程无关
                return getSkuById(skuId, ip);//正确的自旋，当前线程重新尝试访问本方法
            }
        }
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);

            pmsSkuInfo.setSkuAttrValueList(select);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal price) {
        boolean b = false;

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        BigDecimal productPrice = pmsSkuInfo1.getPrice();

        if(productPrice.compareTo(price)==0){
            b = true;
        }
        return b;
    }
}
