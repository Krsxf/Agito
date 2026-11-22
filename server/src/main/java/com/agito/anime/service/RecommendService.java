package com.agito.anime.service;

import com.agito.anime.dto.HomeRecommendVO;

public interface RecommendService {

    /** 首页三栏目推荐（含冷启动降级与缓存） */
    HomeRecommendVO homeRecommend(Long uid);

    /** "换一批"：从候选池随机抽取未展示番剧替换当前栏目（4.2.3.1 节） */
    HomeRecommendVO refreshBatch(Long uid, String column);

    /** 手动触发全量重算（后台紧急通道，30 分钟内完成） */
    void recalculate();
}
