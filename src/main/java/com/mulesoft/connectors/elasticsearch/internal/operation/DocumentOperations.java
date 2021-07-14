/**
 * Copyright (c) 2003-2020, Great Software Laboratory Pvt. Ltd. The software in this package is published under the terms of the Commercial Free Software license V.2, a copy of which has been included with this distribution in the LICENSE.md file.
 */
package com.mulesoft.connectors.elasticsearch.internal.operation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.log4j.Logger;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.common.xcontent.XContentType;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.operation.Result;

import com.mulesoft.connectors.elasticsearch.api.JsonData;
import com.mulesoft.connectors.elasticsearch.api.ResponseAttributes;
import com.mulesoft.connectors.elasticsearch.api.document.DocumentConfiguration;
import com.mulesoft.connectors.elasticsearch.api.document.GetDocumentConfiguration;
import com.mulesoft.connectors.elasticsearch.api.document.IndexDocumentConfiguration;
import com.mulesoft.connectors.elasticsearch.api.document.IndexDocumentOptions;
import com.mulesoft.connectors.elasticsearch.api.document.UpdateDocumentConfiguration;
import com.mulesoft.connectors.elasticsearch.api.response.ElasticsearchGetResponse;
import com.mulesoft.connectors.elasticsearch.api.response.ElasticsearchResponse;
import com.mulesoft.connectors.elasticsearch.internal.connection.ElasticsearchConnection;
import com.mulesoft.connectors.elasticsearch.internal.error.ElasticsearchErrorTypes;
import com.mulesoft.connectors.elasticsearch.internal.error.exception.ElasticsearchException;
import com.mulesoft.connectors.elasticsearch.internal.metadata.DeleteResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.metadata.GetResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.metadata.IndexResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.metadata.ResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.metadata.UpdateResponseOutputMetadataResolver;
import com.mulesoft.connectors.elasticsearch.internal.utils.ElasticsearchDocumentUtils;
import com.mulesoft.connectors.elasticsearch.internal.utils.ElasticsearchUtils;

/**
 * @author Great Software Laboratory Pvt. Ltd.
 *
 */
public class DocumentOperations extends ElasticsearchOperations {

    /**
     * Logging object
     */
    private static final Logger logger = Logger.getLogger(DocumentOperations.class.getName());

