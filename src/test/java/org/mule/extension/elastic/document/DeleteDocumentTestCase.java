/**
 * Copyright (c) 2003-2017, Great Software Laboratory Pvt. Ltd. The software in this package is published under the terms of the Commercial Free Software license V.1, a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.elastic.document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

/**
 * @author Great Software Laboratory Pvt. Ltd.
 *
 */
public class DeleteDocumentTestCase extends MuleArtifactFunctionalTestCase {

    @Override
    protected String getConfigFile() {
        return "testElasticsearchOperations.xml";
    }

    /**
     * Setup the resources required to run the operation
     */
    @Before
    public void setup() {

        try {
            flowRunner("testCreateIndexFlow").run().getMessage().getPayload().getValue();
            flowRunner("indexDocumentFlow").run().getMessage().getPayload().getValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove the resources generated by the operation
     */
    @After
    public void tearDown() {
        try {
            flowRunner("testDeleteIndexFlow").run().getMessage().getPayload().getValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the operation
     */
    @Test
    public void executeDeleteDocumentOperation() {
        try {
            Object payloadValue = flowRunner("testDeleteOperationFlow").run().getMessage().getPayload().getValue();
            assertThat(payloadValue, notNullValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}