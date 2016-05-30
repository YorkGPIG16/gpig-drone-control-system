package gpig.group2.dcs.c2integration;

import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.maps.geographic.Point;
import gpig.group2.maps.geographic.position.BoundingBox;
import gpig.group2.models.alerts.Alert;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.request.task.AerialSurveyTask;
import gpig.group2.models.drone.response.ResponseData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by james on 27/05/2016.
 */
public class DumbC2Integration implements C2Integration {
    static Logger log = LogManager.getLogger(DumbC2Integration.class);

    @Override
    public void sendDroneStatus(StatusWrapper msg) {
        log.info("Send message to c2");
    }

    @Override
    public void sendPOI(ResponseData msg) {
        log.info("Send message to c2");
    }

    @Override
    public void sendBuildingOccupancy(ResponseData rd) {
        log.info("Send message to c2");
    }

    @Override
    public void sendCompletedDeploymentArea(ResponseData rd) {
        log.info("Send message to c2");
    }

    @Override
    public void sendAlert(Alert alert) {
        log.info("Send message to c2");
    }

    @Override
    public RequestMessage getRequests() {

        RequestMessage rm = new RequestMessage();

        rm.setTasks(new ArrayList<>());

        for(int i = 1; i<1000; i++) {
            AerialSurveyTask ast = new AerialSurveyTask();
            ast.setLocation(new BoundingBox(new Point(53.9556075,-1.072157), new Point(53.9538716,-1.0677649)));
            ast.setTimestamp(new Date());
            ast.setPriority(50);
            ast.setId(i);

            rm.getTasksX().add(ast);
        }

        return rm;

    }
}
