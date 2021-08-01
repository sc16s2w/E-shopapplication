package com.leyou.elasticsearch.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.leyou.LeyouSearchService;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.extra.SpuExtra;
import com.leyou.item.pojo.Spu;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Item;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.search.SearchService;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.Item;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticsearchTest {
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GoodsRepository goodsRepository;


    @Test
    public void test() {
        // 创建索引
        this.elasticsearchTemplate.createIndex(Goods.class);
        // 配置映射
        this.elasticsearchTemplate.putMapping(Goods.class);
        Integer page = 1;
        Integer rows = 100;
        do {
            // 分批查询spuBo
            PageResult<SpuExtra> pageResult = this.goodsClient.QuerySpuByPage("", true, page, rows);
            System.out.println(pageResult.getItems());
            // 遍历spubo集合转化为List<Goods>
            List<Goods> goodsList = pageResult.getItems().stream().map(spuExtra -> {
                try {
                    return this.searchService.buildGoods((Spu) spuExtra);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
            this.goodsRepository.saveAll(goodsList);

            // 获取当前页的数据条数，如果是最后一页，没有100条
            rows = pageResult.getItems().size();
            // 每次循环页码加1
            page++;
        } while (rows == 100);
    }
    @Test
    public void ReLoadData(){
        elasticsearchTemplate.createIndex(Goods.class);
        elasticsearchTemplate.putMapping(Goods.class);
        Integer page = 1;
        Integer rows = 100;
        do {
            PageResult<SpuExtra> result = this.goodsClient.QuerySpuByPage("", null, page, rows);
            List<SpuExtra> items = result.getItems();
            List<Goods> goods = items.stream().map(spuExtra -> {
                try {
                    return searchService.buildGoods(spuExtra);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
            goodsRepository.saveAll(goods);
            rows = items.size();
            page++;
        }while (rows == 100);
    }

    @Test
    public void testAgg(){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 不查询任何结果
        queryBuilder.withQuery(QueryBuilders.termQuery("cid3",76)).withSourceFilter(new FetchSourceFilter(new String[]{""},null)).withPageable(PageRequest.of(0,1));
        Page<Goods> goodsPage = this.goodsRepository.search(queryBuilder.build());
        goodsPage.forEach(System.out::println);
    }

}
