package gpig.group2.comms.simulator;

import gpig.group2.comms.OutputHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by james on 23/05/2016.
 */
public class SimulatedDrone implements Runnable, DoesStatusUpdates {

    List<OutputHandler> outputHandlerList = new ArrayList<>();
    Set<OutputHandler> needRemoval = new HashSet<>();

    Logger log = LogManager.getLogger(SimulatedDrone.class);


    @Override
    public void bindOutputHandler(OutputHandler connectionHandler) {
        outputHandlerList.add(connectionHandler);
    }

    @Override
    public void removeOutputHandler(OutputHandler connectionHandler) {
        synchronized (this) {
            needRemoval.add(connectionHandler);
        }
    }

    public SimulatedDrone() {

    }

    @Override
    public void run() {
        while(true) {
            log.info("Sending heartbeats");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int i = 0;


            synchronized (this) {
                if(needRemoval.size()>0) {
                    for (OutputHandler h : needRemoval) {
                        outputHandlerList.remove(h);
                    }
                }
                needRemoval.clear();
            }

            for(OutputHandler oh : outputHandlerList) {

                log.info("Sending heartbeat to client "+ i++);
                oh.onOutput();
            }
        }
    }
}
