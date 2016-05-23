package gpig.group2.comms;

import gpig.group2.models.drone.request.RequestMessage;
import gpig.group2.models.drone.request.Task;
import gpig.group2.models.drone.response.ResponseMessage;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Date;

/**
 * Created by james on 23/05/2016.
 */
public class RequestWrapper implements CommonObject  {
    RequestMessage smg = new RequestMessage();


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

        smg.getTasksX().clear();
        return sw.toString();
    }

    public void addTask(Task t) {
        smg.getTasksX().add(t);
    }

}
