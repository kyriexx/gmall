package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

    @Reference
    SkuService skuService;

    @Autowired
    JestClient jestClient;

    @Test
    public void put() throws IOException {

        // 查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList = skuService.getAllSku();

        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();

        // 转化为es的数据结构
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();

            BeanUtils.copyProperties(pmsSkuInfo, pmsSearchSkuInfo);

            pmsSearchSkuInfo.setId(Long.parseLong(pmsSkuInfo.getId()));

            pmsSearchSkuInfoList.add(pmsSearchSkuInfo);

        }

        // 导入es
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId() + "").build();
            jestClient.execute(put);
        }
    }

    @Test
    public void query() throws IOException {

        /*
         过滤--查询前过滤（推荐）

        GET 库名/表名/_search{
        "query":{
                "bool":{                // bool是联合查询，先过滤，后查询
                "filter":{"term":{},    //filter是过滤，是前端传来的条件，一般都是主外键id
                          "term":{}
                         }
                "must":{"match":{}      //must是搜索，根据输入的关键字搜索
                       }
                    }
                }
            }
        */

        // jest的dsl工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        // filter
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", "53");
        boolQueryBuilder.filter(termQueryBuilder);

        TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("skuAttrValueList.valueId", "42");
        boolQueryBuilder.filter(termQueryBuilder1);

        //TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder("","");

        // must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", "apple");
        boolQueryBuilder.must(matchQueryBuilder);
        // query
        searchSourceBuilder.query(boolQueryBuilder);
        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);
        // highlight
        searchSourceBuilder.highlight(null);

        String dslStr = searchSourceBuilder.toString();

        System.err.println(dslStr);
        /*
        {
          "from" : 0,
          "size" : 20,
          "query" : {
            "bool" : {
              "must" : {
                "match" : {
                  "skuName" : {
                    "query" : "apple"
                  }
                }
              },
              "filter" : [ {
                "term" : {
                  "skuAttrValueList.valueId" : "53"
                }
              }, {
                "term" : {
                  "skuAttrValueList.valueId" : "42"
                }
              } ]
            }
          }
        }
        */

        // 用api执行复杂查询
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        Search search = new Search.Builder(dslStr).addIndex("gmall").addType("PmsSkuInfo").build();

        SearchResult execute = jestClient.execute(search);

        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;

            pmsSearchSkuInfos.add(source);
        }

        System.out.println(pmsSearchSkuInfos.size());//2
    }



    @Test
    public void test() {
        String urlParm = "";
        boolean b = StringUtils.isNotBlank(urlParm);
        System.out.println(b);
    }

}
