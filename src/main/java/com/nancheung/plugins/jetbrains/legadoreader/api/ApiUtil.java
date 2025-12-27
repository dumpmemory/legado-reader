package com.nancheung.plugins.jetbrains.legadoreader.api;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.nancheung.plugins.jetbrains.legadoreader.api.dto.BookChapterDTO;
import com.nancheung.plugins.jetbrains.legadoreader.api.dto.BookDTO;
import com.nancheung.plugins.jetbrains.legadoreader.api.dto.BookProgressDTO;
import com.nancheung.plugins.jetbrains.legadoreader.api.dto.R;
import com.nancheung.plugins.jetbrains.legadoreader.manager.ReadingSessionManager;
import com.nancheung.plugins.jetbrains.legadoreader.storage.AddressHistoryStorage;
import com.nancheung.plugins.jetbrains.legadoreader.storage.PluginSettingsStorage;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API 工具
 *
 * @author NanCheung
 */
@UtilityClass
public class ApiUtil {

    /**
     * 获取书架目录列表
     *
     * @return 书架目录列表
     */
    public List<BookDTO> getBookshelf() {
        String url = AddressHistoryStorage.getInstance().getMostRecent() + AddressEnum.GET_BOOKSHELF.getAddress();

        R<List<BookDTO>> r = get(url, new TypeReference<>() {
        });

        return r.getData();
    }

    /**
     * 获取正文内容
     *
     * @return 正文内容
     */
    public String getBookContent(String bookUrl, int bookIndex) {
        // 调用 API获取正文内容
        String url = AddressHistoryStorage.getInstance().getMostRecent() + AddressEnum.GET_BOOK_CONTENT.getAddress() + "?url=" + URLUtil.encodeAll(bookUrl) + "&index=" + bookIndex;

        R<String> r = get(url, new TypeReference<>() {
        });

        return r.getData();
    }

    /**
     * 获取章节目录列表
     *
     * @return 章节目录列表
     */
    public List<BookChapterDTO> getChapterList(String bookUrl) {
        // 调用 API获取书架目录
        String url = AddressHistoryStorage.getInstance().getMostRecent() + AddressEnum.GET_CHAPTER_LIST.getAddress() + "?url=" + URLUtil.encodeAll(bookUrl);

        R<List<BookChapterDTO>> r = get(url, new TypeReference<>() {
        });

        return r.getData();
    }

    /**
     * 保存阅读进度
     */
    public void saveBookProgress(String author, String name, int index, String title, int durChapterPos) {
        // 调用 API获取书架目录
        String url = AddressHistoryStorage.getInstance().getMostRecent() + AddressEnum.SAVE_BOOK_PROGRESS.getAddress();

        BookProgressDTO bookProgressDTO = BookProgressDTO.builder()
                .author(author)
                .name(name)
                .durChapterIndex(index)
                .durChapterTitle(title)
                .durChapterTime(System.currentTimeMillis())
                .durChapterPos(durChapterPos)
                .url(ReadingSessionManager.getInstance().getCurrentBook().getBookUrl())
                .index(index)
                .build();

        post(url, bookProgressDTO, new TypeReference<>() {
        });
    }


    private <R> R get(String url, TypeReference<R> typeReference) {
        String textBody;

        try {
            textBody = HttpUtil.get(url, parseCustomParams());
        } catch (Exception e) {
            throw new RuntimeException(String.format("\n%s：%s\n参数：\n%s\n", "调用API失败", url, parseCustomParams()), e);
        }

        return JSONUtil.toBean(textBody, typeReference, true);
    }

    private <R> R post(String url, Object body, TypeReference<R> typeReference) {
        String textBody;
        try (HttpResponse execute = HttpUtil.createPost(url)
                .form(parseCustomParams())
                .body(JSONUtil.toJsonStr(body))
                .execute()) {
            textBody = execute.body();
        } catch (Exception e) {
            throw new RuntimeException(String.format("\n%s：%s\n参数：\n%s\n%s\n", "调用API失败", url, parseCustomParams(), body), e);
        }

        return JSONUtil.toBean(textBody, typeReference, true);
    }

    /**
     * 解析 API 自定义参数
     * 格式：参数名:@参数值（每行一个）
     *
     * @return 参数 Map
     */
    private static Map<String, Object> parseCustomParams() {
        String param = PluginSettingsStorage.getInstance().getState().apiCustomParam;
        if (StrUtil.isBlank(param)) {
            return Map.of();
        }

        // 按照回车符分割，取出所有自定义参数
        List<String> apiCustomParamList = StrUtil.split(param, "\n");

        // 按照 :@ 分割，取出参数名和参数值,转成map
        return apiCustomParamList.stream()
                .filter(StrUtil::isNotEmpty)
                .filter(s -> s.contains(StrPool.COLON + StrPool.AT))
                .map(s -> StrUtil.split(s, StrPool.COLON + StrPool.AT))
                .collect(Collectors.toMap(List::getFirst, l -> l.get(1), (a, b) -> b));
    }

}
