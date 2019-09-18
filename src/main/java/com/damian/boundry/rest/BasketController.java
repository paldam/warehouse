package com.damian.boundry.rest;

import com.damian.domain.basket.*;
import com.damian.domain.order.Order;
import com.damian.domain.order.OrderItem;
import com.damian.domain.order_file.DbFile;
import com.damian.domain.product.Product;
import com.damian.domain.product.ProductDao;
import com.damian.domain.product.Supplier;
import com.damian.dto.BasketDto;
import com.damian.dto.BasketExtStockDao;
import com.damian.util.PdfBasketContents;
import com.damian.util.PdfGenerator;
import org.apache.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.damian.config.Constants.ANSI_RESET;
import static com.damian.config.Constants.ANSI_YELLOW;

@RestController
public class BasketController {

    private static final Logger logger = Logger.getLogger(BasketController.class);
    private BasketExtService basketExtService;
    private BasketService basketService;
    private BasketDao basketDao;
    private ProductDao productDao;
    private BasketTypeDao basketTypeDao;
    private BasketSezonDao basketSezonDao;

    public BasketController(ProductDao productDao,BasketSezonDao basketSezonDao, BasketService basketService, BasketDao basketDao, BasketTypeDao basketTypeDao, BasketExtService basketExtService) {
        this.basketDao = basketDao;
        this.productDao =productDao;
        this.basketService = basketService;
        this.basketTypeDao = basketTypeDao;
        this.basketExtService = basketExtService;
        this.basketSezonDao = basketSezonDao;
    }

    @Transactional
    @CrossOrigin
    @GetMapping("/basketconvert")
    ResponseEntity<List<Basket>> convertBasket() {
        List<Basket> basketList = basketDao.findAllBy();
        basketList.forEach(basket -> {
            Optional<BasketSezon> optBasket = basketSezonDao.findByBasketSezonName(basket.getSeason());
            if (!optBasket.isPresent()) {
                BasketSezon basketSezonTmp = basketSezonDao.save(new BasketSezon(basket.getSeason()));
                basket.setBasketSezon(basketSezonTmp);
                basketDao.save(basket);
            } else {
                basket.setBasketSezon(optBasket.get());
                basketDao.save(basket);
            }
        });
        return new ResponseEntity<List<Basket>>(basketList, HttpStatus.OK);
    }

    @Transactional
    @CrossOrigin
    @GetMapping("/basketconvert3")
    ResponseEntity<List<Basket>> convertBasket3() {
        List<Basket> basketList = basketDao.findAllBy();




        for (Basket basket : basketList) {
            Integer total = 0;
            for (BasketItems bi : basket.getBasketItems()) {
                total += bi.getProduct().getPrice() * bi.getQuantity();
            }
            basket.setBasketProductsPrice(total);
            basketDao.save(basket);
        }
        return new ResponseEntity<List<Basket>>(basketList, HttpStatus.OK);
    }



    @CrossOrigin
    @GetMapping("/baskets")
    ResponseEntity<List<Basket>> getBaskets() {
        List<Basket> basketList = basketDao.findAllWithoutDeleted();
        return new ResponseEntity<List<Basket>>(basketList, HttpStatus.OK);
    }


    @CrossOrigin
    @GetMapping("/baskets_seasons")
    ResponseEntity<List<BasketSezon>> getBasketsSeasons() {
        List<BasketSezon> basketList = basketSezonDao.findAllBy();
        return new ResponseEntity<List<BasketSezon>>(basketList, HttpStatus.OK);
    }


    @CrossOrigin
    @GetMapping("/basketpage")
    ResponseEntity<BasketPageRequest> getBasketsPage(@RequestParam(value = "page", required = true, defaultValue = "0") int page,
                                                     @RequestParam(value = "size", required = true) int size,
                                                     @RequestParam(value = "searchtext", required = false) String text,
                                                     @RequestParam(value = "orderBy", required = false) String orderBy,
                                                     @RequestParam(value = "sortingDirection", required = false, defaultValue = "1") int sortingDirection,
                                                     @RequestParam(value = "onlyArchival", required = false) boolean onlyArchival,
                                                     @RequestParam(value = "basketSeasonFilter", required = false) List<Integer> basketSeasonFilter)

