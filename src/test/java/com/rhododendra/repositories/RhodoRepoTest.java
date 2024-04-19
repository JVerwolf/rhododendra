package com.rhododendra.repositories;

import com.rhododendra.model.RhodoDBPractice;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.service.JSONLoaderService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Propagation;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class RhodoRepoTest {

    @Autowired
    RhodoRepository rhodoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void newMethod() {
        var test = new RhodoDBPractice("test", "test");
        RhodoDBPractice savedPractice = rhodoRepository.save(test);

        var savedTest = rhodoRepository.load(test.getId());

    }

    //https://stackoverflow.com/questions/39802264/jpa-how-to-persist-many-to-many-relation
    @Test
    @Commit
    public void testJPA() throws IOException {
        var photos = JSONLoaderService.loadPhotoDetails();
        photos.forEach(photoDetails -> {
            entityManager.persist(photoDetails);
        });
        entityManager.flush();

        List<Rhododendron> rhodos = JSONLoaderService.loadRhodos();
        rhodos.forEach(rhododendron ->
            entityManager.persist(rhododendron)
        );
        entityManager.flush();

        assertTrue(true);
    }
}
