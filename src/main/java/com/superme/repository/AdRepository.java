package com.superme.repository;


import com.superme.enums.AdStatus;
import com.superme.model.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Ad, Long>, JpaSpecificationExecutor<Ad> {

    List<Ad> findByStatus(AdStatus status);

    List<Ad> findByTargetScreenAndStatus(String targetScreen, AdStatus status);

    List<Ad> findByStatusAndStartDateTimeBeforeAndEndDateTimeAfter(
            AdStatus status, LocalDateTime now1, LocalDateTime now2);

    boolean existsByAdName(String adName);

    // Add this method for stats
    long countByStatus(AdStatus status);
}
