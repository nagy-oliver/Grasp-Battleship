package com.compsci.Coms;
import java.net.*;
import java.util.*;

import com.compsci.Board;
import com.compsci.Config;
import com.compsci.Utils;

import java.io.*; 
import org.slf4j.*;
import org.slf4j.helpers.Util;

public class GameClient extends Socket {
    // coms
    private PrintWriter out;
    private BufferedReader in;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    // conn
    private Socket dataTransfer;

    // data
    private Config localConfig;
    private Board localPlacement;
    public boolean started; 
    int move;
    boolean okFlag;

    // Classwide logger
    Logger logger;

    public GameClient(String host, int port) throws UnknownHostException, IOException, InterruptedException {
        super(host, port);
        logger = LoggerFactory.getLogger(GameClient.class);

        // init data
        localConfig = new Config();
        localPlacement = new Board(localConfig.size);

        // Init streams
        out = new PrintWriter(getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(getInputStream()));
        
        // Check data transfer  (order S-R)
        out.println(Utils.testBundle);
        if (Utils.testBundle.equals(in.readLine())) {
            logger.debug("Recognized test greeting");
        }
        else {
            logger.debug("Unrecognized test greeting");
        }

        logger.info("Successfully connected to server");

        dataTransfer = new Socket(host, port);

        // Send config for validation
        oos = new ObjectOutputStream(dataTransfer.getOutputStream());
        ois = new ObjectInputStream(dataTransfer.getInputStream());

        // After initial check enter permanent reciever thread
        Thread commands = new Thread() {
            public void run() {
                while(true) {
                    try {
                        String data = in.readLine();
                        if (data == null) continue;
                        //logger.debug(data);
                        String[] splitPacket = data.split(" ");

                        switch(splitPacket[0]) {
                            case "START":
                                started = true;
                                Utils.Clear();
                                System.out.println("Starting game! Random selected player to start: " + splitPacket[1]);
                                okFlag = true;
                            case "REQ_CONF":
                                localConfig = new Config();
                                oos.writeUnshared(localConfig);
                                oos.flush();
                                break;
                            case "REQ_BOARD":
                                localPlacement = new Board(localConfig.size);
                                oos.writeUnshared(localPlacement);
                                oos.flush();
                                break;
                            case "TERM":
                                System.exit(Integer.parseInt(splitPacket[1]));
                                break;
                            case "INV_SBOARD":
                                System.out.println("Opponents board doesnt match the config requirements!");
                                break;
                            case "INV_BOARD":
                                System.out.println("Your board doesnt match the config requirements!");
                                break;
                            case "MSG":
                                System.out.println(data.substring(3));
                                break;
                            case "CLEAR":
                                Utils.Clear();
                                break;
                        }
                    } catch (EOFException e) { } catch (IOException e) { } 
                }

            }
        };
        commands.start(); 

        Thread stateMonitor = new Thread() {
            public void run() {
                while(true) {
                    try {
                        Board a = (Board) ois.readUnshared();
                        if (a == null) continue;
                        
                        // logic
                        System.out.println(a);
                        System.out.print("");
                        System.out.print(">: ");
                     
                    } catch (IOException e) { } catch (ClassNotFoundException e) { } 
                }

            }
        };
        stateMonitor.start();

        EnterCommandLoop();
    }

    void EnterCommandLoop() throws IOException {
        // Enter command loop
        Scanner input = new Scanner(System.in);
        System.out.print(">: ");
        while(true) {
            String data = input.nextLine();
            if (data == null) continue;
            String[] splitCommand = data.split(" ");

            // Handle data logic 
            switch(splitCommand[0]) {
                case "move":
                    System.out.println(data);
                    if (started) {
                        out.println(data); // relay
                    }
                    break;
                case "ok":
                    out.println("ok");
                    Utils.Clear();
                    break;
            }

            System.out.print(">: ");
        }
    }
}
