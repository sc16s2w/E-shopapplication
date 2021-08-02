package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.extra.SpuExtra;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.pojo.Stock;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Service
public class GoodService {
    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 根据条件分页查询商品信息
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    public PageResult<SpuExtra> querySpuByPage(String key, Boolean saleable, Integer page, Integer rows) {
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //添加查询条件
        if (!key.isEmpty()) {
            criteria.andLike("title", "%" + key + "%");
        }
        //添加上下架的过滤条件
        if(saleable!=null){
            criteria.andEqualTo("saleable",saleable);
        }
        PageHelper.startPage(page, rows);
        //执行查询，获取spu集合
        List<Spu> spus = this.spuMapper.selectByExample(example);
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);
        //spu集合转化成spubo集合
        List<SpuExtra> spuExtras= new ArrayList<>();
        spus.forEach(spu->{
            SpuExtra spuExtra = new SpuExtra();
            // copy共同属性的值到新的对象
            BeanUtils.copyProperties(spu, spuExtra);
            // 查询分类名称
            List<String> names = this.categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            spuExtra.setCname(String.join("/", names));

            // 查询品牌的名称
            spuExtra.setBname(this.brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());

            spuExtras.add(spuExtra);
        });

        return new PageResult<>(pageInfo.getTotal(), spuExtras);
        //返回pageresult<spuBo>
    }

    /**
     * 新增商品
     * @param spuExtra
     */
    @Transactional
    public void AddGoods(SpuExtra spuExtra) {
        //先新增spu
        spuExtra.setId(null);
        spuExtra.setSaleable(true);
        spuExtra.setValid(true);
        spuExtra.setCreateTime(new Date());
        spuExtra.setLastUpdateTime(spuExtra.getCreateTime());
        this.spuMapper.insertSelective(spuExtra);
        //新增spudetail
        SpuDetail spudetail = spuExtra.getSpuDetail();
        spudetail.setSpuId(spuExtra.getId());
        this.spuDetailMapper.insertSelective(spudetail);
        //新增sku
        List<Sku> skus = spuExtra.getSkus();
        System.out.println(skus.size());
        for(Sku sku:skus){
            sku.setId(null);
            sku.setSpuId(spuExtra.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insertSelective(sku);
            //新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            this.stockMapper.insertSelective(stock);
        }
//        try{
//            this.amqpTemplate.convertAndSend("item.insert",spuExtra.getId());
//        }catch (AmqpException e){
//            e.printStackTrace();
//        }
        sendMessage(spuExtra.getId(),"insert");
    }

    /**
     * 根据id查商品
     * @param id
     * @return
     */
    public SpuDetail QueryGoodsById(Long id) {
        return this.spuDetailMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据spuid来查询skus
     * @param spuId
     * @return
     */
    public List<Sku> QuerySkuByid(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skus = this.skuMapper.select(sku);
        skus.forEach(s -> {
            Stock stock = this.stockMapper.selectByPrimaryKey(s.getId());
            s.setStock(stock.getStock());
        });
        return skus;
    }

    /**
     * 更新商品
     * @param spuExtra
     */
    @Transactional
    public void UpdateGoods(SpuExtra spuExtra) {
        List<Sku> skus = this.QuerySkuByid(spuExtra.getId());
        if(!CollectionUtils.isEmpty(skus)) {
            for (Sku sku: skus) {
                Stock stock = new Stock();
                stock.setStock(sku.getStock());
                stock.setSkuId(sku.getId());
                this.stockMapper.delete(stock);
                this.skuMapper.delete(sku);
            }
        }
        skus = spuExtra.getSkus();
        for(Sku sku: skus){
            sku.setId(null);
            sku.setSpuId(spuExtra.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insertSelective(sku);
            //新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            this.stockMapper.insertSelective(stock);
        }
        spuExtra.setLastUpdateTime(new Date());
        spuExtra.setCreateTime(null);
        spuExtra.setValid(null);
        spuExtra.setSaleable(null);
        this.spuMapper.updateByPrimaryKeySelective(spuExtra);
        this.spuDetailMapper.updateByPrimaryKeySelective(spuExtra.getSpuDetail());
        sendMessage(spuExtra.getId(),"update");
    }

    /**
     * 商品上下架
     * @param id
     */
    public void UpdateSaleableById(Long id) {
        Spu spu = new Spu();
        spu.setId(id);
        spu = this.spuMapper.selectByPrimaryKey(spu);
        if(spu.getSaleable() == false) spu.setSaleable(true);
        else spu.setSaleable(false);
        this.spuMapper.updateByPrimaryKeySelective(spu);
    }


    /**
     * 删除商品
     * @param id
     */
    @Transactional
    public void DeleteGoodsByid(Long id){
        List<Sku> skus = this.QuerySkuByid(id);
        for(Sku sku:skus){
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            this.stockMapper.delete(stock);
            this.skuMapper.delete(sku);
        }
        Spu spu = new Spu();
        spu.setId(id);
        this.spuMapper.delete(spu);
        SpuDetail spuDetail = new SpuDetail();
        spuDetail.setSpuId(id);
        this.spuDetailMapper.delete(spuDetail);
    }

    /**
     * 根据id查询spu对象
     * @param id
     * @return
     */
    public Spu querySpuByid(Long id) {
        return this.spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 发送消息到rabbitmq里面
     * @param id
     * @param type
     */
    private void sendMessage(Long id, String type){
        // 发送消息
        try {
            this.amqpTemplate.convertAndSend("item." + type, id);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }
}
