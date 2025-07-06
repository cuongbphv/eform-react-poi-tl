package com.ceent.eform.repository;

import com.ceent.eform.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    Optional<Template> findByName(String name);

    List<Template> findByNameContainingIgnoreCase(String name);

    @Query("SELECT t FROM Template t ORDER BY t.createdAt DESC")
    List<Template> findAllOrderByCreatedAtDesc();

    boolean existsByName(String name);
}
