package com.rhododendra.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.rhododendra.model.RhodoDBPractice;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public class RhodoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public RhodoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public RhodoDBPractice save(RhodoDBPractice rhododendron) {
        entityManager.persist(rhododendron);
        entityManager.flush();
        return rhododendron;
    }

    public RhodoDBPractice load(String id) {
        return entityManager.find(RhodoDBPractice.class, id);
    }
    
    public List<RhodoDBPractice> findAll() {
        TypedQuery<RhodoDBPractice> query = entityManager.createQuery("SELECT r FROM RhodoDBPractice r", RhodoDBPractice.class);
        return query.getResultList();
    }


    
    
}