package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

public interface ISvnDiffGenerator {

    void setEncoding(String encoding);

    String getEncoding();

    String getGlobalEncoding();

    void setEOL(byte[] eol);

    byte[] geEOL();

    void displayDeletedDirectory(String displayPath, String revision1, String revision2, OutputStream outputStream) throws SVNException;

    void displayAddedDirectory(String displayPath, String revision1, String revision2, OutputStream outputStream) throws SVNException;

    void displayPropDiff(String displayPath, boolean dirWasAdded, SVNProperties originalProps, SVNProperties propChanges, OutputStream myResult) throws SVNException;

    void displayContentChanged(String displayPath, File leftFile, File rightFile, String revision1, String revision2, String mimeType1, String mimeType2, SvnDiffCallback.OperationKind operation, File copyFromPath, OutputStream outputStream) throws SVNException;
}