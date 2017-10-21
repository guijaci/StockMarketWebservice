package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;
import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.Stocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Classe implementa as principais funções para gerenciamento de ações e lançamento de eventos relacionados à ações
 */
@Service
public class VolatileStockRepositoryService implements IStockRepositoryService {
    private final Map<String, List<StockOrder>> buyOrders;
    private final Map<String, List<StockOrder>> sellOrders;
    private final List<StockOrder> allOrders;

    private IStockOrderMatcherService matcher;

    /**
     * Constroi um gerenciador vazio
     */
    public VolatileStockRepositoryService(){
        buyOrders = new HashMap<>();
        sellOrders = new HashMap<>();
        allOrders = new ArrayList<>();
    }

    /**
     * Define o combinador de ordens usado pela classe para verificar se duas ordens são compatíveis
     * @param matcher combinador de ordens compatíveis
     * @return o próprio objeto para construção encadeada
     */
    @Autowired
    public IStockRepositoryService setMatcher(IStockOrderMatcherService matcher) {
        this.matcher = matcher;
        return this;
    }

    /**
     * Adiciona uma nova ordem de compra ou venda de ações
     * @param stockOrder ordem de compra ou venda de ações
     */
    @Override
    public void addOrder(StockOrder stockOrder) {
        if(stockOrder == null)
            return;
        Stocks s = stockOrder.getStocks();
        if(s == null)
            return;
        String e = s.getEnterprise();
        if(e == null)
            return;

        //Determina a lista de ordens que será utilizada para se verificar ordens compatíveis com a adicionada
        Map<String, List<StockOrder>> matchingMap = null;
        if(stockOrder.isBuying())
            matchingMap = sellOrders;
        else if(stockOrder.isSelling())
            matchingMap = buyOrders;
        List<StockOrder> matchingList = null;
        if(matchingMap != null)
            matchingList = matchingMap.get(e);

        Stocks t;   //Ação que será transacionada
        //Enquanto houver ordens compatíveis e enquanto houver ações para realizar transação,
        //procure por possível transação
        do {
            t = null;
            StockOrder matched = null;
            if (matchingList != null) {
                for (StockOrder iter : matchingList) {
                    //Tentativa de combinar duas ordens compatíveis
                    t = matcher.matchOrders(stockOrder, iter);
                    //Se for possivel realizar uma transação, então t nao é nulo
                    if (t != null) {
                        matched = iter;
                        break;
                    }
                }
            }
            if (t != null) {
                StockOrder prev = matched.clone();
                Stocks m = matched.getStocks();
                //Diminui de cada ação a quantidade transacionada
                s.setQuantity(s.getQuantity() - t.getQuantity());
                m.setQuantity(m.getQuantity() - t.getQuantity());
                //Se a quantidade da ação encontrada durante combinação chegar a 0,
                //remove ordem da lista e lança evento de remoção
                if (m.getQuantity() == 0) {
                    removeOrder(matched, matchingMap);
                    allOrders.remove(matched);
                }

                //Envia evento de transação realizada
                StockOrder bought = null;
                StockOrder sold = null;
                if(stockOrder.isBuying())
                    bought = stockOrder;
                else if(stockOrder.isSelling())
                    sold = stockOrder;
                if(matched.isBuying())
                    bought = matched;
                else if (matched.isSelling())
                    sold = matched;
            }
        }while (s.getQuantity() > 0 && t != null);

        //Caso sobre ações para adicionar depois de se tentar realizar as transações,
        //adicione ordem à lista e lance evento de ordem adicionada
        if(s.getQuantity() > 0){
            if (stockOrder.isBuying())
                addOrder(stockOrder, buyOrders);
            if (stockOrder.isSelling())
                addOrder(stockOrder, sellOrders);
            allOrders.add(stockOrder);
        }
    }

    /**
     * Simula flutuação de preços de ações
     * @param random fornecedor de valor aleatórios
     * @param price preço anterior à flutuação
     * @return valor de preço aleatorizado
     */
    private double randomFluctuation(Random random, Double price) {
        return Math.abs(price + random.nextGaussian()*0.2*price + random.nextGaussian());
    }

    /**
     * Adiciona ordem a um mapa de lista de ordens correspondente
     * @param stockOrder ordem para adição
     * @param ordersMap mapa de ordens para adição
     */
    private static void addOrder(StockOrder stockOrder, Map<String, List<StockOrder>> ordersMap) {
        String enterprise = stockOrder.getStocks().getEnterprise();
        List<StockOrder> orderList = null;
        if (!ordersMap.containsKey(enterprise)) {
            orderList = new ArrayList<>();
            ordersMap.put(enterprise, orderList);
        }
        else
            orderList = ordersMap.get(enterprise);
        orderList.add(stockOrder);
    }

    /**
     * Remove ordem de um mapa de lista de ordens correspondente
     * @param stockOrder ordem para remoção
     * @param ordersMap mapa de ordens para remoção
     */
    private static void removeOrder(StockOrder stockOrder, Map<String, List<StockOrder>> ordersMap) {
        String enterprise = stockOrder.getStocks().getEnterprise();
        List<StockOrder> matchingList = null;
        if(ordersMap.containsKey(enterprise)) {
            matchingList = ordersMap.get(enterprise);
            matchingList.remove(stockOrder);
            if (matchingList.isEmpty())
                ordersMap.remove(enterprise);
        }
    }
}
