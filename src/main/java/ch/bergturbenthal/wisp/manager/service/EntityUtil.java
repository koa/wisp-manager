package ch.bergturbenthal.wisp.manager.service;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

public class EntityUtil {
	public static <T> Collection<T> queryAll(final Class<T> type, final EntityManager entityManager) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(type);
		criteriaQuery.from(type);
		return entityManager.createQuery(criteriaQuery).getResultList();
	}

}
