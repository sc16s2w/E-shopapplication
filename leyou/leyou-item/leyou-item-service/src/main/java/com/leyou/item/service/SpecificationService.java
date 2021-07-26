package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecificationService {
    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 查询group
     * @param cid
     * @return
     */
    public List<SpecGroup> QuerySpecGroupByCid(Long cid) {
        SpecGroup record = new SpecGroup();
        record.setCid(cid);
        return this.specGroupMapper.select(record);
    }

    /**
     * 查询param
     * @param gid
     * @return
     */
    public List<SpecParam> QuerySpecParamByQid(Long gid,Long cid,Boolean generic,Boolean searching){
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setGeneric(generic);
        specParam.setSearching(searching);
        return this.specParamMapper.select(specParam);
    }

    /**
     * 增加group
     * @param specGroup
     */
    public void AddSpecGroup(SpecGroup specGroup) {
        this.specGroupMapper.insert(specGroup);
    }

    /**
     * 更新group
     * @param specGroup
     */
    public void UpdateSpecGroup(SpecGroup specGroup) {
        this.specGroupMapper.updateByPrimaryKey(specGroup);

    }

    /**
     * 根据id删除group
     * @param id
     */
    public void DeleteGroupByid(Long id) {
        this.specGroupMapper.deleteByPrimaryKey(id);
    }

    /**
     * 增加参数
     * @param specParam
     */
    public void AddSpecParam(SpecParam specParam) {
        this.specParamMapper.insert(specParam);
    }

    /**
     * 更新参数
     * @param specParam
     */
    public void UpdateSpecParam(SpecParam specParam) {
        this.specParamMapper.updateByPrimaryKey(specParam);
    }

    /**
     * 删除参数
     * @param id
     */
    public void DeleteSpecParamById(Long id) {
        this.specParamMapper.deleteByPrimaryKey(id);
    }
}
