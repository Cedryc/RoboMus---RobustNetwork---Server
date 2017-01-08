/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SynchNTest extends TimerTask{
    // variables declaration     
    private List<Contact> contacts;
    int synchInterval;
     
    public SynchNTest(List contacts, int synchInterval) {
        this.contacts = contacts; //list of robots contacts
        this.synchInterval = synchInterval; //synchronisation interval
    }
    
    // a simple sender function who sends a "synch" type of messages
    // if the "firstTime" value of a robot is true, SynchSender will send a "synch start" message instead
    // for more info about sender functions, see ServerSender in class ServerEx
    public void SynchSender(Contact contact){
        
        Contact target = contact;
        OSCPortOut sender = null;

        if (contact.hasMaster == true ){
            target = contact.master;
        }        
        
        try {
        sender = new OSCPortOut(InetAddress.getByName(target.contactAddress), target.receivePort);
        } catch (SocketException ex) {
        Logger.getLogger(SynchNTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (UnknownHostException ex) {
        Logger.getLogger(SynchNTest.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        List args = new ArrayList<>();
        args.add("server");
        args.add(contact.name);
                                
        OSCMessage msg = null;
        
        // if it is a robot's first synch, a "synch start" message is sent
        // synch start messages additionnaly communicates the synchInterval and synchthreshold values to the robot
        if(contact.firstTime){
            args.add("synch start");      // synch start will make the robot start his buffer  
            args.add(System.currentTimeMillis());
            args.add(synchInterval);
            args.add(contact.threshold); // the threshold is currently the same for every robot
            contact.firstTime = false;
        
        // synch messages countain only the currentTime value of the server
        // by regularly sending this info, robots can synchronise their clock to the server's clock
        }else{
            args.add("synch");       
            args.add(System.currentTimeMillis()); 
        }
            
        msg = new OSCMessage(target.contactOsc, args);
           
        try {
        sender.send(msg);
        } catch (IOException ex) {
        Logger.getLogger(SynchNTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    // a sender function who sends "test messages" types of message
    // the server will periodically send test messages to a disconnected robot
    // if the server receives an answer to one of these messages, the server will consider the robot connected again
    public void TstSender ( Contact contact){
        
        OSCPortOut sender = null;
        
        try {
        sender = new OSCPortOut(InetAddress.getByName(contact.contactAddress), contact.receivePort);
        } catch (UnknownHostException ex) {
        Logger.getLogger(SynchNTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SocketException ex) {
        Logger.getLogger(SynchNTest.class.getName()).log(Level.SEVERE, null, ex);
        }
                    
        List args = new ArrayList<>();  
        args.add("server");
        args.add(contact.name);
        args.add("test answer");
                    
        OSCMessage test = null;
        test = new OSCMessage(contact.contactOsc, args);
                    
        try {
        sender.send(test);
        } catch (IOException ex) {
        Logger.getLogger(SynchNTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void run(){
        for (Contact contact : contacts) {
            
        /*
        killSwitch is a way to simulate a connection loss with a robot
        if killSwitch is true, the sending functions won't send the messages
        */
           
            
            if (contact.killSwitch == false || (contact.hasMaster == true && contact.master.killSwitch == false)) {
            
                
                /*
                if the contact has a master, it means he has lost connection with the server => send test messages                
                */
                if (contact.killSwitch == false && contact.hasMaster == true ){
                    TstSender(contact);                           
                }                           
    
                SynchSender(contact);
    
            }
            else{
            System.out.println("i can't synch with " + contact.name);
            }
        }
    }
}
