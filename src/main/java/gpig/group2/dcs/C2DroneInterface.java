package gpig.group2.dcs;

import gpig.group2.dcs.c2integration.C2Integration;
import gpig.group2.dcs.wrapper.RequestWrapper;
import gpig.group2.dcs.wrapper.ResponseWrapper;
import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.maps.geographic.Point;
import gpig.group2.maps.geographic.position.BoundingBox;
import gpig.group2.model.sensor.StrandedPerson;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.request.Task;
import gpig.group2.models.drone.request.task.AerialSurveyTask;
import gpig.group2.models.drone.response.ResponseData;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.response.responsedatatype.BuildingOccupancyResponse;
import gpig.group2.models.drone.response.responsedatatype.Completed;
import gpig.group2.models.drone.response.responsedatatype.ManDownResponse;
import gpig.group2.models.drone.status.DroneStatusMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by james on 23/05/2016.
 */
public class C2DroneInterface implements DroneInterface {
    final RequestWrapper rw = new RequestWrapper();


    C2Integration c2;
    Logger log = LogManager.getLogger(C2DroneInterface.class);

    @Override
    public void handleStatusMessage(int id, DroneStatusMessage sm) {
        StatusWrapper sw = new StatusWrapper();
        sm.setId(id);

        sw.setStatus(sm);

        if(c2!=null) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    c2.sendDroneStatus(sw);
                }
            });
            t.start();
        }
    }

    @Override
    public void handleResponseMessage(int id, ResponseMessage rm) {
        if(c2!=null && rm!=null && rm.getResponseX() != null) {

            for(ResponseData rd : rm.getResponseX()){


                if(rd instanceof Completed) {

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            c2.sendCompletedDeploymentArea(rd);
                        }
                    });
                    t.start();

                } else {

                    if(rd instanceof ManDownResponse) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                c2.sendPOI(rd);
                            }
                        });
                        t.start();
                    } else if (rd instanceof BuildingOccupancyResponse) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                c2.sendBuildingOccupancy(rd);
                            }
                        });
                        t.start();
                    }

                }
            }



        }
    }

    @Override
    public void handleRequestMessage(int id, RequestMessage rq) {
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


                        synchronized (rw) {
                            if (rw.numTasks() > 0) {
                                log.info("sending tasks to client");
                                handler.onOutput(rw);
                                rw.clearTasks();
                            }
                        }

                    }
                }
            });

            t.start();
        }




    }

    public C2DroneInterface(C2Integration c2) {
        this.c2 = c2;


        if(c2 != null)
        //Thread to read map and generate requests
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


                        RequestMessage rm = c2.getRequests();
                        if(rm!=null) {

                            List<Task> tasks = c2.getRequests().getTasksX();
                            for (Task t : tasks) {
                                synchronized (rw) {
                                    rw.addTask(t);
                                }
                            }
                        }

                    }
                }
            });

            t.start();
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        RequestMessage rm = new RequestMessage();
                        rm.setTasks(new ArrayList<>());
                        rm.setTimestamp(new Date());

                        AerialSurveyTask ast = new AerialSurveyTask();
                        ast.setLocation(new BoundingBox(new Point(100.0,200.0),new Point(200.0,30.0)));
                        ast.setPriority(100);
                        ast.setId(5);


                        rm.getTasksX().add(ast);


                        log.info("Adding an AerialSurveyTask");

                        for (Task t : rm.getTasksX()) {
                            synchronized (rw) {
                                rw.addTask(t);
                            }
                        }


                    }
                }
            });
            t.start();
        }
    }


}
