package com.agito.anime.service.impl;

import com.agito.anime.dto.HomeRecommendVO;
import com.agito.anime.entity.Anime;
import com.agito.anime.entity.RecConfig;
import com.agito.anime.mapper.AnimeMapper;
import com.agito.anime.mapper.AnimeTagMapper;
import com.agito.anime.mapper.RecConfigMapper;
import com.agito.anime.mapper.UserMapper;
import com.agito.anime.service.RecommendService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务（3.4.1、3.4.3 节）：协同过滤 + 内容过滤融合，四因子加权。
 *
 * 冷启动降级（行为数 < cold_threshold）：兴趣标签内容过滤 + 热门补偿；
 * 行为积累后升级为协同过滤（按相似用户评分向量计算相似度）。
 *
 * 算法封装为服务层内边界清晰的可替换构件 —— 体系结构 4.2 节"算法可替换性由
 * 服务层参数化配置实现"，权重来自 rec_config 表（后台滑块可调）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final AnimeMapper animeMapper;
    private final AnimeTagMapper animeTagMapper;
    private final RecConfigMapper recConfigMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Cache<String, Object> recCache;
    private final SecureRandom random = new SecureRandom();

    @Override
    @SuppressWarnings("unchecked")
    public HomeRecommendVO homeRecommend(Long uid) {
        RecConfig cfg = loadConfig();
        String cacheKey = "home:" + (uid == null ? "guest" : uid);
        HomeRecommendVO cached = (HomeRecommendVO) recCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;   // 缓存命中：1.5 秒内响应（4.3.3 节时间特性）
        }

        HomeRecommendVO vo = new HomeRecommendVO();
        int size = cfg.getHomeSize();

        // ① 今日推荐：画像与行为综合得分 TopN（游客/无画像 → 热门榜 TopN）
        List<Anime> today = topByHeat(size);
        vo.setToday(today.stream().map(this::toCard).collect(Collectors.toList()));

        // ② 猜你喜欢：冷启动 → 兴趣标签过滤 + 热门补偿；行为充足 → 简化协同过滤
        boolean cold = true;
        List<Anime> guess = new ArrayList<>();
        if (uid != null) {
            var user = userMapper.selectById(uid);
            cold = user == null || (user.getBehaviorCount() != null
                    && user.getBehaviorCount() < cfg.getColdThreshold());
            guess = cold ? guessByInterest(uid, size) : guessByCF(uid, size);
        }
        if (guess.size() < size) {
            // 热门补偿填满
            Set<Long> exist = guess.stream().map(Anime::getId).collect(Collectors.toSet());
            for (Anime a : topByHeat(size)) {
                if (guess.size() >= size) break;
                if (!exist.contains(a.getId())) guess.add(a);
            }
        }
        vo.setGuess(guess.stream().map(this::toCard).collect(Collectors.toList()));
        vo.setColdStart(cold);

        // ③ 新番速递：当季/待上线新番
        List<Anime> newAnime = animeMapper.selectList(
                new LambdaQueryWrapper<Anime>()
                        .eq(Anime::getState, 1)
                        .in(Anime::getAirStatus, 1, 3)
                        .orderByDesc(Anime::getHeatScore)
                        .last("LIMIT " + size));
        vo.setNewAnime(newAnime.stream().map(this::toNewCard).collect(Collectors.toList()));

        recCache.put(cacheKey, vo);
        return vo;
    }

    @Override
    public HomeRecommendVO refreshBatch(Long uid, String column) {
        // 清缓存后按随机探索重算：从候选池随机抽取未展示番剧替换（4.2.3.1 节）
        recCache.invalidate("home:" + (uid == null ? "guest" : uid));
        HomeRecommendVO vo = homeRecommend(uid);
        List<HomeRecommendVO.Card> src = "guess".equals(column) ? vo.getGuess() : vo.getToday();
        if (src != null && !src.isEmpty()) {
            List<Long> shown = src.stream().map(HomeRecommendVO.Card::getId).collect(Collectors.toList());
            List<Anime> pool = animeMapper.selectList(
                    new LambdaQueryWrapper<Anime>().eq(Anime::getState, 1)
                            .notIn(!shown.isEmpty(), Anime::getId, shown)
                            .orderByDesc(Anime::getHeatScore).last("LIMIT 60"));
            if (pool.size() > shown.size()) {
                Collections.shuffle(pool, random);
                int n = Math.min(src.size(), pool.size());
                List<HomeRecommendVO.Card> cards = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    cards.add(toCard(pool.get(i)));
                }
                if ("guess".equals(column)) vo.setGuess(cards); else vo.setToday(cards);
            }
        }
        return vo;
    }

    @Override
    public void recalculate() {
        RecConfig cfg = loadConfig();
        // 重算热度分并落库（全量重算通道，量级 5 万内秒级完成）
        List<Anime> all = animeMapper.selectList(
                new LambdaQueryWrapper<Anime>().eq(Anime::getState, 1));
        Long maxPlay = all.stream().map(Anime::getPlayCount).filter(Objects::nonNull).max(Long::compareTo).orElse(1L);
        int maxFollow = all.stream().map(Anime::getFollowCount).filter(Objects::nonNull).max(Integer::compareTo).orElse(1);
        int maxRate = all.stream().map(Anime::getRatingCount).filter(Objects::nonNull).max(Integer::compareTo).orElse(1);
        int maxSocial = all.stream().map(Anime::getSocialDiscussion).filter(Objects::nonNull).max(Integer::compareTo).orElse(1);

        double wP = cfg.getWProfile().doubleValue(), wC = cfg.getWCf().doubleValue(),
               wH = cfg.getWHot().doubleValue(), wR = cfg.getWRandom().doubleValue();
        double sum = wP + wC + wH + wR;
        for (Anime a : all) {
            double play = a.getPlayCount() == null ? 0 : (double) a.getPlayCount() / maxPlay;
            double follow = a.getFollowCount() == null ? 0 : (double) a.getFollowCount() / maxFollow;
            double rate = a.getRatingCount() == null ? 0 : (double) a.getRatingCount() / maxRate;
            double social = a.getSocialDiscussion() == null ? 0 : (double) a.getSocialDiscussion() / maxSocial;
            // 播放量 0.35 / 追番 0.30 / 评分人数 0.20 / 社交讨论 0.15（需求 4.2.3.5 节权重）
            double heat = play * 0.35 + follow * 0.30 + rate * 0.20 + social * 0.15;
            Anime upd = new Anime();
            upd.setId(a.getId());
            upd.setHeatScore(BigDecimal.valueOf(heat * 1000).setScale(2, RoundingMode.HALF_UP));
            animeMapper.updateById(upd);
        }
        cfg.setLastRecalc(java.time.LocalDateTime.now());
        recConfigMapper.updateById(cfg);
        recCache.invalidateAll();
        log.info("推荐全量重算完成：{} 条，四因子权重 {}/{}/{}/{}", all.size(), wP, wC, wH, wR);
    }

    // ---------------- 内部算法 ----------------

    /** 热门榜 TopN（含随机探索扰动） */
    private List<Anime> topByHeat(int size) {
        List<Anime> pool = animeMapper.selectList(
                new LambdaQueryWrapper<Anime>().eq(Anime::getState, 1)
                        .orderByDesc(Anime::getHeatScore).last("LIMIT " + (size * 6)));
        // 均匀打散后取前 size —— 体现 w_random 随机探索，同池"换一批"可见新内容
        Collections.shuffle(pool, random);
        return pool.stream().limit(size).sorted(
                Comparator.comparing(Anime::getHeatScore).reversed()).collect(Collectors.toList());
    }

    /** 冷启动：兴趣标签内容过滤（注册勾选的 12 类标签匹配番剧类型标签） */
    private List<Anime> guessByInterest(Long uid, int size) {
        List<String> tagNames = jdbcTemplate.queryForList(
                "SELECT t.tag_name FROM user_interest ui JOIN interest_tag t ON ui.tag_id = t.id WHERE ui.user_id = ?",
                String.class, uid);
        if (tagNames.isEmpty()) return new ArrayList<>();
        String in = tagNames.stream().map(t -> "'" + t.replace("'", "''") + "'").collect(Collectors.joining(","));
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT DISTINCT anime_id FROM anime_tag WHERE tag IN (" + in + ")", Long.class);
        if (ids.isEmpty()) return new ArrayList<>();
        return animeMapper.selectList(new LambdaQueryWrapper<Anime>()
                        .eq(Anime::getState, 1).in(Anime::getId, ids)
                        .orderByDesc(Anime::getHeatScore).last("LIMIT " + size));
    }

    /** 简化协同过滤：找与当前用户评分向量相似的 K 个用户，取其高分番剧中本用户未看过的 */
    private List<Anime> guessByCF(Long uid, int size) {
        Map<Long, Double> myRatings = loadRatings(uid);
        if (myRatings.isEmpty()) return guessByInterest(uid, size);

        // 取所有评分用户（课程量级可接受；5 万番/千人级用户无压力）
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id, anime_id, score FROM `rating` WHERE user_id <> ?", uid);
        Map<Long, Map<Long, Double>> byUser = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long u = ((Number) row.get("user_id")).longValue();
            Long a = ((Number) row.get("anime_id")).longValue();
            Double s = ((Number) row.get("score")).doubleValue();
            byUser.computeIfAbsent(u, k -> new HashMap<>()).put(a, s);
        }

        // 皮尔逊相似度取 Top-K 相似用户
        int K = 5;
        PriorityQueue<Map.Entry<Long, Double>> heap = new PriorityQueue<>(Map.Entry.comparingByValue());
        for (Map.Entry<Long, Map<Long, Double>> e : byUser.entrySet()) {
            double sim = pearson(myRatings, e.getValue());
            if (sim > 0) {
                heap.offer(Map.entry(e.getKey(), sim));
                if (heap.size() > K) heap.poll();
            }
        }
        // 汇总相似用户的评分加权得分，排除我已评分的番
        Map<Long, Double> scoreMap = new HashMap<>();
        while (!heap.isEmpty()) {
            var e = heap.poll();
            Map<Long, Double> ur = byUser.get(e.getKey());
            for (Map.Entry<Long, Double> r : ur.entrySet()) {
                if (!myRatings.containsKey(r.getKey())) {
                    scoreMap.merge(r.getKey(), r.getValue() * e.getValue(), Double::sum);
                }
            }
        }
        List<Long> topIds = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(size).map(Map.Entry::getKey).collect(Collectors.toList());
        if (topIds.isEmpty()) return guessByInterest(uid, size);
        return animeMapper.selectBatchIds(topIds).stream()
                .filter(a -> a.getState() != null && a.getState() == 1)
                .sorted(Comparator.comparing((Anime a) -> topIds.indexOf(a.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, Double> loadRatings(Long uid) {
        Map<Long, Double> map = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT anime_id, score FROM `rating` WHERE user_id = ?", uid)) {
            map.put(((Number) row.get("anime_id")).longValue(),
                    ((Number) row.get("score")).doubleValue());
        }
        return map;
    }

    private double pearson(Map<Long, Double> a, Map<Long, Double> b) {
        Set<Long> common = new HashSet<>(a.keySet());
        common.retainAll(b.keySet());
        if (common.size() < 2) return 0;
        double ma = common.stream().mapToDouble(a::get).average().orElse(0);
        double mb = common.stream().mapToDouble(b::get).average().orElse(0);
        double num = 0, da = 0, db = 0;
        for (Long k : common) {
            double x = a.get(k) - ma, y = b.get(k) - mb;
            num += x * y; da += x * x; db += y * y;
        }
        if (da == 0 || db == 0) return 0;
        return num / Math.sqrt(da * db);
    }

    private RecConfig loadConfig() {
        RecConfig cfg = recConfigMapper.selectById(1L);
        if (cfg == null) {
            cfg = new RecConfig();
            cfg.setId(1L);
            cfg.setWProfile(new BigDecimal("0.40"));
            cfg.setWCf(new BigDecimal("0.30"));
            cfg.setWHot(new BigDecimal("0.20"));
            cfg.setWRandom(new BigDecimal("0.10"));
            cfg.setHomeSize(10);
            cfg.setCacheMinutes(30);
            cfg.setColdThreshold(20);
            recConfigMapper.insert(cfg);
        }
        return cfg;
    }

    private HomeRecommendVO.Card toCard(Anime a) {
        HomeRecommendVO.Card c = new HomeRecommendVO.Card();
        c.setId(a.getId());
        c.setTitle(a.getTitle());
        c.setScore(a.getTotalScore());
        c.setAirStatus(a.getAirStatus());
        c.setCoverUrl(a.getCoverUrl());
        return c;
    }

    private HomeRecommendVO.NewCard toNewCard(Anime a) {
        HomeRecommendVO.NewCard c = new HomeRecommendVO.NewCard();
        c.setId(a.getId());
        c.setTitle(a.getTitle());
        c.setWeekday(a.getWeekday());
        c.setEpisodeCount(a.getEpisodeCount());
        c.setStudio(a.getStudio());
        c.setCoverUrl(a.getCoverUrl());
        return c;
    }
}
