/*
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
package org.isisaddons.module.pdf.fixture.dom.templates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.PostConstruct;
import com.google.common.io.Resources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.isisaddons.module.pdf.fixture.dom.Order;
import org.isisaddons.module.pdf.fixture.dom.OrderLine;
import org.apache.isis.applib.annotation.ActionSemantics;
import org.apache.isis.applib.annotation.ActionSemantics.Of;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NotContributed;
import org.apache.isis.applib.annotation.NotInServiceMenu;
import org.apache.isis.applib.value.Blob;

@DomainService
public class CustomerConfirmation {

    //region > init

    private byte[] pdfAsBytes;

    @PostConstruct
    public void init() throws IOException {
        pdfAsBytes = Resources.toByteArray(Resources.getResource(this.getClass(), "CustomerConfirmation.pdf"));
    }
    //endregion

    //region > downloadCustomerConfirmation (action)

    @NotContributed(NotContributed.As.ASSOCIATION) // ie contributed as action
    @NotInServiceMenu
    @ActionSemantics(Of.SAFE)
    @MemberOrder(sequence = "10")
    public Blob downloadCustomerConfirmation(
            final Order order) throws Exception {

        try (PDDocument pdfDocument = loadAndPopulateTemplate(order)) {

            final ByteArrayOutputStream target = new ByteArrayOutputStream();
            pdfDocument.save(target);

            final String name = "customerConfirmation-" + order.getNumber() + ".pdf";
            final String mimeType = "application/pdf";
            final byte[] bytes = target.toByteArray();

            return new Blob(name, mimeType, bytes);
        }
    }
    //endregion (

    /**
     * Loads the template pdf file and populates it with the order details
     *
     * @param order The order with the details for the pdf document
     * @return The populated PDF document
     * @throws Exception If the loading or the populating of the document fails for some reason
     */
    private PDDocument loadAndPopulateTemplate(Order order) throws Exception {
        PDDocument pdfDocument = PDDocument.load(new ByteArrayInputStream(pdfAsBytes));

        PDAcroForm pdfForm = pdfDocument.getDocumentCatalog().getAcroForm();

        List<PDField> fields = pdfForm.getFields();
        SortedSet<OrderLine> orderLines = order.getOrderLines();
        for (PDField field : fields) {

            String fullyQualifiedName = field.getFullyQualifiedName();
            if ("orderDate".equals(fullyQualifiedName)) {
                field.setValue(order.getDate().toString());
            } else if ("orderNumber".equals(fullyQualifiedName)) {
                field.setValue(order.getNumber());
            } else if ("customerName".equals(fullyQualifiedName)) {
                field.setValue(order.getCustomerName());
            } else if ("message".equals(fullyQualifiedName)) {
                String message = "You have ordered '" + orderLines.size() +"' products";
                field.setValue(message);
            } else if ("preferences".equals(fullyQualifiedName)) {
                field.setValue(order.getPreferences());
            }
        }

        int i = 1;
        Iterator<OrderLine> orderLineIterator = orderLines.iterator();
        while (i < 7 && orderLineIterator.hasNext()) {
            OrderLine orderLine = orderLineIterator.next();

            String descriptionFieldName = "orderLine|"+i+"|desc";
            pdfForm.getField(descriptionFieldName).setValue(orderLine.getDescription());

            String costFieldName = "orderLine|"+i+"|cost";
            pdfForm.getField(costFieldName).setValue(orderLine.getDescription());

            String quantityFieldName = "orderLine|"+i+"|quantity";
            pdfForm.getField(quantityFieldName).setValue(orderLine.getDescription());
            i++;
        }

        return pdfDocument;
    }
}
