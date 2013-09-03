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
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
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
package org.dcm4chee.proxy.resteasy;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.dcm4che.util.StringUtils;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PostProcessInterceptor;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 *
 */
@Provider
@ServerInterceptor
public class LogInterceptor implements PreProcessInterceptor, PostProcessInterceptor {

    static Logger LOG = LoggerFactory.getLogger(LogInterceptor.class);

    @Context
    private HttpServletRequest servletRequest;

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method)
            throws Failure, WebApplicationException {
        LOG.info("{}@{}:{} >> {}.{}: {} {}{}", new Object[] {
                servletRequest.getRemoteUser(),
                servletRequest.getRemoteHost(),
                servletRequest.getRemotePort(),
                method.getResourceClass().getName(),
                method.getMethod().getName(),
                request.getHttpMethod(),
                request.getUri().getRequestUri(),
                toString(request.getHttpHeaders().getRequestHeaders()),
            });
        return null;
    }

    public static <V> String toString(Map<String, List<V>> map) {
        if (map.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        for (Entry<String, List<V>> entry : map.entrySet()) {
            sb.append(StringUtils.LINE_SEPARATOR)
              .append("  ")
              .append(entry.getKey())
              .append('=')
              .append(entry.getValue());
        }
        return sb.toString();
    }

    @Override
    public void postProcess(ServerResponse response) {
        LOG.info("{}@{}:{} << {}.{}: status={}{}", new Object[] {
                servletRequest.getRemoteUser(),
                servletRequest.getRemoteHost(),
                servletRequest.getRemotePort(),
                response.getResourceClass().getName(),
                response.getResourceMethod().getName(),
                response.getStatus(),
                toString(response.getMetadata())
            });
    }

}
