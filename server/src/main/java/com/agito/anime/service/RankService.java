package com.agito.anime.service;

import java.util.Map;

public interface RankService {

    /**
     * 排行榜（4.2.3.5 节）：三榜 Tab + 周/月/总榜切换。
     * @param rankType 1 综合热度榜 / 2 新番榜 / 3 经典高分榜
     * @param period   1 周榜 / 2 月榜 / 3 总榜
     * @return { list: [...Top50], weight: 综合分四因子权重说明 }
     */
    Map<String, Object> list(int rankType, int period);
}
