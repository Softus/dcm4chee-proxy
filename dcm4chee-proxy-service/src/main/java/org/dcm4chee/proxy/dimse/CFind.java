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

package org.dcm4chee.proxy.dimse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.dcm4che3.conf.api.ApplicationEntityCache;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.TransferCapability.Role;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.AbstractDicomService;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4chee.proxy.conf.ForwardRule;
import org.dcm4chee.proxy.conf.ProxyAEExtension;
import org.dcm4chee.proxy.pix.PIXConsumer;
import org.dcm4chee.proxy.utils.AttributeCoercionUtils;
import org.dcm4chee.proxy.utils.ForwardConnectionUtils;
import org.dcm4chee.proxy.utils.ForwardRuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class CFind extends AbstractDicomService {

    protected static final Logger LOG = LoggerFactory.getLogger(CFind.class);

    private PIXConsumer pixConsumer;
    private ApplicationEntityCache aeCache;

    public CFind(ApplicationEntityCache aeCache, PIXConsumer pixConsumer, String... sopClasses) {
        super(sopClasses);
        this.aeCache = aeCache;
        this.pixConsumer = pixConsumer;
    }

    @Override
    public void onDimseRQ(final Association asAccepted, final PresentationContext pc, Dimse dimse, Attributes rq,
            Attributes data) throws IOException {
        if (dimse != Dimse.C_FIND_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        ApplicationEntity ae = asAccepted.getApplicationEntity();
        ProxyAEExtension proxyAEE = ae.getAEExtension(ProxyAEExtension.class);
        data = AttributeCoercionUtils.coerceDataset(proxyAEE, asAccepted, Role.SCU, Dimse.C_FIND_RQ, data, rq);
        Object forwardAssociationProperty = asAccepted.getProperty(ProxyAEExtension.FORWARD_ASSOCIATION);
        if (forwardAssociationProperty == null) {
            List<ForwardRule> forwardRules = ForwardRuleUtils.filterForwardRulesOnDimseRQ(
                    proxyAEE.getCurrentForwardRules(asAccepted), rq.getString(dimse.tagOfSOPClassUID()), dimse);
            HashMap<String, Association> fwdAssocs = ForwardConnectionUtils.openForwardAssociations(proxyAEE,
                    asAccepted, forwardRules, data, aeCache);
            if (fwdAssocs.isEmpty())
                throw new DicomServiceException(Status.UnableToProcess);

            try {
                asAccepted.setProperty(ProxyAEExtension.FORWARD_ASSOCIATION, fwdAssocs);
                new ForwardDimseRQ(asAccepted, pc, rq, data, dimse, pixConsumer, aeCache, fwdAssocs.values().toArray(
                        new Association[fwdAssocs.size()])).execute();
            } catch (InterruptedException e) {
                LOG.error("Error forwarding C-FIND request: " + e.getMessage());
                if(LOG.isDebugEnabled())
                    e.printStackTrace();
                throw new DicomServiceException(Status.UnableToProcess, e.getCause());
            }
        } else
            try {
                if (forwardAssociationProperty instanceof Association)
                    new ForwardDimseRQ(asAccepted, pc, rq, data, dimse, pixConsumer, aeCache,
                            (Association) forwardAssociationProperty).execute();
                else {
                    @SuppressWarnings("unchecked")
                    HashMap<String, Association> fwdAssocs = (HashMap<String, Association>) forwardAssociationProperty;
                    new ForwardDimseRQ(asAccepted, pc, rq, data, dimse, pixConsumer, aeCache, fwdAssocs.values()
                            .toArray(new Association[fwdAssocs.size()])).execute();
                }
            } catch (InterruptedException e) {
                LOG.error("Error forwarding C-FIND request: " + e.getMessage());
                if(LOG.isDebugEnabled())
                    e.printStackTrace();
                throw new DicomServiceException(Status.UnableToProcess, e.getCause());
            }
    }

}
