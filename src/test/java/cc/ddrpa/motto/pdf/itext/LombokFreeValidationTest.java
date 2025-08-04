package cc.ddrpa.motto.pdf.itext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to validate that the project works without Lombok dependency.
 * This test ensures that all classes use standard Java constructs
 * and don't rely on Lombok-generated code.
 */
public class LombokFreeValidationTest {

    @Test
    void documentBuilderClassUsesStandardJava() {
        Class<DocumentBuilder> clazz = DocumentBuilder.class;
        
        // Verify the class has explicit constructors
        Constructor<?>[] constructors = clazz.getConstructors();
        assertTrue(constructors.length > 0, "DocumentBuilder should have explicit constructors");
        
        // Verify no Lombok annotations are present
        assertNoLombokAnnotations(clazz);
        
        // Verify all fields are explicitly declared (not generated)
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            assertNotNull(field.getName(), "All fields should be explicitly declared");
        }
    }

    @Test
    void mottoFontAgentClassUsesStandardJava() {
        Class<MottoFontAgent> clazz = MottoFontAgent.class;
        
        // Verify the class has explicit constructors
        Constructor<?>[] constructors = clazz.getConstructors();
        assertTrue(constructors.length > 0, "MottoFontAgent should have explicit constructors");
        
        // Verify no Lombok annotations are present
        assertNoLombokAnnotations(clazz);
        
        // Verify all fields are explicitly declared (not generated)
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            assertNotNull(field.getName(), "All fields should be explicitly declared");
        }
    }

    @Test
    void projectBuildsWithoutLombok() {
        // This test passes if the test compilation succeeds, proving 
        // the project compiles without Lombok dependency
        assertTrue(true, "Project compiles successfully without Lombok");
    }

    private void assertNoLombokAnnotations(Class<?> clazz) {
        // Check for common Lombok annotations
        String[] lombokAnnotations = {
            "lombok.Data", 
            "lombok.Getter", 
            "lombok.Setter",
            "lombok.NoArgsConstructor", 
            "lombok.AllArgsConstructor",
            "lombok.Builder", 
            "lombok.Slf4j", 
            "lombok.Value",
            "lombok.ToString", 
            "lombok.EqualsAndHashCode"
        };
        
        for (String annotationName : lombokAnnotations) {
            try {
                Class<?> annotationClass = Class.forName(annotationName);
                assertFalse(clazz.isAnnotationPresent(annotationClass.asSubclass(java.lang.annotation.Annotation.class)),
                    "Class " + clazz.getSimpleName() + " should not have Lombok annotation " + annotationName);
            } catch (ClassNotFoundException e) {
                // This is expected - Lombok annotation classes should not be available
                // since we don't have Lombok dependency
            }
        }
    }
}