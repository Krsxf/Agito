package com.agito.anime.service;

import com.agito.anime.entity.Anime;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface AdminService {

    /** 后台番剧管理：分页查询（状态筛选） */
    Map<String, Object> pageAnimes(long pageNum, long pageSize, Integer state, String keyword);

    /** 新增 / 编辑番剧（弹出表单） */
    void saveAnime(Anime anime);

    /** 为番剧新增观看平台外链 */
    void addPlatform(Long animeId, String platformName, String url);

    /** 上传番剧封面图片（存本地静态目录，返回可访问 URL） */
    String uploadCover(MultipartFile file);

    /** 上架 / 下架 */
    void changeState(Long animeId, int state);

    /** 修复失效链接 */
    void fixPlatform(Long platformId, String newUrl);

    /** 人工审核队列：待审核列表 / 通过（可编辑作品信息与链接）/ 驳回 */
    Map<String, Object> reviewList();
    void reviewPass(Long reviewId, Map<String, Object> edited);
    void reviewReject(Long reviewId);

    /** Excel 批量导入番剧（管道-过滤器：导入→清洗→归一化→审核→入库） */
    Map<String, Object> importExcel(MultipartFile file);

    /** Excel 导出 */
    byte[] exportExcel(String keyword) throws Exception;

    /** 推荐参数保存（四因子权重滑块） */
    void saveRecConfig(Map<String, Object> params);

    /** 运营概览统计（番剧总量/注册用户/今日跳转/失效链接数） */
    Map<String, Object> overview();

    /** 每小时榜单刷新（@Scheduled 调度） */
    void refreshRank();

    /** 每日推荐重算（凌晨执行，3.4.3 节） */
    void dailyRecalc();
}
