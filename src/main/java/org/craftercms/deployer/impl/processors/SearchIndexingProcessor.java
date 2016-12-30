package org.craftercms.deployer.impl.processors;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.TargetContext;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.deployer.impl.CommonConfigurationProperties.TARGET_ROOT_FOLDER_PROPERTY_NAME;

/**
 * Created by alfonsovasquez on 12/26/16.
 */
public class SearchIndexingProcessor extends AbstractDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingProcessor.class);

    public static final String DEFAULT_INDEX_ID_FORMAT = "%s-default";

    public static final String INDEX_ID_PROPERTY_NAME = "indexId";
    public static final String SITE_NAME_PROPERTY_NAME = "siteName";
    public static final String INDEX_ID_FORMAT_PROPERTY_NAME = "indexIdFormat";
    public static final String IGNORE_INDEX_ID_PROPERTY_NAME = "ignoreIndexId";

    protected String rootFolder;
    protected String indexId;
    protected String siteName;
    protected SearchService searchService;
    protected List<BatchIndexer> batchIndexers;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setBatchIndexer(BatchIndexer batchIndexer) {
        this.batchIndexers = Collections.singletonList(batchIndexer);
    }

    public void setBatchIndexers(List<BatchIndexer> batchIndexers) {
        this.batchIndexers = batchIndexers;
    }

    @Override
    public void doInit(Configuration mainConfig, Configuration processorConfig) throws DeploymentException {
        // Force, we never want to execute on empty change set
        executeOnEmptyChangeSet = false;
        rootFolder = ConfigurationUtils.getRequiredString(mainConfig, TARGET_ROOT_FOLDER_PROPERTY_NAME);
        indexId = ConfigurationUtils.getString(processorConfig, INDEX_ID_PROPERTY_NAME);
        siteName = ConfigurationUtils.getString(processorConfig, SITE_NAME_PROPERTY_NAME);

        if (StringUtils.isEmpty(siteName)) {
            siteName = targetId;
        }

        boolean ignoreIndexId = ConfigurationUtils.getBoolean(processorConfig, IGNORE_INDEX_ID_PROPERTY_NAME, false);
        String indexIdFormat = ConfigurationUtils.getString(processorConfig, INDEX_ID_FORMAT_PROPERTY_NAME, DEFAULT_INDEX_ID_FORMAT);

        if (ignoreIndexId) {
            indexId = null;
        } else if (StringUtils.isEmpty(indexId)) {
            indexId = String.format(indexIdFormat, siteName);
        }

        if (CollectionUtils.isEmpty(batchIndexers)) {
            throw new IllegalStateException("At least one batch indexer should be provided");
        }
    }

    @Override
    public void destroy() throws DeploymentException {
    }

    @Override
    public void doExecute(Deployment deployment, ProcessorExecution execution, TargetContext context) throws DeploymentException {
        logger.info("Performing search indexing...");

        ChangeSet changeSet = deployment.getChangeSet();
        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();
        IndexingStatus indexingStatus = new IndexingStatus();

        execution.setStatusDetails(indexingStatus);

        try {
            if (CollectionUtils.isNotEmpty(createdFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, rootFolder, createdFiles, false, indexingStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, rootFolder, updatedFiles, false, indexingStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, rootFolder, deletedFiles, true, indexingStatus);
                }
            }

            if (indexingStatus.getAttemptedUpdatesAndDeletes() > 0) {
                searchService.commit(indexId);
            } else {
                logger.info("No files indexed");
            }
        } catch (Exception e) {
            throw new DeploymentException("Error while performing search indexing", e);
        }
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

}
