package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.service.SpuService;
import com.atguigu.gmall.util.PmsUploadUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Kyrie
 * @create 2019-10-18 16:52
 */
@CrossOrigin
@RestController
public class SpuController {

    @Reference
    SpuService spuService;

    // 根据三级分类id获取属性列表spuList?catalog3Id
    @RequestMapping("spuList")
    public List<PmsProductInfo> spuList(String catalog3Id) {
        List<PmsProductInfo> pmsProductInfos = spuService.spuList(catalog3Id);
        return pmsProductInfos;
    }

    /*
    springmvc 实现文件上传
    <!-- 配置CommonsMultipartResolver，用于实现文件上传，将其加入SpringIOC容器.
		springIoc容器在初始化时，会自动寻找一个Id="multipartResolver"的bean，并将其加入Ioc容器 -->

    <form action="testUpload" method="post" enctype="multipart/form-data">

		描述:<input name="desc" type="text" />
		<input type="file" name="file" />

		<input type="submit" value="上传">
	</form>

	<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
			<property name="defaultEncoding" value="UTF-8"></property>
			<!-- 上传单个文件的最大值，单位Byte;如果-1，表示无限制 -->
			<property name="maxUploadSize"  value="104857600"></property>
	</bean>

    // 文件上传处理方法
	@RequestMapping(value = "testUpload") // abc.png
	public String testUpload(@RequestParam("desc") String desc,
	                         @RequestParam("file") MultipartFile file)
			throws IOException {

		System.out.println("文件描述信息：" + desc);
		// jsp中上传的文件：file

		InputStream input = file.getInputStream();// IO
		String fileName = file.getOriginalFilename();//文件名

		OutputStream out = new FileOutputStream("d:\\" + fileName);

		byte[] bs = new byte[1024];
		int len = -1;
		while ((len = input.read(bs)) != -1) {
			out.write(bs, 0, len);
		}
		out.close();
		input.close();
		// 将file上传到服务器中的某一个硬盘文件中
		System.out.println("上传成功！");

		return "success";
	}

    */
    //将form表单中的File类型转为为springmvc的MultipartFile类型
    //@RequestParam("file")告诉MultipartFile转换表单中的对象名是file的（默认名就是file）
    @RequestMapping("fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile) {
        // 将图片或者音视频上传到分布式的文件存储系统

        String imgUrl = PmsUploadUtil.uploadImage(multipartFile);
        // 将图片的存储路径返回给页面
        System.out.println(imgUrl);
        return imgUrl;
    }

    // 保存Spu
    @RequestMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo) {
        spuService.saveSpuInfo(pmsProductInfo);
        return "success";
    }

    // 根据spuId获取图片列表
    @RequestMapping("spuImageList")
    public List<PmsProductImage> spuImageList(String spuId){

        List<PmsProductImage> pmsProductImages = spuService.spuImageList(spuId);
        return pmsProductImages;
    }


    // 根据spuId获取销售属性列表
    @RequestMapping("spuSaleAttrList")
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId){

        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrList(spuId);
        return pmsProductSaleAttrs;
    }
}
