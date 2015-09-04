/*
    This file is part of Dodgy HTTP Server.

    Dodgy HTTP Server Copyright (C) 2011-2014 UstadMobile Inc.

    Dodgy HTTP Server is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version with the following additional terms:

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Dodgy HTTP Server is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 */
package com.ustadmobile.dodgyhttpd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author mike
 */
public class RawSocketReceiver implements Runnable{
    
    private int port;
    
    private boolean running;
    
    private ServerSocket serverSocket;
    
    private Thread serverThread;
    
    private String clientName;
    
    private File logFile;
    
    public RawSocketReceiver(int port, String clientName, File logFile) throws IOException{
        this.port = port;
        this.clientName = clientName;
        this.logFile = logFile;
        running = false;
    }
    
    public boolean startListening() {
        try {
            serverSocket = new ServerSocket(port);
            serverThread = new Thread(this);
            running = true;
            serverThread.start();
            return true;
        }catch(IOException e) {
            return false;
        }
    }

    @Override
    public void run() {
        Socket clientSocket = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        try {
            clientSocket = serverSocket.accept();
            in  = new BufferedReader(new InputStreamReader(
                clientSocket.getInputStream()));
            out = new BufferedWriter(new FileWriter(logFile));

            String inLine;
            while((inLine = in.readLine()) != null) {
                out.write(inLine);
                out.write('\n');
                out.flush();
            }
        }catch(IOException e) {
            System.err.println("Error reading client socket");
            e.printStackTrace();
        }finally {
            if(in != null) {
                try { in.close(); }
                catch(IOException e) {
                    System.err.println("Error closing input stream");
                }
            }
            
            if(clientSocket != null) {
                try { clientSocket.close(); }
                catch(IOException e) {
                    System.err.println("Error closing client socket");
                }
            }
            
            try { serverSocket.close(); }
            catch(IOException e) {
                System.err.println("Error closing server socekt");
            }
        }
    }
}

