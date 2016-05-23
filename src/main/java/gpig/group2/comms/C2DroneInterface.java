package gpig.group2.comms;

import gpig.group2.comms.simulator.DoesStatusUpdates;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.StatusMessage;

/**
 * Created by james on 23/05/2016.
 */
public class C2DroneInterface extends ConnectionManager implements DroneInterface, DoesStatusUpdates {
    RequestWrapper rw = new RequestWrapper();

    @Override
    public void handleStatusMessage(StatusMessage sm) {

    }

    @Override
    public void handleResponseMessage(ResponseMessage rm) {

    }

    @Override
    public void handleRequestMessage(RequestMessage rq) {

    }

    public C2DroneInterface() {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {


                while(true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (OutputHandler h : outputHandlerList) {
                        h.onOutput(rw);
                    }
                }
            }
        });

        t.start();

    }


}
