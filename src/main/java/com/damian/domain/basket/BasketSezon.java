package com.damian.domain.basket;

import javax.persistence.*;

@Entity
@Table(name = "basket_sezon")
public class BasketSezon {

    private Integer basketSezonId;
    private String basketSezonName;

    public BasketSezon() {

    }

    public BasketSezon(String basketSezonName) {
        this.basketSezonName = basketSezonName;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "basket_sezon_id", nullable = false)
    public Integer getBasketSezonId() {
        return basketSezonId;
    }

    public void setBasketSezonId(Integer basketSezonId) {
        this.basketSezonId = basketSezonId;
    }

    @Basic
    @Column(name = "basket_sezon_name", nullable = false, length = 100)
    public String getBasketSezonName() {
        return basketSezonName;
    }

    public void setBasketSezonName(String basketSezonName) {
        this.basketSezonName = basketSezonName;
    }
}