package com.damian.boundry.rest;

import com.damian.domain.audit.OrderAuditedRevisionEntity;
import com.damian.domain.notification.Notification;
import com.damian.domain.notification.NotificationDao;
import com.damian.domain.order.*;
import com.damian.domain.order_file.DbFile;
import com.damian.domain.order_file.DbFileDao;
import com.damian.domain.product.ProductDao;
import com.damian.domain.user.User;
import com.damian.domain.user.UserRepository;
import com.damian.dto.NumberOfBasketOrderedByDate;
import com.damian.dto.OrderDto;
import com.damian.domain.order.exceptions.OrderStatusException;
import com.damian.domain.order_file.DbFileService;
import com.damian.dto.OrderItemsDto;
import com.damian.security.SecurityUtils;
import com.damian.util.PdfOrderProductCustom;
import org.apache.log4j.Logger;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.hibernate.envers.AuditReader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static com.damian.config.Constants.ANSI_RESET;
import static com.damian.config.Constants.ANSI_YELLOW;

@Transactional
@RestController
public class OrderController {
    public static final List<SseEmitter> emitters = Collections.synchronizedList( new ArrayList<>());
    private static final Logger logger = Logger.getLogger(OrderController.class);

    @Autowired
    private EntityManagerFactory factory;
    private OrderDao orderDao;
    private OrderService orderService;
    private OrderStatusDao orderStatusDao;
    private ProductDao productDao;
    private DbFileDao dbFileDao;
    private DbFileService dbFileService;
    private UserRepository userRepository;
    private NotificationDao notificationDao;

    OrderController(UserRepository userRepository, NotificationDao notificationDao, DbFileService dbFileService, OrderDao orderDao, OrderService orderService, DeliveryTypeDao deliveryTypeDao, OrderStatusDao orderStatusDao, ProductDao productDao, DbFileDao dbFileDao) {
        this.orderDao = orderDao;
        this.notificationDao = notificationDao;
        this.orderService = orderService;
        this.orderStatusDao = orderStatusDao;
        this.productDao = productDao;
        this.dbFileDao = dbFileDao;
        this.dbFileService = dbFileService;
        this.userRepository = userRepository;

    }

