    package edu.utfpr.guilhermej.sd.stockmarketwebservice.services;

    import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockEvent;
    import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.StockOrder;
    import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.Stockholder;
    import edu.utfpr.guilhermej.sd.stockmarketwebservice.model.Stocks;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;

    import java.util.*;
    import java.util.concurrent.BlockingQueue;
    import java.util.concurrent.LinkedBlockingQueue;
    import java.util.function.Predicate;

    /**
     * Classe implementa as principais funções para gerenciamento de ações e lançamento de eventos relacionados à ações
     */
    @Service
    public class VolatileStockRepositoryService implements IStockRepositoryService {
        private final Map<String, List<StockOrder>> buyOrders;
        private final Map<String, List<StockOrder>> sellOrders;
        private final List<StockOrder> allOrders;

        private final Map<Stockholder, Predicate<StockEvent>> subscriptionFilterMap;
        private final Map<Stockholder, BlockingQueue<StockEvent>> subscriptionEventsMap;

        private IStockOrderMatcherService matcher;

        /**
         * Constroi um gerenciador vazio
         */
        public VolatileStockRepositoryService(){
            buyOrders = new HashMap<>();
            sellOrders = new HashMap<>();
            allOrders = new ArrayList<>();

            subscriptionFilterMap = new HashMap<>();
            subscriptionEventsMap = new HashMap<>();
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
            Stockholder p = stockOrder.getOrderPlacer();
            if(p == null)
                return;

            initializeSubscribersFilter(p);
            initializeSubscribersEventQueue(p);

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
                        launchRemovedStockOrderEvent(matched);
                    }
                    else
                        launchUpdatedStockOrderEvent(matched, prev);

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
                    launchTradedStockOrderEvent(bought, sold, t);
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
                launchAddedStockOrderEvent(stockOrder);
            }
        }

        @Override
        public void addSubscriberFilter(Stockholder subscriber, Predicate<StockEvent> filter) {
            initializeSubscribersFilter(subscriber);
            subscriptionFilterMap.put(subscriber, subscriptionFilterMap.get(subscriber).or(filter));
        }

        @Override
        public List<StockEvent> getEvents(Stockholder subscriber) {
            List<StockEvent> events = new LinkedList<>();
            subscriptionEventsMap.get(subscriber).drainTo(events);
            return events;
        }

        @Override
        public List<StockOrder> listOrders() {
            return new ArrayList<>(allOrders);
        }

        /**
         * Lança evento de ordem de ação adicionada
         * @param stockOrder ordem adicionada
         */
        private void launchAddedStockOrderEvent(StockOrder stockOrder) {
            initializeSubscribersEventQueue(stockOrder.getOrderPlacer());
            initializeSubscribersFilter(stockOrder.getOrderPlacer());

            StockEvent event = StockEvent.createAddedStockOrderEvent(stockOrder, this);
            launchEvent(event);
        }

        /**
         * Lança evento de ordem de ação atualizada
         * @param newOrder ordem após alteração
         * @param prevOrder ordem antes da alteração
         */
        private void launchUpdatedStockOrderEvent(StockOrder newOrder, StockOrder prevOrder) {
            initializeSubscribersEventQueue(newOrder.getOrderPlacer());
            initializeSubscribersEventQueue(prevOrder.getOrderPlacer());

            initializeSubscribersFilter(prevOrder.getOrderPlacer());
            initializeSubscribersFilter(newOrder.getOrderPlacer());

            StockEvent event = StockEvent.createUpdatedStockOrderEvent(prevOrder, newOrder, this);
            launchEvent(event);
        }

        /**
         * Lança evento de ações transacionada
         * @param bought ordem de compra relacionada à transação
         * @param sold ordem de venda relacionada à transação
         * @param tradedStock ações transacionadas entre os insersores das ordens
         */
        private void launchTradedStockOrderEvent(StockOrder bought, StockOrder sold, Stocks tradedStock) {
            initializeSubscribersEventQueue(bought.getOrderPlacer());
            initializeSubscribersEventQueue(sold.getOrderPlacer());

            initializeSubscribersFilter(bought.getOrderPlacer());
            initializeSubscribersFilter(sold.getOrderPlacer());

            StockEvent event = StockEvent.createTradedStockOrderEvent(bought, sold, tradedStock, this);
            launchEvent(event);
        }

        /**
         * Lança evento de ordem de ação removida
         * @param stockOrder ordem removida
         */
        private void launchRemovedStockOrderEvent(StockOrder stockOrder) {
            initializeSubscribersEventQueue(stockOrder.getOrderPlacer());
            initializeSubscribersFilter(stockOrder.getOrderPlacer());

            StockEvent event = StockEvent.createRemovedStockOrderEvent(stockOrder, this);
            launchEvent(event);
        }

        private void launchEvent(StockEvent event) {
            synchronized (subscriptionFilterMap) {
                subscriptionFilterMap.entrySet().parallelStream().forEach(p -> {
                    if (p.getValue().test(event))
                        subscriptionEventsMap.get(p.getKey()).add(event);
                });
            }
        }

        private void initializeSubscribersFilter(Stockholder stockholder) {
            synchronized (subscriptionFilterMap) {
                if (!subscriptionFilterMap.containsKey(stockholder))
                    subscriptionFilterMap.put(stockholder, event -> event.isParticipant(stockholder));
            }
        }

        private void initializeSubscribersEventQueue(Stockholder stockholder) {
            synchronized (subscriptionEventsMap) {
                if (!subscriptionEventsMap.containsKey(stockholder))
                    subscriptionEventsMap.put(stockholder, new LinkedBlockingQueue<>());
            }
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
