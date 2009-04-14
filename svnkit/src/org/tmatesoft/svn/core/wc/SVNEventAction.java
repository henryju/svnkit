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
package org.tmatesoft.svn.core.wc;

/**
 * The <b>SVNEventAction</b> class is used to describe an action 
 * which generated an <b>SVNEvent</b> object. 
 * <p>
 * Each operation invoked by 
 * a do*() method of an <b>SVN</b>*<b>Client</b> class consists of 
 * several actions that can be considered as operation steps. For example,      
 * an update operation receives changes for files, adds new ones, deletes
 * another ones and so on. And for every such action (for every file
 * updated, deleted, added, etc.) the 
 * {@link SVNUpdateClient#doUpdate(java.io.File, SVNRevision, org.tmatesoft.svn.core.SVNDepth, boolean, boolean) doUpdate()}
 * method generates an <b>SVNEvent</b> objects which contains information
 * on the type of this action that can be retrieved simply calling
 * the <b>SVNEvent</b>'s {@link SVNEvent#getAction() getAction()} method:
 * <pre class="javacode">
 * <span class="javakeyword">import</span> org.tmatesoft.svn.core.wc.SVNEvent;
 * <span class="javakeyword">import</span> org.tmatesoft.svn.core.wc.SVNEventAction;
 * ...
 *   
 *   SVNEventAction action = event.getAction();
 *   <span class="javacomment">//parse the action according to the type of</span> 
 *   <span class="javacomment">//operation and your needs</span>
 *   <span class="javakeyword">if</span> (action == SVNEventAction.UPDATE_UPDATE){
 *       ...
 *   }
 *   ...</pre>
 * <p>
 * <b>SVNEventAction</b> is just a set of predefined constant fields of
 * the same type. Each constant is applicable only to a certain type
 * of operation - for example those constants that names start with the 
 * <i>UPDATE_</i> prefix are relevant only for update related operations
 * (update, checkout, switch, etc.). 
 *  
 * @version 1.2.0
 * @author  TMate Software Ltd.
 * @see     SVNEvent
 * @see     ISVNEventHandler
 * @see     <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public class SVNEventAction {

    private int myID;
    private String myName;

    private SVNEventAction(int id, String name) {
        myID = id;
        myName = name;
    }

    /**
     * Returns this object's identifier.
     * Each constant field of the <b>SVNEventAction</b> class is also an 
     * <b>SVNEventAction</b> object with its own id. 
     * 
     * @return id of this object 
     */
    public int getID() {
        return myID;
    }

    /**
     * Returns a string representation of this object. 
     * As a matter of fact this is a string representation of this 
     * object's id.
     * 
     * @return a string representing this object
     */
    public String toString() {
        return myName == null ? Integer.toString(myID) : myName;
    }
    
    /**
     * Reserved for future purposes.
     */
    public static final SVNEventAction PROGRESS = new SVNEventAction(-1, "progress");
    
    /**
     * Denotes that a new item is scheduled for addition. Generated
     * by the {@link SVNWCClient#doAdd(java.io.File, boolean, boolean, boolean, org.tmatesoft.svn.core.SVNDepth, boolean, boolean) doAdd()} 
     * method. 
     */
    public static final SVNEventAction ADD = new SVNEventAction(0, "add");

    /**
     * Denotes that the item is copied with history. 
     * 
     * @see SVNCopyClient
     */
    public static final SVNEventAction COPY = new SVNEventAction(1, "copy");

    /**
     * Denotes that the item is scheduled for deletion. Generated
     * by the {@link SVNWCClient#doDelete(java.io.File, boolean, boolean) doDelete()} 
     * method. 
     */
    public static final SVNEventAction DELETE = new SVNEventAction(2, "delete");
    
    /**
     * Denotes that the deleted item is restored (prior to be updated).
     */
    public static final SVNEventAction RESTORE = new SVNEventAction(3, "restore");
    
    /**
     * Denotes that all local changes to the item were reverted. Generated by 
     * the {@link SVNWCClient#doRevert(java.io.File[], org.tmatesoft.svn.core.SVNDepth, java.util.Collection) doRevert()} 
     * method.
     */
    public static final SVNEventAction REVERT = new SVNEventAction(4, "revert");
    
    /**
     * Denotes that a revert operation failed. Generated by the
     * {@link SVNWCClient#doRevert(java.io.File[], org.tmatesoft.svn.core.SVNDepth, java.util.Collection) doRevert()} method.
     */
    public static final SVNEventAction FAILED_REVERT = new SVNEventAction(5, "failed_revert");
    
    /**
     * Denotes that the conflict on the item is resolved (the item is
     * marked resolved). Such an event is generated by the
     * {@link SVNWCClient#doResolve(java.io.File, org.tmatesoft.svn.core.SVNDepth, SVNConflictChoice) doResolve()} method.
     */
    public static final SVNEventAction RESOLVED = new SVNEventAction(6, "resolved");
    
    /**
     * Denotes that the operation is skipped due to errors (inability to 
     * be performed, etc.).
     */
    public static final SVNEventAction SKIP = new SVNEventAction(7, "skip");
    
    /**
     * In an update operation denotes that the item is deleted from
     * the Working Copy (as it was deleted in the repository).
     */
    public static final SVNEventAction UPDATE_DELETE = new SVNEventAction(8, "update_delete");
    
    /**
     * In an update operation denotes that the item is added to
     * the Working Copy (as it was added in the repository).
     */
    public static final SVNEventAction UPDATE_ADD = new SVNEventAction(9, "update_add");
    
    /**
     * In an update operation denotes that the item is modified (there 
     * are changes received from the repository).
     * 
     */
    public static final SVNEventAction UPDATE_UPDATE = new SVNEventAction(10, "update_update");
    
    /**
     * In an update operation denotes that the item is not modified, but its children are.
     * 
     */
    public static final SVNEventAction UPDATE_NONE = new SVNEventAction(10, "update_none");

    /**
     * In an update operation denotes that the operation itself is completed
     * (for instance, in a console client can be used to print out the
     * revision updated to).
     */
    public static final SVNEventAction UPDATE_COMPLETED = new SVNEventAction(11, "update_completed");
    
    /**
     * In an update operation denotes that the item being updated is 
     * external.
     */
    public static final SVNEventAction UPDATE_EXTERNAL = new SVNEventAction(12, "update_external");
    
    /**
     * In a remote status operation denotes that the operation itself is completed - 
     * used to get the latest repository revision against which the status was
     * invoked.  
     */
    public static final SVNEventAction STATUS_COMPLETED = new SVNEventAction(13, "status_completed");
    
    /**
     * In a status operation denotes that the status is performed on an 
     * external item. To find out the item's current status use 
     * {@link SVNEvent#getContentsStatus() getContentsStatus()}, 
     * {@link SVNEvent#getPropertiesStatus() getPropertiesStatus()}.
     * The {@link SVNStatusType#STATUS_EXTERNAL} constant says only that the 
     * item belongs to externals definitions. 
     * 
     */
    public static final SVNEventAction STATUS_EXTERNAL = new SVNEventAction(14, "status_external");
    
    /**
     * In a commit operation denotes sending the item's modifications to the
     * repository.
     */
    public static final SVNEventAction COMMIT_MODIFIED = new SVNEventAction(15, "commit_modified");
    
    /**
     * In a commit operation denotes adding a new item to the repository.
     */
    public static final SVNEventAction COMMIT_ADDED = new SVNEventAction(16, "commit_added");
    
    /**
     * In a commit operation denotes deleting the item from the
     * repository.
     */
    public static final SVNEventAction COMMIT_DELETED = new SVNEventAction(17, "commit_deleted");
    
    /**
     * In a commit operation denotes replacing (one item was deleted while 
     * another one with the same name was added) the item in the repository. 
     */
    public static final SVNEventAction COMMIT_REPLACED = new SVNEventAction(18, "commit_replaced");

    /**
     * In a commit operation denotes the final stage of the operation - 
     * sending all file data and finalizing the commit.
     */
    public static final SVNEventAction COMMIT_DELTA_SENT = new SVNEventAction(19, "commit_delta_sent");

    /**
     * In a commit operation denotes that the operation itself is completed
     * (for instance, in a console client can be used to print out the
     * commited revsion).
     */
    public static final SVNEventAction COMMIT_COMPLETED = new SVNEventAction(32, "commit_completed");

    /**
     * Denotes that file blaming is started.
     */
    public static final SVNEventAction ANNOTATE = new SVNEventAction(20, "annotate");
    
    /**
     * Denotes that the file item is locked as a result of a locking 
     * operation. Generated by a <b>doLock()</b> method of {@link SVNWCClient}.
     */
    public static final SVNEventAction LOCKED = new SVNEventAction(21, "locked");

    /**
     * Denotes that the file item is unlocked as a result of an unlocking 
     * operation. Generated by a <b>doUnlock()</b> method of {@link SVNWCClient}.
     */
    public static final SVNEventAction UNLOCKED = new SVNEventAction(22, "unlocked");

    /**
     * Denotes that locking a file item failed. Generated by a <b>doLock()</b> 
     * method of {@link SVNWCClient}.
     */
    public static final SVNEventAction LOCK_FAILED = new SVNEventAction(23, "lock_failed");

    /**
     * Denotes that unlocking a file item failed. Generated by a <b>doUnlock()</b> 
     * method of {@link SVNWCClient}.
     */
    public static final SVNEventAction UNLOCK_FAILED = new SVNEventAction(24, "unlock_failed");
    
    /**
     * Denotes that the current format of the working copy administrative 
     * area is upgraded to a newer one. 
     */
    public static final SVNEventAction UPGRADE = new SVNEventAction(-2, "wc_upgrade");
    
    /**
     * Denotes that tried adding a path that already exists.
     * @since 1.2.0, SVN 1.5.0
     */
    public static final SVNEventAction UPDATE_EXISTS = new SVNEventAction(25, "update_exists");
    
    /**
     * Denotes that changelist name is set.
     * @since 1.2.0, SVN 1.5.0
     */
    public static final SVNEventAction CHANGELIST_SET = new SVNEventAction(26, "changelist_set");

    /**
     * Denotes that changelist name is cleared.
     * @since 1.2.0, SVN 1.5.0
     */
    public static final SVNEventAction CHANGELIST_CLEAR = new SVNEventAction(27, "changelist_clear");

    /**
     * Denotes that a path has moved from one changelist to another.
     * @since 1.2.0, SVN 1.5.0
     */
    public static final SVNEventAction CHANGELIST_MOVED = new SVNEventAction(31, "changelist_moved");

    /**
     * Denotes that a merge operation (to path) has begun. See {@link SVNEvent#getMergeRange()}.
     * @since 1.2.0, SVN 1.5.0
     */
    public static final SVNEventAction MERGE_BEGIN = new SVNEventAction(28, "merge_begin");

    /**
     * Denotes that a merge operation (to path) from a foreign repository has begun.
     * See {@link SVNEvent#getMergeRange()}.
     * @since 1.2.0, SVN 1.5.0
     */
    public static final SVNEventAction FOREIGN_MERGE_BEGIN = new SVNEventAction(29, "foreign_merge_begin");

    /**
     * Denotes a replace notification.
     * @since 1.2.0, SVN 1.5.0
     */
    public static final SVNEventAction UPDATE_REPLACE = new SVNEventAction(30, "update_replace");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction PROPERTY_ADD = new SVNEventAction(31, "property_added");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction PROPERTY_MODIFY = new SVNEventAction(32, "property_modified");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction PROPERTY_DELETE = new SVNEventAction(33, "property_deleted");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction PROPERTY_DELETE_NONEXISTENT = new SVNEventAction(34, "property_deleted_nonexistent");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction REVPROPER_SET = new SVNEventAction(35, "revprop_set");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction REVPROP_DELETE = new SVNEventAction(36, "revprop_deleted");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction MERGE_COMPLETE = new SVNEventAction(37, "merge_completed");
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction TREE_CONFLICT = new SVNEventAction(38, "tree_conflict");
    
    /**
     * @since 1.3, SVN 1.6
     */
    public static final SVNEventAction FAILED_EXTERNAL = new SVNEventAction(39, "failed_external");
    
}
