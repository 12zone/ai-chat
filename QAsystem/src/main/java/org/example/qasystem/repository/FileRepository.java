package org.example.qasystem.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.example.qasystem.domain.File;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File,Long> {

    long countByIsDeletedFalse();

    List<File> findByIsDeletedFalse(Sort sort);

    Page<File> findByIsDeletedFalse(Pageable pageable);

    List<File> findByIsDeletedTrue(Sort sort);

    List<File> findByCategoryAndIsDeletedFalse(String category,Sort sort);
    Page<File> findByCategoryAndIsDeletedFalse(String category,Pageable pageable);
    Optional<File> findByFileIdAndIsDeletedFalse(Long fileId);
    Page<File> findByTitleAndIsDeletedFalse(String title, Pageable pageable);

    @Query("select distinct f.fileId from File f where f.isDeleted = false order by f.fileId asc")
    List<Long> findDistinctFileIds();

    @Query("select distinct f.category from File f where f.isDeleted = false order by f.category asc")
    List<String> findDistinctCategories();

    @Query("select distinct f.title from File f where f.isDeleted = false order by f.title asc")
    List<String> findDistinctTitles();
}
