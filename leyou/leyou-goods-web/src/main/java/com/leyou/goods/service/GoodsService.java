package com.leyou.goods.service;

import com.leyou.goods.client.BrandClient;
import com.leyou.goods.client.CategoryClient;
import com.leyou.goods.client.GoodsClient;
import com.leyou.goods.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoodsService {
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private SpecificationClient specificationClient;

    public Map<String,Object> loadData(Long spuID){
        Map<String,Object> model = new HashMap<>();
        //根据spuid来查询spu id
        Spu spu = this.goodsClient.querySpuByid(spuID);
        //根据spuid来查询spudetail
        SpuDetail spuDetail = this.goodsClient.QueryGoodsById(spuID);
        //根据spuid查询分类
        List<Long> cids = Arrays.asList(spu.getCid1(),spu.getCid2(),spu.getCid3());
        List<String> names = this.categoryClient.queryNamesByIds(cids);
        List<Map<String,Object>> categories = new ArrayList<>();
        for(int i = 0;i< cids.size();i++){
            Map<String,Object> categoryMap = new HashMap<>();
            categoryMap.put("id",cids.get(i));
            categoryMap.put("name",names.get(i));
            categories.add(categoryMap);
        }

        //查询brand
        Brand brand = this.brandClient.QueryBrandById(spu.getBrandId());
        //查询skus
        List<Sku> skus = this.goodsClient.QuerySkuByid(spuID);
        //查询规格参数组
        List<SpecGroup> groups = this.specificationClient.queryGroupsWithParam(spu.getCid3());
        //查询特殊的规格参数
        List<SpecParam> params = this.specificationClient.QuertSpecParamByQid(null,spu.getCid3(),false,null);
        Map<Long, String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(), param.getName());
        });
        model.put("spu",spu);
        model.put("spuDetail",spuDetail);
        model.put("categories",categories);
        model.put("brand",brand);
        model.put("skus",skus);
        model.put("groups",groups);
        model.put("paramMap",paramMap);
        return model;
    }
}
