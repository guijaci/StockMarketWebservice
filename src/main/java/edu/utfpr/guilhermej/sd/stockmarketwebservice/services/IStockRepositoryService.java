package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;

import java.util.List;

public interface IStockRepositoryService {
    void addOrder(StockOrder stockOrder);
    List<StockOrder> listOrders();
}
