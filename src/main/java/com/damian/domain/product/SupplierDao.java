package com.damian.domain.product;

import com.damian.domain.product.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface SupplierDao  extends JpaRepository<Supplier,Long> {

    Supplier findBySupplierName(String name) ;
    List<Supplier> findAllByOrderBySupplierNameAsc();
}