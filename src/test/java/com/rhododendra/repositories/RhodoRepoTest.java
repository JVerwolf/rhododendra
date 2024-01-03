package com.rhododendra.repositories;

import com.rhododendra.model.RhodoDBPractice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RhodoRepoTest {

    @Autowired
    RhodoRepository rhodoRepository;

    @Test
    public void newMethod() {
        var test = new RhodoDBPractice("test","test");
        RhodoDBPractice savedPractice = rhodoRepository.save(test);

        var savedTest = rhodoRepository.load(test.getId());

    }
}
