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
package org.craftercms.deployer.impl.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.rest.RestServiceUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.commons.validation.ValidationException;
import org.craftercms.commons.validation.ValidationResult;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.craftercms.deployer.impl.rest.RestConstants.ENV_PATH_VAR_NAME;
import static org.craftercms.deployer.impl.rest.RestConstants.SITE_NAME_PATH_VAR_NAME;

/**
 * Main controller for target related operations.
 *
 * @author avasquez
 */
@RestController
@RequestMapping(TargetController.BASE_URL)
public class TargetController {

    public static final String BASE_URL = "/api/1/target";
    public static final String CREATE_TARGET_URL = "/create";
    public static final String GET_TARGET_URL = "/get/{" + ENV_PATH_VAR_NAME + "}/{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String GET_ALL_TARGETS_URL = "/get-all";
    public static final String DELETE_TARGET_URL = "/delete/{" + ENV_PATH_VAR_NAME + "}/{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String DEPLOY_TARGET_URL = "/deploy/{" + ENV_PATH_VAR_NAME + "}/{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String DEPLOY_ALL_TARGETS_URL = "/deploy-all";

    public static final String REPLACE_PARAM_NAME = "replace";
    public static final String TEMPLATE_NAME_PARAM_NAME = "template_name";

    protected TargetService targetService;
    protected DeploymentService deploymentService;

    @Autowired
    public TargetController(TargetService targetService, DeploymentService deploymentService) {
        this.targetService = targetService;
        this.deploymentService = deploymentService;
    }

    /**
     * Creates a Deployer {@link Target}.
     *
     * @param parameters the body of the request with the template parameters that will be used to create the target. The body must
     *                   contain at least a {@code env} and {@code site_name} parameter. Other required parameters depend on the
     *                   template used.
     *
     * @return the response entity 201 CREATED status
     *
     * @throws DeployerException   if an error ocurred during target creation
     * @throws ValidationException if a required parameter is missing
     */
    @RequestMapping(value = CREATE_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Result> createTarget(@RequestBody Map<String, Object> parameters) throws DeployerException,
        ValidationException {
        String env = Objects.toString(parameters.get(ENV_PATH_VAR_NAME), "");
        String siteName = Objects.toString(parameters.get(SITE_NAME_PATH_VAR_NAME), "");
        boolean replace = BooleanUtils.toBoolean(parameters.get(REPLACE_PARAM_NAME));
        String templateName = Objects.toString(parameters.get(TEMPLATE_NAME_PARAM_NAME), "");
        ValidationResult validationResult = new ValidationResult();
        
        if (StringUtils.isEmpty(env)) {
            validationResult.addMissingFieldError(ENV_PATH_VAR_NAME);
        }
        if (StringUtils.isEmpty(siteName)) {
            validationResult.addMissingFieldError(SITE_NAME_PATH_VAR_NAME);
        }
        if (CollectionUtils.isNotEmpty(validationResult.getFieldErrors())) {
            throw new ValidationException(validationResult);
        }

        parameters.keySet().removeIf(key -> key.equals(REPLACE_PARAM_NAME) || key.equals(TEMPLATE_NAME_PARAM_NAME));

        targetService.createTarget(env, siteName, replace, templateName, parameters);

        return new ResponseEntity<>(Result.OK,
                                    RestServiceUtils.setLocationHeader(new HttpHeaders(), BASE_URL + GET_TARGET_URL, env, siteName),
                                    HttpStatus.CREATED);
    }

    /**
     * Returns a {@link Target}.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the response entity with the target's properties and 200 OK status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = GET_TARGET_URL, method = RequestMethod.GET)
    public ResponseEntity<Target> getTarget(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                            @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName) throws DeployerException {
        Target target = targetService.getTarget(env, siteName);

        return new ResponseEntity<>(target,
                                    RestServiceUtils.setLocationHeader(new HttpHeaders(), BASE_URL + GET_TARGET_URL, env, siteName),
                                    HttpStatus.OK);
    }

    /**
     * Returns all current {@link Target}s
     *
     * @return the response entity with all the properties of the targets and 200 OK status
     *
     * @throws DeployerException if an error ocurred
     */
    @RequestMapping(value = GET_ALL_TARGETS_URL, method = RequestMethod.GET)
    public ResponseEntity<List<Target>> getAllTargets() throws DeployerException {
        List<Target> targets = targetService.getAllTargets();

        return new ResponseEntity<>(targets,
                                    RestServiceUtils.setLocationHeader(new HttpHeaders(), BASE_URL + GET_ALL_TARGETS_URL),
                                    HttpStatus.OK);
    }

    /**
     * Deletes the {@link Target} with the specified environment and site name.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the response entity with a 204 NO CONTENT status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = DELETE_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Void> deleteTarget(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                             @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName) throws DeployerException {
        targetService.deleteTarget(env, siteName);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Deploys the {@link Target} with the specified environment and site name.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     * @param params    any additional parameters that can be used by the {@link org.craftercms.deployer.api.DeploymentProcessor}s, for
     *                  example {@code reprocess_all_files}
     *
     * @return the response entity with a 200 OK status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = DEPLOY_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Deployment> deployTarget(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                                   @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName,
                                                   @RequestBody(required = false) Map<String, Object> params) throws DeployerException {
        if (params == null) {
            params = new HashMap<>();
        }

        Deployment deployment = deploymentService.deployTarget(env, siteName, params);

        return new ResponseEntity<>(deployment, new HttpHeaders(), HttpStatus.OK);
    }

    /**
     * Deploys all current {@link Target}s.
     *
     * @return the response entity with a 200 OK status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = DEPLOY_ALL_TARGETS_URL, method = RequestMethod.POST)
    public ResponseEntity<List<Deployment>> deployAllTargets(
        @RequestBody(required = false) Map<String, Object> params) throws DeployerException {
        if (params == null) {
            params = new HashMap<>();
        }

        List<Deployment> deployments = deploymentService.deployAllTargets(params);

        return new ResponseEntity<>(deployments, new HttpHeaders(), HttpStatus.OK);
    }

}
