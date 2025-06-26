package kopo.poly.util;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class MarkdownUtil {

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();

    // Markdown → HTML 변환 + XSS 방어 처리
    public static String toSafeHtml(String markdownText) {
        if (markdownText == null || markdownText.isEmpty()) {
            return "";
        }

        // 1단계: Markdown → HTML
        String rawHtml = renderer.render(parser.parse(markdownText));

        // 2단계: XSS 방지를 위한 HTML 정제
        String safeHtml = Jsoup.clean(rawHtml, Safelist.basicWithImages());

        return safeHtml;
    }
}