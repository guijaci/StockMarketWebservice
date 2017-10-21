package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.Stocks;

public interface IStockOrderMatcherService {
    Stocks matchOrders(StockOrder firstOrder, StockOrder secondOrder);
}
