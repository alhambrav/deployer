/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.deployer.impl.rest;

import java.util.List;

import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by alfonsovasquez on 12/14/16.
 */
@RestController
@RequestMapping("/api/1/deployment")
public class DeploymentController {

    protected final DeploymentService deploymentService;

    @Autowired
    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @RequestMapping("/deploy/all")
    public List<Deployment> deployAll() throws DeploymentException {
        return deploymentService.deployAllTargets();
    }

    @RequestMapping("/deploy/{" + RestConstants.TARGET_ID_PATH_VAR + "}")
    public Deployment deployTarget(@PathVariable String targetId) throws DeploymentException {
        return deploymentService.deployTarget(targetId);
    }

}
