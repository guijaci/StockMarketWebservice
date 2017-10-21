package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;

public interface IStockRepositoryService {
    void addOrder(StockOrder stockOrder);
}
