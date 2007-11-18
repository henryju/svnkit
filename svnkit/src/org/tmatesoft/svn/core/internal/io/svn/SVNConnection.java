/*
 * ====================================================================
 * Copyright (c) 2004-2007 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package org.tmatesoft.svn.core.internal.io.svn;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;

/**
 * @version 1.1.1
 * @author  TMate Software Ltd.
 */
class SVNConnection {

    private final ISVNConnector myConnector;
    private String myRealm;
    private String myRoot;
    private OutputStream myOutputStream;
    private InputStream myInputStream;
    private SVNRepositoryImpl myRepository;
    private boolean myIsSVNDiff1;
    private boolean myIsCommitRevprops;
    private boolean myIsMergeInfo;
    private boolean myIsReopening = false;
    private boolean myIsCredentialsReceived = false;
    private InputStream myLoggingInputStream;

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";
    private static final String STEP = "step";
    private static final String EDIT_PIPELINE = "edit-pipeline";
    private static final String SVNDIFF1 = "svndiff1";
    private static final String ABSENT_ENTRIES = "absent-entries";
    private static final String COMMIT_REVPROPS = "commit-revprops";
    private static final String MERGE_INFO = "mergeinfo";
    private static final String DEPTH = "depth";
    private static final String LOG_REVPROPS = "log-revprops";

    public SVNConnection(ISVNConnector connector, SVNRepositoryImpl repository) {
        myConnector = connector;
        myRepository = repository;
    }

    public void open(SVNRepositoryImpl repository) throws SVNException {
        myIsReopening = true;
        try {
            myIsCredentialsReceived = false;
            myConnector.open(repository);
            myRepository = repository;
            handshake(repository);
        } finally {
            myIsReopening = false;
        }
    }

    public String getRealm() {
        return myRealm;
    }
    
    public boolean isSVNDiff1() {
        return myIsSVNDiff1;
    }

    public boolean isCommitRevprops() {
        return myIsCommitRevprops;
    }

    public boolean isMergeInfo() {
        return myIsMergeInfo;
    }
    
