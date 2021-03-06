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
        //????????????id??????????????????
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(),
                spu.getCid2(), spu.getCid3()));
        //????????????id????????????
        Brand brand = this.brandClient.QueryBrandById(spu.getBrandId());
        //??????spuid????????????sku
        List<Sku> skus = this.goodsClient.QuerySkuByid(spu.getId());
        List<Long> prices = new ArrayList<>();
        //??????sku?????????????????????
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        List<SpecParam> params = this.specificationClient.QuertSpecParamByQid(null, spu.getCid3(), null, true);
        // ??????spuDetail????????????????????????
        SpuDetail spuDetail = this.goodsClient.QueryGoodsById(spu.getId());
        // ???????????????????????????
        Map<String, Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String, Object>>() {
        });
        // ???????????????????????????
        Map<String, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String, List<Object>>>() {
        });
        // ??????map??????{?????????????????????????????????}
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
            // ??????????????????????????????
            if (param.getGeneric()) {
                // ???????????????????????????
                String value = genericSpecMap.get(param.getId().toString()).toString();
                // ???????????????????????????
                if (param.getNumeric()){
                    // ?????????????????????????????????????????????????????????
                    value = chooseSegment(value, param);
                }
                // ????????????????????????????????????
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
        //??????all??????
        goods.setAll(spu.getTitle()+" "+String.join(" ",names)+" "+ brand.getName());
        goods.setPrice(prices);
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        goods.setSpecs(paramMap );
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "??????";
        // ???????????????
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // ??????????????????
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // ????????????????????????
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "??????";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "??????";
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
        //?????????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //??????????????????
        MatchQueryBuilder basicQuery = QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND);
        queryBuilder.withQuery(basicQuery);
        //?????????????????????????????????????????????????????????????????????????????????
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
        //??????????????? ???????????????
        Page<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());
        List<Map<String,Object>> categories = getCategoryAggResult(((AggregatedPage<Goods>) goodsPage).getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(((AggregatedPage<Goods>) goodsPage).getAggregation(brandAggName));
        int size = SearchRequest.getDefaultSize();
        Long total = goodsPage.getTotalElements();
        int totalPage = (total.intValue()+size-1)/size;
        //???????????????????????????????????????????????????????????????????????????
        List<Map<String,Object>> specs = null;
        if(categories.size()==1){
            specs = getParamAggResult((Long)categories.get(0).get("id"),basicQuery);
        }
        return new SearchResult(total,totalPage,goodsPage.getContent(),categories,brands,specs);

    }

    /**
     * ????????????????????????
     * @param aggregation
     * @return
     */
    private List<Map<String,Object>> getCategoryAggResult(Aggregation aggregation){
        LongTerms terms = (LongTerms) aggregation;
        List<LongTerms.Bucket> store = terms.getBuckets();
        List<Map<String,Object>> final_result = new ArrayList<>();
        List<Long> cids = new ArrayList<>();
        // ???????????????id??????????????????
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
     * ????????????????????????
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
     * ????????????????????????????????????
     * @param id
     * @param basicQuery
     * @return
     */
    private List<Map<String,Object>> getSpecsAggResult(Long id, QueryBuilder basicQuery){
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        //??????????????????????????????
        List<SpecParam> params = this.specificationClient.QuertSpecParamByQid(null,null,null,true);
        //???????????????????????????
        for (SpecParam specParam:params){
            queryBuilder.addAggregation(AggregationBuilders.terms(specParam.getName()).field("specs."+specParam.getName()+".keyword"));
        }
        //?????????????????????
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{},null));
        //??????????????????
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());
        //?????????????????????
        Map<String,Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
        List<Map<String,Object>> result = new ArrayList<>();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            //?????????map??????????????????????????????????????????????????????
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
            // ??????????????????????????????
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            // ????????????????????????????????????????????????
            queryBuilder.withQuery(basicQuery);
            // ??????????????????????????????
            List<SpecParam> params = this.specificationClient.QuertSpecParamByQid(null,null,null,true);
            // ????????????
            params.forEach(param -> {
                queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
            });
            // ???????????????????????????????????????????????????
            queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));

            // ??????????????????
            AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

            // ??????????????????????????????????????????
            List<Map<String, Object>> specs = new ArrayList<>();
            // ??????????????????????????????
            Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
            for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
                // ???????????????map {k-??????????????? : options-????????????????????????}
                Map<String, Object> map = new HashMap<>();
                // ?????????????????????
                map.put("k", entry.getKey());
                // ???????????????key-???????????????
                List<Object> options = new ArrayList<>();
                // ??????????????????
                StringTerms terms = (StringTerms) entry.getValue();
                // ????????????????????????????????????key????????????????????????????????????
                terms.getBuckets().forEach(bucket -> {
                    options.add(bucket.getKeyAsString());
                });
                map.put("options", options);
                specs.add(map);
            }

            return specs;
        } catch (Exception e) {
            logger.error("???????????????????????????", e);
            return null;
        }
    }

    public void createIndex(Long id) throws IOException {

        Spu spu = this.goodsClient.querySpuByid(id);
        // ????????????
        Goods goods = this.buildGoods(spu);

        // ????????????????????????
        this.goodsRepository.save(goods);
    }

    public void deleteIndex(Long id) {
        this.goodsRepository.deleteById(id);
    }
}
