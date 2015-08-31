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

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;


/**
 * The control server that accepts requests to start, stop and set the parameters
 * on running servers
 * 
 * @author Mike Dawson <mike@ustadmobile.com>
 */
public class DodgyHTTPDServer extends NanoHTTPD {
    
    private Map<Integer, DodgyHTTPD> operatingServers;
    
    public static int startingPortNum = 8060;
    
    private File baseDir;
    
    private static DodgyHTTPDServer controlServer;
    
    private int nextPortNum;
    
    private File saveResultsDir = null;
    
    
    /**
     * Creates a new control server
     * 
     * @param host The interface on which we will listen (null for all)
     * @param port Port on which to listen
     * @param baseDir Directory from which we will serve files
     */
    public DodgyHTTPDServer(String host, int port, File baseDir) {
        super(host, port);
        nextPortNum = port+1;
        this.baseDir= baseDir;
        operatingServers = new HashMap<>();
    }
    
    public static void main(String[] args) throws IOException{
        String baseDir = ".";
        
        File saveResultsDirArg = new File(".");
        
        for(int i = 0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase("-p") || args[i].equalsIgnoreCase("--port")) {
                startingPortNum = Integer.parseInt(args[i+1]);
            }else if(args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--dir")) {
                baseDir = args[i + 1];
            }else if(args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--resultdir")) {
                saveResultsDirArg = new File(args[i+1]);
            }
        }
        
        
        File ourBaseDir = new File(baseDir);
        DodgyHTTPDServer.controlServer = new DodgyHTTPDServer(null, startingPortNum,
            ourBaseDir);
        System.out.println("Starting DodgyHTTPD control server on http://localhost:" 
            + startingPortNum + "/");
        
