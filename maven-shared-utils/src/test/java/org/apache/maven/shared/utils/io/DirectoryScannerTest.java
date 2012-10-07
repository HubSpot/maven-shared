package org.apache.maven.shared.utils.io;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.maven.shared.utils.testhelpers.FileTestHelper;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryScannerTest
{
    private static final String[] NONE = new String[0];

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();



    private void createTestData() throws IOException
    {
        File rootDir = tempFolder.getRoot();
        File folder1 = new File( rootDir, "folder1" );
        folder1.mkdirs();

        FileTestHelper.generateTestFile( new File( rootDir, "file1.txt" ), 11 );
        FileTestHelper.generateTestFile( new File( rootDir, "file2.txt" ), 12 );
        FileTestHelper.generateTestFile( new File( rootDir, "file3.dat" ), 13 );

        FileTestHelper.generateTestFile( new File( folder1, "file4.txt" ), 14 );
        FileTestHelper.generateTestFile( new File( folder1, "file5.dat" ), 15 );

        File folder2 = new File( folder1, "ignorefolder" );
        folder2.mkdirs();
        FileTestHelper.generateTestFile( new File( folder2, "file7.txt" ), 17 );

    }

    @Test
    public void testSimpleScan() throws Exception
    {
        createTestData();

        fitScanTest( true, true, true,
                /* includes */        null,
                /* excludes */        null,
                /* expInclFiles */    new String[]{ "file1.txt", "file2.txt", "file3.dat", "folder1/file4.txt", "folder1/file5.dat" },
                /* expInclDirs */     new String[]{ "", "folder1" },
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expNotExclFiles */ NONE,
                /* expNotExclDirs  */ NONE );

        // same without followSymlinks
        fitScanTest( true, false, true,
                /* includes */        null,
                /* excludes */        null,
                /* expInclFiles */    new String[]{ "file1.txt", "file2.txt", "file3.dat", "folder1/file4.txt", "folder1/file5.dat" },
                /* expInclDirs */     new String[]{ "", "folder1" },
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expNotExclFiles */ NONE,
                /* expNotExclDirs  */ NONE );
    }

    @Test
    public void testSimpleIncludes() throws Exception
    {
        createTestData();

        fitScanTest( true, true, true,
                /* includes        */ new String[]{ "**/*.dat", "*.somethingelse" },
                /* excludes        */ null,
                /* expInclFiles    */ new String[]{ "file3.dat", "folder1/file5.dat" },
                /* expInclDirs     */ NONE,
                /* expNotInclFiles */ new String[]{ "file1.txt", "file2.txt", "folder1/file4.txt" },
                /* expNotInclDirs  */ new String[]{ "", "folder1" },
                /* expExclFiles    */ NONE,
                /* expExclDirs     */ NONE );

        // same without followSymlinks
        fitScanTest( true, false, true,
                /* includes        */ new String[]{ "**/*.dat", "*.somethingelse" },
                /* excludes        */ null,
                /* expInclFiles    */ new String[]{ "file3.dat", "folder1/file5.dat" },
                /* expInclDirs     */ NONE,
                /* expNotInclFiles */ new String[]{ "file1.txt", "file2.txt", "folder1/file4.txt" },
                /* expNotInclDirs  */ new String[]{ "", "folder1" },
                /* expExclFiles    */ NONE,
                /* expExclDirs     */ NONE );
    }

    @Test
    public void testSimpleExcludes() throws Exception
    {
        createTestData();

        fitScanTest( true, true, true,
                /* includes        */ null,
                /* excludes        */ new String[]{ "**/*.dat", "*.somethingelse" },
                /* expInclFiles    */ new String[]{ "file1.txt", "file2.txt", "folder1/file4.txt" },
                /* expInclDirs     */ new String[]{ "", "folder1" },
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expExclFiles    */ new String[]{ "file3.dat", "folder1/file5.dat" },
                /* expExclDirs     */ NONE );

        // same without followSymlinks
        fitScanTest( true, false, true,
                /* includes        */ null,
                /* excludes        */ new String[]{ "**/*.dat", "*.somethingelse" },
                /* expInclFiles    */ new String[]{ "file1.txt", "file2.txt", "folder1/file4.txt" },
                /* expInclDirs     */ new String[]{ "", "folder1" },
                /* expNotInclFiles */ NONE,
                /* expNotInclDirs  */ NONE,
                /* expExclFiles    */ new String[]{ "file3.dat", "folder1/file5.dat" },
                /* expExclDirs     */ NONE );
    }


    /**
     * Performs a scan and test for the given parameters if not null.
     */
    private void fitScanTest( boolean caseSensitive,
                              boolean followSymLinks,
                              boolean addDefaultExcludes,
                              String[] includes, String[] excludes,
                              String[] expectedIncludedFiles,
                              String[] expectedIncludedDirectories,
                              String[] expectedNotIncludedFiles,
                              String[] expectedNotIncludedDirectories,
                              String[] expectedExcludedFiles,
                              String[] expectedExcludedDirectories )
            throws Exception
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( tempFolder.getRoot() );

        ds.setCaseSensitive( caseSensitive );
        ds.setFollowSymlinks( followSymLinks );

        if ( addDefaultExcludes )
        {
            ds.addDefaultExcludes();
        }
        if ( includes != null )
        {
            ds.setIncludes( includes );
        }
        if ( excludes != null )
        {
            ds.setExcludes( excludes );
        }

        TestScanConductor scanConductor =  new TestScanConductor();

        ds.setScanConductor( scanConductor );

        ds.scan();

        checkFiles( "expectedIncludedFiles", expectedIncludedFiles, ds.getIncludedFiles() );
        checkFiles( "expectedIncludedDirectories", expectedIncludedDirectories, ds.getIncludedDirectories() );
        checkFiles( "expectedNotIncludedFiles", expectedNotIncludedFiles, ds.getNotIncludedFiles() );
        checkFiles( "expectedNotIncludedDirectories", expectedNotIncludedDirectories, ds.getNotIncludedDirectories() );
        checkFiles( "expectedExcludedFiles", expectedExcludedFiles, ds.getExcludedFiles() );
        checkFiles( "expectedExcludedDirectories", expectedExcludedDirectories, ds.getExcludedDirectories() );

        checkFiles( "visitedFiles", expectedIncludedFiles, scanConductor.visitedFiles.toArray( new String[ 0 ] ) );
    }

    /**
     * Check if the resolved files match the rules of the expected files.
     * @param expectedFiles
     * @param resolvedFiles
     */
    private void checkFiles( String category, String[] expectedFiles, String[] resolvedFiles )
    {
        if ( expectedFiles != null )
        {
            String msg = category + " expected: " + Arrays.toString( expectedFiles ) + " but got: " + Arrays.toString( resolvedFiles );
            Assert.assertNotNull( msg, resolvedFiles );
            Assert.assertEquals( msg, expectedFiles.length, resolvedFiles.length );

            Arrays.sort( expectedFiles );
            Arrays.sort( resolvedFiles );

            for ( int i = 0; i < resolvedFiles.length; i++ )
            {
                Assert.assertEquals( msg, expectedFiles[ i ], resolvedFiles[ i ].replace( "\\", "/" ) );
            }
        }
    }

    private static class TestScanConductor implements ScanConductor
    {
        List<String> visitedFiles = new ArrayList<String>();

        public ScanConductor.ScanAction visitDirectory( String name, File directory )
        {
            Assert.assertTrue( directory.isDirectory() );

            if ( directory.getName().equals( "ignorefolder" ) )
            {
                return ScanAction.NO_RECURSE;
            }

            return ScanAction.CONTINUE;
        }

        public ScanConductor.ScanAction visitFile( String name, File file )
        {
            Assert.assertTrue( file.isFile() );
            visitedFiles.add( name );
            return ScanAction.CONTINUE;
        }
    }


    private void removeAndAddSomeFiles() throws IOException
    {
        File rootDir = tempFolder.getRoot();
        File file2 = new File( rootDir, "file2.txt" );
        file2.delete();

        FileTestHelper.generateTestFile( new File( rootDir, "folder1/file9.txt" ), 15 );

        File folder2 = new File( rootDir, "folder1/ignorefolder" );
        FileUtils.deleteDirectory( folder2 );
    }

    @Test
    public void testScanDiff() throws Exception
    {
        createTestData();

        DirectoryScanner dss = new DirectoryScanner();
        dss.setBasedir( tempFolder.getRoot() );
        Assert.assertNotNull( dss );

        // we take the initial snapshot
        dss.scan();
        String[] oldFiles = dss.getIncludedFiles();

        // now we change 3 files. add one and remove
        removeAndAddSomeFiles();

        dss.scan();

        DirectoryScanResult dsr = dss.diffIncludedFiles( oldFiles );

        String[] addedFiles = dsr.getFilesAdded();
        String[] removedFiles = dsr.getFilesRemoved();
        Assert.assertNotNull( addedFiles );
        Assert.assertNotNull( removedFiles );
        Assert.assertEquals( 1, addedFiles.length );
        Assert.assertEquals(2, removedFiles.length );
    }



    @Ignore("Enable this test to run performance checks")
    @Test
    public void performanceTest() throws Exception {

        File rootFolder = tempFolder.getRoot();

        // do some warmup
        for ( int i = 1; i < 200; i++ )
        {
            createTestData();
            removeAndAddSomeFiles();
            FileUtils.deleteDirectory( rootFolder );
        }

        int cycles = 2000;

        // and now we take the time _without_
        long startTime = System.nanoTime();
        for ( int i = 1; i < cycles; i++ )
        {
            createTestData();
            removeAndAddSomeFiles();
            FileUtils.deleteDirectory( rootFolder );
            rootFolder.mkdir();
        }
        long endTime = System.nanoTime();

        long durationEmptyRun = endTime - startTime;
        System.out.println( "durationEmptyRun            [ns]: " + durationEmptyRun);

        startTime = System.nanoTime();
        for ( int i = 1; i < cycles; i++ )
        {
            createTestData();
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir( rootFolder );
            directoryScanner.scan();
            String[] oldFiles = directoryScanner.getIncludedFiles();

            removeAndAddSomeFiles();

            directoryScanner.scan();

            DirectoryScanResult directoryScanResult = directoryScanner.diffIncludedFiles( oldFiles );
            Assert.assertNotNull( directoryScanResult );

            FileUtils.deleteDirectory( rootFolder );
            rootFolder.mkdir();
        }
        endTime = System.nanoTime();

        long durationWithSnapshotScanner = endTime - startTime;
        System.out.println( "durationWithSnapshotScanner [ns]: " + durationWithSnapshotScanner);

        long dirScannerOverhead = durationWithSnapshotScanner - durationEmptyRun;

        System.out.println( "Overhead for n cycles [ns]: " + dirScannerOverhead);
    }

}