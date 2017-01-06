
import java.net.InetAddress;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * Contact Class for ServerEx
 * 
 */
public class Contact {
    
    protected String name; //name of the contact
    protected String contactAddress; // IP address of the contact
    protected String contactOsc;  // OSC address of the contact
    protected int receivePort;  // receive port of the contact
    protected boolean killSwitch; //artificially disconnect Robot from Server, disconnected when true
    protected boolean hasMaster;  // this robot is disconnected from server and has a master when true
    protected Contact master;  // coordinates of the master
    protected int threshold;   // synch threshold
    protected boolean firstTime;  // when true, the server restarts synch
        
    public Contact(String name,String robotAddress, String robotOsc, int sendPort, int threshold) {
       
        this.name = name;
        this.contactAddress = robotAddress;
        this.contactOsc = robotOsc;
        this.receivePort = sendPort;
        this.hasMaster = false;
        this.threshold = threshold;
        this.master = null;
        this.firstTime = true;
    }
    
}