package gpig.group2.dcs.wrapper;

import gpig.group2.dcs.CommonObject;
import gpig.group2.models.drone.response.ResponseMessage;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

/**
 * Created by james on 23/05/2016.
 */
public class ResponseWrapper implements CommonObject {
    ResponseMessage smg = new ResponseMessage();


    @Override
    public String getText() {
        StringWriter sw = new StringWriter();
        JAXBContext c = null;
        try {
            c = JAXBContext.newInstance(ResponseMessage.class);
            Marshaller m = c.createMarshaller();

            m.marshal(smg,sw);

        } catch (JAXBException e) {
            e.printStackTrace();
        }



        return sw.toString();
    }
}