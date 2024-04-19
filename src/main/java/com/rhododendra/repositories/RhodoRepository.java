package com.rhododendra.repositories;

import com.rhododendra.infrastructure.persisted.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.rhododendra.model.RhodoDBPractice;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
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

    // TODO
    public RhododendronEntity getRhodoWithOldId(String oldId) {
        TypedQuery<RhododendronEntity> query = entityManager.createQuery("SELECT r FROM RhodoDBPractice r", RhodoDBPractice.class);
        return query.getSingleResult();
    }

    // TODO
    public HybridizerEntity getHybridizerWithOldId(String oldId) {
        TypedQuery<HybridizerEntity> query = entityManager.createQuery("SELECT r FROM RhodoDBPractice r", RhodoDBPractice.class);
        return query.getSingleResult();
    }

    // TODO
    public BotanistEntity getBotanistWithOldId(String oldId) {
        TypedQuery<BotanistEntity> query = entityManager.createQuery("SELECT r FROM RhodoDBPractice r", RhodoDBPractice.class);
        return query.getSingleResult();
    }

    // TODO
    public BotanicalSynonymEntity getBotanicalSynonymByName(String name){
        TypedQuery<BotanicalSynonymEntity> query = entityManager.createQuery("SELECT r FROM RhodoDBPractice r", RhodoDBPractice.class);
        return query.getSingleResult();
    }

    // TODO
    public RhodoPhotoEntity getRhodoPhotoEntity(String regularPhotoFileName) {
        TypedQuery<RhodoPhotoEntity> query = entityManager.createQuery("SELECT r FROM RhodoDBPractice r", RhodoDBPractice.class);
        return query.getSingleResult();
    }

    // TODO
    public PhotoDetailsEntity getPhotoDetailsEntity(String regularPhotoFileName) {
        TypedQuery<PhotoDetailsEntity> query = entityManager.createQuery("SELECT r FROM RhodoDBPractice r", RhodoDBPractice.class);
        return query.getSingleResult();
    }




}