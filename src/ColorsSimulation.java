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
import java.util.logging.Level;
import java.util.logging.Logger;



public class ColorsSimulation extends Thread {
    //variables declaration
    List<Contact> contacts; // the contacts list
    long noteInterval; // interval between two notes, ie. color changes
    long originTime; // is the value of the currentTime when the simulation starts
    int k;

    public ColorsSimulation(List contacts){
        this.contacts = contacts; 
        this.noteInterval = 500; // noteInterval in millisecs
    }
    
    // sender function, sends "message to R" type of message which are to be stocked in the buffer and transmittted to the hardware
    public void ColorsSender(Contact finTarget, long playTime, String colorString){
        
        Contact target = finTarget;
    
        // tests wether the finTarget has a master or not
        if (finTarget.hasMaster == true){
            target = finTarget.master;
        }
    
        // tests wether the message can be sent or not
        if (target.killSwitch == false || (target.hasMaster == true && target.master.killSwitch == false)){
                        
            OSCPortOut sender = null;
        
            try {
            sender = new OSCPortOut(InetAddress.getByName(target.contactAddress) , target.receivePort);
            } catch (UnknownHostException | SocketException ex) {
            Logger.getLogger(ServerEx.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            List args = new ArrayList<>();
            args.add("server");
            args.add(finTarget.name);
            args.add("message to R");
            args.add(playTime);   //communicate to the robot at what time he is to change colors
            args.add(colorString);  // communicate to the robot what color he will change to
        
            OSCMessage msg = new OSCMessage(target.contactOsc, args);
        
            try {
            sender.send(msg);
            } catch (IOException ex) {
            Logger.getLogger(ServerEx.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }
        
    public void run(){  
        
        try { //wait a bit before starting the messages
            Thread.sleep(200);
            } catch (InterruptedException ex) {
            Logger.getLogger(ColorsSimulation.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        // set the origin time at the currenttime value when the simulation is launched        
        originTime = System.currentTimeMillis();
        k = 0;
        
        int cont = 0;
        
        /*
        here are two ways of sending the messages:
        1) send them all at once while specifying a playtime in the message
            good: uses the robot's buffer stocking ability, allows the robot to keep playing in case he loses connection with the server
            bad: might overflow the buffer and lead to errors
        2) send them only a few milliseconds before the message is to be played
            good: doesn't overflow the buffer
            bad: robots don't play when they lose connection with server
        */
        while (true){ // this one sends messages 500ms before they are to be played by the robot
            
            for (Contact contact : contacts) {
                
                // tells every contacts to play "blue" 500ms after the message is sent
                ColorsSender(contact, System.currentTimeMillis()+ 500 , "blue");
                System.out.println("blue sent");
            }
            
            try {  //tells the server to wait noteInterval millisecs before sending the next message
            Thread.sleep(noteInterval);
            } catch (InterruptedException ex) {
            Logger.getLogger(ColorsSimulation.class.getName()).log(Level.SEVERE, null, ex);
            }
                        
            for (Contact contact : contacts) {
                                
                ColorsSender(contact, System.currentTimeMillis()+ 500, "yellow");
            }
            
            try {
            Thread.sleep(noteInterval);
            } catch (InterruptedException ex) {
            Logger.getLogger(ColorsSimulation.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            for (Contact contact : contacts) {
                                
                ColorsSender(contact, System.currentTimeMillis()+ 500, "green");
            }
            
            try {
            Thread.sleep(noteInterval);
            } catch (InterruptedException ex) {
            Logger.getLogger(ColorsSimulation.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            for (Contact contact : contacts) {
                                
                ColorsSender(contact, System.currentTimeMillis()+ 500, "red");
            }
            
            try {
            Thread.sleep(noteInterval);
            } catch (InterruptedException ex) {
            Logger.getLogger(ColorsSimulation.class.getName()).log(Level.SEVERE, null, ex);
            } 
        
        /*while (true){ //this one sends a bunch of messages all at once, then waits a while to make sure the robot's buffer doesn't overflow
            
            for (Contact contact : contacts) {
                
                 tells every contacts to play "blue" k*noteInterval millisecs after the simulation started
                 the value of k increases after each message sent
                ColorsSender(contact, originTime + k*noteInterval , "blue");
                k++;
            }
                        
            for (Contact contact : contacts) {
                                
                ColorsSender(contact, originTime + k*noteInterval, "yellow");
                k++;
            }
            
            for (Contact contact : contacts) {
                                
                ColorsSender(contact, originTime + k*noteInterval, "green");
                k++;
            }
            
            for (Contact contact : contacts) {
                                
                ColorsSender(contact, originTime + k*noteInterval, "red");
                k++;
            }
                
            cont++;
            
            // every 16 messages, the server pauses for a while before sending new ones
            // needs some improvements, since the value of 16 will not suit every playNote values
            
            if ( cont == 4){ 
                cont = 0;
                try {
                Thread.sleep(15*noteInterval);
                } catch (InterruptedException ex) {
                Logger.getLogger(ColorsSimulation.class.getName()).log(Level.SEVERE, null, ex);
                }*/
                
             
        }
    }
    
}
