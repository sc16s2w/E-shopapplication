package com.leyou.search.search;

import com.leyou.item.pojo.Spu;
import com.leyou.search.pojo.Goods;

public class SearchService {
    public Goods buildGoods(Spu spu){
        Goods goods = new Goods();
        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        //拼接all字段
        goods.setAll(spu.getTitle()+" "+null+" "+ null);
        goods.setPrice(null);
        goods.setSkus(null);
        goods.setSpecs(null);
        return goods;
    }
}
