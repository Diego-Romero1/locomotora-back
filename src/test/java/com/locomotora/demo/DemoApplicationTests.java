package com.locomotora.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "locomotora.jobs.enabled=false")
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
