package com.agito.anime.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.BizException;
import com.agito.anime.common.ErrorCode;
import com.agito.anime.dto.LoginDTO;
import com.agito.anime.dto.RegisterDTO;
import com.agito.anime.entity.InterestTag;
import com.agito.anime.entity.Role;
import com.agito.anime.entity.User;
import com.agito.anime.mapper.InterestTagMapper;
import com.agito.anime.mapper.RoleMapper;
import com.agito.anime.mapper.UserMapper;
import com.agito.anime.service.AuthService;
import com.agito.anime.service.SysLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鉴权服务：注册（BCrypt 加密 + 兴趣标签初始画像）/ 登录（密码错误锁定）/ 会话角色绑定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final InterestTagMapper interestTagMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SysLogService sysLogService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final Map<String, Integer> failCount = new ConcurrentHashMap<>();
    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 30;

    @Override
    @Transactional
    public String register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, "用户名已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null ? dto.getUsername() : dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        user.setBehaviorCount(0);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.insert(user);

        Role userRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "USER"));
        jdbcTemplate.update("INSERT INTO user_role(user_id, role_id) VALUES(?, ?)",
                user.getId(), userRole.getId());

        if (dto.getInterestCodes() != null && !dto.getInterestCodes().isEmpty()) {
            List<InterestTag> tags = interestTagMapper.selectList(
                    new LambdaQueryWrapper<InterestTag>()
                            .in(InterestTag::getTagCode, dto.getInterestCodes()));
            for (InterestTag tag : tags) {
                jdbcTemplate.update("INSERT INTO user_interest(user_id, tag_id) VALUES(?, ?)",
                        user.getId(), tag.getId());
            }
        }

        StpUtil.login(user.getId());
        bindRolesToSession(user.getId());
        sysLogService.log(user.getId(), "LOGIN", "register", "新用户注册：" + dto.getUsername());
        return StpUtil.getTokenValue();
    }

    @Override
    public String login(LoginDTO dto) {
        String key = dto.getUsername().toLowerCase();
        Integer fails = failCount.getOrDefault(key, 0);
        if (fails >= MAX_FAIL) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED,
                    "密码错误次数过多，账号已锁定，请 " + LOCK_MINUTES + " 分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            failCount.put(key, fails + 1);
            throw new BizException(ErrorCode.LOGIN_FAILED,
                    "用户名或密码错误（" + (fails + 1) + "/" + MAX_FAIL + "）");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED, "账号已被禁用，请联系管理员");
        }

        failCount.remove(key);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        StpUtil.login(user.getId());
        bindRolesToSession(user.getId());
        sysLogService.log(user.getId(), "LOGIN", "login", "用户登录：" + dto.getUsername());
        return StpUtil.getTokenValue();
    }

    @Override
    public Map<String, Object> userInfo() {
        long uid = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(uid);
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("isAdmin", StpUtil.getRoleList().contains("ADMIN"));

        List<String> interests = jdbcTemplate.queryForList(
                "SELECT t.tag_name FROM user_interest ui JOIN interest_tag t ON ui.tag_id = t.id WHERE ui.user_id = ?",
                String.class, uid);
        info.put("interests", interests);
        info.put("behaviorCount", user.getBehaviorCount());
        return info;
    }

    /**
     * 登录后把角色列表挂到会话（RBAC：用户-角色-权限）。
     * Sa-Token 角色存取走 StpInterface 实现（SaRoleImpl），此处仅写入 session 数据。
     */
    public void bindRolesToSession(long uid) {
        List<String> roleCodes = jdbcTemplate.queryForList(
                "SELECT r.role_code FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE ur.user_id = ?",
                String.class, uid);
        StpUtil.getSession().set("roleList", roleCodes);
    }
}
