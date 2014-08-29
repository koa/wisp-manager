package ch.bergturbenthal.wisp.manager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.system.ApplicationPidListener;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class WispManager {

	public static void main(final String[] args) throws Exception {
		final SpringApplication springApplication = new SpringApplication(WispManager.class);
		springApplication.addListeners(new ApplicationPidListener("wisp-manager.pid"));
		springApplication.run(args);
	}

	@Bean
	public ScheduledExecutorService executorService() {
		return Executors.newScheduledThreadPool(10);
	}
}
