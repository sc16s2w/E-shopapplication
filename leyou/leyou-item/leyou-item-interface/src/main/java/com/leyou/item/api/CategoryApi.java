package com.leyou.item.api;

import com.leyou.item.pojo.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface CategoryApi {
    /**
     * 根据父id查询子节点
     * @param pid
     * @return
     */
    @GetMapping("list")
    public List<Category> queryCategoriesByPid(@RequestParam("pid") String pid);


    @GetMapping("bid/{bid}")
    public List<Category> queryByBrandID(@PathVariable("bid") Long bid);


    /**
     * 根据id查询名字
     * @param ids
     * @return
     */
    @GetMapping
    public List<String> queryNamesByIds(@RequestParam("ids")List<Long> ids);
}
