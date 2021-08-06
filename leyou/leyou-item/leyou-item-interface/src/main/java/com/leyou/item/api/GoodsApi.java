package com.leyou.item.api;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.extra.SpuExtra;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GoodsApi {


    /**
     * 分批查询spu的服务
     * @param id
     * @return
     */
    @GetMapping("spu/detail/{id}")
    public SpuDetail QueryGoodsById(@PathVariable("id") Long id);

    @GetMapping("spu/page")
    public PageResult<SpuExtra> QuerySpuByPage(@RequestParam(value = "key",required = false) String key,
                                               @RequestParam(value = "saleable",required = false) Boolean saleable,
                                               @RequestParam(value = "page", defaultValue = "1") Integer page,
                                               @RequestParam(value = "rows", defaultValue = "5") Integer rows
    );

    @PostMapping("goods")
    public Void AddGoods(@RequestBody SpuExtra spuExtra);

    @GetMapping("sku/list")
    public List<Sku> QuerySkuByid(@RequestParam("id") Long spuId);

    @GetMapping("{id}")
    public Spu querySpuByid(@PathVariable("id") Long id);

    @GetMapping("sku/{id}")
    public Sku querySkuById(@PathVariable("id")Long id);

}
