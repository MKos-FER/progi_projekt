package hr.fer.letsdosports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LDS_INTEGRATION_TESTS", matches = "true")
class LetsDoSportsApplicationTests {

    @Test
    void contextLoads() {
    }

}
