package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

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
     * Cria uma ordem de transação de ações
     * @return ordem de compra de ação criada
     * @throws RemoteException em erros de conexão
     */
    @Override
    public void addOrder(StockOrder order) {
        if(order == null)
            throw new NullPointerException("Empty order");
        if(order.getOrderPlacer() == null)
            throw new NullPointerException("No order placer in order");
        if(order.getStocks() == null)
            throw new NullPointerException("No stocks in order");
        manager.addOrder(order);
    }

    @Override
    public List<StockOrder> listOrders() {
        return manager.listOrders();
    }
}
