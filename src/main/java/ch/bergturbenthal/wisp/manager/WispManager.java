package ch.bergturbenthal.wisp.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class WispManager {
	public static void main(final String[] args) throws Exception {
		SpringApplication.run(WispManager.class, args);
	}

	// @Bean
	// public FilterRegistrationBean HibernateTransactionFilter() {
	// final FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
	// filterRegistrationBean.setUrlPatterns(Arrays.asList("/UIDL"));
	// filterRegistrationBean.setFilter(new TransactionFilterBean());
	// return filterRegistrationBean;
	// }
}
