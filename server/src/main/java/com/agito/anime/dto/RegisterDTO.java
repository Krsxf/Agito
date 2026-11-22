package com.agito.anime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度须为 3~20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须为 6~20 位")
    private String password;

    private String nickname;
    private String phone;
    private String email;

    /** 注册时勾选的兴趣标签 code 列表（冷启动初始画像，12 类多选） */
    private List<String> interestCodes;
}
