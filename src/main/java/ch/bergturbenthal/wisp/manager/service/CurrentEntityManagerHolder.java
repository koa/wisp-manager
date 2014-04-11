package ch.bergturbenthal.wisp.manager.service;

import javax.persistence.EntityManager;

public interface CurrentEntityManagerHolder {
	EntityManager getCurrentEntityManager();
}
