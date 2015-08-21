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
import fi.iki.elonen.SimpleWebServer;
import java.io.File;
import java.io.InputStream;

/**
 * Simple HTTP server that wraps responses using DodgyInputStream .  Used by 
 * clients to test their ability to react to errors.
 * 
 * @author mike
 */
public class DodgyHTTPD extends SimpleWebServer {
    
    private int speedLimit;
    
    private int forceErrorAfter;
    
    /**
     * Creates a new server instance
     * 
     * @param portnum portnum to listen on
     * @param baseDir Base directory to serve files from
     */
    public DodgyHTTPD(int portnum, File baseDir) {
        super(null, portnum, baseDir, true);        
        speedLimit = 0;
        forceErrorAfter = 0;
    }

    /**
     * Gets the speed limit
     * 
     * @return speed limit of replies in bytes per second 0 = no limit
     */
    public int getSpeedLimit() {
        return speedLimit;
    }
    
    /**
     * Sets the speed limit
     * 
     * @param speedLimit speed limit for replies in bytes per second 0 = no limit
     */
    public void setSpeedLimit(int speedLimit) {
        this.speedLimit = speedLimit;
    }

    /**
     * Get force error setting
     * 
     * @return number of bytes after which an error is forced to happen 0 = none
     */
    public int getForceErrorAfter() {
        return forceErrorAfter;
    }

    /**
     * Set the force error setting
     * 
     * @param forceErrorAfter number of bytes after which an error will be forced to happen 0 = none
     */
    public void setForceErrorAfter(int forceErrorAfter) {
        this.forceErrorAfter = forceErrorAfter;
    }
    
    @Override
    public Response newFixedLengthResponse(Response.IStatus status, String mimeType, InputStream data, long totalBytes) {
        InputStream newIn = new DodgyInputStream(data, speedLimit, forceErrorAfter);
        return super.newFixedLengthResponse(status, mimeType, newIn, totalBytes); //To change body of generated methods, choose Tools | Templates.
    }
    
}