        DodgyHTTPDServer.controlServer.setSaveResultsDir(saveResultsDirArg);
        DodgyHTTPDServer.controlServer.start();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        System.out.println("Type stop [enter] to stop or hit ctrl+c");
        boolean keepRunning = true;
        while(keepRunning) {
            String input = in.readLine();
            if(input != null && input.equals("stop")) {
                keepRunning = false;
            }
        }
        
        
        DodgyHTTPDServer.controlServer.stop();
    }
    
    public void setSaveResultsDir(File saveResultsDir) {
        this.saveResultsDir = saveResultsDir;
    }    

    /**
     * Create a new DodgyHTTPDServer
     * 
     * @return port number on which the new server is running
     * @throws IOException when an exception occurs starting server
     */
    public int startNewServer()  throws IOException{
        int ourPortNum = nextPortNum;
        nextPortNum++;
        DodgyHTTPD newServer = new DodgyHTTPD(ourPortNum, baseDir);
        operatingServers.put(ourPortNum, newServer);
        newServer.start();
        return ourPortNum;
    }
    
    
    private DodgyHTTPD getServer(int serverPort) {
        return operatingServers.get(serverPort);
    }
    
    /**
     * Stop a server that was running (e.g. to test reaction of code when server
     * is not available)
     * 
     * @param serverPort The port number of the server we want to stop
     * @return true if it was running and now stopped, false otherwise
     */
    public boolean stopServer(int serverPort) {
        DodgyHTTPD httpd = getServer(serverPort);
        if(httpd != null && httpd.isAlive()) {
            httpd.stop();
            return true;
        }else {
            return false;
        }
    }
    
    /**
     * Starts a server that was previously stopped
     * 
     * @param serverPort the port number of the server we want to start
     * @return true if it was stopped and now running, false otherwise
     * @throws IOException 
     */
    public boolean startServer(int serverPort) throws IOException{
        DodgyHTTPD httpd = getServer(serverPort);
        if(httpd != null && !httpd.isAlive()) {
            httpd.start();
            return true;
        }else {
            return false;
        }
    }
    
    /**
     * Sets the parameters for an operating server
     * 
     * @param serverPort the port number of the server we want to set parameters for
     * @param params Map with speedlimit and/or forceerrorafter as Strings
     * @return true if successfully set
     */
    public boolean setServerParams(int serverPort, Map<String, String> params) {
        DodgyHTTPD httpd = operatingServers.get(serverPort);
        if(params.containsKey("speedlimit")) {
            httpd.setSpeedLimit(Integer.parseInt(params.get("speedlimit")));
        }
        
        if(params.containsKey("forceerrorafter")) {
            httpd.setForceErrorAfter(Integer.parseInt(params.get("forceerrorafter")));
        }
        
        return true;
    }
    
    public boolean saveResults(int numPassed, int numFailed, String device, String log) {
        String filePrefix = (device == null ? "" : device + "-");
        String resultFilename = filePrefix + "result";
        String logFilename = filePrefix + "testresults.txt";
        
        String result = numFailed == 0 ? "PASS" : "FAIL";
        
        OutputStream resultOut = null;
        OutputStream logOut = null;
        boolean savedOK = false;
        try {
            File resultFile = new File(this.saveResultsDir, resultFilename);
            resultOut = new FileOutputStream(resultFile);
            resultOut.write(result.getBytes("UTF-8"));
            resultOut.flush();
            
            File logFile = new File(this.saveResultsDir, logFilename);
            logOut = new FileOutputStream(logFile);
            logOut.write(log.getBytes("UTF-8"));
            logOut.flush();
            savedOK = true;
            System.out.println("Saved results to " + resultFilename + " / " +
                    logFilename);
        }catch(IOException e) {
            System.err.println("Exception saving results:");
            e.printStackTrace();
        }finally {
            if(resultOut != null) {
                try { resultOut.close(); }
                catch(IOException e) {}
            }
            
            if(logOut != null) {
                try { logOut.close(); }
                catch(IOException e) {}
            }
        }
        return savedOK;
    }
    
    
    /**
     * Main method listening for server setup requests over HTTP
     * 
     * @param session
     * @return 
     */
    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> filesMap = new HashMap<>();
        
        try {
            if(session.getMethod().equals(NanoHTTPD.Method.POST) || session.getMethod().equals(NanoHTTPD.Method.PUT)) {
                session.parseBody(filesMap);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        Map<String, String> parms = session.getParms();
        
        System.out.println("Request: " + session.getUri());
        String action = parms.get("action");
        String message = "";
        
        
        
        if(action == null) {
            message = "DodgyHTTPDServer: see the docs for how to talk to me";
        }else {
            try {
                JSONObject response = new JSONObject();
                boolean hasFailed = false;
                if(action.equals("newserver")) {
                    response.put("status", "OK");
                    response.put("port", startNewServer());
                }else if(action.equals("startserver")) {
                    int portNum = Integer.parseInt(parms.get("port"));
                    boolean started = startServer(portNum);
                    response.put("started", started);
                }else if(action.equals("stopserver")) {
                    int portNum = Integer.parseInt(parms.get("port"));
                    boolean stopped = stopServer(portNum);
                    response.put("stopped", stopped);
                }else if(action.equals("setparams")) {                    
                    int portNum = Integer.parseInt(parms.get("port"));
                    boolean setOK = setServerParams(portNum, session.getParms());
                    response.put("set", setOK);
                }else if(action.equals("saveresults")) {
                    int numPassed = Integer.parseInt(parms.get("numPass"));
                    int numFailed = Integer.parseInt(parms.get("numFail"));
                    String device = parms.get("device");
                    String logTxt = parms.get("logtext");
                    hasFailed = !saveResults(numPassed, numFailed, device, logTxt);
                    response.put("saved", !hasFailed);
                }

                String jsonStr = response.toString();
            
                Response r = newFixedLengthResponse(
                    hasFailed ? Response.Status.INTERNAL_ERROR  : Response.Status.OK, 
                        "application/json", jsonStr);
                r.setKeepAlive(false);
                r.setGzipEncoding(false);
                return r;
            }catch(IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, 
                    "text/plain", e.toString());
            }
            
        }
        
        return newFixedLengthResponse(Response.Status.OK, "text/plain", message);
        
    }
    
    
}
