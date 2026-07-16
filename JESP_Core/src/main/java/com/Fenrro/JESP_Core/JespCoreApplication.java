package com.Fenrro.JESP_Core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class JespCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(JespCoreApplication.class, args);
	}

}
