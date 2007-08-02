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
package org.tmatesoft.svn.cli2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public class SVNCommandLine {
    
    private static final Map ourOptions = new HashMap();
    
    public static void registerOption(SVNOption option) {
        ourOptions.put("--" + option.getName(), option);
        ourOptions.put("-" + option.getAlias(), option);
    }

    private String myCommand;
    private Collection myArguments;
    private Map myOptions;

    public SVNCommandLine() {
        myArguments = new LinkedList();
        myOptions = new HashMap();
    }

    public void init(String[] args) throws SVNException {
        for (int i = 0; i < args.length; i++) {
           String value = args[i];
           if (ourOptions.containsKey(value)) {
               SVNOption option = (SVNOption) ourOptions.get(value);
               String parameter = null;
               if (!option.isUnary()) {
                   if (i + 1 < args.length) {
                       parameter = args[i + 1];
                       i++;
                   } else {
                       SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CL_INSUFFICIENT_ARGS, "missing argument: {0}", value);
                       SVNErrorManager.error(err);
                   }
               }
               myOptions.put(option, new SVNOptionValue(option, value, parameter));
           } else if (myCommand == null) {
               myCommand = value;
           } else {
               myArguments.add(value);
           }
        }
    }

    public boolean hasOption(SVNOption option) {
        return myOptions.containsKey(option);
    }
    
    public String getOptionValue(SVNOption option) {
        if (hasOption(option)) {
            return ((SVNOptionValue) myOptions.get(option)).getValue();
        }
        return null;
    }
    
    public Iterator optionValues() {
        return myOptions.values().iterator();
    }

    public String getCommandName() {
        return myCommand;
    }
    
    public String[] getArguments() {
        return (String[]) myArguments.toArray(new String[myArguments.size()]);
    }

}
