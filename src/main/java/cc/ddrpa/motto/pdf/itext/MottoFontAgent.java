package cc.ddrpa.motto.pdf.itext;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.font.FontSelectorStrategy;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Check
 * https://itextpdf.com/blog/technical-notes/another-case-pdf-detectives-mystery-disappearing-appearing-text
 * for more details
 */
public class MottoFontAgent {

    protected static Map<String, PdfFont> __fontDictionary = new HashMap<>(2);

    private FontProvider provider;
    private List<String> fontFamilies;


    public MottoFontAgent() {
        provider = new FontProvider();
        __fontDictionary.values().forEach(pdfFont -> provider.addFont(pdfFont.getFontProgram()));
        fontFamilies = __fontDictionary.keySet().stream().toList();
    }

    /**
     * 注册字体
     *
     * @param fontFilePath 字体文件路径
     * @throws IOException
     */
    public static PdfFont addFont(String fontFilePath) throws IOException {
        PdfFont font = PdfFontFactory.createFont(fontFilePath, PdfEncodings.IDENTITY_H,
            EmbeddingStrategy.PREFER_EMBEDDED);
        __fontDictionary.put(font.getFontProgram().getFontNames().getFontName(), font);
        return font;
    }

    /**
     * 根据内容和声明的字体族选择字体
     *
     * @param claimedFontFamily 声明的字体族，如果有注册该字体族则优先使用
     * @param content           通过要渲染的内容选择一个可用的字体
     * @return
     */
    public PdfFont pick(String claimedFontFamily, String content) {
        if (StringUtils.isNotBlank(claimedFontFamily)) {
            if (__fontDictionary.containsKey(claimedFontFamily)) {
                return __fontDictionary.get(claimedFontFamily);
            }
        }
        FontSelectorStrategy strategy = provider.getStrategy(content, fontFamilies);
        strategy.nextGlyphs();
        PdfFont pdfFont = strategy.getCurrentFont();
        return pdfFont;
    }
}