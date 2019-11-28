package com.atguigu.gmall.manage;

import com.atguigu.gmall.util.PmsUploadUtil;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

    @Test
    public void contextLoads() throws IOException, MyException {
        String imgUrl = "http://192.168.2.125";

        // 上传图片到服务器
        // 配置fdfs的全局链接地址
        // 获得配置文件的路径
        String tracker = PmsUploadUtil.class.getResource("/tracker.conf").getPath();

        System.out.println("配置文件的路径"+tracker);

        ClientGlobal.init(tracker);

        TrackerClient trackerClient = new TrackerClient();

        // 获得一个trackerServer的实例
        TrackerServer trackerServer = null;

        trackerServer = trackerClient.getConnection();

        // 通过tracker获得一个Storage链接客户端
        StorageClient storageClient = new StorageClient(trackerServer, null);

        try {
            String[] uploadInfos = storageClient.upload_file("E:\\QQDownload\\软件\\tanj.jpg", ".jpg", null);

            for (String uploadInfo : uploadInfos) {
                imgUrl += "/" + uploadInfo;
                System.out.println(uploadInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("可访问fastDFS服务器上的图片的地址"+imgUrl);
    }

}
