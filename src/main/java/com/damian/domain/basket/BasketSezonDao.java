package com.damian.domain.basket;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Transactional(readOnly = true)
public interface BasketSezonDao extends CrudRepository<BasketSezon,Long> {
    public List<BasketSezon> findAllBy();
    public List<BasketSezon> findByIsActiveTrue();
    public BasketSezon findByBasketSezonId(Integer id);
    public Optional<BasketSezon> findByBasketSezonName(String name);


}
