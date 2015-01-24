package ai.context.trading;

import ai.context.learning.neural.NeuronCluster;
import ai.context.util.common.ScratchPad;
import ai.context.util.configuration.PropertiesHolder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class DukascopyConnection implements Runnable{

    private static final Logger LOGGER = LoggerFactory.getLogger(DukascopyConnection.class);

    private String jnlpDemoUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    private String jnlpLiveUrl = "https://www.dukascopy.com/client/live/jclient/jforex.jnlp";

    private String userName;
    private String password;

    private IClient client = ClientFactory.getDefaultInstance();

    public DukascopyConnection(final String userName, final String password) throws Exception {

        this.userName = userName;
        this.password = password;

        //get the instance of the IClient interface
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {
            private int lightReconnects = 3;

            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
                lightReconnects = 3;
            }

            @Override
            public void onDisconnect() {
                LOGGER.warn("Disconnected");
                if (lightReconnects > 0) {
                    client.reconnect();
                    --lightReconnects;
                }
            }
        });

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        connectClient();

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }
        NeuronCluster.getInstance().addTask(this);
        NeuronCluster.getInstance().getEmailSendingService().queueEmail(
                "algo@balgobin.london",
                "hans@balgobin.london",
                "Dukascopy Client Connected",
                "The algo is now connected to the broker API");

        //subscribe to the instruments
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        instruments.add(Instrument.GBPUSD);
        instruments.add(Instrument.USDJPY);
        instruments.add(Instrument.AUDUSD);
        instruments.add(Instrument.USDCHF);
        instruments.add(Instrument.XAUUSD);

        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);

        //workaround for LoadNumberOfCandlesAction for JForex-API versions > 2.6.64
        Thread.sleep(5000);
    }

    public IClient getClient() {
        return client;
    }

    private void connectClient() throws Exception {
        if(PropertiesHolder.isLiveAccount) {
            Semaphore captchaLock = new Semaphore(0);

            ScratchPad.memory.put(ScratchPad.CAPTCHA_IMAGE, client.getCaptchaImage(jnlpLiveUrl));
            ScratchPad.memory.put(ScratchPad.CAPTCHA_IMAGE_LOCK, captchaLock);

            System.out.println("Live PIN required. Please go to ~/liveTrading.html");
            captchaLock.acquire(1);
            String pin = (String) ScratchPad.memory.remove(ScratchPad.CAPTCHA_IMAGE_RESPONSE);
            System.out.println("Got PIN: " + pin);
            client.connect(jnlpLiveUrl, userName, password, pin);

            System.out.println("Connected to live broker account...");
            ScratchPad.memory.remove(ScratchPad.CAPTCHA_IMAGE);
            ScratchPad.memory.remove(ScratchPad.CAPTCHA_IMAGE_LOCK);
        } else {
            client.connect(jnlpDemoUrl, userName, password);
        }
    }

    @Override
    public void run() {
        while (true){
            try {
                Thread.sleep(10000);

                if(!client.isConnected()){
                    NeuronCluster.getInstance().getEmailSendingService().queueEmail(
                            "algo@balgobin.london",
                            "hans@balgobin.london",
                            "Dukascopy Client Disconnected",
                            "Please reconnect via the web page asap!");
                    connectClient();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
