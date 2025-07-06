package com.ceent.eform.repository;

import com.ceent.eform.entity.Form;
import com.ceent.eform.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

    List<Form> findByTemplate(Template template);

    List<Form> findByTemplateId(Long templateId);

    List<Form> findByNameContainingIgnoreCase(String name);

    @Query("SELECT f FROM Form f WHERE f.template.id = :templateId ORDER BY f.createdAt DESC")
    List<Form> findByTemplateIdOrderByCreatedAtDesc(@Param("templateId") Long templateId);

    @Query("SELECT f FROM Form f ORDER BY f.createdAt DESC")
    List<Form> findAllOrderByCreatedAtDesc();

    long countByTemplateId(Long templateId);
}
