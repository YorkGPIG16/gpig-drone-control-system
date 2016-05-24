package gpig.group2.dcs;

import gpig.group2.dcs.wrapper.RequestWrapper;
import gpig.group2.maps.geographic.Point;
import gpig.group2.maps.geographic.position.BoundingBox;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.request.task.AerialSurveyTask;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.status.DroneStatusMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Date;

/**
 * Created by james on 23/05/2016.
 */
public class C2DroneInterface implements DroneInterface {
    RequestWrapper rw = new RequestWrapper();

    Logger log = LogManager.getLogger(C2DroneInterface.class);

    @Override
    public void handleStatusMessage(DroneStatusMessage sm) {

    }

    @Override
    public void handleResponseMessage(ResponseMessage rm) {

    }

    @Override
    public void handleRequestMessage(RequestMessage rq) {
        throw new NotImplementedException();
    }

    @Override
    public void registerOutputHandler(OutputHandler handler) {
        //Thread to send ALL requests to ALL drones
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        if (rw.numTasks() > 0) {
                            log.info("sending tasks to client");
                            handler.onOutput(rw);
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
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        AerialSurveyTask ast = new AerialSurveyTask();
                        ast.setLocation(new BoundingBox(new Point(100,200),new Point(300,400)));
                        ast.setPriority(1000);
                        ast.setTimestamp(new Date());
                        //Check maps server

                        rw.addTask(ast);
                        log.info("Adding an AerialSurveyTask");

                    }
                }
            });

            t.start();
        }
    }

    public C2DroneInterface() {


    }


}
