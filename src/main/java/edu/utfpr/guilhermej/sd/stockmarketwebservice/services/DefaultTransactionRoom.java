package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Servente da sala de transações, trata chamadas à interface remotamente
 */
@Service
public class DefaultTransactionRoom extends UnicastRemoteObject implements ITransactionRoomService {
    private IStockRepositoryService manager;

    /**
     * Constroi uma nova sala de transação
     * @throws RemoteException em erros de conexão
     */
    public DefaultTransactionRoom() throws RemoteException{

    }

    /**
     * Define o gerente de ações da sala de transações
     * @param manager gerente de transações que será utilizado durante as chamadas
     * @return o próprio objeto para construção encadeada
     */
    @Autowired
    public ITransactionRoomService setManager(IStockRepositoryService manager) {
        this.manager = manager;
        return this;
    }

    /**
     * Cria uma ordem de compra de ações
     * @param placer acionista requerente da ordem
     * @param wantedStocks ações desejadas na compra
     * @return ordem de compra de ação criada
     * @throws RemoteException em erros de conexão
     */
    @Override
    public StockOrder createBuyOrder(Stockholder placer, Stocks wantedStocks) throws RemoteException{
        if(placer.getName() == null || placer.getName().isEmpty()
                || placer.getId() == null || placer.getVersion() == null)
            return null;
        if(wantedStocks.getEnterprise() == null || wantedStocks.getEnterprise().isEmpty() ||
                wantedStocks.getPrice() == null || wantedStocks.getQuantity() == null ||
                wantedStocks.getVersion() == null)
            return null;
        StockOrder stockOrder = new BuyStockOrder()
                .setStocks(wantedStocks)
                .setOrderPlacer(placer);
        manager.addOrder(stockOrder);
        return stockOrder;
    }

    /**
     * Cria uma ordem de venda de ações
     * @param placer acionista requerente da ordem
     * @param sellingStocks ações sendo vendidas
     * @return ordem de venda de ação criada
     * @throws RemoteException em erros de conexão
     */
    @Override
    public StockOrder createSellOrder(Stockholder placer, Stocks sellingStocks) throws RemoteException {
        if(placer.getName() == null || placer.getName().isEmpty()
                || placer.getId() == null || placer.getVersion() == null)
            return null;
        if(sellingStocks.getEnterprise() == null || sellingStocks.getEnterprise().isEmpty() ||
                sellingStocks.getPrice() == null || sellingStocks.getQuantity() == null ||
                sellingStocks.getVersion() == null)
            return null;
        StockOrder stockOrder = new SellStockOrder()
                .setStocks(sellingStocks)
                .setOrderPlacer(placer);
        manager.addOrder(stockOrder);
        return stockOrder;
    }
}
