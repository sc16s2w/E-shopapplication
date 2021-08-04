package com.leyou.user.controller;

import com.leyou.user.pojo.User;
import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 数据检验用户名和密码
     * @param data
     * @param type
     * @return
     */
    @GetMapping("/check/{data}/{type}")
    private ResponseEntity<Boolean> dataCheck(@PathVariable("data") String data,@PathVariable("type") Integer type){
        Boolean check = this.userService.dataCheck(data,type);
        if(check == null){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(check);
    }

    /**
     * 发送手机验证码
     * @param phone
     * @return
     */
    @PostMapping("/code")
    private ResponseEntity<Void> generateVerificationCode(@RequestParam("phone") String phone){
        this.userService.generateVerificationCode(phone);
        return ResponseEntity.noContent().build();
    }

    /**
     * 用用户信息注册
     * @param user
     * @param code
     * @return
     */
    @PostMapping("/register")
    private ResponseEntity<Void> register(@Valid User user, @RequestParam("code") String code){
        this.userService.register(user,code);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    /**
     * 登陆
     * @param username
     * @param password
     * @return
     */
    @GetMapping("/query")
    private ResponseEntity<User> query(@RequestParam("username") String username,
                                       @RequestParam("password") String password){
        User user = this.userService.query(username,password);
        if(user == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(user);
    }
}
