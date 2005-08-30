/*
 * ====================================================================
 * Copyright (c) 2004 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://tmate.org/svn/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package org.tmatesoft.svn.core.internal.io.dav;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;


/**
 * @version 1.0
 * @author  TMate Software Ltd.
 */
public class DAVRepositoryFactory extends SVNRepositoryFactory {
    
    public static void setup() {
        setup(IHTTPConnectionFactory.DEFAULT);
    }

    public static void setup(IHTTPConnectionFactory connectionFactory) {
        if (!SVNRepositoryFactory.hasRepositoryFactory("^https?://.*$")) {
            connectionFactory = connectionFactory == null ? IHTTPConnectionFactory.DEFAULT : connectionFactory;
            DAVRepositoryFactory factory = new DAVRepositoryFactory(connectionFactory);
            SVNRepositoryFactory.registerRepositoryFactory("^https?://.*$", factory);
        }
    }

    private IHTTPConnectionFactory myConnectionFactory;
    
    private DAVRepositoryFactory(IHTTPConnectionFactory connectionFactory) {
        myConnectionFactory = connectionFactory;
    }

    public SVNRepository createRepositoryImpl(SVNURL location, boolean session) {
        return new DAVRepository(myConnectionFactory, location, session);
    }
}
