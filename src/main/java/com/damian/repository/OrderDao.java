package com.damian.repository;

import com.damian.dto.NumberOfBasketOrderedByDate;
import com.damian.model.Order;
import com.damian.model.Product;
import com.damian.model.ProductToOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;


public interface OrderDao extends JpaRepository<Order,Long> {
    public List<Order> findAllBy();
    public List<Order> findAllByOrderByOrderIdDesc();
    public List<Order> findAllByOrderStatus_OrderStatusId(Integer i);
    public Order findByOrderId(Integer id);
    public List<Order> findByAddress_AddressId(Long id);
    public List<Order> findByCustomer_CustomerId(Integer id);
    //public Order (Integer id);


    @Query(value = "SELECT * FROM orders WHERE customer_id = ?1", nativeQuery = true)
    public List<Order> findByCustomerId(Integer id);




    @Query(value = "SELECT * FROM orders WHERE order_status_id != 99 ORDER BY order_date DESC ", nativeQuery = true)
    public List<Order> findAllWithoutDeleted();

    @Query(value = "SELECT * FROM orders WHERE delivery_date BETWEEN ?1 AND ?2 AND delivery_type !=2", nativeQuery = true)
    public List<Order> findOrdersByDateRange(Date startDate, Date endDate);

//    @Query(value = "SELECT NEW com.damian.model.ProductToOrder(p.id,p.productName) FROM Product p")
//
//    public List<ProductToOrder> findProductToOrder();




    @Query(value = "SELECT NEW com.damian.model.ProductToOrder(p.id,p.productName,p.supplier, p.stock,sum(oi.quantity*bi.quantity),p.capacity) FROM Order o JOIN o.orderItems oi " +
                                          "JOIN oi.basket b " +
                                          "JOIN b.basketItems bi JOIN bi.product p WHERE o.orderStatus.orderStatusId=1 AND o.deliveryDate BETWEEN ?1 AND ?2  GROUP BY p.id")

    public List<Order> findProductToOrder(Date startDate, Date endDate);



    @Query(value = "select new com.damian.dto.NumberOfBasketOrderedByDate(b.basketName,sum(oi.quantity)) FROM Order o JOIN o.orderItems oi JOIN oi.basket b  where " +
            "o.orderDate between ?1 and ?2 and o.orderStatus !=99 group by b.basketName")
    public List<NumberOfBasketOrderedByDate> getNumberOfBasketOrdered (Date startDate, Date endDate);




    @Query(value = "SELECT NEW com.damian.dto.OrderDto(o.orderId,o.orderFvNumber,o.customer, o.orderDate,o.additionalInformation,o.deliveryDate," +
            " o.deliveryType,o.orderStatus, o.orderTotalAmount, o.address, f.dbFile) FROM Order o JOIN a. DbFile", nativeQuery = true)

    public List<Order> getOrdersDto();










}
