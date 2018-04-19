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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.dcm4chee.proxy.conf.ProxyAEExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The .info file helper functions.
 *
 * @author Michael Backhaus &lt;michael.backhaus@agfa.com&gt;
 * @author Pavel Bludov
 */
public final class InfoFileUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(InfoFileUtils.class);
    private static final String SUFFIX = ".info";

    /** Stop instances being created. **/
    private InfoFileUtils()
    {
    }

    /**
     * Load the property set to the .info file associated with {@code file}.
     * @param proxyAEE proxy AE extension instance
     * @param file DCM file to load the property set
     * @return loaded property set
     * @throws IOException if error occurs
     */
    public static Properties getFileInfoProperties(
        ProxyAEExtension proxyAEE,
        File file)
    throws IOException
    {
        final File infoFile = findInfoFile(file);
        if (infoFile == null)
        {
            LOG.warn("findInfoFile: {} has no .info", file);
            throw new FileNotFoundException(
                "Unable to find information file for " + file);
        }

        final String aeTitle = proxyAEE.getApplicationEntity().getAETitle();
        LOG.debug("{}: Loading info file {}", aeTitle, infoFile);
        return loadPropertiesFromFile(infoFile);
    }

    /**
     * Save the property set to the .info file associated with {@code file}.
     * @param proxyAEE proxy AE extension instance
     * @param file DCM file to add the property
     * @param props properties to save
     * @throws IOException if error occurs
     */
    public static void setFileInfoProperties(
        ProxyAEExtension proxyAEE,
        File file,
        Properties props)
    throws IOException
    {
        File infoFile = new File(file.getParent(), getBaseFileName(file) + SUFFIX);
        final String aeTitle = proxyAEE.getApplicationEntity().getAETitle();
        LOG.debug("{}: Saving info file {}", aeTitle, infoFile);
        savePropertiesToFile(infoFile, props);
    }

    /**
     * Add a property value to the .info file associated with {@code file}.
     * @param proxyAEE proxy AE extension instance
     * @param file DCM file to add the property
     * @param key property name
     * @param value property value
     * @throws IOException if error occurs
     */
    public static void addFileInfoProperty(
        ProxyAEExtension proxyAEE,
        File file,
        String key,
        String value)
    throws IOException
    {
        final String aeTitle = proxyAEE.getApplicationEntity().getAETitle();
        final Properties props;
        File infoFile = findInfoFile(file);
        if (infoFile == null)
        {
            infoFile = new File(file.getParent(), getBaseFileName(file) + SUFFIX);
            props = new Properties();
        }
        else
        {
            props = loadPropertiesFromFile(infoFile);
        }
        LOG.debug("{}: Add property {} to info file {}", key, aeTitle, infoFile);
        props.setProperty(key, value);
        savePropertiesToFile(infoFile, props);
    }

    /**
     * Returns the file filter for .info files.
     * @return file filter
     */
    public static FileFilter infoFileFilter()
    {
        return new FileFilter()
        {
            @Override
            public boolean accept(
                File pathname)
            {
                return pathname.getName().endsWith(SUFFIX);
            }
        };
    }

    /**
     * Find the .info file associated with {@code file}.
     * @param file DCM file to find the .info file
     * @return .info file if exist, {@code null} otherwise
     */
    public static File findInfoFile(
        File file)
    {
        final String baseFileName = getBaseFileName(file);
        File infoFile = null;
        for (File f: file.getParentFile().listFiles(infoFileFilter()))
        {
            if (baseFileName.equals(getBaseFileName(f)))
            {
                infoFile = f;
                LOG.debug("findInfoFile: {} => {}", file, infoFile);
                break;
            }
        }

        return infoFile;
    }

    private static String getBaseFileName(
        File file)
    {
        final String name = file.getName();
        final int dotIndex = name.indexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    private static Properties loadPropertiesFromFile(
        File infoFile)
    throws IOException
    {
        final Properties prop = new Properties();
        try (InputStream inStream = Files.newInputStream(infoFile.toPath()))
        {
            prop.load(inStream);
        }
        return prop;
    }

    private static void savePropertiesToFile(
        File infoFile,
        Properties prop)
    throws IOException
    {
        try (OutputStream inStream = Files.newOutputStream(infoFile.toPath()))
        {
            prop.store(inStream, null);
        }
    }
}
