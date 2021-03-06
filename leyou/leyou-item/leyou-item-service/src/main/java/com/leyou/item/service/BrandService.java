package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.StringUtil;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 根据查询条件分页并排序查询品牌信息
     *
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    public PageResult<Brand> queryBrandsByPage(String key, Integer page, Integer rows, String sortBy, Boolean desc) {

        // 初始化example对象
        Example example = new Example(Brand.class);
        Example.Criteria criteria = example.createCriteria();

        // 根据name模糊查询，或者根据首字母查询
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("name", "%" + key + "%").orEqualTo("letter", key);
        }

        // 添加分页条件
        PageHelper.startPage(page, rows);

        // 添加排序条件
        if (StringUtils.isNotBlank(sortBy)) {
            example.setOrderByClause(sortBy + " " + (desc ? "desc" : "asc"));
        }

        List<Brand> brands = this.brandMapper.selectByExample(example);
        // 包装成pageInfo
        PageInfo<Brand> pageInfo = new PageInfo<>(brands);
        // 包装成分页结果集返回
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    //自动会滚
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        //先新增brand表，再新增中间表
        Boolean flag = this.brandMapper.insertSelective(brand) == 1;
        if(flag){
            cids.forEach(cid -> {
                this.brandMapper.insertCategoryAndBrand(cid, brand.getId());
            });
        }
    }


    /**
     * 更新一个品牌
     * @param brand
     * @param cids
     */
    @Transactional
    public void UpdateBrand(Brand brand, List<Long> cids) {
        Boolean flag = this.brandMapper.updateByPrimaryKey(brand) == 1;
        if(flag){
            cids.forEach(cid -> {
                this.brandMapper.updateCategoryAndBrand(cid, brand.getId());
            });
        }
    }

    /**
     * 删除一个品牌
     * @param bid
     */
    @Transactional
    public void deleteBrand(Long bid) {
        Boolean flag = this.brandMapper.deleteByPrimaryKey(bid)==1;
        if(flag){
            this.brandMapper.deleteCategoryAndBrand(bid);
        }
    }

    public List<Brand> queryBrandsByCid(Long cid) {
        return this.brandMapper.selectBrandByCid(cid);
    }

    public Brand queryBrandsById(Long id) {
        return this.brandMapper.selectByPrimaryKey(id);
    }
}