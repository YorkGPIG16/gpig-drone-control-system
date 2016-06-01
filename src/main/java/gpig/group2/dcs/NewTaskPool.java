package gpig.group2.dcs;

import gpig.group2.dcs.c2integration.C2Integration;
import gpig.group2.dcs.wrapper.RequestWrapper;
import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.models.alerts.Alert;
import gpig.group2.models.alerts.Priority;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.request.Task;
import gpig.group2.models.drone.request.task.GoToLocationTask;
import gpig.group2.models.drone.response.ResponseData;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.response.responsedatatype.Aborted;
import gpig.group2.models.drone.response.responsedatatype.BuildingOccupancyResponse;
import gpig.group2.models.drone.response.responsedatatype.Completed;
import gpig.group2.models.drone.response.responsedatatype.ManDownResponse;
import gpig.group2.models.drone.status.DroneStatusMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Benjy on 26/05/2016.
 */
public class NewTaskPool implements DroneInterface {
    public class TaskPriorityComparator implements Comparator<Task> {
        @Override
        public int compare(Task t1, Task t2) {
            return Integer.compare(t2.getPriorityX(),t1.getPriorityX());
        }
    }

    public class TaskIDComparator implements Comparator<Task> {
        @Override
        public int compare(Task t1, Task t2) {

            return Integer.compare(t1.getIdX(), t2.getIdX());
        }
    }


    final RequestWrapper rw = new RequestWrapper();
    C2Integration c2;
    Logger log = LogManager.getLogger(NewTaskPool.class);


    HashMap<Integer, Set<FAILURE_MODE>> alerts = new HashMap<>();

    ConcurrentHashMap<Integer, Task> droneAllocations = new ConcurrentHashMap<>();
    final List<DroneConnectionHandler> registeredDrones = new ArrayList<>();
    final PriorityQueue<Task> unallocatedTasks = new PriorityQueue<>(new TaskPriorityComparator());



    final Set<Task> allocatedTasks = new HashSet<>();
    final Set<Task> completedTasks = new HashSet<>();

    private enum FAILURE_MODE {
        BATTERY_LOW,
        BATTERY_CRITICAL,
        MOTOR,
        ENGINE,
        ABORTED
    }

    public void handleStatusMessage(int id, DroneStatusMessage sm) {
        log.info("Handling status message");

        StatusWrapper sw = new StatusWrapper();
        sm.setId(id);

        sw.setStatus(sm);


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





        c2.sendDroneStatus(sw);

    }

    public void handleResponseMessage(int id, ResponseMessage rm) {
        log.info("Handling response message");

        if(rm.getResponseX() == null) {
            log.error("Received null response from drone");
        } else {
            for (ResponseData rd : rm.getResponseX()) {

                if (rd instanceof Completed) {
                    log.info("Response message is COMPLETED type");

                    Task t = GetTaskById(allocatedTasks,rd.getTaskIdX());
                    if(t==null) {
                        t = GetTaskById(unallocatedTasks, rd.getTaskIdX());
                    }

                    if(t!=null) {
                        markTaskAsCompleted(t);
                    }

                    droneAllocations.remove(id);
                    c2.sendCompletedDeploymentArea(rd);

                } else if (rd instanceof Aborted) {
                    log.info("Response message is ABORTED type");

                    Task t = GetTaskById(allocatedTasks,rd.getTaskIdX());
                    if(t!=null) {
                        releaseTaskIntoPool(t);
                    }

                    alerts.get(id).add(FAILURE_MODE.ABORTED);

                    droneAllocations.remove(id);

                } else if (rd instanceof ManDownResponse) {
                    log.info("Response message is MANDOWN type");

                    if(rd.getTaskIdX()==null) {
                        if(droneAllocations.get(id) != null) {
                            rd.setTaskId(droneAllocations.get(id).getIdX());
                        }
                    }
                    c2.sendPOI(rd);

                } else if (rd instanceof BuildingOccupancyResponse) {
                    log.info("Response message is BUILDING type");

                    Alert a = new Alert();
                    a.message = ("Occupied building at (lat: "+rd.getOriginX().getLatitudeX() + ", long: "+rd.getOriginX().getLongitudeX()+").");
                    a.priority = Priority.PRIORITY_LOW;

                    c2.sendAlert(a);
                    c2.sendBuildingOccupancy(rd);

                }

            }
        }

    }

    private Task GetTaskById(Collection<Task> tasks, Integer id) {
        for(Task t : tasks) {
            if(t.getIdX().equals(id)) {
                return t;
            }
        }
        return null;
    }


    public void handleRequestMessage(int id, RequestMessage rq) {
        throw new NotImplementedException();
    }

