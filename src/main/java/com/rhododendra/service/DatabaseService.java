package com.rhododendra.service;

import com.rhododendra.repositories.RhodoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class DatabaseService {
    @Autowired
    private EntityManager entityManager;

    public void testJPA() throws IOException {
        var photos = JSONLoaderService.loadPhotoDetails();
        photos.forEach(photoDetails -> {
            entityManager.persist(photoDetails);
        });
        entityManager.flush();
    }
}
