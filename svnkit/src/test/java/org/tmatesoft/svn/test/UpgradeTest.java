package org.tmatesoft.svn.test;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea15;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea16;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgDowngrade;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpgradeTest {

    @Test
    public void testUpgradeTempDirectoryIsNotEmpty() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        Assume.assumeTrue(!TestUtil.isNewWorkingCopyOnly());
        Assume.assumeTrue(TestUtil.isNewWorkingCopyTest());

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpgradeTempDirectoryIsNotEmpty", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("directory/file1");
            commitBuilder.addFile("directory/file2");
            commitBuilder.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");

            //checkout working copy in old format
            checkout(svnOperationFactory, url, workingCopyDirectory, SvnWcGeneration.V16);

            final File tmpDirectory = SVNWCUtils.admChild(workingCopyDirectory, "tmp");
            final File wcngDirectory = SVNFileUtil.createFilePath(tmpDirectory, "wcng");

            //not let's create some contents in .svn/tmp/wcng directory (for instace from the previous upgrade attempt if JVM crashed);
            // every contents there shouldn't be considered and can be removed
            checkout(svnOperationFactory, url, wcngDirectory, SvnWcGeneration.V17);

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            upgrade.run();

            //let's check that generation is ok
            Assert.assertEquals(SvnWcGeneration.V17, SvnOperationFactory.detectWcGeneration(workingCopyDirectory, false));

            final Map<File,SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopyDirectory);
            Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(workingCopyDirectory).getNodeStatus());
            Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(SVNFileUtil.createFilePath(workingCopyDirectory, "directory")).getNodeStatus());
            Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(SVNFileUtil.createFilePath(workingCopyDirectory, "directory/file1")).getNodeStatus());
            Assert.assertEquals(SVNStatusType.STATUS_NORMAL, statuses.get(SVNFileUtil.createFilePath(workingCopyDirectory, "directory/file2")).getNodeStatus());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testUpgradeOnReplacedWorkingCopyRoot() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        Assume.assumeTrue(TestUtil.isNewWorkingCopyTest());
        Assume.assumeTrue(!TestUtil.isNewWorkingCopyOnly());

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpgradeOnReplacedWorkingCopyRoot", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("directory/file");
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, SVNRevision.HEAD.getNumber(), true, SvnWcGeneration.V16);
            final File directory = workingCopy.getFile("directory");

            SVNFileUtil.deleteAll(directory, true);

            final SvnScheduleForRemoval scheduleForRemoval = svnOperationFactory.createScheduleForRemoval();
            scheduleForRemoval.setSingleTarget(SvnTarget.fromFile(directory));
            scheduleForRemoval.run();

            SVNFileUtil.ensureDirectoryExists(directory);

            final SvnScheduleForAddition scheduleForAddition = svnOperationFactory.createScheduleForAddition();
            scheduleForAddition.setSingleTarget(SvnTarget.fromFile(directory));
            scheduleForAddition.run();

            //delete .svn from working copy root, now the replaced directory becomes working copy root
            SVNFileUtil.deleteAll(new File(workingCopy.getWorkingCopyDirectory(), SVNFileUtil.getAdminDirectoryName()), true);

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setSingleTarget(SvnTarget.fromFile(directory));
            try {
                upgrade.run();
                Assert.fail("An exception should be thrown");
            } catch (SVNException e) {
                //expected
                Assert.assertEquals(SVNErrorCode.WC_INVALID_SCHEDULE, e.getErrorMessage().getErrorCode());
            }

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Ignore("SVNKIT-276")
    @Test
    public void testUpgradeWorkingCopyWithReplacedDirectoryInside() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        Assume.assumeTrue(TestUtil.isNewWorkingCopyTest());
        Assume.assumeTrue(!TestUtil.isNewWorkingCopyOnly());

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpgradeWorkingCopyWithReplacedDirectoryInside", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("directory/file");
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, SVNRevision.HEAD.getNumber(), true, SvnWcGeneration.V16);
            final File directory = workingCopy.getFile("directory");

            SVNFileUtil.deleteAll(directory, true);

            final SvnScheduleForRemoval scheduleForRemoval = svnOperationFactory.createScheduleForRemoval();
            scheduleForRemoval.setSingleTarget(SvnTarget.fromFile(directory));
            scheduleForRemoval.run();

            SVNFileUtil.ensureDirectoryExists(directory);

            final SvnScheduleForAddition scheduleForAddition = svnOperationFactory.createScheduleForAddition();
            scheduleForAddition.setSingleTarget(SvnTarget.fromFile(directory));
            scheduleForAddition.run();

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopy.getWorkingCopyDirectory()));
            upgrade.run();

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testFilesHaveUnknownDepthAfterUpgrade() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        Assume.assumeTrue(TestUtil.isNewWorkingCopyTest());
        Assume.assumeTrue(!TestUtil.isNewWorkingCopyOnly());

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testFilesHaveUnknownDepthAfterUpgrade", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file");
            commitBuilder.addFile("directory/file");
            commitBuilder.addFile("directory/subdirectory/file");
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, SVNRevision.HEAD.getNumber(), true, SvnWcGeneration.V16);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            upgrade.run();

            assertFilesRecordsHaveUknownDepth(workingCopy);

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testBasics() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        Assume.assumeTrue(TestUtil.isNewWorkingCopyTest());
        Assume.assumeTrue(!TestUtil.isNewWorkingCopyOnly());

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testBasics", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file", "contents".getBytes());
            commitBuilder.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");
            final File file = new File(workingCopyDirectory, "file");

            checkout(svnOperationFactory, url, workingCopyDirectory, SvnWcGeneration.V16);

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            upgrade.run();

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            final SvnCat cat = svnOperationFactory.createCat();
            cat.setSingleTarget(SvnTarget.fromFile(file, SVNRevision.BASE));
            cat.setOutput(byteArrayOutputStream);
            cat.run();

            Assert.assertEquals("contents", byteArrayOutputStream.toString());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testUpgradeFromWC17() throws Exception {
        //SVNKIT-447
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpgradeFromWC17", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder = new CommitBuilder(url);
            commitBuilder.addFile("file", "contents".getBytes());
            commitBuilder.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

            final SVNWCContext context = new SVNWCContext(svnOperationFactory.getOptions(), svnOperationFactory.getEventHandler());
            try {
                new SvnNgDowngrade().downgrade(context, workingCopyDirectory);
            } finally {
                context.close();
            }

            final List<SVNEvent> events = new ArrayList<SVNEvent>();
            svnOperationFactory.setEventHandler(new ISVNEventHandler() {
                public void handleEvent(SVNEvent event, double progress) throws SVNException {
                    events.add(event);
                }

                public void checkCancelled() throws SVNCancelException {
                }
            });

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            upgrade.run();

            Assert.assertEquals(1, events.size());
            Assert.assertEquals(workingCopyDirectory, events.get(0).getFile());
            Assert.assertEquals(SVNEventAction.UPGRADED_PATH, events.get(0).getAction());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testUpgradeWC16toWC17() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpgradeWC16toWC17", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", "base".getBytes());
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.changeFile("file", "theirs".getBytes());
            commitBuilder2.commit();

            final WorkingCopy workingCopy = sandbox.checkoutNewWorkingCopy(url, 1, false, SvnWcGeneration.V16);
            final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();
            final File file = workingCopy.getFile("file");
            TestUtil.writeFileContentsString(file, "ours");

            final SvnUpdate update = svnOperationFactory.createUpdate();
            update.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            update.run();

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setTargetWorkingCopyFormat(ISVNWCDb.WC_FORMAT_17);
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            upgrade.run();

            final SvnGetStatus getStatus = svnOperationFactory.createGetStatus();
            getStatus.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SvnStatus status = getStatus.run();

            Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopyDirectory);
            Assert.assertEquals(ISVNWCDb.WC_FORMAT_17, status.getWorkingCopyFormat());
            Assert.assertTrue(statuses.get(file).isConflicted());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Test
    public void testUpgradeWC15toWC17() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpgradeWC15toWC17", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", "base".getBytes());
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.changeFile("file", "theirs".getBytes());
            commitBuilder2.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");
            final File file = new File(workingCopyDirectory, "file");

            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(url));
            checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            checkout.setRevision(SVNRevision.create(1));
            checkout.setTargetWorkingCopyFormat(SVNAdminArea15.WC_FORMAT);
            checkout.run();

            TestUtil.writeFileContentsString(file, "ours");

            final SvnGetStatus getStatus15 = svnOperationFactory.createGetStatus();
            getStatus15.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SvnStatus status15 = getStatus15.run();
            Assert.assertEquals(SVNAdminArea15.WC_FORMAT, status15.getWorkingCopyFormat());

            final SvnUpdate update = svnOperationFactory.createUpdate();
            update.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            update.run();

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setTargetWorkingCopyFormat(ISVNWCDb.WC_FORMAT_17);
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            upgrade.run();

            final SvnGetStatus getStatus = svnOperationFactory.createGetStatus();
            getStatus.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SvnStatus status = getStatus.run();

            Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopyDirectory);
            Assert.assertEquals(ISVNWCDb.WC_FORMAT_17, status.getWorkingCopyFormat());
            Assert.assertTrue(statuses.get(file).isConflicted());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    @Ignore
    @Test
    public void testUpgradeWC15toWC16() throws Exception {
        final TestOptions options = TestOptions.getInstance();

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpgradeWC15toWC16", options);
        try {
            final SVNURL url = sandbox.createSvnRepository();

            final CommitBuilder commitBuilder1 = new CommitBuilder(url);
            commitBuilder1.addFile("file", "base".getBytes());
            commitBuilder1.commit();

            final CommitBuilder commitBuilder2 = new CommitBuilder(url);
            commitBuilder2.changeFile("file", "theirs".getBytes());
            commitBuilder2.commit();

            final File workingCopyDirectory = sandbox.createDirectory("wc");
            final File file = new File(workingCopyDirectory, "file");

            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(url));
            checkout.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            checkout.setRevision(SVNRevision.create(1));
            checkout.setTargetWorkingCopyFormat(SVNAdminArea15.WC_FORMAT);
            checkout.run();

            TestUtil.writeFileContentsString(file, "ours");

            final SvnGetStatus getStatus15 = svnOperationFactory.createGetStatus();
            getStatus15.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SvnStatus status15 = getStatus15.run();
            Assert.assertEquals(SVNAdminArea15.WC_FORMAT, status15.getWorkingCopyFormat());

            final SvnUpdate update = svnOperationFactory.createUpdate();
            update.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            update.run();

            final SvnUpgrade upgrade = svnOperationFactory.createUpgrade();
            upgrade.setTargetWorkingCopyFormat(SVNAdminArea16.WC_FORMAT);
            upgrade.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            upgrade.run();

            final SvnGetStatus getStatus = svnOperationFactory.createGetStatus();
            getStatus.setSingleTarget(SvnTarget.fromFile(workingCopyDirectory));
            final SvnStatus status = getStatus.run();

            Map<File, SvnStatus> statuses = TestUtil.getStatuses(svnOperationFactory, workingCopyDirectory);
            Assert.assertEquals(SVNAdminArea16.WC_FORMAT, status.getWorkingCopyFormat());
            Assert.assertTrue(statuses.get(file).isConflicted());

        } finally {
            svnOperationFactory.dispose();
            sandbox.dispose();
        }
    }

    private void checkout(SvnOperationFactory svnOperationFactory, SVNURL url, File wcngDirectory, SvnWcGeneration primaryWcGeneration) throws SVNException {
        svnOperationFactory.setPrimaryWcGeneration(primaryWcGeneration);

        final SvnCheckout checkout = svnOperationFactory.createCheckout();
        checkout.setSource(SvnTarget.fromURL(url));
        checkout.setSingleTarget(SvnTarget.fromFile(wcngDirectory));
        checkout.run();
    }

    private void assertFilesRecordsHaveUknownDepth(WorkingCopy workingCopy) throws SqlJetException {
        final SqlJetDb db = SqlJetDb.open(workingCopy.getWCDbFile(), false);
        try {
            final ISqlJetTable table = db.getTable(SVNWCDbSchema.NODES.name());
            db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
            final ISqlJetCursor cursor = table.open();

            for (; !cursor.eof(); cursor.next()) {
                final ISVNWCDb.SVNWCDbKind kind = SvnWcDbStatementUtil.parseKind(cursor.getString(SVNWCDbSchema.NODES__Fields.kind.name()));
                if (kind != ISVNWCDb.SVNWCDbKind.File) {
                    continue;
                }

                final String depthString = cursor.getString(SVNWCDbSchema.NODES__Fields.depth.name());
                Assert.assertNull(depthString);
                final SVNDepth depth = SvnWcDbStatementUtil.parseDepth(depthString);
                Assert.assertEquals(SVNDepth.UNKNOWN, depth);
            }
            cursor.close();
            db.commit();
        } finally {
            db.close();
        }
    }

    private String getTestName() {
        return "UpgradeTest";
    }
}
