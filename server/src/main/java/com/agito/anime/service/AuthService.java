package com.agito.anime.service;

import com.agito.anime.dto.LoginDTO;
import com.agito.anime.dto.RegisterDTO;

import java.util.Map;

public interface AuthService {

    /** 注册：校验用户名唯一、BCrypt 加密落库、写入兴趣标签初始画像，返回登录 token */
    String register(RegisterDTO dto);

    /** 登录：密码错误 5 次锁定 30 分钟（4.5 节账号锁定），成功签发 token（2 小时） */
    String login(LoginDTO dto);

    /** 当前登录用户信息（昵称、角色、兴趣标签、未读提醒数） */
    Map<String, Object> userInfo();
}
