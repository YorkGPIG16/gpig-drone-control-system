package gpig.group2.dcs;

import gpig.group2.dcs.c2integration.C2Integration;
import gpig.group2.dcs.wrapper.RequestWrapper;
import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.maps.geographic.Point;
import gpig.group2.maps.geographic.position.BoundingBox;
import gpig.group2.maps.platform.Drone;
import gpig.group2.models.alerts.Alert;
import gpig.group2.models.alerts.Priority;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseData;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.response.responsedatatype.Aborted;
import gpig.group2.models.drone.response.responsedatatype.BuildingOccupancyResponse;
import gpig.group2.models.drone.response.responsedatatype.Completed;
import gpig.group2.models.drone.response.responsedatatype.ManDownResponse;
import gpig.group2.models.drone.status.DroneStatusCollection;
import gpig.group2.models.drone.status.DroneStatusMessage;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gpig.group2.models.drone.request.Task;
import gpig.group2.models.drone.request.task.AerialSurveyTask;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.util.*;

/**
 * Created by Benjy on 26/05/2016.
 */
public class TaskPool implements DroneInterface {

    public class TaskComparator implements Comparator<Task> {
        @Override
        public int compare(Task t1, Task t2) {


            return Integer.compare(t1.getPriorityX(), t2.getPriorityX());
        }
    }

    final RequestWrapper rw = new RequestWrapper();
    C2Integration c2;
    Logger log = LogManager.getLogger(TaskPool.class);
    ArrayList<Task> tasks = new ArrayList<Task>();
    ArrayList<DroneConnectionHandler> idleWorkers = new ArrayList<DroneConnectionHandler>();
    ArrayList<DroneConnectionHandler> workers = new ArrayList<DroneConnectionHandler>();
    ArrayList<Task> assignedTasks = new ArrayList<Task>();
    ArrayList<Task> completedTasks = new ArrayList<Task>();

    HashMap<Integer, Set<FAILURE_MODE>> alerts = new HashMap<>();


    private enum FAILURE_MODE {
        BATTERY_LOW,
        BATTERY_CRITICAL,
        MOTOR,
        ENGINE,
    }