    private InputStream skipLeadingGrabage() throws SVNException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
        // buffer should be large enough to receive complete greeting. 
        byte[] bytes = new byte[8192];
        int r = 0;
        try {
            r = getInputStream().read(bytes);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Handshake failed: ''{0}''", e.getMessage());
            SVNErrorManager.error(err);
        }
        if (r >= 0) {
            buffer.write(bytes, 0, r);
        }
        bytes = buffer.toByteArray();
        // look for '( '.
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == '(' && bytes[i + 1] == ' ') {
                return new ByteArrayInputStream(bytes, i, bytes.length - i);
            }
        }
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Handshake failed, received: ''{0}''", new String(bytes));
        SVNErrorManager.error(err);
        return null;
    }

    protected void handshake(SVNRepositoryImpl repository) throws SVNException {
        checkConnection();
        InputStream is = skipLeadingGrabage();
        List items = null;
        try {
            items = SVNReader.parse(is, "nnll", null);
        } catch (SVNException e) {
            handleIOError(e, true);
        } finally {
            myRepository.getDebugLog().flushStream(myLoggingInputStream);
        }
        Long minVer = (Long) items.get(0);
        Long maxVer = (Long) items.get(1);
        if (minVer.longValue() > 2) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_BAD_VERSION, "Server requires minimum version {0,number,integer}", minVer));
        } else if (maxVer.longValue() < 2) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_BAD_VERSION, "Server requires maximum version {0,number,integer}", maxVer));
        }
        if (!SVNReader.hasValue(items, 3, EDIT_PIPELINE)) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_BAD_VERSION, "Only servers with 'edit-pipeline' capability is supported"));
        }
        myIsSVNDiff1 = SVNReader.hasValue(items, 3, SVNDIFF1);
        myIsCommitRevprops = SVNReader.hasValue(items, 3, COMMIT_REVPROPS);
        myIsMergeInfo = SVNReader.hasValue(items, 3, MERGE_INFO);
        write("(n(wwwwww)s)", new Object[]{"2", EDIT_PIPELINE, SVNDIFF1, ABSENT_ENTRIES, DEPTH, MERGE_INFO, LOG_REVPROPS,
                repository.getLocation().toString()});
    }

    public void authenticate(SVNRepositoryImpl repository) throws SVNException {
        SVNErrorMessage failureReason = null;
        List items = read("lc", null, true);
        List mechs = SVNReader.getList(items, 0);
        myRealm = SVNReader.getString(items, 1);
        if (mechs == null || mechs.size() == 0) {
            receiveRepositoryCredentials(repository);
            return;
        }
        ISVNAuthenticationManager authManager = myRepository.getAuthenticationManager();
        if (authManager != null && authManager.isAuthenticationForced() && mechs.contains("ANONYMOUS") && mechs.contains("CRAM-MD5")) {
            mechs.remove("ANONYMOUS");
        }
        SVNURL location = myRepository.getLocation();
        SVNPasswordAuthentication auth = null;
        if (repository.getExternalUserName() != null && mechs.contains("EXTERNAL")) {
            write("(w(s))", new Object[]{"EXTERNAL", repository.getExternalUserName()});
            failureReason = readAuthResponse();
        } else if (mechs.contains("ANONYMOUS")) {
            write("(w())", new Object[]{"ANONYMOUS"});
            failureReason = readAuthResponse();
        } else if (mechs.contains("CRAM-MD5")) {
            while (true) {
                CramMD5 authenticator = new CramMD5();
                String realm = getRealm();
                if (location != null) {
                    realm = "<" + location.getProtocol() + "://"
                            + location.getHost() + ":"
                            + location.getPort() + "> " + realm;
                }
                if (auth == null && authManager != null) {
                    auth = (SVNPasswordAuthentication) authManager.getFirstAuthentication(ISVNAuthenticationManager.PASSWORD, realm, location);
                } else if (authManager != null) {
                    authManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.PASSWORD, realm, failureReason, auth);
                    auth = (SVNPasswordAuthentication) authManager.getNextAuthentication(ISVNAuthenticationManager.PASSWORD, realm, location);
                }
                if (auth == null || auth.getUserName() == null || auth.getPassword() == null) {
                    failureReason = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Can't get password. Authentication is required for ''{0}''", realm);
                    break;
                }
                write("(w())", new Object[]{"CRAM-MD5"});
                while (true) {
                    authenticator.setUserCredentials(auth);
                    items = readTuple("w(?c)", true);
                    String status = SVNReader.getString(items, 0);
                    if (SUCCESS.equals(status)) {
                        authManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.PASSWORD, realm, null, auth);
                        receiveRepositoryCredentials(repository);
                        return;
                    } else if (FAILURE.equals(status)) {
                        failureReason = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Authentication error from server: {0}", SVNReader.getString(items, 1));
                        break;
                    } else if (STEP.equals(status)) {
                        try {
                            byte[] response = authenticator.buildChallengeResponse(SVNReader.getBytes(items, 1));
                            getOutputStream().write(response);
                            getOutputStream().flush();
                        } catch (IOException e) {
                            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getMessage()), e);
                        }
                    }
                }
            }
        } else {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Cannot negotiate authentication mechanism"));
        }
        if (failureReason == null) {
            receiveRepositoryCredentials(repository);
            return;
        }
        SVNErrorManager.error(failureReason);
    }

    private void receiveRepositoryCredentials(SVNRepositoryImpl repository) throws SVNException {
        if (myIsCredentialsReceived) {
            return;
        }
        List creds = read("c?c", null, true);
        myIsCredentialsReceived = true;
        if (creds != null && creds.size() == 2 && creds.get(0) != null && creds.get(1) != null) {
            SVNURL rootURL = creds.get(1) != null ? SVNURL.parseURIEncoded(SVNReader.getString(creds, 1)) : null;
            if (rootURL != null && rootURL.toString().length() > repository.getLocation().toString().length()) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Impossibly long repository root from server"));
            }
            if (repository != null && repository.getRepositoryRoot(false) == null) {
                repository.updateCredentials(SVNReader.getString(creds, 0), rootURL);
            }
            if (myRealm == null) {
                myRealm = SVNReader.getString(creds, 0);
            }
            if (myRoot == null) {
                myRoot = SVNReader.getString(creds, 1);
            }
        }
    }

    private SVNErrorMessage readAuthResponse() throws SVNException {
        List items = readTuple("w(?c)", true);
        if (SUCCESS.equals(SVNReader.getString(items, 0))) {
            return null;
        } else if (FAILURE.equals(SVNReader.getString(items, 0))) {
            return SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Authentication error from server: {0}", SVNReader.getString(items, 1));
        }
        return SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Unexpected server response to authentication");
    }

    public void close() throws SVNException {
        myInputStream = null;
        myLoggingInputStream = null;
        myOutputStream = null;
        myConnector.close(myRepository);
    }

    public List read(String template, List items, boolean readMalformedData) throws SVNException {
        try {
            checkConnection();
            return SVNReader.parse(getInputStream(), template, items);
        } catch (SVNException e) {
            handleIOError(e, readMalformedData);
            return null;
        } finally {
            myRepository.getDebugLog().flushStream(myLoggingInputStream);
        }
    }

    public List readTuple(String template, boolean readMalformedData) throws SVNException {
        try {
            checkConnection();
            return SVNReader.readTuple(getInputStream(), template);
        } catch (SVNException e) {
            handleIOError(e, readMalformedData);
            return null;
        } finally {
            myRepository.getDebugLog().flushStream(myLoggingInputStream);
        }        
    }

    public SVNItem readItem(boolean readMalformedData) throws SVNException {
        try {
            checkConnection();
            return SVNReader.readItem(getInputStream());
        } catch (SVNException e) {
            handleIOError(e, readMalformedData);
            return null;
        } finally {
            myRepository.getDebugLog().flushStream(myLoggingInputStream);
        }
    }

    private void handleIOError(SVNException e, boolean readMalformedData) throws SVNException {
        if (readMalformedData && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_SVN_MALFORMED_DATA) {
                byte[] malfored = new byte[1024];
                try {
                    getInputStream().read(malfored);
                } catch (IOException e1) {
                    //
                }
            }
            throw e;        
    }
    
    public void write(String template, Object[] items) throws SVNException {
        try {
            SVNWriter.write(getOutputStream(), template, items);
        } finally {
            try {
                getOutputStream().flush();
            } catch (IOException e) {
                //
            } catch (SVNException e) {
                //
            }
            myRepository.getDebugLog().flushStream(getOutputStream());
        }
    }
    
    public boolean isConnectionStale() {
        return myConnector.isStale();
    }

    private void checkConnection() throws SVNException {
        if (!myIsReopening && !myConnector.isConnected(myRepository)) {
            myIsReopening = true;
            try {
                close();
                open(myRepository);
            } finally {
                myIsReopening = false;
            }
        }
    }

    public OutputStream getOutputStream() throws SVNException {
        if (myOutputStream == null) {
            try {
                myOutputStream = myRepository.getDebugLog().createLogStream(myConnector.getOutputStream());
            } catch (IOException e) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getMessage()), e);
            }
        }
        return myOutputStream;
    }

    public InputStream getInputStream() throws SVNException {
        if (myInputStream == null) {
            try {
                myInputStream = myRepository.getDebugLog().createLogStream(new BufferedInputStream(myConnector.getInputStream()));
                myLoggingInputStream = myInputStream;
            } catch (IOException e) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getMessage()), e);
            }
        }
        return myInputStream;
    }
}