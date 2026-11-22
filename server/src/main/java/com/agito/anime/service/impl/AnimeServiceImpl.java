package com.agito.anime.service.impl;

import com.agito.anime.common.BizException;
import com.agito.anime.common.ErrorCode;
import com.agito.anime.dto.AnimeDetailVO;
import com.agito.anime.dto.AnimeQueryDTO;
import com.agito.anime.entity.Anime;
import com.agito.anime.entity.AnimePlatform;
import com.agito.anime.entity.AnimeScore;
import com.agito.anime.mapper.AnimeMapper;
import com.agito.anime.mapper.AnimePlatformMapper;
import com.agito.anime.mapper.AnimeScoreMapper;
import com.agito.anime.mapper.AnimeTagMapper;
import com.agito.anime.service.AnimeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimeServiceImpl implements AnimeService {

    private final AnimeMapper animeMapper;
    private final AnimeScoreMapper animeScoreMapper;
    private final AnimePlatformMapper animePlatformMapper;
    private final AnimeTagMapper animeTagMapper;

    @Override
    public Page<Anime> list(AnimeQueryDTO q) {
        LambdaQueryWrapper<Anime> qw = new LambdaQueryWrapper<>();
        qw.eq(Anime::getState, 1);   // 只展示已上线数据
        qw.like(q.getKeyword() != null && !q.getKeyword().isEmpty(),
                        Anime::getTitle, q.getKeyword())
                .or(q.getKeyword() != null && !q.getKeyword().isEmpty(),
                        w -> w.like(Anime::getAlias, q.getKeyword()));
        qw.eq(q.getRegion() != null && !q.getRegion().isEmpty(), Anime::getRegion, q.getRegion());
        qw.eq(q.getYear() != null, Anime::getYear, q.getYear());
        qw.eq(q.getAirStatus() != null, Anime::getAirStatus, q.getAirStatus());
        qw.ge(q.getYearFrom() != null, Anime::getYear, q.getYearFrom());
        qw.le(q.getYearTo() != null, Anime::getYear, q.getYearTo());

        // 标签筛选（先查 anime_tag 得到 id 集）
        if (q.getTag() != null && !q.getTag().isEmpty()) {
            Set<Long> ids = animeTagMapper.selectList(
                            new LambdaQueryWrapper<com.agito.anime.entity.AnimeTag>()
                                    .eq(com.agito.anime.entity.AnimeTag::getTag, q.getTag()))
                    .stream().map(com.agito.anime.entity.AnimeTag::getAnimeId).collect(Collectors.toSet());
            if (ids.isEmpty()) return new Page<>(q.getPageNum(), q.getPageSize());
            qw.in(Anime::getId, ids);
        }

        // 排序
        if ("score".equals(q.getSortBy())) qw.orderByDesc(Anime::getTotalScore);
        else if ("year".equals(q.getSortBy())) qw.orderByDesc(Anime::getYear);
        else qw.orderByDesc(Anime::getHeatScore);

        return animeMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), qw);
    }

    @Override
    public AnimeDetailVO detail(Long id) {
        Anime a = animeMapper.selectById(id);
        if (a == null || a.getState() == null || a.getState() == 0) {
            throw BizException.notFound("番剧不存在或尚未上线");
        }
        AnimeDetailVO vo = new AnimeDetailVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setAlias(a.getAlias());
        vo.setCoverUrl(a.getCoverUrl());
        vo.setYear(a.getYear());
        vo.setRegion(a.getRegion());
        vo.setAirStatus(a.getAirStatus());
        vo.setEpisodeCount(a.getEpisodeCount());
        vo.setDuration(a.getDuration());
        vo.setStudio(a.getStudio());
        vo.setDirector(a.getDirector());
        vo.setVoiceActor(a.getVoiceActor());
        vo.setWeekday(a.getWeekday());
        vo.setDescription(a.getDescription());
        vo.setTotalScore(a.getTotalScore());
        vo.setHeatScore(a.getHeatScore());
        vo.setPlayCount(a.getPlayCount());
        vo.setFollowCount(a.getFollowCount());
        vo.setRatingCount(a.getRatingCount());
        vo.setSocialDiscussion(a.getSocialDiscussion());

        // 类型标签
        vo.setTags(animeTagMapper.selectList(
                        new LambdaQueryWrapper<com.agito.anime.entity.AnimeTag>()
                                .eq(com.agito.anime.entity.AnimeTag::getAnimeId, id))
                .stream().map(com.agito.anime.entity.AnimeTag::getTag).collect(Collectors.toList()));

        // 多源评分卡：综合加权 + 豆瓣 + B站 + MAL
        List<AnimeScore> scores = animeScoreMapper.selectList(
                new LambdaQueryWrapper<AnimeScore>().eq(AnimeScore::getAnimeId, id)
                        .orderByDesc(AnimeScore::getConfidence));
        List<AnimeDetailVO.ScoreItem> board = new ArrayList<>();
        AnimeDetailVO.ScoreItem total = new AnimeDetailVO.ScoreItem();
        total.setSourceName("综合加权");
        total.setScore(a.getTotalScore());
        total.setRatingCount(a.getRatingCount());
        board.add(total);
        for (AnimeScore s : scores) {
            AnimeDetailVO.ScoreItem item = new AnimeDetailVO.ScoreItem();
            item.setSourceName(s.getSourceName());
            item.setScore(s.getScore());
            item.setRatingCount(s.getRatingCount());
            board.add(item);
        }
        vo.setScoreBoard(board);

        // 可观看平台（按权重排序）
        List<AnimePlatform> platforms = animePlatformMapper.selectList(
                new LambdaQueryWrapper<AnimePlatform>()
                        .eq(AnimePlatform::getAnimeId, id)
                        .orderByDesc(AnimePlatform::getWeight));
        vo.setPlatforms(platforms.stream().map(p -> {
            AnimeDetailVO.PlatformItem item = new AnimeDetailVO.PlatformItem();
            item.setId(p.getId());
            item.setPlatformName(p.getPlatformName());
            item.setUrl(p.getUrl());
            item.setIsPaid(p.getIsPaid());
            item.setRegionLimit(p.getRegionLimit());
            item.setLinkStatus(p.getLinkStatus());
            return item;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public List<String> suggest(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        LambdaQueryWrapper<Anime> qw = new LambdaQueryWrapper<>();
        qw.eq(Anime::getState, 1)
                .and(w -> w.like(Anime::getTitle, keyword.trim())
                        .or().like(Anime::getAlias, keyword.trim()))
                .orderByDesc(Anime::getHeatScore)
                .last("LIMIT 8");
        return animeMapper.selectList(qw).stream().map(Anime::getTitle).collect(Collectors.toList());
    }

    @Override
    public List<String> hotWords() {
        return animeMapper.selectList(new LambdaQueryWrapper<Anime>()
                        .eq(Anime::getState, 1).orderByDesc(Anime::getHeatScore).last("LIMIT 8"))
                .stream().map(Anime::getTitle).collect(Collectors.toList());
    }
}
