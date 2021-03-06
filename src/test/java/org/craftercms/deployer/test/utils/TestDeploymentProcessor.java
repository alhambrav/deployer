/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.test.utils;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility processor that just logs that it's running.
 *
 * @author avasquez
 */
public class TestDeploymentProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TestDeploymentProcessor.class);

    private String text;

    public String getText() {
        return text;
    }

    @Override
    public void destroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected void doInit(Configuration config) throws DeployerException {
        text = ConfigUtils.getStringProperty(config, "text");
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
        logger.info("Test deployment processor running");

        return filteredChangeSet;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

}