    public void handleStatusMessage(int id, DroneStatusMessage sm) {
        StatusWrapper sw = new StatusWrapper();
        sm.setId(id);

        sw.setStatus(sm);


        if (c2 != null) {
            if (alerts.get(id) == null) {
                alerts.put(id, new HashSet<>());
            }


            if (sm.getFailuresX() != null) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        if (sm.getFailuresX().contains("MOTOR") && !alerts.get(id).contains(FAILURE_MODE.MOTOR)) {

                            Alert a = new Alert();
                            a.priority = Priority.PRIORITY_MEDIUM;
                            a.message = "Drone Motor FAIL";

                            alerts.get(id).add(FAILURE_MODE.MOTOR);
                            c2.sendAlert(a);
                        }


                        if (sm.getFailuresX().contains("ENGINE") && !alerts.get(id).contains(FAILURE_MODE.ENGINE)) {
                            Alert a = new Alert();
                            a.priority = Priority.PRIORITY_HIGH;
                            a.message = "Drone Engine FAIL";

                            alerts.get(id).add(FAILURE_MODE.ENGINE);
                            c2.sendAlert(a);
                        }
                    }
                });
                t.start();
            }


            if (sm.getBatteryX() < 10 && !alerts.get(id).contains(FAILURE_MODE.BATTERY_LOW)) {
                Alert a = new Alert();
                a.priority = Priority.PRIORITY_MEDIUM;
                a.message = "Drone Low Battery";

                alerts.get(id).add(FAILURE_MODE.BATTERY_LOW);
                c2.sendAlert(a);

            }

            if (sm.getBatteryX() < 5 && !alerts.get(id).contains(FAILURE_MODE.BATTERY_CRITICAL)) {
                Alert a = new Alert();
                a.priority = Priority.PRIORITY_HIGH;
                a.message = "Drone Critical Battery";

                alerts.get(id).add(FAILURE_MODE.BATTERY_CRITICAL);
                c2.sendAlert(a);
            }


        }


        if (c2 != null) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    c2.sendDroneStatus(sw);
                }
            });
            t.start();
        }
    }

    public void handleResponseMessage(int id, ResponseMessage rm) {
        if (rm != null && rm.getResponseX() != null) {

            DroneConnectionHandler completedWorker = null;
            for (DroneConnectionHandler worker : workers) {
                if (worker.getDroneNumber() == id) {
                    completedWorker = worker;
                    break;
                }
            }

            for (ResponseData rd : rm.getResponseX()) {

                if (rd instanceof Completed) {

                    int taskId = rd.getTaskIdX();
                    Task completedTask = null;
                    for (Task t : assignedTasks) {
                        if (t.getIdX() == taskId) {
                            completedTask = t;
                            break;
                        }
                    }

                    assignedTasks.remove(completedTask);
                    completedTasks.add(completedTask);

                    if (!tasks.isEmpty()) {
                        Task t = tasks.get(0);
                        assignedTasks.add(t);
                        tasks.remove(0);

                        RequestWrapper rw = new RequestWrapper();
                        rw.addTask(t);
                        completedWorker.onOutput(rw);
                    } else {
                        workers.remove(completedWorker);
                        idleWorkers.add((DroneConnectionHandler) completedWorker);
                    }

                    if (c2 != null) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                c2.sendCompletedDeploymentArea(rd);
                            }
                        });
                        t.start();
                    }
                } else if (rd instanceof Aborted) {
                    int taskId = rd.getTaskIdX();
                    Task abortedTask = null;
                    for (Task t : assignedTasks) {
                        if (t.getIdX() == taskId) {
                            abortedTask = t;
                            break;
                        }
                    }

                    if (idleWorkers.isEmpty())
                    {
                        assignedTasks.remove(abortedTask);
                        tasks.add(abortedTask);
                    }
                    else
                    {
                        DroneConnectionHandler worker = idleWorkers.get(0);
                        workers.add(worker);
                        idleWorkers.remove(0);

                        RequestWrapper rw = new RequestWrapper();
                        rw.addTask(abortedTask);
                        worker.onOutput(rw);
                    }
                } else {
                    if (c2 != null) {
                        if (rd instanceof ManDownResponse) {
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
    }

    public void handleRequestMessage(int id, RequestMessage rq) {
        throw new NotImplementedException();
    }

    public void registerOutputHandler(OutputHandler handler) {

        if (!tasks.isEmpty()) {
            workers.add((DroneConnectionHandler) handler);
            Task t = tasks.get(0);
            assignedTasks.add(t);
            tasks.remove(0);

            RequestWrapper rw = new RequestWrapper();
            rw.addTask(t);
            handler.onOutput(rw);
        } else {
            idleWorkers.add((DroneConnectionHandler) handler);
        }
    }

    public TaskPool(C2Integration c2) {
        this.c2 = c2;

        if (c2 != null)
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

                        ArrayList<DroneConnectionHandler> deadHandlers = new ArrayList<DroneConnectionHandler>();

                        // Check all the drones are alive.
                        for (DroneConnectionHandler handler : workers)
                        {
                            try
                            {
                                handler.serviceSocket.getOutputStream().write(' ');
                            }
                            catch (IOException ex)
                            {
                                log.warn("Killed worker which disconnected");
                                // Drone has died, remove it from the workers list.
                                deadHandlers.add(handler);
                            }
                        }
                        for (DroneConnectionHandler handler : idleWorkers)
                        {
                            try
                            {
                                handler.serviceSocket.getOutputStream().write(' ');
                            }
                            catch (IOException ex)
                            {
                                log.warn("Killed idle drone which disconnected");
                                // Drone has died, remove it from the workers list.
                                deadHandlers.add(handler);
                            }
                        }

                        for (DroneConnectionHandler handler : deadHandlers)
                        {

                            idleWorkers.remove(handler);
                            workers.remove(handler);



                        }

                        RequestMessage rm = c2.getRequests();
                        if (rm != null && c2.getRequests().getTasksX() != null) {

                            List<Task> newTasks = c2.getRequests().getTasksX();


                            for (Task t : newTasks) {
                                boolean inTasks = tasks.stream().filter(o -> o.getIdX().equals(t.getIdX())).findFirst().isPresent();
                                boolean inCompleted = completedTasks.stream().filter(o -> o.getIdX().equals(t.getIdX())).findFirst().isPresent();
                                boolean inAssigned = assignedTasks.stream().filter(o -> o.getIdX().equals(t.getIdX())).findFirst().isPresent();


                                if (t.getPriorityX() == null) {
                                    t.setPriority(0);
                                }


                                if (!inTasks && !inCompleted && !inAssigned) {
                                    boolean added = false;
                                    for(DroneConnectionHandler worker : idleWorkers) {
                                        if(alerts.get(worker.getDroneNumber()) == null || alerts.get(worker.getDroneNumber()).size()==0) {

                                            workers.add(worker);
                                            idleWorkers.remove(0);
                                            assignedTasks.add(t);
                                            RequestWrapper rw = new RequestWrapper();
                                            rw.addTask(t);
                                            worker.onOutput(rw);
                                            added = true;
                                            break;
                                        }


                                    }


                                    if (!added) {
                                        tasks.add(t);
                                        Collections.sort(tasks, new TaskComparator());
                                    }

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
                    int count = 1;

                    while (true) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        ArrayList<DroneConnectionHandler> deadHandlers = new ArrayList<DroneConnectionHandler>();

                        // Check all the drones are alive.
                        for (DroneConnectionHandler handler : workers)
                        {
                            try
                            {
                                handler.serviceSocket.getOutputStream().write(' ');
                            }
                            catch (IOException ex)
                            {
                                // Drone has died, remove it from the workers list.
                                log.warn("Killed drone which disconnected");
                                deadHandlers.add(handler);
                            }
                        }
                        for (DroneConnectionHandler handler : idleWorkers)
                        {
                            try
                            {
                                handler.serviceSocket.getOutputStream().write(' ');
                            }
                            catch (IOException ex)
                            {
                                // Drone has died, remove it from the workers list.
                                log.warn("Killed drone which disconnected");
                                deadHandlers.add(handler);
                            }
                        }

                        for (DroneConnectionHandler handler : deadHandlers)
                        {
                            idleWorkers.remove(handler);
                            workers.remove(handler);
                        }

                        RequestMessage rm = new RequestMessage();
                        rm.setTasks(new ArrayList<>());
                        rm.setTimestamp(new Date());

                        AerialSurveyTask ast = new AerialSurveyTask();
                        ast.setLocation(new BoundingBox(new Point(53.9550257f, -1.0700746f), new Point(53.9544843f, -1.069574f)));
                        ast.setPriority(100);
                        ast.setId(count++);

                        rm.getTasksX().add(ast);

                        log.info("Adding an AerialSurveyTask");

                        List<Task> newTasks = new ArrayList<Task>();
                        newTasks.add(ast);
                        for (Task t : newTasks) {
                            boolean inTasks = TaskWithIDExistsInList(t.getIdX(),tasks); //.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();
                            boolean inCompleted = TaskWithIDExistsInList(t.getIdX(),completedTasks); //completedTasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();
                            boolean inAssigned = TaskWithIDExistsInList(t.getIdX(),assignedTasks);// assignedTasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();

                            if (!inTasks && !inCompleted && !inAssigned) {
                                if (idleWorkers.isEmpty()) {
                                    log.info("No idle workers, sent new task.");
                                    tasks.add(t);
                                    Collections.sort(tasks, new TaskComparator());
                                } else {
                                    log.info("Idle worker, assigned task.");
                                    DroneConnectionHandler worker = idleWorkers.get(0);
                                    workers.add(worker);
                                    idleWorkers.remove(0);
                                    assignedTasks.add(t);
                                    RequestWrapper rw = new RequestWrapper();
                                    rw.addTask(t);
                                    worker.onOutput(rw);
                                }
                            }
                        }


                    }
                }
            });
            t.start();
        }


    }


    public static boolean TaskWithIDExistsInList(Integer id, ArrayList<Task> tasks) {
        for(Task t: tasks) {
            if ( t.getIdX() != null ) {
                if (t.getIdX().equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

}
