package edu.utfpr.guilhermej.sd.stockmarketwebservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.*;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.services.ITransactionRoomService;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Controller
public class TransactionRoomController {
    private ITransactionRoomService transactionRoomService;

    @Autowired
    public void setTransactionRoomService(ITransactionRoomService transactionRoomService) {
        this.transactionRoomService = transactionRoomService;
    }

    @RequestMapping("/stock/order/buy")
    public ResponseEntity createStockOrder(@RequestBody BuyStockOrder order){
        transactionRoomService.addOrder(order);
        return ResponseEntity.ok(order);
    }

    @RequestMapping("/stock/order/sell")
    public ResponseEntity createStockOrder(@RequestBody SellStockOrder order){
        transactionRoomService.addOrder(order);
        return ResponseEntity.ok(order);
    }

    @RequestMapping("/stock/event/subscribe")
    public ResponseEntity createSubscription(
            @RequestBody Stockholder subscriber,
            @RequestParam(value = "eventType")  StockEvent.StockEventType   eventType,
            @RequestParam(value = "enterprise") String                      enterprise,
            @RequestParam(value = "isBuying",   required = false)   Boolean isBuying,
            @RequestParam(value = "isSelling",  required = false)   Boolean isSelling){
        if((isBuying != null || isSelling != null) && !eventType.equals(StockEvent.StockEventType.ADDED))
            throw new IllegalArgumentException("isBuying/isSelling arguments are valid only for ADDED type events");

        Predicate<StockEvent> filter = event ->
                event.isFromEnterprise(enterprise) && event.getEventType().equals(eventType);
        if(isBuying != null)
            filter = filter.and(event -> event.getNewOrder().isBuying() == isBuying);
        if(isSelling != null)
            filter = filter.and(event -> event.getNewOrder().isSelling() == isSelling);

        transactionRoomService.addSubscriberFilter(subscriber, filter);
        return ResponseEntity.ok(subscriber);
    }

    @RequestMapping("/stock/events")
    public ResponseEntity RetrieveEvents(@RequestBody Stockholder subscriber){
        List<StockEvent> events = transactionRoomService.getEvents(subscriber);
        return ResponseEntity.ok(events);
    }

    @RequestMapping("/stock/order/list")
    public ResponseEntity listStockOrders(
            @RequestParam(value = "enterprise",  required = false) String  enterprise,
            @RequestParam(value = "placerName",  required = false) String  placerName,
            @RequestParam(value = "placerId",    required = false) UUID    placerId,
            @RequestParam(value = "isBuyOrder",  required = false) Boolean isBuyOrder,
            @RequestParam(value = "isSellOrder", required = false) Boolean isSellOrder){
        Predicate<StockOrder> filter = o -> true;
        if(!StringUtils.isNullOrEmpty(enterprise))
            filter = filter.and(o -> o.getStocks().getEnterprise().equals(enterprise));
        if(!StringUtils.isNullOrEmpty(placerName))
            filter = filter.and(o -> o.getOrderPlacer().getName().equals(placerName));
        if(placerId != null)
            filter = filter.and(o -> o.getOrderPlacer().getId().equals(placerId));
        if(isBuyOrder != null)
            filter = filter.and(o -> o.isBuying() == isBuyOrder);
        if(isSellOrder != null)
            filter = filter.and(o -> o.isSelling() == isSellOrder);

        List<StockOrder> list = transactionRoomService.listOrders()
                .parallelStream()
                .filter(filter)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }





    @RequestMapping("/post/test")
    public ResponseEntity postTest(HttpServletRequest request) throws IOException {
        ServletInputStream inputStream = request.getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = mapper.readValue(inputStream, Map.class);
        return ResponseEntity.ok(null);
    }

    public static final ObjectNode JSON_HELLO_WORLD =
            new ObjectMapper().createObjectNode().put("test", "Hello World!");

    @RequestMapping("/get/test")
    public ResponseEntity test(){
        return ResponseEntity.ok(JSON_HELLO_WORLD);
    }
}
