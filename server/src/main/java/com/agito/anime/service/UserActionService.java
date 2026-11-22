package com.agito.anime.service;

import com.agito.anime.entity.Anime;
import com.agito.anime.entity.Catchup;
import com.agito.anime.entity.Follow;

import java.util.Map;

public interface UserActionService {

    /** 评分（1~10 星级）：回流更新画像与行为计数（闭环 3.4.1 节） */
    void rate(Long animeId, double score);

    /** 收藏 */
    void favorite(Long animeId, boolean on);

    /** 加入追番（建立 NEW 角标与更新提醒订阅） */
    void follow(Long animeId);

    /** 追番进度一键确认（界面文档 4.7 节："上次看到第 X 集"） */
    void confirmProgress(Long animeId, int episode);

    /** 取消追番 */
    void unfollow(Long animeId);

    /** 加入补番清单 */
    void addCatchup(Long animeId);

    /** 补番完成 / 弃番 */
    void catchupDone(Long animeId, boolean dropped);

    /** 我的追番列表 + 周视图日历数据 */
    Map<String, Object> myFollows();

    /** 我的补番清单（含进度统计横幅） */
    Map<String, Object> myCatchup();

    /** 补番优先级拖拽排序 */
    void reorderCatchup(java.util.List<Long> orderedIds);

    /** 我的收藏 */
    Map<String, Object> myFavorites();

    /** 我的评分（用户中心） */
    Map<String, Object> myRatings();

    /** 提交番剧补充建议（想上架的番剧 + 跳转链接，进入管理员审核队列） */
    void suggestAnime(com.agito.anime.dto.SuggestDTO dto);

    /** 我的提交记录（含审核状态） */
    Map<String, Object> mySuggestions();
}
