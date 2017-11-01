package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockEvent;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.Stockholder;

import java.util.List;
import java.util.function.Predicate;

public interface IStockRepositoryService {
    void addOrder(StockOrder stockOrder);
    void addSubscriberFilter(Stockholder subscriber, Predicate<StockEvent> filter);
    List<StockEvent> getEvents(Stockholder subscriber);
    List<StockOrder> listOrders();
}
