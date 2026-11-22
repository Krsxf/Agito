package com.agito.anime.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.BizException;
import com.agito.anime.common.ErrorCode;
import com.agito.anime.entity.AnimePlatform;
import com.agito.anime.entity.JumpLog;
import com.agito.anime.mapper.AnimePlatformMapper;
import com.agito.anime.mapper.JumpLogMapper;
import com.agito.anime.service.JumpService;
import com.agito.anime.service.SysLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跳转外链服务：体系结构 4.2 节"跳转外链独立成服务，合规边界在结构上清晰可见"。
 * 白名单校验（anime.compliance.platform-whitelist）确保只跳转正版平台；
 * 低频 HEAD 探测做链接有效性校验（正常功能，非爬取行为）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JumpServiceImpl implements JumpService {

    private final AnimePlatformMapper platformMapper;
    private final JumpLogMapper jumpLogMapper;
    private final SysLogService sysLogService;

    @Value("${anime.compliance.platform-whitelist:}")
    private List<String> platformWhitelist;

    @Override
    public Map<String, Object> jump(Long animeId, String platformName) {
        AnimePlatform p = platformMapper.selectOne(new LambdaQueryWrapper<AnimePlatform>()
                .eq(AnimePlatform::getAnimeId, animeId)
                .eq(AnimePlatform::getPlatformName, platformName)
                .orderByDesc(AnimePlatform::getWeight)
                .last("LIMIT 1"));
        if (p == null) {
            throw BizException.notFound("未找到该平台的观看入口");
        }
        if (p.getLinkStatus() != null && p.getLinkStatus() == 0) {
            throw BizException.linkInvalid("链接已失效，正在等待管理员修复");
        }
        // 平台白名单（合规边界：只跳转正版平台）
        if (platformWhitelist != null && !platformWhitelist.isEmpty()
                && !platformWhitelist.contains(platformName)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "平台不在白名单内，拒绝跳转");
        }

        // 记录跳转日志（游客 uid 为空）
        Long uid = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        JumpLog jumpLog = new JumpLog();
        jumpLog.setUserId(uid);
        jumpLog.setAnimeId(animeId);
        jumpLog.setPlatformName(platformName);
        jumpLog.setUrl(p.getUrl());
        jumpLog.setIsValid(1);
        jumpLog.setJumpTime(LocalDateTime.now());
        jumpLogMapper.insert(jumpLog);
        sysLogService.log(uid, "JUMP", "jump", animeId + " → " + platformName);

        Map<String, Object> data = new HashMap<>();
        data.put("url", p.getUrl());
        data.put("platformName", platformName);
        data.put("isPaid", p.getIsPaid());
        data.put("regionLimit", p.getRegionLimit());
        return data;
    }

    @Override
    public int checkLinks() {
        List<AnimePlatform> all = platformMapper.selectList(null);
        int invalid = 0;
        for (AnimePlatform p : all) {
            boolean ok = headCheck(p.getUrl());
            AnimePlatform upd = new AnimePlatform();
            upd.setId(p.getId());
            upd.setLinkStatus(ok ? 1 : 0);
            upd.setCheckTime(LocalDateTime.now());
            platformMapper.updateById(upd);
            if (!ok) {
                invalid++;
                log.warn("链接失效：animeId={} platform={} url={}", p.getAnimeId(), p.getPlatformName(), p.getUrl());
            }
        }
        sysLogService.log(null, "IMPORT", "checkLinks", "批量校验链接完成，失效 " + invalid + " 条");
        return invalid;
    }

    /** 低频 HEAD 探测（连接 3 秒超时；探测失败视为失效——供管理端修复演示） */
    private boolean headCheck(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }
}
