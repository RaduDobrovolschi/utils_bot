package com.utilsbot;

import com.utilsbot.config.CRLFLogConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class UtilsbotApplication {
	private static final Logger log = LoggerFactory.getLogger(UtilsbotApplication.class);

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(UtilsbotApplication.class, args);
		log.info(CRLFLogConverter.CRLF_SAFE_MARKER,
				"\n--------------------------------------------\n\t\tApplication is running!\n--------------------------------------------"
		);
	}

}