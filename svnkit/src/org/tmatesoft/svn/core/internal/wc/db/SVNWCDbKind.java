/*
 * ====================================================================
 * Copyright (c) 2004-2009 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.db;

import org.tmatesoft.svn.core.SVNNodeKind;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNWCDbKind {

    public static final SVNWCDbKind DIR = new SVNWCDbKind("dir");
    public static final SVNWCDbKind FILE = new SVNWCDbKind("file");
    public static final SVNWCDbKind SYMLINK = new SVNWCDbKind("symlink");
    public static final SVNWCDbKind UNKNOWN = new SVNWCDbKind("unknown");    
    public static final SVNWCDbKind SUBDIR = new SVNWCDbKind("subdir");    
    
    private String myName;
    
    private SVNWCDbKind(String name) {
        myName = name;
    }
    
    public String toString() {
        return myName;
    }
    
    public static SVNWCDbKind parseKind(String kind) {
        if (FILE.myName.equals(kind)) {
            return FILE;
        } else if (DIR.myName.equals(kind)) {
            return DIR;
        } else if (SYMLINK.myName.equals(kind)) {
            return SYMLINK;
        } else if (SUBDIR.myName.equals(kind)) {
            return SUBDIR;
        }
        return UNKNOWN;
    }
    
    public static SVNNodeKind convertWCDbKind(SVNWCDbKind kind) {
        if (kind == DIR) {
            return SVNNodeKind.DIR;
        } else if (kind == FILE) {
            return SVNNodeKind.FILE;
        } else if (kind == SYMLINK) {
            return SVNNodeKind.FILE;
        } 
        return SVNNodeKind.UNKNOWN;
    }
}
