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
package org.craftercms.deployer.impl;

import org.craftercms.deployer.api.exceptions.DeployerException;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Created by alfonso on 3/22/17.
 */
public interface ProcessedCommitsStore {

    ObjectId load(String targetId) throws DeployerException;

    void store(String targetId, ObjectId commitId) throws DeployerException;

    void delete(String targetId) throws DeployerException;

}
