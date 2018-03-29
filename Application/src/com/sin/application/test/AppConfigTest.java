package com.sin.application.test;

import java.net.URL;

import org.junit.Test;

import com.sin.application.config.AppConfig;
import com.sin.application.config.AppConfigManager;

public class AppConfigTest {
	@Test
	public void testConfigLoad() {
		
		try {
			AppConfigManager configManager = new AppConfigManager();
			
			URL path = this.getClass().getClassLoader().getResource("applications.xml");
			
			configManager.loadAllApplicationConfigs(path.toURI());
			
			for(AppConfig config : configManager.getConfigs()){
				System.out.println(config.getName() + ":" + config.getFile());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
