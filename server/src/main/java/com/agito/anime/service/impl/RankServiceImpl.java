package com.agito.anime.service.impl;

import com.agito.anime.entity.Anime;
import com.agito.anime.mapper.AnimeMapper;
import com.agito.anime.service.RankService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final AnimeMapper animeMapper;

    @Override
    public Map<String, Object> list(int rankType, int period) {
        LambdaQueryWrapper<Anime> qw = new LambdaQueryWrapper<>();
        qw.eq(Anime::getState, 1);

        switch (rankType) {
            case 2 -> qw.in(Anime::getAirStatus, 1, 3).ge(Anime::getYear, 2025);   // 新番榜
            case 3 -> qw.eq(Anime::getAirStatus, 2)                                // 经典高分榜
                        .ge(Anime::getYear, 1990)
                        .le(Anime::getYear, 2024)
                        .isNotNull(Anime::getTotalScore)
                        .ge(Anime::getTotalScore, 8.0);
            default -> { }                                                          // 综合热度榜
        }

        // 时间范围：演示实现按热度/评分直接排序；接入真实周期统计后按 stat_date 过滤
        switch (rankType) {
            case 3 -> qw.orderByDesc(Anime::getTotalScore);
            default -> qw.orderByDesc(Anime::getHeatScore);
        }
        qw.last("LIMIT 50");
        List<Anime> top = animeMapper.selectList(qw);

        List<Map<String, Object>> list = new ArrayList<>();
        int rankNo = 1;
        for (Anime a : top) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rankNo", rankNo++);
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("year", a.getYear());
            item.put("episodeCount", a.getEpisodeCount());
            item.put("score", a.getTotalScore());
            item.put("heat", a.getHeatScore());
            item.put("followCount", a.getFollowCount());
            list.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        // 综合热度指标权重说明卡（界面文档 4.9 节）
        Map<String, Object> weight = new LinkedHashMap<>();
        weight.put("播放量", 0.35);
        weight.put("追番人数", 0.30);
        weight.put("评分人数", 0.20);
        weight.put("社交讨论", 0.15);
        result.put("weight", weight);
        result.put("period", period);
        return result;
    }
}
