/**
 * Copyright (c) 2003-2020, Great Software Laboratory Pvt. Ltd. The software in this package is published under the terms of the Commercial Free Software license V.2, a copy of which has been included with this distribution in the LICENSE.md file.
 */
package com.mulesoft.connectors.elasticsearch.internal.operation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.elasticsearch.client.indices.CloseIndexRequest;
import org.elasticsearch.client.indices.CloseIndexResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.apache.log4j.Logger;

import com.mulesoft.connectors.elasticsearch.api.IndexConfiguration;
import com.mulesoft.connectors.elasticsearch.api.IndexOptions;
import com.mulesoft.connectors.elasticsearch.api.ResponseAttributes;
import com.mulesoft.connectors.elasticsearch.internal.connection.ElasticsearchConnection;
import com.mulesoft.connectors.elasticsearch.internal.error.ElasticsearchErrorTypes;
import com.mulesoft.connectors.elasticsearch.internal.error.exception.ElasticsearchException;
import com.mulesoft.connectors.elasticsearch.internal.metadata.CreateIndexResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.metadata.AcknowledgedResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.metadata.CloseIndexResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.metadata.OpenIndexResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.utils.ElasticsearchIndexUtils;
import com.mulesoft.connectors.elasticsearch.internal.utils.ElasticsearchUtils;

/**
 * @author Great Software Laboratory Pvt. Ltd.
 *
 */
public class IndexOperations extends ElasticsearchOperations {

    /**
     * Logging object
     */
    private static final Logger logger = Logger.getLogger(IndexOperations.class.getName());

