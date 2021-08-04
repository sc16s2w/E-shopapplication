package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class UserService {
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;



    static final String KEY_PREFIX = "user:code:phone:";

    static final Logger logger = LoggerFactory.getLogger(UserService.class);


    /**
     * 检验数据是否可用（检验手机号以及用户名）
     * @param data
     * @param type
     * @return
     */
    public Boolean dataCheck(String data, Integer type) {
        User user = new User();
        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                return null;
        }
        return this.userMapper.selectCount(user) == 0;
    }

    /**
     * 发送手机验证码
     * @param phone
     * @return
     */
    public void generateVerificationCode(String phone) {
        if(phone == null) return;
        //生成验证码
        String code = NumberUtils.generateCode(6);
        //发送消息到rabbitmq
        Map<String,String> msg = new HashMap<>();
        msg.put("phone",phone);
        msg.put("code",code);
        this.amqpTemplate.convertAndSend("leyou.sms.exchange","verifycode.sms",msg);
        //把验证码保存到redis
        this.redisTemplate.opsForValue().set(KEY_PREFIX+phone,code,5, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {
        //查询redis中的验证码
        String code_store = this.redisTemplate.opsForValue().get(KEY_PREFIX+user.getPhone());
        //检查验证码
        if(!code_store.equals(code)){
            return;
        }
        //生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        //加盐加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        user.setId(null);
        user.setCreated(new Date());
        //新增用户
        this.userMapper.insertSelective(user);
    }

    /**
     * 登陆
     * @param username
     * @param password
     * @return
     */
    public User query(String username, String password) {
        User user = new User();
        user.setUsername(username);
        User search = this.userMapper.selectOne(user);
        if(search == null){
            return null;
        }
        if (!search.getPassword().equals(CodecUtils.md5Hex(password, search.getSalt()))) {
            return null;
        }
        // 用户名密码都正确
        return search;



    }
}
