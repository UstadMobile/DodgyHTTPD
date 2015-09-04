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
import java.util.Properties;

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
    
    private static final String CODELU = ":codelu:";
    
    private Properties codeLookups;
    
    public RawSocketReceiver(int port, String clientName, File logFile, Properties codeLookups) throws IOException{
        this.port = port;
        this.clientName = clientName;
        this.logFile = logFile;
        running = false;
        this.codeLookups = codeLookups;
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
            StringBuilder codeSb;
            while((inLine = in.readLine()) != null) {
                int codeLuPos = inLine.indexOf(CODELU);
                if(codeLuPos != -1) {
                    codeSb = new StringBuilder();
                    int endPos;
                    for(endPos = codeLuPos + CODELU.length(); endPos < inLine.length(); endPos++) {
                        if(!Character.isWhitespace(inLine.charAt(endPos))) {
                            codeSb.append(inLine.charAt(endPos));
                        }else {
                            break;
                        }
                    }
                    
                    String codeDesc = codeLookups.getProperty(codeSb.toString(), 
                            "");
                    inLine = inLine.substring(0, codeLuPos) + "Code: " + codeSb.toString() 
                            + ": " + codeDesc + " " 
                            + inLine.substring(codeLuPos + CODELU.length()+codeSb.length());
                }
                
                
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

