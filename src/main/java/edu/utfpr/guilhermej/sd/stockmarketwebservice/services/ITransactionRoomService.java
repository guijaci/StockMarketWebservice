package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;

import java.util.List;

public interface ITransactionRoomService {
    void addOrder(StockOrder order);
    List<StockOrder> listOrders();
}
