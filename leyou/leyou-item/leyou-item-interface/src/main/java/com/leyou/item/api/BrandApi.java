package com.leyou.item.api;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Brand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface BrandApi {
    /**
     * 根据查询条件分页并排序查询品牌信息
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    @GetMapping("page")
    public PageResult<Brand> queryBrandsByPage(
            @RequestParam(value = "key", required = false)String key,
            @RequestParam(value = "page", defaultValue = "1")Integer page,
            @RequestParam(value = "rows", defaultValue = "5")Integer rows,
            @RequestParam(value = "sortBy", required = false)String sortBy,
            @RequestParam(value = "desc", required = false)Boolean desc
    );

    @PostMapping
    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    public Void saveBrand(Brand brand, @RequestParam("cids") List<Long> cids);

    @PutMapping
    public Void updateBrand(Brand brand, @RequestParam("cids") List<Long> cids);

    @DeleteMapping("delete/{bid}")
    public Void deleteBrand(@PathVariable("bid") Long bid);

    /**
     * 根据cid查询brand信息
     * @param cid
     * @return
     */
    @GetMapping("cid/{cid}")
    public List<Brand> QueryBrandByCid(@PathVariable("cid") Long cid);


    @GetMapping("{id}")
    public Brand QueryBrandById(@PathVariable("id")Long id);
}
