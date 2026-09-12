package com.victrom.jobtracker.repository;

import com.victrom.jobtracker.model.JobApplication;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>,
        JpaSpecificationExecutor<JobApplication> {

    @EntityGraph(attributePaths = "events")
    @Query("select a from JobApplication a where a.id = :id")
    Optional<JobApplication> findDetailedById(@Param("id") Long id);
}
