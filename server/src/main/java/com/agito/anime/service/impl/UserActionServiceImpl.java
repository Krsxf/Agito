package com.agito.anime.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.BizException;
import com.agito.anime.dto.SuggestDTO;
import com.agito.anime.entity.*;
import com.agito.anime.mapper.*;
import com.agito.anime.service.SysLogService;
import com.agito.anime.service.UserActionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private final RatingMapper ratingMapper;
    private final FavoriteMapper favoriteMapper;
    private final FollowMapper followMapper;
    private final CatchupMapper catchupMapper;
    private final AnimeMapper animeMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final SysLogService sysLogService;

    private long uid() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    @Transactional
    public void rate(Long animeId, double score) {
        if (score < 1 || score > 10) {
            throw BizException.param("评分须在 1~10 之间");
        }
        Anime anime = animeMapper.selectById(animeId);
        if (anime == null) throw BizException.notFound("番剧不存在");

        Rating exists = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, uid()).eq(Rating::getAnimeId, animeId));
        if (exists == null) {
            Rating r = new Rating();
            r.setUserId(uid());
            r.setAnimeId(animeId);
            r.setScore(BigDecimal.valueOf(score));
            ratingMapper.insert(r);
            // 行为计数 +1（冷启动判定依据）
            bumpBehavior(1);
            anime.setRatingCount((anime.getRatingCount() == null ? 0 : anime.getRatingCount()) + 1);
        } else {
            exists.setScore(BigDecimal.valueOf(score));
            ratingMapper.updateById(exists);
        }
        animeMapper.updateById(anime);
        sysLogService.log(uid(), "OPER", "rate", "评分 " + anime.getTitle() + " → " + score + " 分");
    }

    @Override
    public void favorite(Long animeId, boolean on) {
        LambdaQueryWrapper<Favorite> qw = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid()).eq(Favorite::getAnimeId, animeId);
        if (on) {
            if (favoriteMapper.selectCount(qw) == 0) {
                Favorite f = new Favorite();
                f.setUserId(uid());
                f.setAnimeId(animeId);
                favoriteMapper.insert(f);
                bumpBehavior(1);
                sysLogService.log(uid(), "OPER", "favorite", "收藏 animeId=" + animeId);
            }
        } else {
            favoriteMapper.delete(qw);
        }
    }

    @Override
    @Transactional
    public void follow(Long animeId) {
        Anime anime = animeMapper.selectById(animeId);
        if (anime == null) throw BizException.notFound("番剧不存在");
        if (followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, uid()).eq(Follow::getAnimeId, animeId)) > 0) {
            return;
        }
        Follow f = new Follow();
        f.setUserId(uid());
        f.setAnimeId(animeId);
        f.setCurrentEpisode(0);
        f.setTotalEpisode(anime.getEpisodeCount());
        f.setProgress(0);
        f.setFollowStatus(1);
        f.setNextEpisodeTime(LocalDateTime.now().plusDays(7));
        followMapper.insert(f);
        anime.setFollowCount((anime.getFollowCount() == null ? 0 : anime.getFollowCount()) + 1);
        animeMapper.updateById(anime);
        bumpBehavior(1);
        sysLogService.log(uid(), "OPER", "follow", "追番 " + anime.getTitle());
    }

    @Override
    public void confirmProgress(Long animeId, int episode) {
        Follow f = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, uid()).eq(Follow::getAnimeId, animeId));
        if (f == null) throw BizException.notFound("未追番，无法确认进度");
        if (episode < 0 || (f.getTotalEpisode() != null && episode > f.getTotalEpisode())) {
            throw BizException.param("集数超出范围");
        }
        f.setCurrentEpisode(episode);
        f.setProgress(f.getTotalEpisode() == null || f.getTotalEpisode() == 0 ? 0
                : (int) Math.round(100.0 * episode / f.getTotalEpisode()));
        if (f.getTotalEpisode() != null && episode >= f.getTotalEpisode()) {
            f.setFollowStatus(2);   // 已看完
        }
        f.setNextEpisodeTime(LocalDateTime.now().plusDays(7));
        followMapper.updateById(f);
    }

    @Override
    public void unfollow(Long animeId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, uid()).eq(Follow::getAnimeId, animeId));
        sysLogService.log(uid(), "OPER", "unfollow", "取消追番 animeId=" + animeId);
    }

    @Override
    public void addCatchup(Long animeId) {
        if (catchupMapper.selectCount(new LambdaQueryWrapper<Catchup>()
                .eq(Catchup::getUserId, uid()).eq(Catchup::getAnimeId, animeId)) > 0) {
            return;
        }
        Catchup c = new Catchup();
        c.setUserId(uid());
        c.setAnimeId(animeId);
        c.setPriority((int) (catchupMapper.selectCount(
                new LambdaQueryWrapper<Catchup>().eq(Catchup::getUserId, uid())) + 1) * 10);
        c.setCatchupStatus(1);
        catchupMapper.insert(c);
        bumpBehavior(1);
        sysLogService.log(uid(), "OPER", "addCatchup", "加入补番清单 animeId=" + animeId);
    }

    @Override
    public void catchupDone(Long animeId, boolean dropped) {
        Catchup c = catchupMapper.selectOne(new LambdaQueryWrapper<Catchup>()
                .eq(Catchup::getUserId, uid()).eq(Catchup::getAnimeId, animeId));
        if (c == null) throw BizException.notFound("不在补番清单中");
        c.setCatchupStatus(dropped ? 3 : 2);
        c.setCompleteTime(dropped ? null : LocalDateTime.now());
        catchupMapper.updateById(c);
        sysLogService.log(uid(), "OPER", "catchupDone",
                (dropped ? "弃番" : "补番完成") + " animeId=" + animeId);
    }

    @Override
    public Map<String, Object> myFollows() {
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, uid()).orderByDesc(Follow::getUpdateTime));
        List<Map<String, Object>> cards = new ArrayList<>();
        // 本周更新数（演示数据按 next_episode_time 在 7 天内计）
        long weekUpdate = follows.stream()
                .filter(f -> f.getNextEpisodeTime() != null
                        && f.getNextEpisodeTime().isAfter(LocalDateTime.now())
                        && f.getNextEpisodeTime().isBefore(LocalDateTime.now().plusDays(7))).count();
        for (Follow f : follows) {
            Anime a = animeMapper.selectById(f.getAnimeId());
            if (a == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("animeId", a.getId());
            m.put("title", a.getTitle());
            m.put("airStatus", a.getAirStatus());
            m.put("totalEpisode", f.getTotalEpisode());
            m.put("currentEpisode", f.getCurrentEpisode());
            m.put("progress", f.getProgress());
            m.put("weekday", a.getWeekday());
            m.put("nextEpisodeTime", f.getNextEpisodeTime());
            m.put("score", a.getTotalScore());
            // NEW 角标：连载中且当前集数落后于最新集数（演示：episode_count 即最新集数）
            m.put("isNew", a.getAirStatus() == 1 && a.getEpisodeCount() != null
                    && f.getCurrentEpisode() != null && a.getEpisodeCount() > f.getCurrentEpisode());
            cards.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", cards);
        result.put("totalCount", cards.size());
        result.put("weekUpdate", weekUpdate);
        return result;
    }

    @Override
    public Map<String, Object> myCatchup() {
        List<Catchup> list = catchupMapper.selectList(new LambdaQueryWrapper<Catchup>()
                .eq(Catchup::getUserId, uid()).orderByAsc(Catchup::getPriority));
        List<Map<String, Object>> cards = new ArrayList<>();
        int done = 0, total = 0, dropped = 0;
        long minutes = 0;
        for (Catchup c : list) {
            Anime a = animeMapper.selectById(c.getAnimeId());
            if (a == null) continue;
            if (c.getCatchupStatus() != 3) total++;
            if (c.getCatchupStatus() == 2) {
                done++;
                minutes += (a.getEpisodeCount() == null ? 0 : a.getEpisodeCount()) * 24L; // 演示估算
            }
            if (c.getCatchupStatus() == 3) dropped++;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("animeId", a.getId());
            m.put("title", a.getTitle());
            m.put("status", c.getCatchupStatus());
            m.put("priority", c.getPriority());
            m.put("score", a.getTotalScore());
            m.put("episodeCount", a.getEpisodeCount());
            m.put("year", a.getYear());
            cards.add(m);
        }
        Map<String, Object> banner = new HashMap<>();
        banner.put("done", done);
        banner.put("total", total);
        banner.put("dropped", dropped);
        banner.put("percent", total == 0 ? 0 : Math.round(100.0 * done / total));
        banner.put("minutes", minutes);
        Map<String, Object> result = new HashMap<>();
        result.put("list", cards);
        result.put("banner", banner);
        return result;
    }

    @Override
    @Transactional
    public void reorderCatchup(List<Long> orderedIds) {
        int prio = 10;
        for (Long animeId : orderedIds) {
            Catchup c = catchupMapper.selectOne(new LambdaQueryWrapper<Catchup>()
                    .eq(Catchup::getUserId, uid()).eq(Catchup::getAnimeId, animeId));
            if (c != null) {
                c.setPriority(prio);
                catchupMapper.updateById(c);
                prio += 10;
            }
        }
    }

    @Override
    public Map<String, Object> myFavorites() {
        List<Favorite> list = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid()).orderByDesc(Favorite::getCreateTime));
        List<Map<String, Object>> cards = list.stream().map(f -> {
            Anime a = animeMapper.selectById(f.getAnimeId());
            Map<String, Object> m = new LinkedHashMap<>();
            if (a != null) {
                m.put("animeId", a.getId());
                m.put("title", a.getTitle());
                m.put("score", a.getTotalScore());
                m.put("year", a.getYear());
                m.put("airStatus", a.getAirStatus());
            }
            return m;
        }).filter(m -> !m.isEmpty()).collect(Collectors.toList());
        return Map.of("list", cards);
    }

    @Override
    public Map<String, Object> myRatings() {
        List<Rating> list = ratingMapper.selectList(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, uid()).orderByDesc(Rating::getUpdateTime));
        List<Map<String, Object>> cards = list.stream().map(r -> {
            Anime a = animeMapper.selectById(r.getAnimeId());
            Map<String, Object> m = new LinkedHashMap<>();
            if (a != null) {
                m.put("animeId", a.getId());
                m.put("title", a.getTitle());
                m.put("myScore", r.getScore());
                m.put("avgScore", a.getTotalScore());
                m.put("year", a.getYear());
            }
            return m;
        }).filter(m -> !m.isEmpty()).collect(Collectors.toList());
        return Map.of("list", cards);
    }

    /** 行为计数 +1：画像闭环与冷启动判定的数据基础 */
    private void bumpBehavior(int delta) {
        User u = userMapper.selectById(uid());
        if (u != null) {
            u.setBehaviorCount((u.getBehaviorCount() == null ? 0 : u.getBehaviorCount()) + delta);
            userMapper.updateById(u);
        }
    }

    // ---------------- 用户提交番剧补充建议（原案用户反馈闭环） ----------------

    private final com.agito.anime.mapper.ReviewQueueMapper reviewQueueMapper;

    @Override
    public void suggestAnime(com.agito.anime.dto.SuggestDTO dto) {
        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        String url = dto.getPlatformUrl() == null ? "" : dto.getPlatformUrl().trim();
        if (title.isEmpty()) throw BizException.param("番剧名称不能为空");
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw BizException.param("观看链接须以 http(s):// 开头");
        }
        // 已上线番剧查重
        Long dup = animeMapper.selectCount(new LambdaQueryWrapper<Anime>()
                .eq(Anime::getTitle, title).eq(Anime::getState, 1));
        if (dup != null && dup > 0) {
            throw BizException.param("《" + title + "》已上架，无需重复提交");
        }
        // 待审核队列查重
        Long pending = reviewQueueMapper.selectCount(
                new LambdaQueryWrapper<ReviewQueue>()
                        .eq(ReviewQueue::getSourceType, "USER")
                        .eq(ReviewQueue::getStatus, 0)
                        .eq(ReviewQueue::getPlatformUrl, url));
        if (pending != null && pending > 0) {
            throw BizException.param("相同链接已在审核队列中，请耐心等待");
        }

        ReviewQueue r = new ReviewQueue();
        r.setSourceType("USER");
        r.setSubmitterId(uid());
        r.setAnimePayload(String.format("{\"title\":\"%s\",\"alias\":\"%s\"}",
                title.replace("\"", "'"),
                dto.getAlias() == null ? "" : dto.getAlias().replace("\"", "'")));
        r.setReason("用户提交的番剧补充建议");
        r.setStatus(0);
        r.setSubmitterNote(dto.getNote());
        r.setPlatformName(dto.getPlatformName());
        r.setPlatformUrl(url);
        reviewQueueMapper.insert(r);
        sysLogService.log(uid(), "OPER", "suggest", "提交番剧建议：" + title);
    }

    @Override
    public Map<String, Object> mySuggestions() {
        List<ReviewQueue> list = reviewQueueMapper.selectList(
                new LambdaQueryWrapper<ReviewQueue>()
                        .eq(ReviewQueue::getSourceType, "USER")
                        .eq(ReviewQueue::getSubmitterId, uid())
                        .orderByDesc(ReviewQueue::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ReviewQueue r : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("title", extractPayloadTitle(r.getAnimePayload()));
            m.put("platformUrl", r.getPlatformUrl());
            m.put("note", r.getSubmitterNote());
            m.put("status", r.getStatus());   // 0待审核 1已上架 2已驳回
            m.put("createTime", r.getCreateTime());
            rows.add(m);
        }
        return Map.of("list", rows);
    }

    private String extractPayloadTitle(String payload) {
        if (payload == null) return "";
        for (String pair : payload.replace("{", "").replace("}", "").split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2 && kv[0].trim().replace("\"", "").equals("title")) {
                return kv[1].trim().replace("\"", "");
            }
        }
        return "";
    }
}
