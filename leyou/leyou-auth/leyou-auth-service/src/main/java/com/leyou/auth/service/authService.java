package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class authService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登陆
     * @param username
     * @param password
     */
    public String login(String username, String password) {
       //根据用户名和密码去查询
        User user = this.userClient.query(username,password);
       //判断user
        if(user == null){
            return null;
        }
        try{
            //jwtutils生成jwt类型的token
            UserInfo userInfo = new UserInfo();
            userInfo.setId(userInfo.getId());
            userInfo.setUsername(user.getUsername());
            return JwtUtils.generateToken(userInfo,this.jwtProperties.getPrivateKey(),1800);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
