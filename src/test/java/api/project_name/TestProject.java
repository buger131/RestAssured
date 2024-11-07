package api.project_name;

import api.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestProject extends BaseTest {

    @Test
    @DisplayName("test method")
    void testExample() {
        extractResponse("url",
                "src/test/java/api/project_name/schema/test_schema.json",
                "application/json");
    }
}
