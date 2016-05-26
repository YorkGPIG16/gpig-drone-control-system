package gpig.group2.dcs;

import gpig.group2.dcs.c2integration.C2Integration;
import gpig.group2.dcs.wrapper.RequestWrapper;
import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.maps.geographic.Point;
import gpig.group2.maps.geographic.position.BoundingBox;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.response.ResponseData;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.response.responsedatatype.Completed;
import gpig.group2.models.drone.status.DroneStatusMessage;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gpig.group2.models.drone.request.Task;
import gpig.group2.models.drone.request.task.AerialSurveyTask;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * Created by Benjy on 26/05/2016.
 */
public class TaskPool implements DroneInterface {

    public class TaskComparator implements Comparator<Task>
    {
        @ Override
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

    public void handleResponseMessage(int id, ResponseMessage rm) {
        if(rm!=null && rm.getResponseX() != null) {

            DroneConnectionHandler completedWorker = null;
            for (DroneConnectionHandler worker : workers) {
                if (worker.getDroneNumber() == id) {
                    completedWorker = worker;
                    break;
                }
            }

            for(ResponseData rd : rm.getResponseX()){

                if(rd instanceof Completed) {

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
                } else {
                    if (c2 != null) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                c2.sendPOI(rd);
                            }
                        });
                        t.start();
                    }
                }
            }
        }
    }

    public void handleRequestMessage(int id, RequestMessage rq) {
        throw new NotImplementedException();
    }

    public void registerOutputHandler(OutputHandler handler) {

        if (!tasks.isEmpty())
        {
            workers.add((DroneConnectionHandler) handler);
            Task t = tasks.get(0);
            assignedTasks.add(t);
            tasks.remove(0);

            RequestWrapper rw = new RequestWrapper();
            rw.addTask(t);
            handler.onOutput(rw);
        }
        else
        {
            idleWorkers.add((DroneConnectionHandler) handler);
        }
    }

    public TaskPool(C2Integration c2) {
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

                            List<Task> newTasks = c2.getRequests().getTasksX();
                            for (Task t : newTasks){
                                boolean inTasks = tasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();
                                boolean inCompleted = completedTasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();
                                boolean inAssigned = assignedTasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();

                                if (!inTasks && !inCompleted && !inAssigned)
                                {
                                    if (idleWorkers.isEmpty())
                                    {
                                        tasks.add(t);
                                        Collections.sort(tasks, new TaskComparator());
                                    }
                                    else {
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

                        RequestMessage rm = new RequestMessage();
                        rm.setTasks(new ArrayList<>());
                        rm.setTimestamp(new Date());

                        AerialSurveyTask ast = new AerialSurveyTask();
                        ast.setLocation(new BoundingBox(new Point(53.9560689f, -1.0691074f), new Point(53.9544974f, -1.0662036f)));
                        ast.setPriority(100);
                        ast.setId(count++);

                        rm.getTasksX().add(ast);

                        log.info("Adding an AerialSurveyTask");

                        List<Task> newTasks = new ArrayList<Task>();
                        newTasks.add(ast);
                        for (Task t : newTasks){
                            boolean inTasks = tasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();
                            boolean inCompleted = completedTasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();
                            boolean inAssigned = assignedTasks.stream().filter(o -> o.getIdX() == t.getIdX()).findFirst().isPresent();

                            if (!inTasks && !inCompleted && !inAssigned)
                            {
                                if (idleWorkers.isEmpty())
                                {
                                    log.info("No idle workers, sent new task.");
                                    tasks.add(t);
                                    Collections.sort(tasks, new TaskComparator());
                                }
                                else {
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
}