    {

         System.out.println(ANSI_YELLOW + onlyArchival + " | " + basketSeasonFilter+ ANSI_RESET);




        BasketPageRequest basketsPage = basketService.getBasketsPege(page, size, text, orderBy, sortingDirection,onlyArchival,basketSeasonFilter);




        return new ResponseEntity<BasketPageRequest>(basketsPage, HttpStatus.OK);
    }


    @CrossOrigin
    @GetMapping("/basketsdto")
    ResponseEntity<List<BasketDto>> getBasketsDto() {
        List<BasketDto> basketList = basketDao.findBasketDto();
        return new ResponseEntity<List<BasketDto>>(basketList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/basketswithdeleted")
    ResponseEntity<List<Basket>> getBasketsWithDeleted() {
        List<Basket> basketList = basketDao.findAllWithDeleted();
        return new ResponseEntity<List<Basket>>(basketList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/deletedbaskets/")
    ResponseEntity<List<Basket>> getDeletedBaskets() {
        List<Basket> basketList = basketDao.findAllDeleted();
        return new ResponseEntity<List<Basket>>(basketList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/basket/{id}")
    ResponseEntity<Basket> getBaskets(@PathVariable Long id) {
        Optional<Basket> basket = basketDao.findById(id);
        return new ResponseEntity<Basket>(basket.get(), HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/baskets/types")
    ResponseEntity<List<BasketType>> getBasketsTypes() {
        List<BasketType> basketTypesList = basketTypeDao.findAllBy();
        return new ResponseEntity<List<BasketType>>(basketTypesList, HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping("/basket/add")
    ResponseEntity<Basket> addBasket(@RequestBody Basket basket) throws URISyntaxException {
        basketDao.save(basket);
        return new ResponseEntity<Basket>(basket, HttpStatus.CREATED);
    }

    @CrossOrigin
    @PostMapping("/basketswithoutimage")
    ResponseEntity<Basket> createBasket(@RequestBody Basket basket) throws URISyntaxException {
        Basket basketTmp = basketDao.findById(basket.getBasketId()).get();
        basket.setBasketImageData(basketTmp.getBasketImageData());
        basketDao.save(basket);
        return new ResponseEntity<Basket>(basket, HttpStatus.CREATED);
    }

    @CrossOrigin
    @PostMapping("/baskets")
        // handle file and object in one request
    ResponseEntity<Basket> createBasket2(@RequestPart("basketimage") MultipartFile[] basketMultipartFiles, @RequestPart("basketobject") Basket basket) throws URISyntaxException {
        try {
            basket.setBasketImageData(basketMultipartFiles[0].getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        basket.setIsBasketImg(1);
        basketDao.save(basket);
        return new ResponseEntity<Basket>(basket, HttpStatus.CREATED);
    }

    @CrossOrigin
    @RequestMapping(value = {"/basket/find/{priceMin}/{priceMax}/{basketPrice}/{productsSubTypes}", "/basket/find/{priceMin}/{priceMax}/{basketPrice}"}, method = RequestMethod.GET)
    public ResponseEntity<List<Basket>> getBasketsWithFilters(@PathVariable Integer priceMin, @PathVariable Integer priceMax,@PathVariable Boolean basketPrice, @PathVariable Optional<List<Integer>> productsSubTypes) {
        priceMax = priceMax * 100;
        priceMin = priceMin * 100;
        List<Basket> basketList;
        if (!productsSubTypes.isPresent()) {

            if(basketPrice){
                basketList = basketDao.findBasketsWithFilterWithoutTypes(priceMin, priceMax);
            }else{
                basketList = basketDao.findBasketsWithFilterWithoutTypesByProductsPrice(priceMin, priceMax);
            }


        } else {
            if(basketPrice){
                basketList = basketDao.findBasketsWithFilter(priceMin, priceMax, productsSubTypes.get(),productsSubTypes.get().size());

            }else{
                basketList = basketDao.findBasketsWithFilterByProductsPrice(priceMin, priceMax, productsSubTypes.get(),productsSubTypes.get().size());

            }

        }
        return new ResponseEntity<List<Basket>>(basketList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/basketimage/{basketId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long basketId) {
        byte[] basketFile = basketDao.getBasketImageByBasketId(basketId);
        Optional<byte[]> imgOpt = Optional.ofNullable(basketFile);
        if (!imgOpt.isPresent()) {
            basketFile = new byte[0];
        }
        HttpHeaders header = new HttpHeaders();
        header.setAccessControlExposeHeaders(Collections.singletonList("Content-Disposition"));
        ;
        header.set("Content-Disposition", "attachment; filename=zdjecie kosza");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/jpeg")).headers(header).body(new ByteArrayResource(basketFile));
    }

    @CrossOrigin
    @RequestMapping(value = "/basket/pdf/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> getOrderPdf(@PathVariable Long id) throws IOException {
        byte[] img = basketDao.getBasketImageByBasketId(id);
        Optional<byte[]> imgOpt = Optional.ofNullable(img);
        if (!imgOpt.isPresent()) {
            img = new byte[0];
        }
        Optional<Basket> basketToGen = basketDao.findById(id);
        Basket basketToGenerate = new Basket();
        if (basketToGen.isPresent()) {
            basketToGenerate = basketToGen.get();
        }
        PdfGenerator pdfGenerator = new PdfGenerator();
        ByteArrayInputStream bis = PdfBasketContents.generateBasketProductsListPdf(basketToGenerate, img);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=order.pdf");
        new InputStreamResource(bis);
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
    }

    @CrossOrigin
    @PostMapping("/basketext")
    ResponseEntity<BasketExt> createExternalBasket(@RequestBody BasketExt basketExt) throws URISyntaxException {
        System.out.println("22222" + basketExt.toString());
        basketExtService.saveExternalBasket(basketExt);
        return new ResponseEntity<BasketExt>(basketExt, HttpStatus.CREATED);
    }

    @CrossOrigin
    @PostMapping("/basketextstatus")
    ResponseEntity<BasketExt> externalBasketStatus(@RequestBody BasketExt basketExt) throws URISyntaxException {
        Basket basketToChange = new Basket(basketExt);
        Basket savedBasket = basketDao.save(basketToChange);
        return new ResponseEntity<BasketExt>(basketExt, HttpStatus.CREATED);
    }

    @CrossOrigin
    @GetMapping("/basketsextlist")
    ResponseEntity<List<BasketExt>> getBasketsExtList() {
        List<Basket> basketList = basketDao.findALLExportBasket();
        List<BasketExt> basketExtList = new ArrayList<>();
        basketList.forEach(basket -> {
            BasketExt basketTmp = new BasketExt(basket);
            basketTmp.setBasketTotalPrice(basketTmp.getBasketTotalPrice());
            basketExtList.add(basketTmp);
        });
        return new ResponseEntity<List<BasketExt>>(basketExtList, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/basket_ext_stock")
    ResponseEntity<List<BasketExtStockDao>> getBasketsExtStock() {
        List<Basket> basketList = basketDao.findALLExportBasket();
        List<BasketExtStockDao> basketExtList = new ArrayList<>();
        basketList.forEach(basket -> basketExtList.add(new BasketExtStockDao(basket)));
        return new ResponseEntity<List<BasketExtStockDao>>(basketExtList, HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping(value = "/baskets/stockadd", produces = "application/json; charset=utf-8")
    ResponseEntity<List<OrderItem>> addBasketsToStock(@RequestBody List<OrderItem> orderItems) {
        if (orderItems.isEmpty()) {
            return new ResponseEntity<List<OrderItem>>(orderItems, HttpStatus.BAD_REQUEST);
        }
        basketService.addNumberOfProductsDelivery(orderItems);
        return new ResponseEntity<List<OrderItem>>(orderItems, HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping(value = "/baskets/stockadd/{basketId}/{newValue}", produces = "application/json; charset=utf-8")
    ResponseEntity<Long> addNewBasketsStateOfStock(@PathVariable Long basketId, @PathVariable Integer newValue) {
        basketDao.saveNewStockOfBasket(basketId, newValue);
        return new ResponseEntity<Long>(basketId,HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/extbaskets")
    ResponseEntity<List<Basket>> getBasketsForExternalPartner() {
        List<Basket> basketList = basketDao.findAllBasketForExternalPartner();
        return new ResponseEntity<List<Basket>>(basketList, HttpStatus.OK);
    }
}
