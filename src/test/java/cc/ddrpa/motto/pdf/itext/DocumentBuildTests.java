package cc.ddrpa.motto.pdf.itext;

import com.github.javafaker.Faker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentBuildTests {

    private static final Logger logger = LoggerFactory.getLogger(DocumentBuildTests.class);
    private static final Faker faker = new Faker();
    private static final SecureRandom random = new SecureRandom();

    private String randomFamilyName() {
        return switch (random.nextInt(4)) {
            case 0 -> "赵";
            case 1 -> "钱";
            case 2 -> "孙";
            default -> "李";
        };
    }

    @Test
    void MergeTest() throws IOException {
        List.of("font-seems-okay/Noto_Sans_SC/static/NotoSansSC-Regular.ttf",
                "font-seems-okay/Noto_Serif_SC/static/NotoSerifSC-Regular.ttf")
            .forEach(
                fontFilePath -> {
                    try {
                        MottoFontAgent.addFont(fontFilePath);
                    } catch (IOException e) {
                        logger.atError().setCause(e).log("Failed to add font: {}", fontFilePath);
                    }
                }
            );
        try (FileInputStream fis = new FileInputStream("src/test/resources/lorem.pdf");
            FileOutputStream fos = new FileOutputStream("target/merged.pdf")) {
            BufferedImage avatarImage = ImageIO.read(new File("src/test/resources/avatar.jpeg"));
            DocumentBuilder builder = new DocumentBuilder(fos, new MottoFontAgent());
            builder.loadTemplate(fis)
                .merge(
                    Map.of("Name", faker.name().firstName() + "·" + randomFamilyName(),
                        "IDCardNum", faker.idNumber().ssnValid(),
                        "Type", "吃瓜群众",
                        // load avatar by BufferedImage
                        "avatar", avatarImage,
                        // load huge image by file path
                        "LARGE_PHOTO", "src/test/resources/large-photo.jpeg",
                        // 单选效果展示
                        "Group1", "Choice3",
                        // 复选框效果展示
                        "CheckBoxRow1", false,
                        "CheckBoxRow2", "True"),
                    true)
                .save(true);
        }
    }
}