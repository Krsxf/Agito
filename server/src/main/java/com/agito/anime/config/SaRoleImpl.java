package com.agito.anime.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 的角色/权限数据源（RBAC：用户-角色）。
 * 从会话缓存读取登录时写入的 roleList，避免每次注解校验都查库。
 */
@Component
public class SaRoleImpl implements StpInterface {

    private final JdbcTemplate jdbcTemplate;

    public SaRoleImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Object cached = cn.dev33.satoken.stp.StpUtil.getSessionByLoginId(loginId).get("roleList");
            if (cached instanceof List<?> list) {
                List<String> roles = new ArrayList<>();
                for (Object o : list) roles.add(String.valueOf(o));
                return roles;
            }
        } catch (Exception ignored) {
        }
        // 兜底直查数据库
        return jdbcTemplate.queryForList(
                "SELECT r.role_code FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE ur.user_id = ?",
                String.class, Long.parseLong(String.valueOf(loginId)));
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 课程项目权限粒度到角色级即可
        return new ArrayList<>();
    }
}
