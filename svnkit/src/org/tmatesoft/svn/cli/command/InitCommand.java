/*
 * ====================================================================
 * Copyright (c) 2004-2006 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.cli.command;

import java.io.PrintStream;

import org.tmatesoft.svn.cli.SVNCommand;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNAdminClient;
import org.tmatesoft.svn.core.wc.SVNClientManager;


/**
 * @version 1.1
 * @author  TMate Software Ltd.
 */
public class InitCommand extends SVNCommand {

    public void run(PrintStream out, PrintStream err) throws SVNException {
        if (getCommandLine().hasURLs()) {
            String destURL = getCommandLine().getURL(0);
            if (matchTabsInURL(destURL, err)) {
                return;
            }
            
            String sourceURL = getCommandLine().getURL(1);
            if (matchTabsInURL(sourceURL, err)) {
                return;
            }
            
            SVNClientManager manager = getClientManager();
            SVNAdminClient adminClient = manager.getAdminClient();
            adminClient.doInitialize(SVNURL.parseURIDecoded(sourceURL), SVNURL.parseURIDecoded(destURL));
            SVNCommand.println(out, "Copied properties for revision 0.");
        }
    }
}
