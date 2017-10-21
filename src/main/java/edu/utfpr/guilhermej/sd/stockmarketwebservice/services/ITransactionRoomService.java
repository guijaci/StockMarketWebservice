package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.Stockholder;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.Stocks;

import java.rmi.RemoteException;

public interface ITransactionRoomService {
    StockOrder createBuyOrder(Stockholder placer, Stocks wantedStocks) throws RemoteException;
    StockOrder createSellOrder(Stockholder placer, Stocks sellingStocks) throws RemoteException;
}
