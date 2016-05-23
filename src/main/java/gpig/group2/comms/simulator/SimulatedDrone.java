package gpig.group2.comms.simulator;

import gpig.group2.comms.CommonObject;
import gpig.group2.comms.ConnectionHandler;
import gpig.group2.comms.ConnectionManager;
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
public class SimulatedDrone extends ConnectionManager implements Runnable, DoesStatusUpdates {

    Logger log = LogManager.getLogger(SimulatedDrone.class);

    CommonObject status;
    CommonObject response;



    public SimulatedDrone() {

        status = new StatusWrapper();
        response = new ResponseWrapper();


    }

    @Override
    public void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    clearDeadConnections();

                    int i = 0;
                    for(OutputHandler oh : outputHandlerList) {
                        log.info("Sending status message to client "+ i++);
                        oh.onOutput(status);
                    }
                }
            }
        }).start();


        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    clearDeadConnections();

                    int i = 0;
                    for(OutputHandler oh : outputHandlerList) {
                        log.info("Sending response to client "+ i++);
                        oh.onOutput(response);
                    }
                }
            }
        }).start();

    }
}
