package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.SVNPropertyData;

/**
 * Represents set property operation. 
 * Sets <code>propertyName</code> to <code>propertyValue</code> on target 
 * or
 * sets <code>propertyName</code> to <code>propertyValue</code> on 
 * <code>revision</code> in the repository represented by target.
 * A <code>propertyValue</code> of <code>null</code> will
 * delete the property.
 * 
 * <p/>
 * If <code>propertyName</code> is an svn-controlled property (i.e. prefixed
 * with <code>"svn:"</code>), then the caller is
 * responsible for ensuring that the value is UTF8-encoded and uses LF
 * line-endings.
 * 
 * <p/>
 * <b>If it is the property of the target:</b>
 * 
 * <p/>
 * Target should represent working copy path. 
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#EMPTY},
 * set the property on target only; if {@link SVNDepth#FILES},
 * set it on target and its file children (if any); if
 * {@link SVNDepth#IMMEDIATES}, on target and all of its
 * immediate children (both files and directories); if
 * {@link SVNDepth#INFINITY}, on target and everything beneath
 * it.
 * 
 * <p/>
 * <code>changeLists</code> is a collection of <code>String</code>
 * changelist names, used as a restrictive filter on items whose properties
 * are set; that is, don't set properties on any item unless it's a member
 * of one of those changelists. If <code>changelists</code> is empty (or
 * <span class="javakeyword">null</span>), no changelist filtering occurs.
 *  * 
 * <p>
 * {@link #run()} methods operates only on working copies and does not open any network
 * connection.
 * 
 * <p/>
 * <b>If it is the property of revision</b>:
 * 
 * <p/>
 * Target can be either URL or working copy path. 
 * If target is working copy path, repository URL is obtained from this.
 * 
 * <p/> 
 * <code>Revision must be set</code>.
 * 
 * <p/>
 * The {@link ISVNAuthenticationManager authentication
 * manager}, either provided by a caller or a default one, will be used for
 * authentication. 
 * 
 * <p/>
 * Although this routine accepts a working copy path it doesn't affect the
 * working copy at all; it's a pure network operation that changes an
 * *unversioned* property attached to a revision. This can be used to tweak
 * log messages, dates, authors, and the like. Be careful: it's a lossy
 * operation.
 * 
 * <p>
 * Also note that unless the administrator creates a pre-revprop-change hook
 * in the repository, this feature will fail.
 * 
 * {@link #run()} return {@link SVNPropertyData} information of the property
 * This method throws {@link SVNException} if one of the following is true:
 *             <ul>
 *             <li>target does not exist 
 *             <li>exception with
 *             {@link SVNErrorCode#CLIENT_PROPERTY_NAME} error code - if
 *             <code>propName</code> is a revision property name or not a
 *             valid property name or not a regular property name (one
 *             starting with a <span class="javastring">"svn:entry"</span>
 *             or <span class="javastring">"svn:wc"</span> prefix)
 *             </ul>
 *             
 * @author TMate Software Ltd. 
 * @version 1.7            
 */
public class SvnSetProperty extends SvnReceivingOperation<SVNPropertyData> {
    
    private boolean force;
    private boolean revisionProperty;
    private String propertyName;
    private SVNPropertyValue propertyValue;

    protected SvnSetProperty(SvnOperationFactory factory) {
        super(factory);
    }
    
	/**
	* Sets whether to skip validity checking.
	* 
	* @return <code>true</code> if validity checking should not be done, otherwise <code>false</code>
	* @see #setForce(boolean)
	*/
    public boolean isForce() {
        return force;
    }
    
    /**
    * Sets whether to skip validity checking.
    *  
    * <p/>
    * For target property:
    * If <code>force</code> is <code>true</code>, no validity checking is done. 
    * But if <code>force</code> is <code>false</code>, and <code>propertyName</code> is not a
    * valid property for target, {@link SVNException} is thrown, either with
    * an error code {@link org.tmatesoft.svn.core.SVNErrorCode#ILLEGAL_TARGET}
    * (if the property is not appropriate for target), or with
    * {@link SVNErrorCode#BAD_MIME_TYPE} (if
    * <code>propertyName</code> is <span class="javastring">"svn:mime-type"</span>,
    * but <code>propertyValue</code> is not a valid mime-type).
    * 
    * <p/>
    * For revision property:
    * If <code>force</code> is <code>true</code> new lines in the author property are allowed.
    * 
    * @param force <code>true</code> if validity checking should not be done, otherwise <code>false</code>
    */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Returns property value.
     * 
     * @return value of the property
     */
    public SVNPropertyValue getPropertyValue() {
        return propertyValue;
    }

    /**
     * Sets property value
     * 
     * @param propertyValue value of the property
     */
    public void setPropertyValue(SVNPropertyValue propertyValue) {
        this.propertyValue = propertyValue;
    }

    /**
     * Returns property name.
     * 
     * @return name of the property
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets property name
     * 
     * @param propertyName name of the property
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        
        if (getDepth() == null || getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.EMPTY);
        }
    }

    /**
     * Sets whether it is revision property.
     * 
     * @param revisionProperty <code>true</code> if it is revision property, <code>true</code> if it is target property
     */
    public void setRevisionProperty(boolean revisionProperty) {
        this.revisionProperty = revisionProperty;
    }

    /**
     * Gets whether it is revision property.
     * 
     * @return <code>true</code> if it is revision property, <code>true</code> if it is target property
     */
    public boolean isRevisionProperty() {
        return revisionProperty;
    }
}
