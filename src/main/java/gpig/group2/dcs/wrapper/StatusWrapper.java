package gpig.group2.dcs.wrapper;

import gpig.group2.dcs.CommonObject;
import gpig.group2.models.drone.status.DroneStatusMessage;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

/**
 * Created by james on 23/05/2016.
 */
public class StatusWrapper implements CommonObject {

    DroneStatusMessage smg = new DroneStatusMessage();


    @Override
    public String getText() {
        StringWriter sw = new StringWriter();
        JAXBContext c = null;
        try {
            c = JAXBContext.newInstance(DroneStatusMessage.class);
            Marshaller m = c.createMarshaller();

            m.marshal(smg,sw);

        } catch (JAXBException e) {
            e.printStackTrace();
        }



        return sw.toString();
    }

    public void setStatus(DroneStatusMessage status) {
        this.smg = status;
    }
}