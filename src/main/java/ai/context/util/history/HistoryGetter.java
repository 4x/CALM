package ai.context.util.history;

import ai.context.trading.DukascopyConnection;
import ai.context.util.configuration.PropertiesHolder;
import com.dukascopy.api.system.IClient;

public class HistoryGetter {

    public static void main(String[] args){
        try {
            IClient client = new DukascopyConnection(PropertiesHolder.dukascopyLogin, PropertiesHolder.dukascopyPass).getClient();
            HistoryBarsSynch historyBarsSynch = new HistoryBarsSynch(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