    public void registerOutputHandler(OutputHandler handler) {
        log.info("Receiving message from drone");

        DroneConnectionHandler h = (DroneConnectionHandler)handler;

        if (alerts.get(h.getDroneNumber()) == null) {
            alerts.put(h.getDroneNumber(), new HashSet<>());
        }

        synchronized (registeredDrones) {
            registeredDrones.add(h);
        }
    }

    public NewTaskPool(C2Integration c2) {
        this.c2 = c2;


        if(c2!=null) {

            //Thread to download requests from C2 and add them to the unallocated list
            Thread pollC2Thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        log.info("Downloading requests from C2");
                        //Get request from C2
                        RequestMessage rm = c2.getRequests();

                        log.info("Got requests from C2");

                        if (rm == null) {
                            log.error("Request Message is null");
                        } else if (rm.getTasksX() == null || rm.getTasksX().size() == 0) {
                            log.warn("Request Message is empty");
                        } else {
                            for (Task t : rm.getTasksX()) {

                                if (!taskRegistered(t)) {
                                    addTask(t);
                                }


                            }
                        }

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            pollC2Thread.start();



            //Thread to ping each drone to check for disconnections
            Thread queryDroneThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        log.info("Pinging all drones");

                        ArrayList<DroneConnectionHandler> deadHandlers = new ArrayList<DroneConnectionHandler>();
                        synchronized (registeredDrones) {
                            for (DroneConnectionHandler handler : registeredDrones) {
                                try {
                                    handler.serviceSocket.getOutputStream().write(' ');
                                } catch (IOException ex) {
                                    log.warn("Found worker which disconnected");
                                    deadHandlers.add(handler);
                                }
                            }
                        }

                        for (DroneConnectionHandler dch : deadHandlers) {

                            Alert a = new Alert();
                            a.message = ("Drone COMMS Fail. Releasing tasks.");
                            a.priority = Priority.PRIORITY_LOW;

                            synchronized (registeredDrones) {
                                registeredDrones.remove(dch);
                            }

                            if (droneAllocations.get(dch.getDroneNumber()) != null) {
                                log.warn("Deregistering tasks of dead worker");

                                releaseTaskIntoPool(droneAllocations.get(dch.getDroneNumber()));
                                droneAllocations.remove(dch.getDroneNumber());
                            }

                        }


                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });
            queryDroneThread.start();

            //Thread to look for idle drones and send them a task
            Thread allocateTasksToDronesThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        log.info("Looking for idle drones");

                        synchronized (registeredDrones) {
                            for (DroneConnectionHandler handler : registeredDrones) {
                                if (droneAllocations.get(handler.getDroneNumber()) == null &&
                                        (alerts.get(handler.getDroneNumber()) == null ||
                                                alerts.get(handler.getDroneNumber()).size() == 0)) {

                                    log.info("Drone " + handler.droneNumber + " is idle");


                                    Task toAllocate = null;
                                    synchronized (unallocatedTasks) {

                                        if(unallocatedTasks.peek() instanceof GoToLocationTask) {
                                            toAllocate = unallocatedTasks.peek();
                                        } else {
                                            toAllocate = unallocatedTasks.poll();
                                        }

                                    }

                                    if (toAllocate != null) {
                                        sendTasksToIdleWorkers(handler, toAllocate);
                                    } else {
                                        log.info("No tasks left to allocate");
                                    }
                                } else {
                                    synchronized (unallocatedTasks) {
                                        if (unallocatedTasks.peek() instanceof GoToLocationTask) {
                                            sendTasksToIdleWorkers(handler, unallocatedTasks.peek());
                                        }
                                    }
                                }
                            }
                        }


                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            allocateTasksToDronesThread.start();

        }

    }

    private boolean taskRegistered(Task t) {
        synchronized (unallocatedTasks) {
            return TaskWithIDExistsInList(t.getIdX(), allocatedTasks) ||
                    TaskWithIDExistsInList(t.getIdX(), unallocatedTasks) ||
                    TaskWithIDExistsInList(t.getIdX(), completedTasks);
        }
    }


    public void addTask(Task t) {

        synchronized (unallocatedTasks) {
            unallocatedTasks.offer(t);
        }

    }

    public synchronized void releaseTaskIntoPool(Task t) {
        synchronized (unallocatedTasks) {
            unallocatedTasks.offer(t);
            allocatedTasks.remove(t);
        }
    }

    public synchronized void markTaskAsCompleted(Task t) {

        allocatedTasks.remove(t);
        completedTasks.add(t);

    }

    public synchronized void sendTasksToIdleWorkers(DroneConnectionHandler handler, Task toAllocate) {

        droneAllocations.put(handler.getDroneNumber(),toAllocate);
        allocatedTasks.add(toAllocate);

        RequestWrapper rw = new RequestWrapper();
        rw.addTask(toAllocate);
        handler.onOutput(rw);


    }


    public static boolean TaskWithIDExistsInList(Integer id, Collection<Task> tasks) {
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
