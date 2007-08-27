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
package org.tmatesoft.svn.core.internal.server.dav.handlers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.server.dav.DAVResource;

/**
 * @author TMate Software Ltd.
 * @version 1.1.2
 */
public class DAVMergeInfoRequest extends DAVReportRequest {

    private static final DAVElement INHERIT = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "inherit");

    long myRevision;
    SVNMergeInfoInheritance myInherit;
    String[] myTargetPaths;

    public DAVMergeInfoRequest(Map properties) throws SVNException {
        super(MERGEINFO_REPORT, properties);

        myRevision = DAVResource.INVALID_REVISION;
        myInherit = SVNMergeInfoInheritance.EXPLICIT;
        myTargetPaths = null;

        initialize();
    }


    public long getRevision() {
        return myRevision;
    }

    private void setRevision(long revision) {
        myRevision = revision;
    }

    public SVNMergeInfoInheritance getInherit() {
        return myInherit;
    }

    private void setInherit(SVNMergeInfoInheritance inherit) {
        myInherit = inherit;
    }

    public String[] getTargetPaths() {
        return myTargetPaths;
    }

    private void setTargetPaths(String[] targetPaths) {
        myTargetPaths = targetPaths;
    }

    protected void initialize() throws SVNException {
        for (Iterator iterator = getProperties().entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            DAVElement element = (DAVElement) entry.getKey();
            DAVElementProperty property = (DAVElementProperty) entry.getValue();
            if (element == REVISION) {
                setRevision(Long.parseLong(property.getFirstValue()));
            } else if (element == INHERIT) {
                setInherit(parseInheritance(property.getFirstValue()));
                if (getInherit() != null) {
                    invalidXML();
                }
            } else if (element == PATH) {
                Collection paths = property.getValues();
                String[] targetPaths = new String[paths.size()];
                targetPaths = (String[]) paths.toArray(targetPaths);
                setTargetPaths(targetPaths);
            }
        }
    }

    //TODO: move this method to SVNMergeInfoInheritance
    private SVNMergeInfoInheritance parseInheritance(String inheritance) {
        if (SVNMergeInfoInheritance.EXPLICIT.toString().equals(inheritance)) {
            return SVNMergeInfoInheritance.EXPLICIT;
        } else if (SVNMergeInfoInheritance.INHERITED.toString().equals(inheritance)) {
            return SVNMergeInfoInheritance.INHERITED;
        } else if (SVNMergeInfoInheritance.NEAREST_ANCESTOR.toString().equals(inheritance)) {
            return SVNMergeInfoInheritance.NEAREST_ANCESTOR;
        }
        return null;
    }
}
