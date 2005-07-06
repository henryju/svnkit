/*
 * ====================================================================
 * Copyright (c) 2004 TMate Software Ltd. All rights reserved.
 * 
 * This software is licensed as described in the file COPYING, which you should
 * have received as part of this distribution. The terms are also available at
 * http://tmate.org/svn/license.html. If newer versions of this license are
 * posted there, you may use a newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core;

/**
 * @version 1.0
 * @author TMate Software Ltd.
 */
public class SVNCommitPacket {

    // Fields =================================================================

    private final String myRoot;

    private final SVNStatus[] myStatuses;

    // Setup ==================================================================

    public SVNCommitPacket(String root, SVNStatus[] statuses) {
        this.myRoot = root;
        this.myStatuses = statuses;
    }

    // Accessing ==============================================================

    public String getRoot() {
        return myRoot;
    }

    public SVNStatus[] getStatuses() {
        return myStatuses;
    }
}