package api.project_name;

import api.BaseTest;
import api.annotations.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestProject extends BaseTest {

    @Test
    @DisplayName("test method")
    @User(login = "new_login", password = "new_password")
    void testExample() {
        extractResponse("url",
                "src/test/java/api/project_name/schema/test_schema.json",
                "application/json");
    }
}
