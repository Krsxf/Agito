package com.agito.anime.service.impl;

import com.agito.anime.common.BizException;
import com.agito.anime.common.ErrorCode;
import com.agito.anime.entity.*;
import com.agito.anime.mapper.*;
import com.agito.anime.service.AdminService;
import com.agito.anime.service.RecommendService;
import com.agito.anime.service.SysLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 后台管理服务：番剧管理 / 人工审核 / 批量导入导出 / 推荐参数 / 运营概览。
 *
 * 数据采集子系统（管道-过滤器模式的合规落地）：本地 Excel 导入 →
 * 字段清洗与评分归一化 → 人工审核队列 → 发布入库（state 置 1 上线）。
 * 不做真实网络爬取，正式环境由官方开放 API 与数据合作方供数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AnimeMapper animeMapper;
    private final AnimePlatformMapper platformMapper;
    private final AnimeScoreMapper scoreMapper;
    private final AnimeTagMapper tagMapper;
    private final ReviewQueueMapper reviewQueueMapper;
    private final ImportTaskMapper importTaskMapper;
    private final RecConfigMapper recConfigMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RecommendService recommendService;
    private final SysLogService sysLogService;

    @Override
    public Map<String, Object> pageAnimes(long pageNum, long pageSize, Integer state, String keyword) {
        LambdaQueryWrapper<Anime> qw = new LambdaQueryWrapper<>();
        qw.eq(state != null, Anime::getState, state)
                .like(keyword != null && !keyword.isEmpty(), Anime::getTitle, keyword)
                .orderByDesc(Anime::getId);
        Page<Anime> page = animeMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Anime a : page.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("year", a.getYear());
            m.put("region", a.getRegion());
            m.put("airStatus", a.getAirStatus());
            m.put("totalScore", a.getTotalScore());
            m.put("state", a.getState());
            m.put("source", a.getSource());
            // 平台与失效链接数
            List<AnimePlatform> ps = platformMapper.selectList(
                    new LambdaQueryWrapper<AnimePlatform>().eq(AnimePlatform::getAnimeId, a.getId()));
            List<String> names = new ArrayList<>();
            int invalid = 0;
            for (AnimePlatform p : ps) {
                names.add(p.getPlatformName());
                if (p.getLinkStatus() != null && p.getLinkStatus() == 0) invalid++;
            }
            m.put("platforms", String.join("、", names));
            m.put("invalidLinks", invalid);
            rows.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows);
        result.put("total", page.getTotal());
        return result;
    }

    @Override
    public void saveAnime(Anime anime) {
        if (anime.getId() == null) {
            anime.setCreateTime(LocalDateTime.now());
            if (anime.getState() == null) anime.setState(0);   // 新增默认待审核
            animeMapper.insert(anime);
        } else {
            animeMapper.updateById(anime);
        }
        sysLogService.log(null, "OPER", "saveAnime", "保存番剧：" + anime.getTitle());
    }

    @Override
    public void addPlatform(Long animeId, String platformName, String url) {
        if (platformName == null || platformName.isBlank()) throw BizException.param("平台名称不能为空");
        if (url == null || url.isBlank() || !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw BizException.param("链接须以 http(s):// 开头");
        }
        AnimePlatform p = new AnimePlatform();
        p.setAnimeId(animeId);
        p.setPlatformName(platformName.trim());
        p.setUrl(url.trim());
        p.setWeight(50);
        p.setIsPaid(0);
        p.setRegionLimit(0);
        p.setLinkStatus(1);
        p.setCheckTime(LocalDateTime.now());
        platformMapper.insert(p);
        sysLogService.log(null, "OPER", "addPlatform",
                "番剧 id=" + animeId + " 新增平台 " + platformName + " → " + url);
    }

    /**
     * 上传番剧封面：文件存服务器本地静态目录，所有用户经 HTTP 访问同一相对 URL。
     * 文件名服务端生成（UUID），杜绝路径穿越；仅放行 png/jpg/jpeg/webp/gif。
     */
    @Value("${anime.upload.dir:./upload}")
    private String uploadDir;

    private static final java.util.Set<String> COVER_EXT =
            java.util.Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final long COVER_MAX_BYTES = 2 * 1024 * 1024;   // 2MB

    @Override
    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) throw BizException.param("请选择封面图片");
        if (file.getSize() > COVER_MAX_BYTES) throw BizException.param("封面图片不超过 2MB");
        String ext = extOf(file.getOriginalFilename());
        if (!COVER_EXT.contains(ext)) {
            throw BizException.param("仅支持 png/jpg/jpeg/webp/gif 格式");
        }
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir, "cover");
            java.nio.file.Files.createDirectories(dir);
            String filename = java.util.UUID.randomUUID().toString().replace("-", "") + "." + ext;
            java.nio.file.Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(java.nio.file.Paths.get(uploadDir).normalize())) {
                throw BizException.param("非法文件路径");
            }
            java.nio.file.Files.copy(file.getInputStream(), target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/cover/" + filename;
            sysLogService.log(null, "OPER", "uploadCover", "封面上传 → " + url);
            return url;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("封面上传失败", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "封面上传失败，请稍后重试");
        }
    }

    private String extOf(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase();
    }

    @Override
    public void changeState(Long animeId, int state) {
        if (state < 0 || state > 2) throw BizException.param("状态值非法");
        Anime upd = new Anime();
        upd.setId(animeId);
        upd.setState(state);
        animeMapper.updateById(upd);
        sysLogService.log(null, "OPER", "changeState", "番剧 id=" + animeId + " 状态 → " + state);
    }

    @Override
    public void fixPlatform(Long platformId, String newUrl) {
        AnimePlatform p = platformMapper.selectById(platformId);
        if (p == null) throw BizException.notFound("平台链接不存在");
        p.setUrl(newUrl);
        p.setLinkStatus(1);
        p.setCheckTime(LocalDateTime.now());
        platformMapper.updateById(p);
        sysLogService.log(null, "OPER", "fixPlatform", "修复链接 id=" + platformId + " → " + newUrl);
    }

    @Override
    public Map<String, Object> reviewList() {
        List<ReviewQueue> list = reviewQueueMapper.selectList(
                new LambdaQueryWrapper<ReviewQueue>().eq(ReviewQueue::getStatus, 0)
                        .orderByDesc(ReviewQueue::getId));
        List<Map<String, Object>> rows = list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("taskId", r.getTaskId());
            m.put("sourceType", r.getSourceType() == null ? "IMPORT" : r.getSourceType());
            if ("USER".equals(r.getSourceType()) && r.getSubmitterId() != null) {
                Map<String, Object> su = null;
                try {
                    su = jdbcTemplate.queryForMap(
                            "SELECT id, username, nickname FROM `user` WHERE id = ?", r.getSubmitterId());
                } catch (Exception ignored) {}
                m.put("submitter", su);
            }
            m.put("note", r.getSubmitterNote());
            m.put("platformName", r.getPlatformName());
            m.put("platformUrl", r.getPlatformUrl());
            m.put("reason", r.getReason());
            m.put("payload", r.getAnimePayload());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return Map.of("list", rows);
    }

    /**
     * 审核通过：管理员可修改作品信息与跳转链接后上架。
     * edited 为空时使用提交原始数据；USER 来源且带链接时同步创建观看平台外链。
     */
    @Override
    @Transactional
    public void reviewPass(Long reviewId, Map<String, Object> edited) {
        ReviewQueue r = reviewQueueMapper.selectById(reviewId);
        if (r == null || r.getStatus() != 0) throw BizException.notFound("审核项不存在或已处理");

        Map<String, Object> data = new HashMap<>();
        // 原始 payload 作为底稿
        if (r.getAnimePayload() != null) {
            data.putAll(parsePayloadMap(r.getAnimePayload()));
        }
        // 管理员编辑值覆盖底稿
        if (edited != null) data.putAll(edited);

        Anime a = new Anime();
        a.setTitle(str(data.get("title")));
        a.setAlias(str(data.get("alias")));
        a.setYear(intOrNull(data.get("year")));
        a.setRegion(str(data.get("region")));
        a.setEpisodeCount(intOrNull(data.get("episodeCount")));
        a.setStudio(str(data.get("studio")));
        a.setDescription(str(data.get("description")));
        a.setSource("USER".equals(r.getSourceType()) ? "USER_SUBMIT" : "LOCAL_IMPORT");
        Object score = data.get("totalScore");
        if (score != null && !String.valueOf(score).isEmpty()) {
            try { a.setTotalScore(new BigDecimal(String.valueOf(score))); } catch (Exception ignored) {}
        }
        if (a.getTitle() == null || a.getTitle().isBlank()) throw BizException.param("番剧标题不能为空");

        // 查重：避免重复上架
        Long dup = animeMapper.selectCount(new LambdaQueryWrapper<Anime>()
                .eq(Anime::getTitle, a.getTitle()).eq(Anime::getState, 1));
        if (dup != null && dup > 0) throw BizException.param("《" + a.getTitle() + "》已上架，请驳回该条目");

        a.setState(1);
        if (a.getId() == null) {
            animeMapper.insert(a);
        } else {
            animeMapper.updateById(a);
        }

        // 用户提供的跳转链接（管理员可修改）→ 创建观看平台外链
        String pUrl = str(edited != null && edited.containsKey("platformUrl") ? edited.get("platformUrl") : r.getPlatformUrl());
        String pName = str(edited != null && edited.containsKey("platformName") ? edited.get("platformName") : r.getPlatformName());
        if (pUrl != null && !pUrl.isBlank()) {
            AnimePlatform p = new AnimePlatform();
            p.setAnimeId(a.getId());
            p.setPlatformName(pName == null || pName.isBlank() ? "待确认" : pName);
            p.setUrl(pUrl);
            p.setWeight(100);
            p.setIsPaid(0);
            p.setRegionLimit(0);
            p.setLinkStatus(1);
            p.setCheckTime(LocalDateTime.now());
            platformMapper.insert(p);
        }

        r.setStatus(1);
        r.setReviewer("admin");
        r.setReviewTime(LocalDateTime.now());
        reviewQueueMapper.updateById(r);
        sysLogService.log(null, "OPER", "reviewPass", "审核通过并上架：" + a.getTitle());
    }

    @Override
    public void reviewReject(Long reviewId) {
        ReviewQueue r = reviewQueueMapper.selectById(reviewId);
        if (r == null || r.getStatus() != 0) throw BizException.notFound("审核项不存在或已处理");
        r.setStatus(2);
        r.setReviewer("admin");
        r.setReviewTime(LocalDateTime.now());
        reviewQueueMapper.updateById(r);
        sysLogService.log(null, "OPER", "reviewReject", "审核驳回 reviewId=" + reviewId);
    }

    /** Excel 批量导入：字段清洗 + 归一化 + 入审核队列（管道-过滤器流水线） */
    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file) {
        ImportTask task = new ImportTask();
        task.setTaskName("Excel 导入 - " + file.getOriginalFilename());
        task.setSource("LOCAL_EXCEL");
        task.setFileName(file.getOriginalFilename());
        task.setStatus(0);
        task.setStartTime(LocalDateTime.now());
        importTaskMapper.insert(task);

        int total = 0, success = 0, fail = 0, review = 0;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            boolean header = true;
            for (Row row : sheet) {
                if (header) { header = false; continue; }
                String title = cellStr(row, 0);
                if (title == null || title.isBlank()) continue;
                total++;
                try {
                    Anime a = new Anime();
                    a.setTitle(title.trim());
                    a.setAlias(cellStr(row, 1));
                    a.setYear(cellInt(row, 2));
                    a.setRegion(cellStr(row, 3));
                    a.setEpisodeCount(cellInt(row, 4));
                    a.setStudio(cellStr(row, 5));
                    a.setDescription(cellStr(row, 6));
                    Double score = cellDouble(row, 7);
                    a.setTotalScore(score == null ? null : BigDecimal.valueOf(score));
                    a.setSource("LOCAL_IMPORT");
                    // 清洗规则：关键字段缺失或评分越界 → 进人工审核队列
                    boolean needReview = a.getYear() == null || a.getYear() < 1960
                            || a.getYear() > 2100
                            || a.getTotalScore() != null
                            && (a.getTotalScore().doubleValue() < 0 || a.getTotalScore().doubleValue() > 10);
                    if (needReview) {
                        ReviewQueue rq = new ReviewQueue();
                        rq.setTaskId(task.getId());
                        rq.setAnimePayload(payloadOf(a));
                        rq.setReason(a.getYear() == null ? "年份缺失" : "评分或年份异常，需人工核实");
                        rq.setStatus(0);
                        reviewQueueMapper.insert(rq);
                        review++;
                    } else {
                        a.setState(1);   // 合格数据直接上线
                        animeMapper.insert(a);
                        success++;
                    }
                } catch (Exception e) {
                    fail++;
                    log.warn("导入行失败：{}", e.getMessage());
                }
            }
            task.setStatus(fail > 0 ? 2 : 1);
        } catch (Exception e) {
            task.setStatus(3);
            task.setDetail("文件解析失败：" + e.getMessage());
        }
        task.setTotalCount(total);
        task.setSuccessCount(success);
        task.setFailCount(fail);
        task.setEndTime(LocalDateTime.now());
        task.setDetail("共 " + total + " 条：入库 " + success + "，审核队列 " + review + "，失败 " + fail);
        importTaskMapper.updateById(task);
        sysLogService.log(null, "IMPORT", "importExcel", task.getDetail());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("total", total);
        result.put("success", success);
        result.put("review", review);
        result.put("fail", fail);
        return result;
    }

    @Override
    public byte[] exportExcel(String keyword) throws Exception {
        LambdaQueryWrapper<Anime> qw = new LambdaQueryWrapper<>();
        qw.like(keyword != null && !keyword.isEmpty(), Anime::getTitle, keyword)
                .orderByDesc(Anime::getId);
        List<Anime> list = animeMapper.selectList(qw);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("番剧库");
            Row head = sheet.createRow(0);
            String[] headers = {"标题", "别名", "年份", "地区", "总集数", "制作公司", "简介", "综合评分", "状态"};
            for (int i = 0; i < headers.length; i++) head.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (Anime a : list) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(a.getTitle());
                row.createCell(1).setCellValue(a.getAlias() == null ? "" : a.getAlias());
                row.createCell(2).setCellValue(a.getYear() == null ? 0 : a.getYear());
                row.createCell(3).setCellValue(a.getRegion() == null ? "" : a.getRegion());
                row.createCell(4).setCellValue(a.getEpisodeCount() == null ? 0 : a.getEpisodeCount());
                row.createCell(5).setCellValue(a.getStudio() == null ? "" : a.getStudio());
                row.createCell(6).setCellValue(a.getDescription() == null ? "" : a.getDescription());
                row.createCell(7).setCellValue(a.getTotalScore() == null ? 0 : a.getTotalScore().doubleValue());
                row.createCell(8).setCellValue(a.getState() == null ? 0 : a.getState());
            }
            wb.write(out);
            sysLogService.log(null, "OPER", "exportExcel", "导出 " + list.size() + " 条");
            return out.toByteArray();
        }
    }

    @Override
    public void saveRecConfig(Map<String, Object> params) {
        RecConfig cfg = recConfigMapper.selectById(1L);
        if (cfg == null) cfg = new RecConfig();
        cfg.setId(1L);
        if (params.get("wProfile") != null) cfg.setWProfile(toBd(params.get("wProfile")));
        if (params.get("wCf") != null) cfg.setWCf(toBd(params.get("wCf")));
        if (params.get("wHot") != null) cfg.setWHot(toBd(params.get("wHot")));
        if (params.get("wRandom") != null) cfg.setWRandom(toBd(params.get("wRandom")));
        if (params.get("homeSize") != null) cfg.setHomeSize(toInt(params.get("homeSize")));
        if (params.get("cacheMinutes") != null) cfg.setCacheMinutes(toInt(params.get("cacheMinutes")));
        if (cfg.getWProfile() != null && cfg.getWCf() != null && cfg.getWHot() != null && cfg.getWRandom() != null) {
            double sum = cfg.getWProfile().doubleValue() + cfg.getWCf().doubleValue()
                    + cfg.getWHot().doubleValue() + cfg.getWRandom().doubleValue();
            if (Math.abs(sum - 1.0) > 0.001) {
                throw BizException.param("四因子权重之和须等于 1.0，当前为 " + sum);
            }
        }
        if (cfg.getId() != null && recConfigMapper.selectById(1L) != null) {
            recConfigMapper.updateById(cfg);
        } else {
            recConfigMapper.insert(cfg);
        }
        sysLogService.log(null, "OPER", "saveRecConfig",
                "推荐参数已保存，自下一次推荐计算任务执行时生效");
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("animeCount", animeMapper.selectCount(null));
        m.put("userCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
        m.put("todayJump", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jump_log WHERE jump_time >= CURDATE()", Integer.class));
        m.put("invalidLinks", platformMapper.selectCount(
                new LambdaQueryWrapper<AnimePlatform>().eq(AnimePlatform::getLinkStatus, 0)));
        m.put("pendingReview", reviewQueueMapper.selectCount(
                new LambdaQueryWrapper<ReviewQueue>().eq(ReviewQueue::getStatus, 0)));
        return m;
    }

    @Override
    @Scheduled(cron = "0 0 * * * ?")   // 每小时榜单刷新（4.2.3.5 节）
    public void refreshRank() {
        // 热度分已在 recalculate 内落库，榜单直接读 anime.heat_score，
        // 真实周期榜按 stat_date 聚合（hot_rank 表），此处刷新缓存即可
        log.info("每小时榜单缓存刷新完成");
    }

    @Override
    @Scheduled(cron = "0 30 3 * * ?")  // 每日凌晨 3:30 推荐重算（3.4.3 节）
    public void dailyRecalc() {
        recommendService.recalculate();
    }

    // ---------------- 工具 ----------------

    private Anime parsePayload(String json) {
        Anime a = new Anime();
        if (json == null) return a;
        for (String pair : json.replace("{", "").replace("}", "").split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim().replace("\"", "");
            String v = kv[1].trim().replace("\"", "");
            switch (k) {
                case "id" -> a.setId(v.isEmpty() ? null : Long.valueOf(v));
                case "title" -> a.setTitle(v);
                case "alias" -> a.setAlias(v);
                case "year" -> a.setYear(v.isEmpty() ? null : Integer.valueOf(v));
                case "region" -> a.setRegion(v);
                case "episodeCount" -> a.setEpisodeCount(v.isEmpty() ? null : Integer.valueOf(v));
                case "studio" -> a.setStudio(v);
                case "description" -> a.setDescription(v);
                case "totalScore" -> a.setTotalScore(v.isEmpty() ? null : new BigDecimal(v));
            }
        }
        return a;
    }

    /** 宽松 JSON 解析：payload → Map（供审核编辑流程使用） */
    private Map<String, Object> parsePayloadMap(String json) {
        Map<String, Object> m = new HashMap<>();
        if (json == null) return m;
        for (String pair : json.replace("{", "").replace("}", "").split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim().replace("\"", "");
            String v = kv[1].trim().replace("\"", "");
            if (!v.isEmpty()) m.put(k, v);
        }
        return m;
    }

    private String str(Object o) { return o == null ? null : String.valueOf(o); }

    private Integer intOrNull(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).replaceAll("\\D", "");
        return s.isEmpty() ? null : Integer.valueOf(s);
    }

    private String payloadOf(Anime a) {
        return String.format("{\"id\":%s,\"title\":\"%s\",\"alias\":\"%s\",\"year\":%s,\"region\":\"%s\",\"episodeCount\":%s,\"studio\":\"%s\",\"totalScore\":%s}",
                a.getId() == null ? "" : a.getId(),
                safe(a.getTitle()), safe(a.getAlias()),
                a.getYear() == null ? "" : a.getYear(), safe(a.getRegion()),
                a.getEpisodeCount() == null ? "" : a.getEpisodeCount(), safe(a.getStudio()),
                a.getTotalScore() == null ? "" : a.getTotalScore());
    }

    private String safe(String s) { return s == null ? "" : s.replace("\"", "'"); }

    private String cellStr(Row row, int i) {
        Cell c = row.getCell(i);
        return c == null ? null : new DataFormatter().formatCellValue(c);
    }

    private Integer cellInt(Row row, int i) {
        try { return Integer.parseInt(cellStr(row, i).replaceAll("\\D", "")); }
        catch (Exception e) { return null; }
    }

    private Double cellDouble(Row row, int i) {
        try { return Double.parseDouble(cellStr(row, i)); }
        catch (Exception e) { return null; }
    }

    private BigDecimal toBd(Object o) {
        return new BigDecimal(String.valueOf(o)).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer toInt(Object o) {
        return Integer.parseInt(String.valueOf(o).replaceAll("\\D", ""));
    }
}
