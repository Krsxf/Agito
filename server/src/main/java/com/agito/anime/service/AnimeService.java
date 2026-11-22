package com.agito.anime.service;

import com.agito.anime.dto.AnimeDetailVO;
import com.agito.anime.dto.AnimeQueryDTO;
import com.agito.anime.entity.Anime;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface AnimeService {

    /** 番剧中心：多维筛选 + 关键词搜索（拼音首字母由前端本地映射后传中文/别名检索） */
    Page<Anime> list(AnimeQueryDTO query);

    /** 番剧详情：信息分层出参（多源评分卡 + 平台链接 + 标签） */
    AnimeDetailVO detail(Long id);

    /** 搜索联想：返回联想词列表（4.2.3.2 节） */
    List<String> suggest(String keyword);

    /** 热门搜索词 */
    List<String> hotWords();
}
