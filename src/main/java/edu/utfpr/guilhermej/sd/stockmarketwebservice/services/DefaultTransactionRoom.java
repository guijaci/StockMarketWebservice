package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Null;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.function.Predicate;

/**
 * Servente da sala de transações, trata chamadas à interface remotamente
 */
@Service
public class DefaultTransactionRoom extends UnicastRemoteObject implements ITransactionRoomService {
    private IStockRepositoryService repositoryService;

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
        this.repositoryService = manager;
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
        repositoryService.addOrder(order);
    }

    @Override
    public void addSubscriberFilter(Stockholder subscriber, Predicate<StockEvent> filter) {
        if(subscriber == null)
            throw new NullPointerException("Empty subscriber");
        if(filter == null)
            throw new NullPointerException("Empty filter");
        repositoryService.addSubscriberFilter(subscriber, filter);
    }

    @Override
    public List<StockEvent> getEvents(Stockholder subscriber) {
        if(subscriber == null)
            throw new NullPointerException("Empty subscriber");
        return repositoryService.getEvents(subscriber);
    }

    @Override
    public List<StockOrder> listOrders() {
        return repositoryService.listOrders();
    }
}
