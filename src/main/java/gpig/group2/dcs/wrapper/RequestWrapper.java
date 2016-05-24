
package gpig.group2.dcs.wrapper;


import gpig.group2.dcs.CommonObject;
import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.request.Task;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by james on 23/05/2016.
 */
public class RequestWrapper implements CommonObject {
    RequestMessage smg = new RequestMessage();

    public RequestWrapper() {
        smg.setTasks(new ArrayList<>());
    }

    @Override
    public String getText() {
        smg.setTimestamp(new Date());

        StringWriter sw = new StringWriter();
        JAXBContext c = null;
        try {
            c = JAXBContext.newInstance(RequestMessage.class);
            Marshaller m = c.createMarshaller();
            m.marshal(smg,sw);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return sw.toString();
    }

    public void addTask(Task t) {
        smg.getTasksX().add(t);
    }

    public void clearTasks() {
        smg.getTasksX().clear();
    }

    public int numTasks() {
        return smg.getTasksX().size();
    }
}