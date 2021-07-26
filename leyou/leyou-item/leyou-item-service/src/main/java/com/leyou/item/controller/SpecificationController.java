package com.leyou.item.controller;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("spec")
public class SpecificationController {
    @Autowired
    private SpecificationService specificationService;

    /**
     * 根据分类id查询参数组
     * @param cid
     * @return
     */
    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> QuerySpecGroupByCid(@PathVariable("cid") Long cid){
        List<SpecGroup> result = this.specificationService.QuerySpecGroupByCid(cid);
        if(result == null|| result.size()<1){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("group/{id}")
    public ResponseEntity<Void> DeleteGroupById(@PathVariable("id") Long id){
        this.specificationService.DeleteGroupByid(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * 添加group
     * @param specGroup
     * @return
     */
    @PostMapping("group")
    public ResponseEntity<Void> AddSpecGroup(@RequestBody SpecGroup specGroup){
        this.specificationService.AddSpecGroup(specGroup);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("group")
    public ResponseEntity<Void> UpdateSpecGroup(@RequestBody SpecGroup specGroup){
        this.specificationService.UpdateSpecGroup(specGroup);
        return ResponseEntity.noContent().build();
    }

    /**
     * 根据groupid查询params
     * @param gid
     * @return
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> QuertSpecParamByQid(
            @RequestParam(value = "gid",required = false) Long gid,
            @RequestParam(value = "cid",required = false) Long cid,
            @RequestParam(value = "generic",required = false) Boolean generic,
            @RequestParam(value = "searching",required = false) Boolean searching
            ){
        List<SpecParam> result = this.specificationService.QuerySpecParamByQid(gid,cid,generic,searching);
        if(result == null || result.size()<1){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }


    /**
     * 增加参数
     */
    @PostMapping("param")
    public ResponseEntity<Void> AddSpecParam(@RequestBody SpecParam specParam){
        this.specificationService.AddSpecParam(specParam);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    /**
     * 更新参数
     */
    @PutMapping("param")
    public ResponseEntity<Void> UpdateSpecParam(@RequestBody SpecParam specParam){
        this.specificationService.UpdateSpecParam(specParam);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除参数
     */
    @DeleteMapping("param/{id}")
    public ResponseEntity<Void> DeleteSpecParamById(@PathVariable("id") Long id){
        this.specificationService.DeleteSpecParamById(id);
        return ResponseEntity.noContent().build();
    }

}
