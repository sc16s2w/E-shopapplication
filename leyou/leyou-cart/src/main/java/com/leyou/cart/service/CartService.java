package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.intercepter.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private static final String KEY_PREFIX = "user:cart:";

    @Autowired
    private GoodsClient goodsClient;
    /**
     * 添加购物车里的东西到redis
     * @param cart
     * @return
     */
    public void addCart(Cart cart) {
        //获取用户信息
        UserInfo userInfo = LoginInterceptor.getLoginUser();
        //查询购物车记录
        BoundHashOperations<String,Object,Object> hashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX+userInfo.getId());
        //判断商品在不在购物车中
        Long skuId = cart.getSkuId();
        Integer num = cart.getNum();
        //在的话更新数量
        if(hashOperations.hasKey(skuId.toString())){
            String cartJson = hashOperations.get(skuId.toString()).toString();
            JsonUtils.parse(cartJson,Cart.class);
            cart.setNum(cart.getNum()+num);
            hashOperations.put(skuId.toString(),JsonUtils.serialize(cart));
        }else{
            //不再，新增购物车
            cart.setUserId(userInfo.getId());
            // 其它商品信息，需要查询商品服务
            Sku sku = this.goodsClient.querySkuById(skuId);
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : sku.getImages().split(",")[0]);
            cart.setPrice(sku.getPrice());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());
        }
        hashOperations.put(skuId.toString(),JsonUtils.serialize(cart));
        //不在的话新增数量
    }

    /**
     * 查询购物车里的元素
     * @return
     */
    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.getLoginUser();         //查询购物车记录
        if(!this.redisTemplate.hasKey(KEY_PREFIX+userInfo.getId())){
            return null;
        }
        BoundHashOperations<String,Object,Object> hashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX+userInfo.getId());
        //获取map中所有的cart的值
        List<Object> cartsJson = hashOperations.values();
        if(CollectionUtils.isEmpty(cartsJson)){
            return null;
        }
        //把list<object>集合转为list<cart>集合
        return cartsJson.stream().map(cartJson->JsonUtils.parse(cartJson.toString(),Cart.class)).collect(Collectors.toList());
    }

    /**
     * 更新物品数量
     * @param cart
     */
    public void changeCarts(Cart cart) {
        UserInfo userInfo = LoginInterceptor.getLoginUser();
        if(!this.redisTemplate.hasKey(KEY_PREFIX+userInfo.getId())){
            return;
        }
        BoundHashOperations<String,Object,Object> hashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX+userInfo.getId());
        String cartJson = hashOperations.get(cart.getSkuId().toString()).toString();
        Cart cart1 = JsonUtils.parse(cartJson, Cart.class);
        cart1.setNum(cart.getNum());
        hashOperations.put(cart.getSkuId().toString(),JsonUtils.serialize(cart1));
    }

    /**
     * 删除数量
     * @param skuId
     */
    public void deleteCart(String skuId){
        UserInfo userInfo = LoginInterceptor.getLoginUser();
        BoundHashOperations<String,Object,Object> hashOperations = this.redisTemplate.boundHashOps(KEY_PREFIX+userInfo.getId());
        hashOperations.delete(skuId);
    }
}
