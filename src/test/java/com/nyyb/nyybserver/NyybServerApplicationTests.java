package com.nyyb.nyybserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect"
})
class NyybServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
