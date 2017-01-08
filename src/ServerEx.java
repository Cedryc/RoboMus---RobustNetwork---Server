/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/*
list of imports
some may be unused
*/
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import com.sun.corba.se.pept.transport.ListenerThread;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServerEx { 
    
    // variables declaration    
    String name;       //name of the server, to be used when communicating with robots
    String oscAddress; //OSC address of the server
    
    int receivePort;   //receive port of the server
    int synchInterval; //interval in miliseconds
    int synchThreshold; 
    /*
    synch threshold of the instruments;
    if the instrumentTime - actionTime is higher than this threshold, the action will no be performed;
    see synch related comments in the BufferEx class of the RobotEx project
    may be a good idea to make this threshold individual for each instruments
    */
    
    Timer timer; // timer is used to send synch messages
    
    static List<Contact> contacts; // list of the robots in the performance, is update wen robots join / sign off
    // is not updated when a robot leaves without warning
    
    ColorsSimulation colorsSimulation;
    /* 
    color simulation is used to test a robot's respond to messages, 
    the server sends messages containing colors and the robot software displays the colors he receives
    */
    SynchNTest synchNTest;
    
    public ServerEx() {
        
        /*
        could be a good idea to creat a UI for the server, in a scimilar manner than that of the robot
        as an easy way to modify parameters such as synchInterval and receive Port
        */
        
        //initializing the port, name, oscAddress parameters
        this.receivePort = 001;
        this.name = "Server";
        this.oscAddress = "/Server";
        
        // creating the contact list and the timer
        this.contacts = new ArrayList<Contact>();
        Timer timer = new Timer();
        
        /*
        creating the synchronisation;
        scheduleAtFixedRate periodically performs an action:
            Here the action is to send synch messages, and the period of scheduleAtFixedRate is synchInterval
        */
        this.synchInterval = 500; 
        this.synchThreshold = 500;
        
        this.synchNTest = new SynchNTest(contacts,this.synchInterval);
        timer.scheduleAtFixedRate(synchNTest, this.synchInterval, this.synchInterval);
        
        //initalizing the colorsSimulation class        
        this.colorsSimulation = new ColorsSimulation(contacts);
        
    }
    
    /* Message structure:
    Contact of Origin, Contact destination, type of message, other args.
    Put null in other args if they are not used
    */
    
    /* target = who i send the message to
       finTarget = who the message is for
       if they are different, the robot who receives the message will redirect it to the right destination
       useful in case of lost connection
    */
    
    /*
    ServerSender function isn't actually used in the program
    the actual senders (synch, test messages, colors) are in seperate classes: SynchNTest and ColorsSimulation
    ServerSender is just a template for Sender functions
    */
    public void ServerSender(Contact finTarget, String type, String message){
        //finTarget is the true destination
        // target is created equal to finTarget, but it will become different if finTarget can't be reached
        Contact target = finTarget;
        
        /* 
        if finTarget can't be reached he will find a master on its own
        the server will be aware of this by receiving a message from the master
        therefore, from the server's point of view; robot is disconnected <=> robot has master
        in this case, the actual target for every of finTarget's messages will become finTarget's master
        */
        if (finTarget.hasMaster == true){  
            target = finTarget.master;
        }
        
        // create an OSC sender
        OSCPortOut sender = null;
        
        //assign the target's coordinates to the OSC sender
        try {
        sender = new OSCPortOut(InetAddress.getByName(target.contactAddress) , target.receivePort);
        } catch (UnknownHostException | SocketException ex) {
        Logger.getLogger(ServerEx.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // default arguments of each messages
        List args = new ArrayList<>(); // create list of arguments
        args.add(this.name);           // add name of the origin, here it is Server
        args.add(finTarget.name);      // add name of the finTarget, in case this message isn't sent directly to it
        args.add(type);                // add type of message, so that robot can react to it accordingly
        
        switch(type){
            
            /* an idea could be to replace the full words messages tpyes with single numbers,
            to reduce the size of messages
            */
            
            case "message to R":
                // simple message to be stored in the robot's buffer
                // used to simulate the musical notes messages
            args.add(message);
            break;            
        }
        
        // create the OSC message
        OSCMessage msg = new OSCMessage(target.contactOsc, args);
        
        // send the message
        try {
        sender.send(msg);
        if (target.name.equals(finTarget.name)){
           System.out.println("message " + type + " sent to " + target.name);
        }
        else{
            System.out.println("message " + type + " sent to " + finTarget.name + " through " + target.name);
        }
        } catch (IOException ex) {
        Logger.getLogger(ServerEx.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    // receiver thread to receive the messages sent to the server
    public void ServerReceiver(){
        
        // create an OSC receiver
        OSCPortIn receiver = null;
        
        //assign the receivePort value to the OSC receiver
        try {
            receiver = new OSCPortIn(this.receivePort);           
        } 
        catch (SocketException ex) {
            Logger.getLogger(ServerEx.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        OSCListener listener;
        listener = new OSCListener() {
            
            // how to react when a message is received
            public void acceptMessage(java.util.Date time, OSCMessage message) {
                
                // put the arguments into a list
                List L = message.getArguments();
                                
                // test to sort between different types of messages: arg n°2 off every messages contains the type
                switch((String)L.get(2)){
                    
                    
                    // if message received is handshake type: 
                    case "handshake":
                        //create a contact with the values sent inside the message
                        Contact robot = new Contact((String)L.get(0), (String)L.get(3), (String)L.get(4), (int)L.get(5), synchThreshold);
                        // set up the booleans used to describe a robot's state
                        robot.firstTime = true; // if firstTime is true, a different synch message with additionnal infos will be sent to the robot
                        robot.hasMaster = false; // hasMaster is true <=> the robot has lost connection with the server <=> the robot has a master
                        robot.killSwitch = false; // killSwitch is used to artificially disconnect a robot from the server during simulations
                        
                        // create the RobotIsIn boolean
                        boolean RobotIsIn = false;
                        
                        // test to see if the robot is already a contact of the server
                        // if it is, then the contact's information won't be added, since they already are inthe list
                        if (contacts.size() != 0){                           
                            for(Contact contact : contacts){
                                if (contact.name.equals((String)L.get(0))){
                                    RobotIsIn = true;
                                    contact.firstTime = true; // send additionnal infos and cause the robot to start his sycnh waiter
                                    System.out.println("I already know you");
                                    System.out.println("\n"); 
                                    break; 
                                }
                            }
                        }
                        System.out.println("Contact is in ? " + RobotIsIn);
                        
                        // if the robot is not in the contact list yet:
                        if (RobotIsIn == false){
                            System.out.println("I don't know know you");
                            contacts.add(robot); //add it to the contacts list
                            System.out.println(robot.name + " " + robot.contactAddress + " " + robot.contactOsc + " " + robot.receivePort);
                            System.out.println("robot " + robot.name + " is now part of the performance: " + contacts + "\n");
                            }
                        break;
                
                    // if message received is "test back" type    
                    case "test back":
                        System.out.println("i receive " + L.get(0) + " again\n");
                        
                        // search for that robot in the contact list based on his name, the argument 0 of the message
                        for(Contact contact : contacts){
                            if (contact.name.equals((String)L.get(0))){
                                // update the contacts' booleans 
                                contact.firstTime = true;
                                contact.hasMaster = false;
                                contact.master = null;
                                break;
                            }
                        }
                        break;
                    
                    // if message received is "help for R" type
                    case "help for R":
                        /*search for the contact with a connection problem in the contacts list,
                        based on the name his master sent ( argument 3 of the message )                        
                        */
                        for(Contact contactPb : contacts){
                            if (contactPb.name.equals((String)L.get(3))){
                                
                                // search for te master in the contacts list
                                for(Contact contactM : contacts){
                                    if (contactM.name.equals((String)L.get(0)) && contactM.killSwitch == false){
                                        // update the contactPB booleans; add him a master
                                        contactPb.master = contactM;
                                        contactPb.hasMaster = true;
                                        contactPb.firstTime = true;
                                        System.out.println(contactPb.master.name + " is the master of " + contactPb.name);
                                        System.out.println("The test interval is " + synchInterval);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        break;
                        
                    // if message is of type "sign out" the robot will simply be removed from the contacts
                    case"sign out":
                        for( Contact contact: contacts){
                            if (((String)L.get(0)).equals(contact.name)){
                                contacts.remove(contact);
                            }
                        }
                }
            }
            
        };
        // start the listening thread
        receiver.addListener(this.oscAddress, listener);
        receiver.startListening();   
    } 
    

    public static void main(String[] args) throws SocketException {
        // you need to use a computer with netbeans to launch the server
        // the printed values of name, OscAddress, ipAddr and receive port are those you will need to input when starting a robot
        ServerEx server = new ServerEx(); // create the server
        System.out.println("My name is " + server.name);
        System.out.println("My Osc Address is " + server.oscAddress);
        Scanner scan = new Scanner(System.in); // create a scanner for user inputs
        
        String ipAddr = null;
        // this gives the ip address of the server
        try {             
            InetAddress ipHost = InetAddress.getLocalHost();
            ipAddr = ipHost.getHostAddress();     
            System.out.println("My Ip adress is " + InetAddress.getByName(ipAddr));
            } catch (UnknownHostException ex) {   
            ex.printStackTrace();
            }
    
        System.out.println("My receive port is " + server.receivePort + "\n");
        
        // start the receiver function
        server.ServerReceiver();       
        
        //console inputs
        while(true){
            int i = scan.nextInt();
            switch(i){
                case 1:
                    server.ServerSender(contacts.get(0), "message to R", "hello");
                    break;
                //input 3 to start sending color messages to robots
                case 3:
                    server.colorsSimulation.start();
                    System.out.println("start colors");
                    break;
                
                // input 4 and a contact's number (between 0 and lenght(contact)-1 ) to toggle the killswitch on that contact
                //this allows to artificially disconnect a robot during tests
                case 4:
                    int l = scan.nextInt();
                    contacts.get(l-1).killSwitch = (contacts.get(l-1).killSwitch == false) ? true: false;
                    System.out.println("Robot " + (l) + " is off? " + contacts.get(l-1).killSwitch);
                    break;
                
                // input 5 and a contact's number (between 0 and lenght(contact)-1 ) to have a summary of that contact's state
                case 5:
                    int m = scan.nextInt();
                    System.out.println("firstTime = " + contacts.get(m-1).firstTime);
                    System.out.println("hasMaster = " + contacts.get(m-1).hasMaster);
                    System.out.println("killSwitch = " + contacts.get(m-1).killSwitch);
                    break;
            }     
        }
    }
}
    


