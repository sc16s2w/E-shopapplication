package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.extra.SpuExtra;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.service.GoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
public class GoodController {

    @Autowired
    private GoodService goodService;

    /**
     * 根据条件分页查询商品信息
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    @GetMapping("spu/page")
    public ResponseEntity<PageResult<SpuExtra>> QuerySpuByPage(
            @RequestParam(value = "key",required = false) String key,
            @RequestParam(value = "saleable",required = false) Boolean saleable,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows
            ){
        PageResult<SpuExtra> result = this.goodService.querySpuByPage(key,saleable,page,rows);
        if(result==null|| CollectionUtils.isEmpty(result.getItems())){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 新增商品
     * @param spuExtra
     * @return
     */
    @PostMapping("goods")
    public ResponseEntity<Void> AddGoods(@RequestBody SpuExtra spuExtra){
        this.goodService.AddGoods(spuExtra);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 更新商品
     * @param spuExtra
     * @return
     */
    @PutMapping("goods")
    public ResponseEntity<Void> UpdateGoods(@RequestBody SpuExtra spuExtra){
        this.goodService.UpdateGoods(spuExtra);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    /**
     * 根据id查商品
     * @param id
     * @return
     */
    @GetMapping("spu/detail/{id}")
    public ResponseEntity<SpuDetail> QueryGoodsById(@PathVariable("id") Long id){
        SpuDetail spuDetail = this.goodService.QueryGoodsById(id);
        if(spuDetail== null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(spuDetail);
    }

    /**
     * 根据id查询squ信息
     * @param spuId
     * @return
     */
    @GetMapping("sku/list")
    public ResponseEntity<List<Sku>> QuerySkuByid(@RequestParam("id") Long spuId){
        List<Sku> skus = this.goodService.QuerySkuByid(spuId);
        if(skus == null||skus.size()<1){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(skus);
    }

    /**
     * 对商品进行上下架
     * @param id
     * @return
     */
    @PutMapping("goods/saleable/{id}")
    public ResponseEntity<Void> UpdateSaleableById(@PathVariable("id")Long id){
        this.goodService.UpdateSaleableById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 根据id删除商品
     * @param id
     * @return
     */
    @DeleteMapping("goods/{id}")
    public ResponseEntity<Void> DeleteGoodsById(@PathVariable("id") Long id){
        this.goodService.DeleteGoodsByid(id);
        return ResponseEntity.noContent().build();
    }
}
