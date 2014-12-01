/*
 *  Copyright 2013~2014 Dan Haywood
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.isisaddons.module.docx.integtests;

import java.util.List;
import javax.inject.Inject;
import org.isisaddons.module.docx.fixture.dom.Order;
import org.isisaddons.module.docx.fixture.dom.Orders;
import org.isisaddons.module.docx.fixture.dom.templates.CustomerConfirmation;
import org.isisaddons.module.docx.fixture.scripts.DocxModuleAppSetupFixture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.isis.applib.value.Blob;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class CustomerConfirmationTest extends PdfModuleIntegTest {

    @Before
    public void setUpData() throws Exception {
        scenarioExecution().install(new DocxModuleAppSetupFixture());
    }

    @Inject
    private Orders orders;

    @Inject
    private CustomerConfirmation customerConfirmation;

    private Order order;

    @Before
    public void setUp() throws Exception {
        final List<Order> all = wrap(orders).listAll();
        assertThat(all.size(), is(1));

        order = all.get(0);
    }

    @Test
    public void downloadCustomerConfirmation() throws Exception {
        final Blob blob = customerConfirmation.downloadCustomerConfirmation(order);
        Assert.assertThat(blob.getName(), is("customerConfirmation-1234.pdf"));
        Assert.assertThat(blob.getMimeType().getBaseType(), is("application/pdf"));
        Assert.assertThat(blob.getBytes().length, is(greaterThan(0)));
    }
}
