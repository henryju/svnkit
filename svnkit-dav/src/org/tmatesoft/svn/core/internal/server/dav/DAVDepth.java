/*
 * ====================================================================
 * Copyright (c) 2004-2007 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.server.dav;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public class DAVDepth {
    
    public static final DAVDepth DEPTH_ZERO = new DAVDepth(0, "0");
    public static final DAVDepth DEPTH_ONE = new DAVDepth(1, "1");
    public static final DAVDepth DEPTH_INFINITY = new DAVDepth(-1, "Infinity");
    
    private int myID;
    private String myName;
    
    private DAVDepth(int id, String name) {
        myID = id;
        myName = name;
    }
    
    public int getID() {
        return myID;
    }
    
    public String toString() {
        return myName;
    }
    
    public static DAVDepth parseDepth(String depth) {
        if (DAVDepth.DEPTH_INFINITY.toString().equals(depth)) {
            return DAVDepth.DEPTH_INFINITY;
        } else if (DAVDepth.DEPTH_ZERO.toString().equals(depth)) {
            return DAVDepth.DEPTH_ZERO;
        } else if (DAVDepth.DEPTH_ONE.toString().equals(depth)) {
            return DAVDepth.DEPTH_ONE;
        }
        return null;
    }
}
