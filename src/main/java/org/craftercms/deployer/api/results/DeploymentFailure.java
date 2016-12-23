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
package org.craftercms.deployer.api.results;

/**
 * Created by alfonsovasquez on 12/15/16.
 */
public class DeploymentFailure extends DeploymentResult {

    protected String message;

    public DeploymentFailure(String deploymentId, String message) {
        super(deploymentId, false);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
