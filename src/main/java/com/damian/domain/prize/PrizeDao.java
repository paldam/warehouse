package com.damian.domain.prize;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface PrizeDao extends CrudRepository<Prize, Long> {

    List<Prize> findAllBy();

    @Query(value = "SELECT * FROM prizes WHERE isAvailable = 1", nativeQuery = true)
    List<Prize> findAllWithoutDel();

    @Query(value = "SELECT * FROM prizes WHERE id = ?1", nativeQuery = true)
    Prize findByIdNr(Long id);

    @Transactional
    @Modifying
    @Query(value = "update prizes set stock =  stock - ?2  WHERE id = ?1", nativeQuery = true)
    void updateStockMinus(Long prizeId, Integer minusValue);
}
