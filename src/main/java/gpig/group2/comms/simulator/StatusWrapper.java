package gpig.group2.comms.simulator;

import gpig.group2.comms.CommonObject;
import gpig.group2.models.drone.status.StatusMessage;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

/**
 * Created by james on 23/05/2016.
 */
public class StatusWrapper implements CommonObject {

    StatusMessage smg = new StatusMessage();


    @Override
    public String getText() {
        StringWriter sw = new StringWriter();
        JAXBContext c = null;
        try {
            c = JAXBContext.newInstance(StatusMessage.class);
            Marshaller m = c.createMarshaller();

            m.marshal(smg,sw);

        } catch (JAXBException e) {
            e.printStackTrace();
        }



        return sw.toString();
    }
}
