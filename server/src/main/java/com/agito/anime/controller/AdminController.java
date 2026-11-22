package com.agito.anime.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.Result;
import com.agito.anime.entity.Anime;
import com.agito.anime.entity.Notification;
import com.agito.anime.service.AdminService;
import com.agito.anime.service.RecommendService;
import com.agito.anime.service.SysLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 后台管理（RBAC：仅 ADMIN 角色可访问；操作日志留存 180 天）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
@Tag(name = "07-后台管理")
public class AdminController {

    private final AdminService adminService;
    private final SysLogService sysLogService;
    private final com.agito.anime.mapper.NotificationMapper notificationMapper;
    private final com.agito.anime.mapper.RecConfigMapper recConfigMapper;
    private final RecommendService recommendService;

    @Operation(summary = "运营概览：番剧总量/注册用户/今日跳转/失效链接")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(adminService.overview());
    }

    @Operation(summary = "番剧管理：分页（状态筛选/关键词）")
    @GetMapping("/anime")
    public Result<Map<String, Object>> pageAnimes(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.pageAnimes(pageNum, pageSize, state, keyword));
    }

    @Operation(summary = "新增/编辑番剧（管理员随时修改番剧信息）")
    @PostMapping("/anime")
    public Result<Void> saveAnime(@RequestBody Anime anime) {
        adminService.saveAnime(anime);
        return Result.ok();
    }

    @Operation(summary = "为番剧新增观看平台外链")
    @PostMapping("/platform")
    public Result<Void> addPlatform(@RequestParam Long animeId,
                                    @RequestParam String platformName,
                                    @RequestParam String url) {
        adminService.addPlatform(animeId, platformName, url);
        return Result.ok();
    }

    @Operation(summary = "上传番剧封面（存服务器静态目录，全站用户可见）")
    @PostMapping("/upload-cover")
    public Result<Map<String, Object>> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = adminService.uploadCover(file);
        return Result.ok(Map.of("url", url));
    }

    @Operation(summary = "上架/下架")
    @PostMapping("/anime/{id}/state")
    public Result<Void> changeState(@PathVariable Long id, @RequestParam int state) {
        adminService.changeState(id, state);
        return Result.ok();
    }

    @Operation(summary = "修复失效链接")
    @PostMapping("/platform/{platformId}/fix")
    public Result<Void> fixPlatform(@PathVariable Long platformId, @RequestParam String url) {
        adminService.fixPlatform(platformId, url);
        return Result.ok();
    }

    @Operation(summary = "批量校验链接有效性")
    @PostMapping("/platform/check")
    public Result<Map<String, Object>> checkLinks(
            com.agito.anime.service.JumpService jumpService) {
        return Result.ok(Map.of("invalidCount", jumpService.checkLinks()));
    }

    @Operation(summary = "人工审核队列")
    @GetMapping("/review")
    public Result<Map<String, Object>> reviewList() {
        return Result.ok(adminService.reviewList());
    }

    @Operation(summary = "审核通过（可编辑作品信息与跳转链接后上架）")
    @PostMapping("/review/{id}/pass")
    public Result<Void> reviewPass(@PathVariable Long id,
                                   @RequestBody(required = false) Map<String, Object> edited) {
        adminService.reviewPass(id, edited);
        return Result.ok();
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/review/{id}/reject")
    public Result<Void> reviewReject(@PathVariable Long id) {
        adminService.reviewReject(id);
        return Result.ok();
    }

    @Operation(summary = "Excel 批量导入（导入→清洗→归一化→审核→入库）")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(adminService.importExcel(file));
    }

    @Operation(summary = "Excel 导出")
    @PostMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword) throws Exception {
        byte[] bytes = adminService.exportExcel(keyword);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=anime-export.xlsx")
                .header("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }

    @Operation(summary = "推荐参数读取（四因子权重）")
    @GetMapping("/rec-config")
    public Result<?> getRecConfig() {
        return Result.ok(recConfigMapper.selectById(1L));
    }

    @Operation(summary = "保存推荐参数（权重之和须为 1，下一次计算任务生效）")
    @PostMapping("/rec-config")
    public Result<Void> saveRecConfig(@RequestBody Map<String, Object> params) {
        adminService.saveRecConfig(params);
        return Result.ok();
    }

    @Operation(summary = "手动触发全量重算（30 分钟内完成）")
    @PostMapping("/rec-config/recalc")
    public Result<Void> recalc() {
        recommendService.recalculate();
        return Result.ok();
    }

    @Operation(summary = "日志管理（180 天留存，按类型筛选）")
    @GetMapping("/logs")
    public Result<?> logs(@RequestParam(defaultValue = "1") long pageNum,
                          @RequestParam(defaultValue = "20") long pageSize,
                          @RequestParam(required = false) String logType) {
        Page<com.agito.anime.entity.SysLog> page = sysLogService.page(pageNum, pageSize, logType);
        return Result.ok(Map.of("list", page.getRecords(), "total", page.getTotal()));
    }

    @Operation(summary = "提醒推送测试（演示：生成一条未读提醒）")
    @PostMapping("/notify-test")
    public Result<Void> notifyTest() {
        Notification n = new Notification();
        n.setUserId(StpUtil.getLoginIdAsLong());
        n.setAnimeId(1L);
        n.setContent("《葬送的芙莉莲》第 8 集已更新（演示提醒）");
        n.setIsRead(0);
        notificationMapper.insert(n);
        return Result.ok();
    }
}
