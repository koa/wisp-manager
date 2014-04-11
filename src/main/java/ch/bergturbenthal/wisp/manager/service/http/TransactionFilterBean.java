package ch.bergturbenthal.wisp.manager.service.http;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bergturbenthal.wisp.manager.service.CurrentEntityManagerHolder;

@Component
public class TransactionFilterBean implements Filter, CurrentEntityManagerHolder {
	private final ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<EntityManager>();
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			currentEntityManager.set(null);
			chain.doFilter(request, response);
		} finally {
			currentEntityManager.set(null);
		}
	}

	@Override
	public EntityManager getCurrentEntityManager() {
		final EntityManager savedEntityManager = currentEntityManager.get();
		if (savedEntityManager != null) {
			return savedEntityManager;
		}
		final EntityManager newEntityManager = entityManagerFactory.createEntityManager();
		currentEntityManager.set(newEntityManager);
		return newEntityManager;
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {

	}

}
