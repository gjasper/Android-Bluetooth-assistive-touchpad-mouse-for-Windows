package src;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.microedition.io.StreamConnection;

public class ProcessConnectionThread implements Runnable {

    private StreamConnection mConnection;
    private PointerInfo pInfo = MouseInfo.getPointerInfo();
    private int currX;
    private int currY;
    private Point currPoint;

    public ProcessConnectionThread(StreamConnection connection) {
        mConnection = connection;
    }

    @Override
    public void run() {
        try {
            // prepare to receive data

            InputStream inputStream = mConnection.openInputStream();

            System.out.println("waiting for input");

            while (true) {

                byte[] command = new byte[7];
                inputStream.read(command);

                String commandString = new String(command);

                //processCommand(testString);
                processCommand(commandString);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process the command from client
     *
     * @param command the command code
     */
    private void processCommand(String command) {

        int midCommandPos;

        try {

            Robot robot = new Robot();

if (command.equals("mBTN_LT")) {
    System.out.println("Left button ok");

    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
} else if (command.equals("mBTN_RT")) {
                System.out.println("Right button ok");

                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            } else {

                midCommandPos = command.indexOf(":");
                if (midCommandPos == -1) {
                    return;
                }

                pInfo = MouseInfo.getPointerInfo();
                currPoint = pInfo.getLocation();
                currX = (int) currPoint.getX();
                currY = (int) currPoint.getY();

                System.out.println("Cmd[" + command + "]   " + "Curr[" + currX + ":" + currY + "]");
                int nextX = 0, nextY = 0 ;

                try{
                    nextX = Integer.parseInt(command.substring(0, midCommandPos))/3;
                    nextY = Integer.parseInt(command.substring(midCommandPos + 1))/3;
                                 
                }catch(Exception e){
                    nextX = 0; nextY = 0;
                }           
                
                nextX = currX - nextX;
                nextY = currY - nextY;
                    
                robot.mouseMove(nextX, nextY);

                currX = nextX;
                currY = nextY;
            }

        } catch (AWTException ex) {
            Logger.getLogger(ProcessConnectionThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
