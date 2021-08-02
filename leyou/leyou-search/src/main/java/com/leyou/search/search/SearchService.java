package com.leyou.search.search;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {
    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository goodsRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();
        //根据分类id查询分类名称
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(),
                spu.getCid2(), spu.getCid3()));
        //根据品牌id查询品牌
        Brand brand = this.brandClient.QueryBrandById(spu.getBrandId());
        //根据spuid查询所有sku
        List<Sku> skus = this.goodsClient.QuerySkuByid(spu.getId());
        List<Long> prices = new ArrayList<>();
        //收集sku的必要字段信息
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        List<SpecParam> params = this.specificationClient.QuertSpecParamByQid(null, spu.getCid3(), null, true);
        // 查询spuDetail。获取规格参数值
        SpuDetail spuDetail = this.goodsClient.QueryGoodsById(spu.getId());
        // 获取通用的规格参数
        Map<String, Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String, Object>>() {
        });
        // 获取特殊的规格参数
        Map<String, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String, List<Object>>>() {
        });
        // 定义map接收{规格参数名，规格参数值}
        Map<String, Object> paramMap = new HashMap<>();
        for(Sku sku:skus){
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("title", sku.getTitle());
            skuMap.put("price", sku.getPrice());
            if(sku.getImages()!=null){
                String[] store = sku.getImages().split(",");
                skuMap.put("image",store[0]);
            }
            else{
                skuMap.put("image","");
            }
            skuMapList.add(skuMap);

        }
        params.forEach(param -> {
            // 判断是否通用规格参数
            if (param.getGeneric()) {
                // 获取通用规格参数值
                String value = genericSpecMap.get(param.getId().toString()).toString();
                // 判断是否是数值类型
                if (param.getNumeric()){
                    // 如果是数值的话，判断该数值落在那个区间
                    value = chooseSegment(value, param);
                }
                // 把参数名和值放入结果集中
                paramMap.put(param.getName(), value);
            } else {
                List<Object> valueList = specialSpecMap.get(param.getId().toString());
                paramMap.put(param.getName().toString(), valueList);
            }
        });

        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        //拼接all字段
        goods.setAll(spu.getTitle()+" "+String.join(" ",names)+" "+ brand.getName());
        goods.setPrice(prices);
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        goods.setSpecs(paramMap );
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }


    public SearchResult search(SearchRequest searchRequest) {
        if(searchRequest.getKey()==null){
            return null;
        }
        //自定义查询构建
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //添加查询条件
        MatchQueryBuilder basicQuery = QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND);
        queryBuilder.withQuery(basicQuery);
        //只需要部分字段，并不是需要全部字段，在结果集上进行过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(
                new String[]{"id","skus","subTitle"}, null));
        String sortBy = searchRequest.getSortBy();
        Boolean desc = searchRequest.getDescending();
        if(sortBy != null&&sortBy.length()!=0){
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(desc? SortOrder.DESC:SortOrder.ASC));
        }
        String categoryAggName = "categories";
        String brandAggName = "brands";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        //执行查询， 获取结果集
        Page<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());
        List<Map<String,Object>> categories = getCategoryAggResult(((AggregatedPage<Goods>) goodsPage).getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(((AggregatedPage<Goods>) goodsPage).getAggregation(brandAggName));
        int size = SearchRequest.getDefaultSize();
        Long total = goodsPage.getTotalElements();
        int totalPage = (total.intValue()+size-1)/size;
        //判断是否是一个分类，只有一个分类才能做规格参数聚合
        List<Map<String,Object>> specs = null;
        if(categories.size()==1){
            specs = getParamAggResult((Long)categories.get(0).get("id"),basicQuery);
        }
        return new SearchResult(total,totalPage,goodsPage.getContent(),categories,brands,specs);

    }

    /**
     * 解析分类的聚合集
     * @param aggregation
     * @return
     */
    private List<Map<String,Object>> getCategoryAggResult(Aggregation aggregation){
        LongTerms terms = (LongTerms) aggregation;
        List<LongTerms.Bucket> store = terms.getBuckets();
        List<Map<String,Object>> final_result = new ArrayList<>();
        List<Long> cids = new ArrayList<>();
        // 解析所有的id桶，查询品牌
        store.forEach(bucket -> {
            cids.add(bucket.getKeyAsNumber().longValue());
        });
        List<String> names = this.categoryClient.queryNamesByIds(cids);
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cids.get(i));
            map.put("name", names.get(i));
            final_result.add(map);
        }
        return final_result;
    }

    /**
     * 解析品牌的聚合集
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation){
        LongTerms terms = (LongTerms)aggregation;
        List<LongTerms.Bucket> store = terms.getBuckets();
        List<Brand> brands = new ArrayList<>();
        for(LongTerms.Bucket bucket:store){
            long l = bucket.getKeyAsNumber().longValue();
            Brand brand = this.brandClient.QueryBrandById(l);
            brands.add(brand);
        }
        return brands;
    }

    /**
     * 根据查询条件聚合规格参数
     * @param id
     * @param basicQuery
     * @return
     */
    private List<Map<String,Object>> getSpecsAggResult(Long id, QueryBuilder basicQuery){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        //查询要聚合的规格参数
        List<SpecParam> params = this.specificationClient.QuertSpecParamByQid(null,null,null,true);
        //添加规格参数的聚合
        for (SpecParam specParam:params){
            queryBuilder.addAggregation(AggregationBuilders.terms(specParam.getName()).field("specs."+specParam.getName()+".keyword"));
        }
        //添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{},null));
        //执行聚合查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());
        //解析聚合结果集
        Map<String,Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
        List<Map<String,Object>> result = new ArrayList<>();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            //初始化map格式，规格参数名，聚合的是规格参数值
            Map<String,Object> map = new HashMap<>();
            List<Object> options = new ArrayList<>();
            map.put("k",entry.getKey());
            StringTerms terms = (StringTerms) entry.getValue();
            terms.getBuckets().forEach(bucket -> options.add(bucket.getKeyAsString()));
            map.put("options", options);
            result.add(map);
        }
        return result;
    }
    private List<Map<String, Object>> getParamAggResult(Long cid, QueryBuilder basicQuery) {
        try {
            // 创建自定义查询构建器
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            // 基于基本的查询条件，聚合规格参数
            queryBuilder.withQuery(basicQuery);
            // 查询要聚合的规格参数
            List<SpecParam> params = this.specificationClient.QuertSpecParamByQid(null,null,null,true);
            // 添加聚合
            params.forEach(param -> {
                queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
            });
            // 只需要聚合结果集，不需要查询结果集
            queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));

            // 执行聚合查询
            AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

            // 定义一个集合，收集聚合结果集
            List<Map<String, Object>> specs = new ArrayList<>();
            // 解析聚合查询的结果集
            Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
            for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
                // 初始化一个map {k-规格参数名 : options-聚合的规格参数值}
                Map<String, Object> map = new HashMap<>();
                // 放入规格参数名
                map.put("k", entry.getKey());
                // 收集桶中的key-规格参数值
                List<Object> options = new ArrayList<>();
                // 解析每个聚合
                StringTerms terms = (StringTerms) entry.getValue();
                // 遍历每个聚合中桶，把桶中key放入收集规格参数的集合中
                terms.getBuckets().forEach(bucket -> {
                    options.add(bucket.getKeyAsString());
                });
                map.put("options", options);
                specs.add(map);
            }

            return specs;
        } catch (Exception e) {
            logger.error("规格聚合出现异常：", e);
            return null;
        }
    }

    public void createIndex(Long id) throws IOException {

        Spu spu = this.goodsClient.querySpuByid(id);
        // 构建商品
        Goods goods = this.buildGoods(spu);

        // 保存数据到索引库
        this.goodsRepository.save(goods);
    }

    public void deleteIndex(Long id) {
        this.goodsRepository.deleteById(id);
    }
}
