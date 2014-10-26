package ai.context.util.trading.version_1.trade.filter;

import ai.context.util.trading.version_1.MarketMakerPosition;

public interface TradeFilter {
    public boolean pass(MarketMakerPosition advice);
}
