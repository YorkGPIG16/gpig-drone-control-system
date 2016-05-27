package gpig.group2.dcs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

import gpig.group2.models.drone.response.Image;
import gpig.group2.models.drone.response.ResponseData;
import gpig.group2.models.drone.response.ResponseMessage;
import gpig.group2.models.drone.response.responsedatatype.Aborted;
import gpig.group2.models.drone.response.responsedatatype.BuildingOccupancyResponse;
import gpig.group2.models.drone.response.responsedatatype.Completed;
import gpig.group2.models.drone.response.responsedatatype.ManDownResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gpig.group2.dcs.wrapper.ResponseWrapper;
import gpig.group2.dcs.wrapper.StatusWrapper;
import gpig.group2.maps.geographic.Point;
import gpig.group2.models.drone.status.DroneStatusMessage;

/**
 * Created by james on 24/05/2016.
 */
public class SimulatedDrone {

    Logger log = LogManager.getLogger(SimulatedDrone.class);
    public static void main(String[] args) throws IOException, InterruptedException {
        new SimulatedDrone();
    }

    public SimulatedDrone() throws IOException, InterruptedException {
        Socket skt = new Socket("127.0.0.1",9876);

        DataOutputStream output = new DataOutputStream(skt.getOutputStream());
        DataInputStream input = new DataInputStream(skt.getInputStream());

        //Thread to send status updates
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    StatusWrapper sw = new StatusWrapper();
                    try {
                        while(true) {
                            synchronized (output) {
                            	DroneStatusMessage sm = new DroneStatusMessage();
                            	sm.setBattery(100);
                            	sm.setPosition(new Point(100.0,200.0));
                            	sm.setTimestamp(new Date());
                            	
                            	sw.setStatus(sm);
                            	
                            	
                                System.out.println("Sending:" + sw.getText());
                                output.writeBytes(sw.getText() + "\n");

                            }


                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
            t.start();
        }



        //Thread to send response messages
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    ResponseWrapper rw = new ResponseWrapper();
                    try {
                        while(true) {
                            synchronized (output) {

                                ResponseMessage rm = new ResponseMessage();
                                rm.setTimestamp(new Date());
                                rm.setResponse(new ArrayList<>());

                                ResponseData rd1 = new ManDownResponse();
                                rd1.setImages(new ArrayList<>());

                                Image i = new Image();
                                i.setUrl("http://url");

                                rd1.getImagesX().add(i);
                                rd1.setOrigin(new Point(100.0,200.0));
                                rd1.setTimestamp(new Date());




                                ResponseData rd2 = new Aborted();
                                rd2.setTimestamp(new Date());
                                rd2.setTaskId(2);

                                ResponseData rd3 = new Completed();
                                rd3.setTimestamp(new Date());
                                rd3.setTaskId(4);


                                BuildingOccupancyResponse rd4 = new BuildingOccupancyResponse();
                                rd4.setImages(new ArrayList<>());


                                Image i2 = new Image();
                                i2.setUrl("http://url2");

                                rd4.getImagesX().add(i2);
                                rd4.setOrigin(new Point(100.0,200.0));
                                rd4.setTimestamp(new Date());
                                rd4.setEstimatedNumberOfPeople(5);
                                rd4.setMultiStoryBuilding(true);





                                rm.getResponseX().add(rd1);
                                rm.getResponseX().add(rd2);
                                rm.getResponseX().add(rd3);
                                rm.getResponseX().add(rd4);




                                rw.setMessage(rm);

                                log.debug("Sending:" + rw.getText());







                                output.writeBytes(rw.getText() + "\n");
                            }
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
            t.start();
        }


        //Thread to receive commands
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    String responseLine;

                    try {
                        while ((responseLine = input.readLine()) != null) {
                            log.debug("Received: " + responseLine);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            t.start();
        }


        while(true) {
            Thread.sleep(5000);
        }

    }


}
