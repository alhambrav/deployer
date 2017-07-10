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
package org.craftercms.deployer.impl;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Default implementation of {@link Target}.
 *
 * @author avasquez
 */
public class TargetImpl implements Target {

    private static final Logger logger = LoggerFactory.getLogger(TargetImpl.class);

    protected String env;
    protected String siteName;
    protected String id;
    protected DeploymentPipeline deploymentPipeline;
    protected File configurationFile;
    protected Configuration configuration;
    protected ConfigurableApplicationContext applicationContext;
    protected ZonedDateTime loadDate;
    protected ScheduledFuture<?> scheduledDeploymentFuture;
    protected ThreadPoolExecutor deploymentExecutor;
    protected volatile Deployment currentDeployment;

    public TargetImpl(String env, String siteName, String id, DeploymentPipeline deploymentPipeline, File configurationFile,
                      Configuration configuration, ConfigurableApplicationContext applicationContext) {
        this.env = env;
        this.siteName = siteName;
        this.id = id;
        this.deploymentPipeline = deploymentPipeline;
        this.configurationFile = configurationFile;
        this.configuration = configuration;
        this.applicationContext = applicationContext;
        this.loadDate = ZonedDateTime.now();
        this.deploymentExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Override
    public String getEnv() {
        return env;
    }

    @Override
    public String getSiteName() {
        return siteName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ZonedDateTime getLoadDate() {
        return loadDate;
    }

    @Override
    public File getConfigurationFile() {
        return configurationFile;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Deployment deploy(Map<String, Object> params) {
        Deployment deployment = new Deployment(this, params);

        deploymentExecutor.execute(new DeploymentTask(deployment));

        return deployment;
    }

    @Override
    public void scheduleDeployment(TaskScheduler scheduler, String cronExpression) {
        scheduledDeploymentFuture = scheduler.schedule(new ScheduledDeploymentTask(), new CronTrigger(cronExpression));
    }

    @Override
    public Collection<Deployment> getPendingDeployments() {
        Queue<Runnable> deploymentTasks = deploymentExecutor.getQueue();
        if (CollectionUtils.isNotEmpty(deploymentTasks)) {
            List<Deployment> deployments = new ArrayList<>();
            for (Runnable deploymentTask : deploymentTasks) {
                Deployment deployment = ((DeploymentTask)deploymentTask).deployment;
                deployments.add(deployment);
            }

            return deployments;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Deployment getCurrentDeployment() {
        return currentDeployment;
    }

    @Override
    public Collection<Deployment> getAllDeployments() {
        Collection<Deployment> deployments = getPendingDeployments();
        deployments.add(getCurrentDeployment());

        return deployments;
    }

    @Override
    public void close() {
        MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, id);

        try {
            logger.info("Closing target '{}'...", id);

            if (scheduledDeploymentFuture != null) {
                scheduledDeploymentFuture.cancel(true);
            }

            deploymentExecutor.shutdownNow();

            deploymentPipeline.destroy();

            if (applicationContext != null) {
                applicationContext.close();
            }
        } catch (Exception e) {
            logger.error("Failed to close '" + id + "'", e);
        }

        MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
    }

    protected class ScheduledDeploymentTask implements Runnable {

        protected volatile Future<?> scheduledDeploymentFuture;

        @Override
        public void run() {
            if (scheduledDeploymentFuture == null || scheduledDeploymentFuture.isDone()) {
                Deployment deployment = new Deployment(TargetImpl.this);

                scheduledDeploymentFuture = deploymentExecutor.submit(new DeploymentTask(deployment));
            }
        }

    }

    protected class DeploymentTask implements Runnable {

        protected Deployment deployment;

        public DeploymentTask(Deployment deployment) {
            this.deployment = deployment;
        }

        @Override
        public void run() {
            currentDeployment = deployment;

            MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, id);

            try {
                logger.info("------------------------------------------------------------");
                logger.info("Deployment for {} started", id);
                logger.info("------------------------------------------------------------");

                deploymentPipeline.execute(deployment);

                double durationInSecs = deployment.getDuration() / 1000.0;

                logger.info("------------------------------------------------------------");
                logger.info("Deployment for {} finished in {} secs", id, String.format("%.3f", durationInSecs));
                logger.info("------------------------------------------------------------");
            } finally {
                currentDeployment = null;

                MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
            }
        }

    }

}
