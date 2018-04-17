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
public class InfoFileUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(InfoFileUtils.class);

    public static Properties getFileInfoProperties(
        ProxyAEExtension proxyAEE,
        File file)
    throws IOException
    {
        final String baseFileName = getBaseFileName(file);
        File infoFile = null;
        for (File f: file.getParentFile().listFiles(infoFileFilter()))
        {
            if (baseFileName.equals(getBaseFileName(f)))
            {
                infoFile = f;
                break;
            }
        }
        if (infoFile == null)
        {
            throw new FileNotFoundException("Unable to find information file for " + file);
        }
        return loadPropertiesFromFile(proxyAEE, infoFile);
    }

    private static String getBaseFileName(
        File file)
    {
        final String name = file.getName();
        final int dotIndex = name.indexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    public static Properties loadPropertiesFromFile(
        ProxyAEExtension proxyAEE,
        File infoFile)
    throws IOException
    {
        LOG.debug("{}: Loading info file {}", proxyAEE.getApplicationEntity().getAETitle(), infoFile);
        Properties prop = new Properties();
        try (InputStream inStream = Files.newInputStream(infoFile.toPath()))
        {
            prop.load(inStream);
        }
        return prop;
    }

    public static FileFilter infoFileFilter()
    {
        return new FileFilter()
        {
            @Override
            public boolean accept(
                File pathname)
            {
                return pathname.getName().endsWith(".info");
            }
        };
    }
}
