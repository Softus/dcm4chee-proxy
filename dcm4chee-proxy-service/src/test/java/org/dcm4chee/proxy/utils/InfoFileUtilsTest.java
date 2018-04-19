/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.proxy.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.Properties;

import org.dcm4che3.net.ApplicationEntity;
import org.dcm4chee.proxy.conf.ProxyAEExtension;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test methods for {@link InfoFileUtils}.
 * @author Pavel Bludov
 */
public class InfoFileUtilsTest
{
    final ProxyAEExtension extension = new ProxyAEExtension();
    final File testResourcesDir = new File("src/test/resources/"
        + InfoFileUtils.class.getName().replace('.', '/'));

    private File testFile(
        String fileName)
    {
        return new File(testResourcesDir, fileName);
    }

    @Before
    public void setUp()
    {
        Whitebox.setInternalState(extension, "ae", new ApplicationEntity("AE"));
    }

    @Test
    public void testPrivateConstructor()
    throws IOException, ReflectiveOperationException
    {
        final Constructor<InfoFileUtils> constructor
            = InfoFileUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        // Create an instance to 100% coverage
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testFindInfoFile()
    throws IOException
    {
        final File expected = testFile("x.info");
        assertEquals(expected, InfoFileUtils.findInfoFile(testFile("x.y.z")));
        assertEquals(expected, InfoFileUtils.findInfoFile(testFile("x.z")));
        assertEquals(expected, InfoFileUtils.findInfoFile(testFile("x")));
    }

    @Test
    public void testGetFileInfoProperties()
    throws IOException
    {
        Properties props = InfoFileUtils.getFileInfoProperties(extension, testFile("x.dcm"));

        assertNotNull(props);
        assertEquals("0.0.0.0", props.getProperty("hostname"));
    }

    @Test
    public void testAddFileInfoProperties()
    throws IOException
    {
        final File tempFile = File.createTempFile("x.y.z.", null);
        InfoFileUtils.addFileInfoProperty(extension, tempFile, "prop1", "value1");
        InfoFileUtils.addFileInfoProperty(extension, tempFile, "prop2", "value2");
        Properties props = InfoFileUtils.getFileInfoProperties(extension, tempFile);

        assertNotNull(props);
        assertEquals("value1", props.getProperty("prop1"));
        assertEquals("value2", props.getProperty("prop2"));

        assertTrue(tempFile.delete());
        assertTrue(InfoFileUtils.findInfoFile(tempFile).delete());
    }

    @Test
    public void testSetFileInfoProperties()
    throws IOException
    {
        final File tempFile = File.createTempFile("x.y.", null);
        Properties props = new Properties();
        props.put("prop", "value");
        InfoFileUtils.setFileInfoProperties(extension, tempFile, props);

        final File infoFile = InfoFileUtils.findInfoFile(tempFile);
        props.clear();
        try (InputStream inStream = Files.newInputStream(infoFile.toPath()))
        {
            props.load(inStream);
        }
        assertEquals(1, props.size());
        assertEquals("value", props.getProperty("prop"));

        assertTrue(tempFile.delete());
        assertTrue(infoFile.delete());
    }

    @Test (expected = FileNotFoundException.class)
    public void testGetFileInfoPropertiesNotExistentFile()
    throws IOException
    {
        InfoFileUtils.getFileInfoProperties(extension, testFile("nonExistentFile.xml"));
    }

    @Test (expected = FileNotFoundException.class)
    public void testGetFileInfoPropertiesNotFile()
    throws IOException
    {
        InfoFileUtils.getFileInfoProperties(extension, testFile(""));
    }
}
