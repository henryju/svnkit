/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;


/**
 * @version 1.2.0
 * @author  TMate Software Ltd.
 */
public class SVNConfigFile {

    private File myFile;
    private String[] myLines;
    private long myLastModified;

    public SVNConfigFile(File file) {
        myFile = file.getAbsoluteFile();
    }
    
    protected String[] getLines() {
        return myLines;
    }

    public Map getProperties(String groupName) {
        Map map = new SVNHashMap();
        load();
        boolean groupMatched = false;
        for (int i = 0; i < myLines.length; i++) {
            String line = myLines[i];
            if (line == null) {
                continue;
            }
            if (!groupMatched && matchGroup(line, groupName)) {
                groupMatched = true;
            } else if (groupMatched) {
                if (matchGroup(line, null)) {
                    return map;
                } else if (matchProperty(line, null)) {
                    map.put(getPropertyName(line), getPropertyValue(line));
                }
            }
        }
        return map;
    }

    public String getPropertyValue(String groupName, String propertyName) {
        load();
        boolean groupMatched = false;
        for (int i = 0; i < myLines.length; i++) {
            String line = myLines[i];
            if (line == null) {
                continue;
            }
            if (!groupMatched && matchGroup(line, groupName)) {
                groupMatched = true;
            } else if (groupMatched) {
                if (matchGroup(line, null)) {
                    return null;
                } else if (matchProperty(line, propertyName)) {
                    return getPropertyValue(line);
                }
            }
        }
        return null;
    }

    public void setPropertyValue(String groupName, String propertyName, String propertyValue, boolean save) {
        load();
        boolean groupMatched = false;
        for (int i = 0; i < myLines.length; i++) {
            String line = myLines[i];
            if (line == null) {
                continue;
            }
            if (!groupMatched && matchGroup(line, groupName)) {
                groupMatched = true;
            } else if (groupMatched) {
                if (matchGroup(line, null) /* or last line found*/) {
                    // property was not saved!!!
                    if (propertyValue != null) {
                        String[] lines = new String[myLines.length + 1];
                        System.arraycopy(myLines, 0, lines, 0, i);
                        System.arraycopy(myLines, i, lines, i + 1,
                                myLines.length - i);
                        lines[i] = propertyName + "  = " + propertyValue;
                        myLines = lines;
                        if (save) {
                            save();
                        }
                    }

                    return;
                } else if (matchProperty(line, propertyName)) {
                    if (propertyValue == null) {
                        myLines[i] = null;
                    } else {
                        myLines[i] = propertyName + " = " + propertyValue;
                    }
                    if (save) {
                        save();
                    }
                    return;
                } 
            }
        }
        if (propertyValue != null) {
            
            String[] lines = new String[myLines.length + (groupMatched ? 1 : 2)];
            if (!groupMatched) {
                lines[lines.length - 2] = "[" + groupName + "]";
            }
            lines[lines.length - 1] = propertyName + "  = " + propertyValue;
            System.arraycopy(myLines, 0, lines, 0, myLines.length);
            myLines = lines;
            if (save) {
                save();
            }
        }
    }

    private static boolean matchGroup(String line, String name) {
        line = line.trim();
        if (line.startsWith("[") && line.endsWith("]")) {
            return name == null || line.substring(1, line.length() - 1).equals(name);
        }
        return false;
    }

    private static boolean matchProperty(String line, String name) {
        line = line.trim();
        if (line.startsWith("#")) {
            return false;
        }
        if (line.indexOf('=') < 0) {
            return false;
        }
        line = line.substring(0, line.indexOf('='));
        return name == null || line.trim().equals(name);
    }

    private static String getPropertyValue(String line) {
        line = line.trim();
        if (line.indexOf('=') < 0) {
            return null;
        }
        line = line.substring(line.indexOf('=') + 1);
        return line.trim();
    }

    private static String getPropertyName(String line) {
        line = line.trim();
        if (line.indexOf('=') < 0) {
            return null;
        }
        line = line.substring(0, line.indexOf('='));
        return line.trim();
    }

    // parse all lines from the file, keep them as lines array.
    public void save() {
        if (myLines == null) {
            return;
        }
        if (myFile.isDirectory()) {
            return;
        }
        if (myFile.getParentFile() != null) {
            myFile.getParentFile().mkdirs();
        }
        Writer writer = null;
        String eol = System.getProperty("line.separator");
        eol = eol == null ? "\n" : eol;
        try {
            writer = new FileWriter(myFile);
            for (int i = 0; i < myLines.length; i++) {
                String line = myLines[i];
                if (line == null) {
                    continue;
                }
                writer.write(line);
                writer.write(eol);
            }
        } catch (IOException e) {
            //
        } finally {
            SVNFileUtil.closeFile(writer);
        }
        myLastModified = myFile.lastModified();
        myLines = doLoad(myFile);
    }

    private void load() {
        if (myLines != null && myFile.lastModified() == myLastModified) {
            return;
        }
        myLastModified = myFile.lastModified();
        myLines = doLoad(myFile);
        myLastModified = myFile.lastModified();
    }

    public boolean isModified() {
        if (myLines == null) {
            return false;
        }
        String[] lines = doLoad(myFile);
        if (lines.length != myLines.length) {
            return true;
        }
        for (int i = 0; i < myLines.length; i++) {
            String line = myLines[i];
            if (line == null) {
                return true;
            }
            if (!line.equals(lines[i])) {
                return true;
            }
        }
        return false;
    }

    private String[] doLoad(File file) {
        if (!file.isFile() || !file.canRead()) {
            return new String[0];
        }
        BufferedReader reader = null;
        Collection lines = new ArrayList();
        try {
            reader = new BufferedReader(new FileReader(myFile));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            lines.clear();
        } finally {
            SVNFileUtil.closeFile(reader);
        }
        return (String[]) lines.toArray(new String[lines.size()]);
    }
    
    public static void createDefaultConfiguration(File configDir) {
        if (!configDir.isDirectory()) {
            if (!configDir.mkdirs()) {
                return;
            }
        }
        File configFile = new File(configDir, "config");
        File serversFile = new File(configDir, "servers");
        File readmeFile = new File(configDir, "README.txt");
        
        writeFile("/org/tmatesoft/svn/core/internal/wc/config/config", configFile);
        writeFile("/org/tmatesoft/svn/core/internal/wc/config/servers", serversFile);
        writeFile("/org/tmatesoft/svn/core/internal/wc/config/README.txt", readmeFile);
    }

    private static void writeFile(String url, File configFile) {
        if (url == null || configFile == null || configFile.exists()) {
            return;
        }
        InputStream resource = SVNConfigFile.class.getResourceAsStream(url);
        if (resource == null) {
            return;
        }
        BufferedReader is = new BufferedReader(new InputStreamReader(resource));
        String eol = System.getProperty("line.separator", "\n");
        Writer os = null;
        try {
            os = new BufferedWriter(new OutputStreamWriter(SVNFileUtil.openFileForWriting(configFile)));
            String line;
            while((line = is.readLine()) != null) {
                os.write(line);
                os.write(eol);
            }
        } catch (IOException e) {
            //
        } catch (SVNException e) {
            //
        } finally {
            SVNFileUtil.closeFile(os);
            SVNFileUtil.closeFile(is);
        }
    }
}
