package com.Fenrro.JESP_Core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jesp.ws-port=0",
		"jesp.rules-file=target/test-rules-core.conf",
		"spring.datasource.url=jdbc:sqlite:target/test-db-core.db"
})
class JespCoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