    @RequestMapping(path = "/notification", method = RequestMethod.GET)
    public SseEmitter stream() throws IOException {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));

        return emitter;
    }




    @GetMapping(value = "/order/{id}")
    ResponseEntity<Order> getOrder(@PathVariable Long id) {
        Order order = orderDao.findByOrderId(id);
        return new ResponseEntity<Order>(order, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping(value = "/orderhistory/{id}")
    ResponseEntity<List<Order>> getOrderHistory2(@PathVariable Long id) {
        AuditReader auditReader = AuditReaderFactory.get(factory.createEntityManager());
        //
        AuditQuery query = auditReader.createQuery().forEntitiesModifiedAtRevision(Order.class, id);
        List<Order> orderTmp = (List<Order>) query.getResultList();
        return new ResponseEntity<List<Order>>(orderTmp, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping(value = "/order_history_prev_rev/{id}")
    ResponseEntity<List<Order>> getPreviousVersionOfOrderHistory(@PathVariable Long id) {
        AuditReader auditReader = AuditReaderFactory.get(factory.createEntityManager());
        Optional<BigInteger> revNumber = orderDao.getRevisionNumberOFfPreviousOrderState(id);
        BigInteger orderRevisionToGet;
        orderRevisionToGet = revNumber.orElseGet(() -> BigInteger.valueOf(id));
        AuditQuery query = auditReader.createQuery().forEntitiesModifiedAtRevision(Order.class, orderRevisionToGet.longValue());
        List<Order> orderTmp = (List<Order>) query.getResultList();
        return new ResponseEntity<List<Order>>(orderTmp, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping(value = "/orderitemshistory/{id}")
    ResponseEntity<List<OrderItem>> getOrderItemsHistory(@PathVariable Long id) {
        AuditReader auditReader = AuditReaderFactory.get(factory.createEntityManager());
        //
        Optional<BigInteger> revNumber = orderDao.getRevisionNumberOFfPreviousOrderState(id);
        BigInteger orderRevisionToGet;
        if (revNumber.isPresent()) {
            orderRevisionToGet = revNumber.get();
        } else {
            orderRevisionToGet = BigInteger.valueOf(id);
        }
        AuditQuery query = auditReader.createQuery().forEntitiesModifiedAtRevision(OrderItem.class, orderRevisionToGet.longValue());
        //.forEntitiesAtRevision(OrderItem.class,orderRevisionToGet.longValue());
        // Order order = auditReader.find(Order.class, 1L,1L);
        //ArrayList<Object[]> list = (ArrayList) query.getResultList();
        List<OrderItem> orderTmp = (List<OrderItem>) query.getResultList();
        return new ResponseEntity<List<OrderItem>>(orderTmp, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/order/audit/{id}")
    ResponseEntity<List<OrderAuditedRevisionEntity>> getOrderAudit(@PathVariable Integer id) {
        List<Object[]> orderHistoryListTmp = orderDao.getOrderHistoryById(id);
        List<OrderAuditedRevisionEntity> orderAuditedRevisionEntitiesList = new ArrayList<>();
        orderHistoryListTmp.forEach(objects -> {
            orderAuditedRevisionEntitiesList.add(new OrderAuditedRevisionEntity(((BigInteger) objects[0]).longValue(), ((BigInteger) objects[1]).longValue(), ((BigInteger) objects[3]).longValue(), (String) objects[2]));
        });
        return new ResponseEntity<List<OrderAuditedRevisionEntity>>(orderAuditedRevisionEntitiesList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/orders")
    ResponseEntity<List<Order>> getOrders() {
        List<Order> ordersList = orderDao.findAllWithoutDeleted();
        return new ResponseEntity<List<Order>>(ordersList, HttpStatus.OK);
    }



    //TODO
    @CrossOrigin
    @GetMapping("/orders/production")
    ResponseEntity<List<OrderDto>> getOrdersForProduction() {

        Optional<User> optUser=  userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin());

       if (optUser.isPresent()){
           List<Order> ordersList  = orderDao.getAllOrdersByProductionUserId(optUser.get().getId());

           List<OrderDto> orderDtoList = new ArrayList<>();
           List<DbFile> dbFileDtoList = dbFileDao.findAll();


           ordersList.forEach(order -> {
               List<DbFile> result = new LinkedList<>();
               //result =  dbFileDtoList.stream().filter(data -> data.getOrderHistoryId() == 835).collect(Collectors.toList());

               result = dbFileDtoList.stream()
                   .filter(data -> order.getOrderId().equals(data.getOrderId()))
                   .collect(Collectors.toList());

               Long fileIdTmp = null;

               if (result.size() > 0) {
                   fileIdTmp = result.get(0).getFileId();
               } else {
                   fileIdTmp = 0L;
               }

               orderDtoList.add(new OrderDto(order.getOrderId(), order.getOrderFvNumber(), order.getCustomer(), order.getOrderDate(),
                   order.getAdditionalInformation(), order.getDeliveryDate(), order.getWeekOfYear(), order.getDeliveryType(),
                   order.getOrderStatus(), order.getOrderTotalAmount(), fileIdTmp, order.getOrderItems(), order.getAdditionalSale(),order.getProductionUser()));
           });



           return new ResponseEntity<List<OrderDto>>(orderDtoList, HttpStatus.OK);
        }else{
           return new ResponseEntity<List<OrderDto>>(HttpStatus.FORBIDDEN);
       }
    }

    @CrossOrigin
    @GetMapping("/orderitem")
    ResponseEntity<List<Order>> getOrderItem() {
        List<Order> ordersList = orderDao.findAllWithoutDeleted();
        return new ResponseEntity<List<Order>>(ordersList, HttpStatus.OK);
    }
    //    @CrossOrigin
    //    @GetMapping("/orderswithattch/")
    //    ResponseEntity<List<Order>> getOrdersWithAttach(){
    //        List<Order> ordersList = orderDao.findAllByDbFileIsNotNull();
    //        return new ResponseEntity<List<Order>>(ordersList, HttpStatus.OK);
    //    }

    @CrossOrigin
    @GetMapping("/orders/daterange")
    ResponseEntity<List<Order>> getOrdersByDateRange(@RequestParam(value = "startDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate, @RequestParam(value = "endDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Order> ordersList = orderDao.findOrdersByDateRange(startDate, endDate);
        return new ResponseEntity<List<Order>>(ordersList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/order_status")
    ResponseEntity<List<OrderStatus>> getOrderStatus() {
        List<OrderStatus> ordersStatusList = orderStatusDao.findAllBy();
        return new ResponseEntity<List<OrderStatus>>(ordersStatusList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/orders/products_to_order/daterange")
    ResponseEntity<List<Order>> getProductsToOrder(@RequestParam(value = "startDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate, @RequestParam(value = "endDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {



        List<Order> productToOrderList = orderDao.findProductToOrder(startDate, endDate);

        return new ResponseEntity<List<Order>>(productToOrderList, HttpStatus.OK);
    }





    @CrossOrigin
    @GetMapping("/orders/products_to_order_without_deleted_by_delivery_date/daterange")
    ResponseEntity<List<Order>> getProductsToOrderWithoutDeletedByDeliveryDate(@RequestParam(value = "startDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate, @RequestParam(value = "endDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {

        endDate = setEndOfDay(endDate);

        List<Order> productToOrderList = orderDao.findProductToOrderWithoutDeletedOrderByDeliveryDate(startDate, endDate);
        return new ResponseEntity<List<Order>>(productToOrderList, HttpStatus.OK);
    }





    @CrossOrigin
    @GetMapping("/orders/products_to_order_without_deleted_by_order_date/daterange")
    ResponseEntity<List<Order>> getProductsToOrderWithoutDeletedByOrderDate(@RequestParam(value = "startDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate, @RequestParam(value = "endDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {

        endDate = setEndOfDay(endDate);


        List<Order> productToOrderList = orderDao.findProductToOrderWithoutDeletedOrderByOrderDate(startDate, endDate);
        return new ResponseEntity<List<Order>>(productToOrderList, HttpStatus.OK);
    }



    @CrossOrigin
    @GetMapping("/order/statistic/orderdaterange")
    ResponseEntity<List<Order>> getOrdersByBasket(@RequestParam(value = "basketId", required = true) Long basketId, @RequestParam(value = "startDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate, @RequestParam(value = "endDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        endDate = setEndOfDay(endDate);
        List<Order> orderList = orderDao.findAllOrderByBasketIdAndOrderDate(basketId, startDate, endDate);
        return new ResponseEntity<List<Order>>(orderList, HttpStatus.OK);
    }



    @CrossOrigin
    @GetMapping("/baskets/statistic/daterange")
    ResponseEntity<List<NumberOfBasketOrderedByDate>> getNumberOfBasketOrdered(@RequestParam(value = "startDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate, @RequestParam(value = "endDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        endDate = setEndOfDay(endDate);
        List<NumberOfBasketOrderedByDate> basketList = orderDao.getNumberOfBasketOrdered(startDate, endDate);
        return new ResponseEntity<List<NumberOfBasketOrderedByDate>>(basketList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/baskets/statistic/orderdaterange")
    ResponseEntity<List<NumberOfBasketOrderedByDate>> getNumberOfBasketOrderedFilteredByOrderDate(@RequestParam(value = "startDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate, @RequestParam(value = "endDate", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        endDate = setEndOfDay(endDate);
        List<NumberOfBasketOrderedByDate> basketList = orderDao.getNumberOfBasketOrderedFilteredByOrderDate(startDate, endDate);
        return new ResponseEntity<List<NumberOfBasketOrderedByDate>>(basketList, HttpStatus.OK);
    }




    @CrossOrigin
    @GetMapping("/orderdao")
    ResponseEntity<OrderPageRequest> getOrderDao(
        @RequestParam(value = "page", required = true, defaultValue = "0") int page,
        @RequestParam(value = "size", required = true) int size,
        @RequestParam(value = "searchtext", required = false) String text,
        @RequestParam(value = "orderBy", required = false) String orderBy,
        @RequestParam(value = "sortingDirection", required = false, defaultValue = "1") int sortingDirection,
        @RequestParam(value = "orderStatusFilterList", required = false) List<Integer> orderStatusFilterList,
        @RequestParam(value = "orderYearsFilterList", required = false) List<Integer> orderYearsFilterList) {

        if (orderStatusFilterList == null) {
            orderStatusFilterList = new ArrayList<>();
        }
        if (orderYearsFilterList == null) {
            orderYearsFilterList = new ArrayList<>();
        }
        OrderPageRequest orderDtoList = orderService.getOrderDao(page, size, text, orderBy, sortingDirection, orderStatusFilterList, orderYearsFilterList);
        return new ResponseEntity<OrderPageRequest>(orderDtoList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/ordercount")
    ResponseEntity<Long> getOrderCount() {
        long numberOfRows = orderDao.getCountOfAllOrdersWithoutDeleted();
        return new ResponseEntity<Long>(numberOfRows, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/ordersyears")
    ResponseEntity<int[]> getOrderYears() {
        int[] years = orderDao.getOrdersYears();
        return new ResponseEntity<int[]>(years, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/orderstats")
    ResponseEntity<List<OrderDto>> getOrderStats() {
        List<OrderDto> orderDtoList = orderService.getOrderStats();
        return new ResponseEntity<List<OrderDto>>(orderDtoList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/order/customer/{id}")
    ResponseEntity<List<OrderDto>> getOrdersByCustomer(@PathVariable Integer id) {
        List<OrderDto> ordersList = orderService.getOrderDaoByCustomer(id);
        return new ResponseEntity<List<OrderDto>>(ordersList, HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping( value = "/order/assign_production",produces = "text/plain;charset=UTF-8")
    ResponseEntity assignOrdersToSpecifiedProduction(@RequestParam(value = "ordersIds", required = true) List<Integer> ordersIds,
                                                     @RequestParam(value = "productionId", required = true) Long productionId) {
        try {
            orderService.assignOrdersToSpecifiedProduction(ordersIds, productionId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (OrderStatusException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping("/orders")
    ResponseEntity createOrUpdateOrder(@RequestBody Order order) throws URISyntaxException, OrderStatusException {
        try {
            orderService.createOrUpdateOrder(order);
            return new ResponseEntity<Order>(order, HttpStatus.CREATED);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }


    @CrossOrigin
    @PostMapping("/order/copy/{originOrderIdCopy}")
    ResponseEntity copyOrderFromExistingOne(@RequestBody Order order,@PathVariable Long originOrderIdCopy ) throws URISyntaxException, OrderStatusException {
        Order originOrder = orderDao.findByOrderId(originOrderIdCopy);
        notificationDao.save(new Notification("Dodano zamówienie nr: "+ order.getOrderFvNumber() + " połączone z zamówieniem nr: "+ originOrder.getOrderFvNumber(),null));

        try {
            orderService.createOrderFromCopy(order);

            orderService.pushNotificationForCombinedOrder(originOrder.getOrderFvNumber(),order.getOrderFvNumber());
            return new ResponseEntity<Order>(order, HttpStatus.CREATED);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }


    @CrossOrigin
    @PostMapping(value = "/order/status/{id}/{statusId}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeOrderStatus(@PathVariable Long id, @PathVariable Integer statusId) throws URISyntaxException, OrderStatusException {
        Order updatingOrder = orderDao.findByOrderId(id);
        OrderStatus updattingOrderNewStatus = new OrderStatus();
        updattingOrderNewStatus.setOrderStatusId(statusId);
        updatingOrder.setOrderStatus(updattingOrderNewStatus);
        try {
            orderService.createOrUpdateOrder(updatingOrder);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/order/progress/{id}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeOrderProgress(@PathVariable Long id, @RequestBody List<OrderItem> orderItems) {
        Order updatingOrder = orderDao.findByOrderId(id);
        try {
            orderService.changeOrderProgress(id, orderItems);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }



    @CrossOrigin
    @GetMapping(value = "/order/orderitem/progress/warehouse/{orderItemId}/{newStateValueOnWarehouse}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeSpecyfiedOrderItemProgressOnWarehouseByNewValue(@PathVariable Integer orderItemId, @PathVariable Long newStateValueOnWarehouse) {
        try {
            orderService.changeOrderItemProgressOnSpecifiedPhase(orderItemId,newStateValueOnWarehouse,OrdersPreparePhase.ON_WAREHOUSE);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/order/orderitem/progress/production/{orderItemId}/{newStateValueOnProduction}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeSpecyfiedOrderItemProgressOnProductionByNewValue(@PathVariable Integer orderItemId, @PathVariable Long newStateValueOnProduction) {
        try {
            orderService.changeOrderItemProgressOnSpecifiedPhase(orderItemId,newStateValueOnProduction,OrdersPreparePhase.ON_PRODUCTION);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/order/orderitem/progress/logistics/{orderItemId}/{newStateValueOnLogistics}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeSpecyfiedOrderItemProgressOnLogisticsByNewValue(@PathVariable Integer orderItemId, @PathVariable Long newStateValueOnLogistics) {
        try {
            orderService.changeOrderItemProgressOnSpecifiedPhase(orderItemId, newStateValueOnLogistics,OrdersPreparePhase.ON_LOGISTICS);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/order/orderitem/progress/warehouse/addvalue/{orderItemId}/{newStateValueToAddOnWarehouse}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeSpecifiedOrderItemProgressOnWarehouseByAddValue(@PathVariable Integer orderItemId, @PathVariable Long newStateValueToAddOnWarehouse) {
        try {
            orderService.changeOrderItemProgressOnSpecifiedPhaseByAddValue(orderItemId,newStateValueToAddOnWarehouse,OrdersPreparePhase.ON_WAREHOUSE);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/order/orderitem/progress/production/addvalue/{orderItemId}/{newStateValueToAddOnProduction}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeSpecifiedOrderItemProgressOnProductionByAddValue(@PathVariable Integer orderItemId, @PathVariable Long newStateValueToAddOnProduction) {
        try {
            orderService.changeOrderItemProgressOnSpecifiedPhaseByAddValue(orderItemId,newStateValueToAddOnProduction,OrdersPreparePhase.ON_PRODUCTION);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/order/orderitem/progress/logistics/addvalue/{orderItemId}/{newStateValueToAddOnLogistics}", produces = "text/plain;charset=UTF-8")
    ResponseEntity changeSpecifiedOrderItemProgressOnLogisticsByAddValue(@PathVariable Integer orderItemId, @PathVariable Long newStateValueToAddOnLogistics) {
        try {
            orderService.changeOrderItemProgressOnSpecifiedPhaseByAddValue(orderItemId,newStateValueToAddOnLogistics,OrdersPreparePhase.ON_LOGISTICS);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (OrderStatusException oEx) {
            return ResponseEntity.badRequest().body(oEx.getMessage());
        }
    }

    @CrossOrigin
    @DeleteMapping("/order/{id}")
    ResponseEntity deleteOrderPermanent(@PathVariable Long id) {
        orderDao.deleteById(id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/order/pdf/aaa/{orderId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> getBasketProductsPdf(@RequestBody List<OrderItemsDto> orderItemsDto , @PathVariable Long orderId) throws IOException {
        PdfOrderProductCustom pdfGenerator = new PdfOrderProductCustom();

        Order order = orderDao.findByOrderId(orderId);

        ByteArrayInputStream bis = pdfGenerator.generateBasketsProductsCustomListPdf(orderItemsDto,order);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=produkty_zamowienia.pdf");
        new InputStreamResource(bis);
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
    }




    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(112000000); //12MB
        return multipartResolver;
    }

    private Date setEndOfDay(Date date){
        date.setHours(23);
        date.setMinutes(59);
        date.setSeconds(59);
        return date;
    }


}
