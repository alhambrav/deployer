package org.craftercms.deployer.impl.processors;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.store.impl.filesystem.FileSystemContentStoreAdapter;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 12/26/16.
 */
public class SearchIndexingProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingProcessor.class);

    public static final String DEFAULT_INDEX_ID_FORMAT = "%s";

    public static final String INDEX_ID_CONFIG_KEY = "indexId";
    public static final String INDEX_ID_FORMAT_CONFIG_KEY = "indexIdFormat";
    public static final String IGNORE_INDEX_ID_CONFIG_KEY = "ignoreIndexId";

    protected String targetFolderUrl;
    protected ContentStoreService contentStoreService;
    protected SearchService searchService;
    protected List<BatchIndexer> batchIndexers;
    protected Context context;
    protected String indexId;

    @Required
    public void setTargetFolderUrl(String targetFolderUrl) {
        this.targetFolderUrl = targetFolderUrl;
    }

    @Required
    public void setContentStoreService(ContentStoreService contentStoreService) {
        this.contentStoreService = contentStoreService;
    }

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

    @PostConstruct
    public void init() throws DeployerException {
        context = contentStoreService.createContext(FileSystemContentStoreAdapter.STORE_TYPE, null, null, null, targetFolderUrl,
                                                    false, 0, Context.DEFAULT_IGNORE_HIDDEN_FILES);
    }

    @PostConstruct
    public void destroy() {
        contentStoreService.destroyContext(context);
    }

    @Override
    protected void doConfigure(Configuration config) throws DeployerException {
        boolean ignoreIndexId = ConfigUtils.getBooleanProperty(config, IGNORE_INDEX_ID_CONFIG_KEY, false);
        if (ignoreIndexId) {
            indexId = null;
        } else {
            indexId = ConfigUtils.getStringProperty(config, INDEX_ID_CONFIG_KEY);
            if (StringUtils.isEmpty(indexId)) {
                String indexIdFormat = ConfigUtils.getStringProperty(config, INDEX_ID_FORMAT_CONFIG_KEY, DEFAULT_INDEX_ID_FORMAT);

                indexId = String.format(indexIdFormat, siteName);
            }
        }

        if (CollectionUtils.isEmpty(batchIndexers)) {
            throw new IllegalStateException("At least one batch indexer should be provided");
        }
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
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
                    indexer.updateIndex(indexId, siteName, contentStoreService, context, createdFiles, false, indexingStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, contentStoreService, context, updatedFiles, false, indexingStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, contentStoreService, context, deletedFiles, true, indexingStatus);
                }
            }

            if (indexingStatus.getAttemptedUpdatesAndDeletes() > 0) {
                searchService.commit(indexId);
            } else {
                logger.info("No files indexed");
            }
        } catch (Exception e) {
            throw new DeployerException("Error while performing search indexing", e);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

}
