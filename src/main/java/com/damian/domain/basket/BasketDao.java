package com.damian.domain.basket;


import com.damian.domain.basket.Basket;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BasketDao extends CrudRepository<Basket,Long> {
    public List<Basket> findAllBy();
    public List<Basket> findAllByOrderByBasketIdDesc();


    @Query(value = "SELECT data FROM baskets WHERE basket_id=?1", nativeQuery = true)
    public byte[] getBasketImageByBasketId(Long basketId);


    @Query(value = "SELECT * FROM baskets WHERE basket_type != 99 AND basket_type != 999", nativeQuery = true)
    public List<Basket> findAllWithoutDeleted();

    @Query(value = "SELECT * FROM baskets WHERE  basket_type != 999", nativeQuery = true)
    public List<Basket> findAllWithDeleted();

    @Query(value = "SELECT * FROM baskets WHERE basket_type = 100 ", nativeQuery = true)
    public List<Basket> findAllBasketForExternalPartner();

    @Query(value = "SELECT * FROM baskets WHERE basket_type = 99", nativeQuery = true)
    public List<Basket> findAllDeleted();

    @Query(value = "SELECT * FROM baskets WHERE basket_type = 100", nativeQuery = true)
    public List<Basket> findALLExportBasket();

    @Query(value = "select * from baskets INNER join basket_items on baskets.basket_id = basket_items.basket_id where basket_items.product_id = ?1 and (baskets.basket_type = 1 Or baskets.basket_type =2) ", nativeQuery = true)
    public List<Basket> BasketListByProduct(Integer productId);


    @Query(value = "select * from baskets INNER join basket_items on baskets.basket_id = basket_items.basket_id INNER join products ON basket_items.product_id = products.id" +
        " where product_sub_type_id IN ?3 AND baskets.basket_total_price >= ?1 AND baskets.basket_total_price <=?2", nativeQuery = true)
    public List<Basket> findBasketsWithFilter(Integer priceMin,Integer priceMax, List<Integer> subTypeList);

    @Query(value = "select * from baskets INNER join basket_items on baskets.basket_id = basket_items.basket_id INNER join products ON basket_items.product_id = products.id" +
        " where  baskets.basket_total_price >= ?1 AND baskets.basket_total_price <=?2", nativeQuery = true)
    public List<Basket> findBasketsWithFilterWithoutTypes(Integer priceMin,Integer priceMax);
}
