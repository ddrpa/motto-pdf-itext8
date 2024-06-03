package cc.ddrpa.motto.pdf.itext;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfButtonFormField;
import com.itextpdf.forms.fields.PdfFormCreator;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DocumentBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DocumentBuilder.class);

    private final OutputStream outputStream;
    private final MottoFontAgent mottoFontAgent;
    private PdfDocument pdfDocument;
    private PdfAcroForm pdfAcroForm;

    /**
     * @param outputStream 生成文件输出流
     */
    public DocumentBuilder(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.mottoFontAgent = new MottoFontAgent();
    }

    /**
     * @param outputStream   生成文件输出流
     * @param mottoFontAgent 自定义字体代理
     */
    public DocumentBuilder(OutputStream outputStream, MottoFontAgent mottoFontAgent) {
        this.outputStream = outputStream;
        this.mottoFontAgent = mottoFontAgent;
    }

    /**
     * 批量添加字体
     *
     * @param fontResources 字体文件路径
     */
    public static void addFonts(List<String> fontResources) {
        fontResources.forEach(fontFilePath -> {
            try {
                MottoFontAgent.addFont(fontFilePath);
            } catch (IOException ignored) {
                logger.atError().setCause(ignored).log("Failed to add font: {}", fontFilePath);
            }
        });
    }

    /**
     * 添加字体
     *
     * @param fontFilePath 字体文件路径
     */
    public static void addFont(String fontFilePath) {
        try {
            MottoFontAgent.addFont(fontFilePath);
        } catch (IOException e) {
            logger.atError().setCause(e).log("Failed to add font: {}", fontFilePath);
        }
    }

    /**
     * 从输入流中加载模板
     *
     * @param inputStream 模板输入流
     * @throws IOException
     */
    public DocumentBuilder loadTemplate(InputStream inputStream) throws IOException {
        pdfDocument = new PdfDocument(new PdfReader(inputStream), new PdfWriter(outputStream));
        return this;
    }

    /**
     * 将数据合并到模版中
     *
     * @param dataMap         需要填充的数据
     * @param reduceImageSize 是否需要压缩图片尺寸
     */
    public DocumentBuilder merge(Map<String, Object> dataMap, boolean reduceImageSize) {
        if (Objects.isNull(pdfAcroForm)) {
            pdfAcroForm = PdfFormCreator.getAcroForm(pdfDocument, false);
            if (Objects.isNull(pdfAcroForm)) {
                return this;
            }
        }
        // tells flattenFields() to generate an appearance Stream for all form fields that don't have one.
        pdfAcroForm.setGenerateAppearance(true);
        Map<String, PdfFormField> formFields = pdfAcroForm.getAllFormFields();
        for (Map.Entry<String, PdfFormField> entrySet : formFields.entrySet()) {
            String fieldName = entrySet.getKey();
            PdfFormField field = entrySet.getValue();
            if (field instanceof PdfTextFormField textFormField && dataMap.containsKey(fieldName)) {
                // text field 直接填充文本
                String claimedFontFamily = textFormField.getFont().getFontProgram().getFontNames()
                    .getFontName();
                Object value = dataMap.get(fieldName);
                if (value instanceof String strValue) {
                    textFormField.setValue(strValue)
                        .setFont(mottoFontAgent.pick(claimedFontFamily, strValue));
                } else {
                    textFormField.setValue(String.valueOf(value))
                        .setFont(mottoFontAgent.pick(claimedFontFamily, String.valueOf(value)));
                }
            } else if (field instanceof PdfButtonFormField buttonFormField) {
                // 可能是 radio / checkbox / image field
                if (buttonFormField.isRadio() && dataMap.containsKey(fieldName)) {
                    Object value = dataMap.get(fieldName);
                    if (value instanceof String strValue) {
                        buttonFormField.setValue(strValue);
                    } else {
                        buttonFormField.setValue(String.valueOf(value));
                    }
                } else if (
                    buttonFormField.getPdfObject().get(PdfName.AP).toString().contains("/Off")
                        && dataMap.containsKey(fieldName)) {
                    // it's checkbox
                    Object value = dataMap.get(fieldName);
                    if (value instanceof Boolean booleanValue) {
                        buttonFormField.setValue(booleanValue ? "Yes" : "Off");
                    } else {
                        buttonFormField.setValue(
                            Boolean.parseBoolean(String.valueOf(value)) ? "Yes" : "Off");
                    }
                } else if (dataMap.containsKey(fieldName)) {
                    // 如果既不是 radio 也不是 checkbox，就按照 image field 处理
                    Object value = dataMap.get(fieldName);
                    BufferedImage tempImage = null;
                    try {
                        if (value instanceof String asFilePath) {
                            tempImage = ImageIO.read(new File(asFilePath));
                        } else if (value instanceof BufferedImage asBufferedImage) {
                            tempImage = asBufferedImage;
                        } else if (value instanceof byte[] buff) {
                            try (ByteArrayInputStream bis = new ByteArrayInputStream(buff)) {
                                tempImage = ImageIO.read(bis);
                            }
                        }
                    } catch (IOException e) {
                        logger.atError().setCause(e)
                            .log("Failed to read image: {} for field: {}", value, fieldName);
                        continue;
                    }
                    if (Objects.isNull(tempImage)) {
                        continue;
                    }
                    if (reduceImageSize) {
                        // 获取控件的宽高，图像将被缩放到这个尺寸
                        Rectangle rect = buttonFormField.getWidgets().get(0).getPdfObject()
                            .getAsRectangle(PdfName.Rect);
                        BufferedImage resized = scaleImage(tempImage, Math.round(rect.getWidth()),
                            Math.round(rect.getHeight()));
                        buttonFormField.setValue(
                            Base64.getEncoder().encodeToString(toByteArray(resized)));
                    } else {
                        buttonFormField.setValue(
                            Base64.getEncoder().encodeToString(toByteArray(tempImage)));
                    }
                }
            }
            field.regenerateField();
        }
        return this;
    }

    /**
     * 保存文件
     */
    public void save() {
        save(true);
    }

    /**
     * 保存文件
     *
     * @param flatten 是否要将表单域展开（删除表单，将表单内容绘制在文件中）
     */
    public void save(boolean flatten) {

        if (flatten && Objects.nonNull(pdfDocument)) {
            pdfAcroForm.enableRegenerationForAllFields();
            pdfAcroForm.flattenFields();
        }
        pdfDocument.close();
    }

    /**
     * 使用 {@link java.awt.geom.AffineTransform} 对 {@link java.awt.image.BufferedImage} 进行缩放
     *
     * @param before       待缩放的 BufferedImage
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @return 缩放后的 BufferedImage 实例
     * @see <a href="https://stackoverflow.com/a/4216635">How to scale a BufferedImage</a>
     */
    private BufferedImage scaleImage(BufferedImage before, int targetWidth, int targetHeight) {
        int originalWidth = before.getWidth();
        int originalHeight = before.getHeight();
        BufferedImage after = new BufferedImage(targetWidth, targetHeight, before.getType());
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(
            1.0 * targetWidth / originalWidth, 1.0 * targetHeight / originalHeight);
        AffineTransformOp scaleOp = new AffineTransformOp(scaleTransform,
            AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(before, after);
    }

    /**
     * 将 {@link java.awt.image.BufferedImage} 转换为 byte[] 输出
     *
     * @param image
     * @return
     */
    private byte[] toByteArray(BufferedImage image) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpeg", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}