    private final int SUCCESS_200 = 200;
    /**
     * The createIndex operation allows to instantiate an index.
     *
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            The index to create
     * @param indexConfiguration
     *            The index configuration
     * @return CreateIndexResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Index - Create")
    @OutputResolver(output = CreateIndexResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> createIndex(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") @Summary("The index to create") String index,
            @Placement(tab = "Optional Arguments") @Optional IndexConfiguration indexConfiguration) {
        String response = null;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        try {
            CreateIndexRequest createIndexReq = new CreateIndexRequest(index);
            if(indexConfiguration != null) {
                ElasticsearchIndexUtils.configureCreateIndexReq(createIndexReq, indexConfiguration);
            }

            CreateIndexResponse createIndexResp = esConnection.getElasticsearchConnection().indices().create(createIndexReq, ElasticsearchUtils.getContentTypeJsonRequestOption());
            logger.info("Create index acknowledged : " + createIndexResp.isAcknowledged());
            response = getJsonResponse(createIndexResp);
            inputStreamResponse = new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(SUCCESS_200, new MultiMap<>());
        } catch (IOException e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.OPERATION_FAILED, e);
        } catch (Exception e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.EXECUTION, e);
        }
        return Result.<InputStream, ResponseAttributes>builder() 
                .output(inputStreamResponse)
                .attributes(attributes)
                .build();
    }

    /**
     * The Delete index operation allows to delete an existing index.
     *
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            The index to delete
     * @param timeoutInSec
     *            Timeout in seconds to wait for the all the nodes to acknowledge the index deletion.
     * @param masterNodeTimeoutInSec
     *            Timeout in seconds to connect to the master node.
     * @param indicesOpts
     *            IndicesOptions controls how unavailable indices are resolved and how wildcard expressions are expanded
     * @return AcknowledgedResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Index - Delete")
    @OutputResolver(output = AcknowledgedResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> deleteIndex(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") @Summary("The index to delete") String index,
            @Placement(tab = "Optional Arguments", order = 1) @Optional(defaultValue = "0") @Summary("Timeout in seconds to wait for the all the nodes to acknowledge the index creation") @DisplayName("Timeout (Seconds)") long timeoutInSec,
            @Placement(tab = "Optional Arguments", order = 2) @Optional(defaultValue = "0") @Summary("Timeout in seconds to connect to the master node") @DisplayName("Mater Node Timeout (Seconds)") long masterNodeTimeoutInSec,
            @Placement(tab = "Optional Arguments", order = 3) @Optional IndexOptions indicesOpts) {
        String result = null;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);

        if (timeoutInSec != 0) {
            deleteIndexRequest.timeout(TimeValue.timeValueSeconds(timeoutInSec));
        }
        if (masterNodeTimeoutInSec != 0) {
            deleteIndexRequest.masterNodeTimeout(TimeValue.timeValueSeconds(masterNodeTimeoutInSec));
        }
        if (indicesOpts != null) {
            IndicesOptions indOptions = IndicesOptions.fromOptions(indicesOpts.isIgnoreUnavailable(), indicesOpts.isAllowNoIndices(), indicesOpts.isExpandWildcardsOpen(),
                    indicesOpts.isExpandWildcardsClosed(), indicesOpts.isAllowAliasesToMultipleIndices(), indicesOpts.isForbidClosedIndices(), indicesOpts.isIgnoreAliases(),
                    indicesOpts.isIgnoreThrottled());
            deleteIndexRequest.indicesOptions(indOptions);
        }

        AcknowledgedResponse response;
        try {
            response = esConnection.getElasticsearchConnection().indices().delete(deleteIndexRequest, ElasticsearchUtils.getContentTypeJsonRequestOption());
            logger.info("Delete Index acknowledged : " + response.isAcknowledged());
            result = getJsonResponse(response);
            inputStreamResponse = new ByteArrayInputStream(result.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(SUCCESS_200, new MultiMap<>());
        } catch (IOException e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.OPERATION_FAILED, e);
        } catch (Exception e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.EXECUTION, e);
        }
        return Result.<InputStream, ResponseAttributes>builder() 
                .output(inputStreamResponse)
                .attributes(attributes)
                .build();
    }

    /**
     * Open Index operation allow to open an index.
     *
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            The index to open
     * @param timeoutInSec
     *            Timeout in seconds to wait for the all the nodes to acknowledge the index is opened. It is the time to wait for an open index to become available to
     *            elasticsearch.
     * @param masterNodeTimeoutInSec
     *            Timeout in seconds to connect to the master node
     * @param waitForActiveShards
     *            The number of active shard copies to wait for
     * @param indicesOpts
     *            IndicesOptions controls how unavailable indices are resolved and how wildcard expressions are expanded
     * @return OpenIndexResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Index - Open")
    @OutputResolver(output = OpenIndexResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> openIndex(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") @Summary("The index to open") String index,
            @Placement(tab = "Optional Arguments", order = 1) @Optional(defaultValue = "0") @Summary("Timeout in seconds to wait for the all the nodes to acknowledge the index creation") @DisplayName("Timeout (Seconds)") long timeoutInSec,
            @Placement(tab = "Optional Arguments", order = 2) @Optional(defaultValue = "0") @Summary("Timeout in seconds to connect to the master node") @DisplayName("Mater Node Timeout (Seconds)") long masterNodeTimeoutInSec,
            @Placement(tab = "Optional Arguments", order = 3) @DisplayName("Wait for Active Shards") @Optional(defaultValue = "0") int waitForActiveShards,
            @Placement(tab = "Optional Arguments", order = 4) @Optional IndexOptions indicesOpts) {
        String response = null;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        OpenIndexRequest openIndexRequest = new OpenIndexRequest(index);

        if (timeoutInSec != 0) {
            openIndexRequest.timeout(TimeValue.timeValueSeconds(timeoutInSec));
        }
        if (masterNodeTimeoutInSec != 0) {
            openIndexRequest.masterNodeTimeout(TimeValue.timeValueSeconds(masterNodeTimeoutInSec));
        }

        if (waitForActiveShards != 0) {
            openIndexRequest.waitForActiveShards(waitForActiveShards);
        }

        if (indicesOpts != null) {
            IndicesOptions indOptions = IndicesOptions.fromOptions(indicesOpts.isIgnoreUnavailable(), indicesOpts.isAllowNoIndices(), indicesOpts.isExpandWildcardsOpen(),
                    indicesOpts.isExpandWildcardsClosed(), indicesOpts.isAllowAliasesToMultipleIndices(), indicesOpts.isForbidClosedIndices(), indicesOpts.isIgnoreAliases(),
                    indicesOpts.isIgnoreThrottled());
            openIndexRequest.indicesOptions(indOptions);
        }

        OpenIndexResponse openIndexResp;
        try {
            openIndexResp = esConnection.getElasticsearchConnection().indices().open(openIndexRequest, ElasticsearchUtils.getContentTypeJsonRequestOption());
            logger.info("Open Index acknowledged : " + openIndexResp.isAcknowledged());
            response = getJsonResponse(openIndexResp);
            inputStreamResponse = new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(SUCCESS_200, new MultiMap<>());
        } catch (IOException e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.OPERATION_FAILED, e);
        } catch (Exception e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.EXECUTION, e);
        }
        return Result.<InputStream, ResponseAttributes>builder() 
                .output(inputStreamResponse)
                .attributes(attributes)
                .build();
    }

    /**
     * A closed index has almost no overhead. It is used to close an Index. If you want to keep your data but save resources (memory/CPU), a good alternative to deleting an index
     * is to close them.
     *
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            The index to close
     * @param timeoutInSec
     *            Timeout in seconds to wait for the all the nodes to acknowledge if the index is closed
     * @param masterNodeTimeoutInSec
     *            Timeout in seconds to connect to the master node
     * @param indicesOpt
     *            IndicesOptions controls how unavailable indices are resolved and how wildcard expressions are expanded
     * @return CloseIndexResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Index - Close")
    @OutputResolver(output = CloseIndexResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> closeIndex(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") @Summary("The index to open") String index,
            @Placement(tab = "Optional Arguments", order = 1) @Optional(defaultValue = "0") @Summary("Timeout in seconds to wait for the all the nodes to acknowledge the index creation") @DisplayName("Timeout (Seconds)") long timeoutInSec,
            @Placement(tab = "Optional Arguments", order = 2) @Optional(defaultValue = "0") @Summary("Timeout in seconds to connect to the master node") @DisplayName("Mater Node Timeout (Seconds)") long masterNodeTimeoutInSec,
            @Placement(tab = "Optional Arguments", order = 3) @Optional IndexOptions indicesOpt) {
        String response;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        CloseIndexRequest closeIndexRequest = new CloseIndexRequest(index);

        if (timeoutInSec != 0) {
            closeIndexRequest.setTimeout(TimeValue.timeValueSeconds(timeoutInSec));
        }
        if (masterNodeTimeoutInSec != 0) {
            closeIndexRequest.setMasterTimeout(TimeValue.timeValueSeconds(masterNodeTimeoutInSec));
        }

        if (indicesOpt != null) {
            IndicesOptions indOptions = IndicesOptions.fromOptions(indicesOpt.isIgnoreUnavailable(), indicesOpt.isAllowNoIndices(), indicesOpt.isExpandWildcardsOpen(),
                    indicesOpt.isExpandWildcardsClosed(), indicesOpt.isAllowAliasesToMultipleIndices(), indicesOpt.isForbidClosedIndices(), indicesOpt.isIgnoreAliases(),
                    indicesOpt.isIgnoreThrottled());
            closeIndexRequest.indicesOptions(indOptions);
        }

        CloseIndexResponse closeIndexResp;
        try {
            closeIndexResp = esConnection.getElasticsearchConnection().indices().close(closeIndexRequest, ElasticsearchUtils.getContentTypeJsonRequestOption());
            logger.info("Close Index acknowledged : " + closeIndexResp.isAcknowledged());
            response = getJsonResponse(closeIndexResp);
            inputStreamResponse = new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(SUCCESS_200, new MultiMap<>());
        } catch (IOException e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.OPERATION_FAILED, e);
        } catch (Exception e) {
            logger.error(e);
            throw new ElasticsearchException(ElasticsearchErrorTypes.EXECUTION, e);
        }
        return Result.<InputStream, ResponseAttributes>builder() 
                .output(inputStreamResponse)
                .attributes(attributes)
                .build();
    }

}
