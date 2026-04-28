package com.superme.repository;

import com.superme.model.FaqMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<FaqMaster,Long> {

    List<FaqMaster> findByScreen(String screen);

    List<FaqMaster> findBySection(String section);

    List<FaqMaster> findByScreenAndSection(String screen, String section);
}