    private final int SUCCESS_200 = 200;
    /**
     * Index Document operation adds or updates a typed JSON document in a specific index, making it searchable.
     * 
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            Name of the index
     * @param documentId
     *            ID of the document
     * @param inputSource
     *            Get the JSON input file path or index mapping.
     * @param indexDocumentConfiguration
     *            Index Document Configuration
     * @return IndexResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Document - Index")
    @OutputResolver(output = IndexResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> indexDocument(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") String index,
            @Placement(order = 2) @DisplayName("Document Id") String documentId, @Placement(order = 3) @ParameterGroup(name = "Input Document") IndexDocumentOptions inputSource,
            @Placement(tab = "Optional Arguments") @Optional IndexDocumentConfiguration indexDocumentConfiguration) {

        IndexRequest indexRequest;
        String response = null;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        try {
            if (inputSource.getJsonInputPath() != null) {
                indexRequest = new IndexRequest(index).id(documentId).source(ElasticsearchUtils.readFileToString(inputSource.getJsonInputPath()), XContentType.JSON);
            } else {
                indexRequest = new IndexRequest(index).id(documentId).source(inputSource.getDocumentSource());
            }

            if(indexDocumentConfiguration != null) {
                ElasticsearchDocumentUtils.configureIndexReq(indexRequest, indexDocumentConfiguration);
            }
            
            IndexResponse indexResp = esConnection.getElasticsearchConnection().index(indexRequest, ElasticsearchUtils.getContentTypeJsonRequestOption());

            logger.info("Index Document operation Status : " + indexResp.status());
            response = getJsonResponse(indexResp);
            inputStreamResponse = new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(indexResp.status().getStatus(), new MultiMap<>());
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
     * Get Document operation allows to get a typed JSON document from the index based on its id.
     * 
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            Name of the index
     * @param documentId
     *            ID of the document
     * @param getDocumentConfiguration
     *            Get Document configuration
     * @return GetResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Document - Get")
    @OutputResolver(output = GetResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> getDocument(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") String index,
            @Placement(order = 2) @DisplayName("Document Id") String documentId,
            @Placement(tab = "Optional Arguments") @Optional GetDocumentConfiguration getDocumentConfiguration) {

        GetRequest getRequest = new GetRequest(index, documentId);
        String response = null;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;

        try {
            if(getDocumentConfiguration != null) {
                ElasticsearchDocumentUtils.configureGetReq(getRequest, getDocumentConfiguration);
            }
            
            ElasticsearchGetResponse getResponse = new ElasticsearchGetResponse(esConnection.getElasticsearchConnection().get(getRequest, ElasticsearchUtils.getContentTypeJsonRequestOption()));
            logger.info("Get Response : " + getResponse);
            response = getJsonResponse(getResponse);
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
     * Delete Document operation allows to delete a typed JSON document from a specific index based on its id
     * 
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            Name of the index
     * @param documentId
     *            ID of the document
     * @param deleteDocumentConfiguration
     *            Delete document configuration
     * @return DeleteResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Document - Delete")
    @OutputResolver(output = DeleteResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> deleteDocument(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") String index,
            @Placement(order = 2) @DisplayName("Document Id") String documentId,
            @Placement(tab = "Optional Arguments", order = 1) @Optional DocumentConfiguration deleteDocumentConfiguration ) {
        String response = null;
        DeleteRequest deleteRequest = new DeleteRequest(index, documentId);

        DeleteResponse deleteResp;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        try {
            if(deleteDocumentConfiguration != null) {
                ElasticsearchDocumentUtils.configureDeleteDocumentReq(deleteRequest, deleteDocumentConfiguration);
            }
            
            deleteResp = esConnection.getElasticsearchConnection().delete(deleteRequest, ElasticsearchUtils.getContentTypeJsonRequestOption());
            logger.info("Delete document response : " + deleteResp);
            response = getJsonResponse(deleteResp);
            inputStreamResponse = new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(deleteResp.status().getStatus(), new MultiMap<>());
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
     * Update Document operation allows to update a document based on a script provided.
     * 
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            Name of the index
     * @param documentId
     *            ID of the document
     * @param inputSource
     *            Input document source
     * @param updateDocumentConfiguration
     *            Update document configuration
     * @return UpdateResponse as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Document - Update")
    @OutputResolver(output = UpdateResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> updateDocument(@Connection ElasticsearchConnection esConnection, @Placement(order = 1) @DisplayName("Index") String index,
            @Placement(order = 2) @DisplayName("Document Id") String documentId, @Placement(order = 3) @ParameterGroup(name = "Input Document") IndexDocumentOptions inputSource,
            @Placement(tab = "Optional Arguments") @Optional UpdateDocumentConfiguration updateDocumentConfiguration) {
        
    	String response = null;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        
        try {
            UpdateRequest updateRequest = new UpdateRequest(index, documentId);
            if (inputSource.getJsonInputPath() != null) {
                updateRequest.doc(ElasticsearchUtils.readFileToString(inputSource.getJsonInputPath()), XContentType.JSON);
            } else {
                updateRequest.doc(inputSource.getDocumentSource());
            }
            
            if(updateDocumentConfiguration != null) {
                ElasticsearchDocumentUtils.configureUpdateReq(updateRequest, updateDocumentConfiguration);
            }
            
            UpdateResponse updateResp = esConnection.getElasticsearchConnection().update(updateRequest, ElasticsearchUtils.getContentTypeJsonRequestOption());
            logger.info("Update Response : " + updateResp);
            response = getJsonResponse(updateResp);
            inputStreamResponse = new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(updateResp.status().getStatus(), new MultiMap<>());
        } catch (Exception e) {
            throw new ElasticsearchException(ElasticsearchErrorTypes.OPERATION_FAILED, e);
        }
        return Result.<InputStream, ResponseAttributes>builder() 
                .output(inputStreamResponse)
                .attributes(attributes)
                .build();
    }

    /**
     * Bulk operation makes it possible to perform many create, index, delete and update operations in a single API call.
     * 
     * @param esConnection
     *            The Elasticsearch connection
     * @param index
     *            Index name on which bulk operation performed.
     * @param jsonData
     *            Input file / data with list of operations to be performed like create, index, delete, update.
     * @return Response as JSON String
     */
    @MediaType(MediaType.APPLICATION_JSON)
    @DisplayName("Document - Bulk")
    @OutputResolver(output = ResponseOutputMetadataResolver.class)
    public Result<InputStream, ResponseAttributes> bulkOperation(@Connection ElasticsearchConnection esConnection, @Optional String index,
            @ParameterGroup(name = "Input data") JsonData jsonData) {
        String result = null;
        String resource = index != null ? "/" + index + "/_bulk" : "/_bulk";
        Map<String, String> params = Collections.singletonMap("pretty", "true");
        HttpEntity entity;
        InputStream inputStreamResponse = null;
        ResponseAttributes attributes = null;
        try {
            if (jsonData.getJsonfile() != null) {
                String jsonContent;
                jsonContent = ElasticsearchUtils.readFileToString(jsonData.getJsonfile());
                entity = new NStringEntity(jsonContent, ContentType.APPLICATION_JSON);
            } else {
                entity = new NStringEntity(jsonData.getJsonText(), ContentType.APPLICATION_JSON);
            }

            Request request = new Request("POST", resource);
            request.addParameters(params);
            request.setEntity(entity);
            ElasticsearchResponse response = new ElasticsearchResponse(esConnection.getElasticsearchConnection().getLowLevelClient().performRequest(request));
            logger.info("Bulk operation response : " + response);
            result = getJsonResponse(response);
            MultiMap<String, String> headers = new MultiMap<>();
            for(Header h : response.getHeaders()) {
            	headers.put(h.getName(), h.getValue());
            }
            inputStreamResponse = new ByteArrayInputStream(result.getBytes(Charset.forName("UTF-8")));
            attributes = new ResponseAttributes(response.getStatusLine().getStatusCode(), headers);
        } catch (Exception e) {
            throw new ElasticsearchException(ElasticsearchErrorTypes.OPERATION_FAILED, e);
        }
        return Result.<InputStream, ResponseAttributes>builder() 
                .output(inputStreamResponse)
                .attributes(attributes)
                .build();
    }
}