package gpig.group2.comms;

import gpig.group2.comms.simulator.DoesStatusUpdates;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.StatusMessage;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
        throw new NotImplementedException();
    }

    public C2DroneInterface() {


        //Thread to send ALL requests to ALL drones
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        if (rw.numTasks() > 0) {
                            for (OutputHandler h : outputHandlerList) {
                                h.onOutput(rw);
                            }

                            rw.clearTasks();
                        }

                    }
                }
            });

            t.start();
        }



        //Thread to read map and generate requests
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //Check maps server

                    }
                }
            });

            t.start();
        }


    }


}
