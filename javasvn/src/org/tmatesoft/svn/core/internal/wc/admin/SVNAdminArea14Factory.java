/*
 * ====================================================================
 * Copyright (c) 2004-2006 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://tmate.org/svn/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

public class SVNAdminArea14Factory extends SVNAdminAreaFactory {
    private static final int WC_FORMAT = 8;

    static {
        SVNAdminAreaFactory.registerFactory(new SVNAdminArea14Factory());
    }
    
    protected SVNAdminArea doOpen(File path, int version) throws SVNException {
        if (version != WC_FORMAT) {
            return null;
        }
        return new SVNAdminArea14(path);
    }

    protected SVNAdminArea doUpgrade(SVNAdminArea adminArea) throws SVNException {
        if (adminArea == null || adminArea.getClass() == SVNAdminArea14.class) {
            return adminArea;
        }
        SVNAdminArea newestAdminArea = new SVNAdminArea14(adminArea.getRoot());
        return newestAdminArea.upgradeFormat(adminArea);
    }

    protected int getSupportedVersion() {
        return WC_FORMAT;
    }

    protected SVNAdminArea doCreateVersionedDirectory(File dir) throws SVNException {
        SVNAdminArea adminArea = new SVNAdminArea14(dir);
        return adminArea.createVersionedDirectory(); 
    }

    protected int getVersion(File path) throws SVNException {
        File adminDir = new File(path, SVNFileUtil.getAdminDirectoryName());
        File entriesFile = new File(adminDir, "entries");
        int formatVersion = -1;

        BufferedReader reader = null;
        String line = null;
        boolean readFormatFile = false;
    

        if (!entriesFile.isFile() || !entriesFile.canRead()) {
            readFormatFile = true;
        } else {
            try {
                reader = new BufferedReader(new InputStreamReader(SVNFileUtil.openFileForReading(entriesFile), "UTF-8"));
                line = reader.readLine();
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read entries file ''{0}'': {1}", new Object[] {entriesFile, e.getLocalizedMessage()});
                SVNErrorManager.error(err, e);
            } finally {
                SVNFileUtil.closeFile(reader);
            }
            readFormatFile = line != null ? false : true;
        }
    
        if (!readFormatFile) {
            try {
                formatVersion = Integer.parseInt(line.trim());
                return formatVersion;
            } catch (NumberFormatException e) {
                readFormatFile = true;
            }
        }
        
        if (readFormatFile) {
            File formatFile = new File(adminDir, "format");
            try {
                reader = new BufferedReader(new InputStreamReader(SVNFileUtil.openFileForReading(formatFile), "UTF-8"));
                line = reader.readLine();
                formatVersion = Integer.parseInt(line.trim());
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read format file ''{0}'': {1}", new Object[] {formatFile, e.getLocalizedMessage()});
                SVNErrorManager.error(err, e);
            } catch (NumberFormatException nfe) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy: {1}", new Object[] {path, nfe.getLocalizedMessage()});
                SVNErrorManager.error(err, nfe);
            } catch (SVNException svne) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy", path);
                err.setChildErrorMessage(svne.getErrorMessage());
                SVNErrorManager.error(err, svne);
            } finally {
                SVNFileUtil.closeFile(reader);
            }
        }
        return formatVersion;
    }
}
