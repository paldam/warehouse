package com.damian.domain.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface NoteDao extends JpaRepository<Note,Long> {

    public List<Note> findAll() ;

    @Query(value = "SELECT * FROM notes WHERE note_status != 0", nativeQuery = true)
    public List<Note> findAllWithoutDeleted() ;

    public List<Note> findAllByAddedBy(String addedBy);


}

