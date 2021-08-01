package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequestMapping("spec")
public interface SpecificationApi {
    @GetMapping("groups/{cid}")
    public List<SpecGroup> QuerySpecGroupByCid(@PathVariable("cid") Long cid);

    @DeleteMapping("group/{id}")
    public ResponseEntity<Void> DeleteGroupById(@PathVariable("id") Long id);

    /**
     * 添加group
     * @param specGroup
     * @return
     */
    @PostMapping("group")
    public Void AddSpecGroup(@RequestBody SpecGroup specGroup);

    @PutMapping("group")
    public Void UpdateSpecGroup(@RequestBody SpecGroup specGroup);

    /**
     * 根据groupid查询params
     * @param gid
     * @return
     */
    @GetMapping("params")
    public List<SpecParam> QuertSpecParamByQid(
            @RequestParam(value = "gid",required = false) Long gid,
            @RequestParam(value = "cid",required = false) Long cid,
            @RequestParam(value = "generic",required = false) Boolean generic,
            @RequestParam(value = "searching",required = false) Boolean searching
    );

    /**
     * 增加参数
     */
    @PostMapping("param")
    public Void AddSpecParam(@RequestBody SpecParam specParam);

    /**
     * 更新参数
     */
    @PutMapping("param")
    public Void UpdateSpecParam(@RequestBody SpecParam specParam);

    /**
     * 删除参数
     */
    @DeleteMapping("param/{id}")
    public Void DeleteSpecParamById(@PathVariable("id") Long id);

    @GetMapping("group/param/{cid}")
    public List<SpecGroup> queryGroupsWithParam(@PathVariable("cid")Long cid);
}
