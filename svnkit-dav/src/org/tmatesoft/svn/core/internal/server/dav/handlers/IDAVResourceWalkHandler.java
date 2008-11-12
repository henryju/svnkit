/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.server.dav.handlers;

import org.tmatesoft.svn.core.internal.server.dav.DAVException;
import org.tmatesoft.svn.core.internal.server.dav.DAVResource;


/**
 * @version 1.2.0
 * @author  TMate Software Ltd.
 */
public interface IDAVResourceWalkHandler {

    public DAVResponse handleResource(DAVResource resource, CallType callType) throws DAVException;
    
    public static class CallType {
        public static final CallType MEMBER = new CallType();
        public static final CallType COLLECTION = new CallType();
        public static final CallType LOCKNULL = new CallType();
        
        private CallType() {
        }
    }
}
