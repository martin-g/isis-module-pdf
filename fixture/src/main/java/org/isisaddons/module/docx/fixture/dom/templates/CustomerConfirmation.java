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
package org.isisaddons.module.docx.fixture.dom.templates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.PostConstruct;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.util.PDFText2HTML;
import org.isisaddons.module.docx.dom.LoadTemplateException;
import org.isisaddons.module.docx.dom.PdfService;
import org.isisaddons.module.docx.fixture.dom.Order;
import org.isisaddons.module.docx.fixture.dom.OrderLine;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.ActionSemantics;
import org.apache.isis.applib.annotation.ActionSemantics.Of;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NotContributed;
import org.apache.isis.applib.annotation.NotInServiceMenu;
import org.apache.isis.applib.annotation.Prototype;
import org.apache.isis.applib.value.Blob;
import org.apache.isis.applib.value.Clob;

@DomainService
public class CustomerConfirmation {

    //region > init

    private byte[] pdfAsBytes;

    @PostConstruct
    public void init() throws IOException, LoadTemplateException {
        pdfAsBytes = Resources.toByteArray(Resources.getResource(this.getClass(), "CustomerConfirmation.pdf"));
    }
    //endregion

    private PDDocument loadTemplate(Order order) throws Exception {
        byte[] bbs = Resources.toByteArray(Resources.getResource(this.getClass(), "CustomerConfirmation.pdf"));
        PDDocument pdfDocument = pdfService.loadDocument(new ByteArrayInputStream(bbs));

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
//
//            System.err.println("Field fqn: " + field.getFullyQualifiedName());
//            System.err.println("Field partial name: " + field.getPartialName());
//            System.err.println("Field alternate field name: " + field.getAlternateFieldName());
//            System.err.println("Field type: " + field.getFieldType());
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

    //region > downloadCustomerConfirmation (action)

    @NotContributed(NotContributed.As.ASSOCIATION) // ie contributed as action
    @NotInServiceMenu
    @ActionSemantics(Of.SAFE)
    @MemberOrder(sequence = "10")
    public Blob downloadCustomerConfirmation(
            final Order order) throws Exception {

        try (PDDocument pdfDocument = loadTemplate(order)) {

            final ByteArrayOutputStream docxTarget = new ByteArrayOutputStream();
            pdfDocument.save(docxTarget);
            pdfDocument.close();
            //        pdfService.merge(w3cDocument, pdfForm, docxTarget, PdfService.MatchingPolicy.LAX);

            final String blobName = "customerConfirmation-" + order.getNumber() + ".pdf";
            final String blobMimeType = "application/pdf";
            final byte[] blobBytes = docxTarget.toByteArray();

            return new Blob(blobName, blobMimeType, blobBytes);
        }
    }

    @Hidden // it seems the form data is not exported ...
    @NotContributed(NotContributed.As.ASSOCIATION) // ie contributed as action
    @Prototype
    @NotInServiceMenu
    @ActionSemantics(Of.SAFE)
    @MemberOrder(sequence = "11")
    public Clob downloadCustomerConfirmationInputHtml(
            final Order order) throws Exception {

        try (
            PDDocument pdfDocument = loadTemplate(order);
        ) {
            PDFText2HTML pdf2html = new PDFText2HTML("UTF-8");
            pdf2html.setForceParsing(true);
            pdf2html.setSortByPosition(true);
            pdf2html.setShouldSeparateByBeads(true);
            pdf2html.setStartPage(0);
            pdf2html.setEndPage(Integer.MAX_VALUE);
            String html = pdf2html.getText(pdfDocument);

            final String clobName = "customerConfirmation-" + order.getNumber() + ".html";
            final String clobMimeType = "text/html";
            final String clobBytes = html;

            return new Clob(clobName, clobMimeType, clobBytes);
        }
    }

    private static org.w3c.dom.Document asInputW3cDocument(Order order) throws JDOMException {
        Document orderAsHtmlJdomDoc = asInputDocument(order);

        DOMOutputter domOutputter = new DOMOutputter();
        return domOutputter.output(orderAsHtmlJdomDoc);
    }

    private static Document asInputDocument(Order order) {
        Element html = new Element("html");
        Document document = new Document(html);

        Element body = new Element("body");
        html.addContent(body);

        addPara(body, "OrderNum", "plain", order.getNumber());
        addPara(body, "OrderDate", "date", order.getDate().toString("dd-MMM-yyyy"));
        addPara(body, "CustomerName", "plain", order.getCustomerName());
        addPara(body, "Message", "plain", "Thank you for shopping with us!");

        Element table = addTable(body, "Products");
        for(OrderLine orderLine: order.getOrderLines()) {
            addTableRow(table, new String[]{orderLine.getDescription(), orderLine.getCost().toString(), ""+orderLine.getQuantity()});
        }

        Element ul = addList(body, "OrderPreferences");
        for(String preference: preferencesFor(order)) {
            addListItem(ul, preference);
        }
        return document;
    }

    //endregion (

    //region > helpers

    private static final Function<String, String> TRIM = new Function<String, String>() {
        @Override
        public String apply(String input) {
            return input.trim();
        }
    };

    private static Iterable<String> preferencesFor(Order order) {
        final String preferences = order.getPreferences();
        if(preferences == null) {
            return Collections.emptyList();
        }
        return Iterables.transform(Splitter.on(",").split(preferences), TRIM);
    }


    private static void addPara(Element body, String id, String clazz, String text) {
        Element p = new Element("p");
        body.addContent(p);
        p.setAttribute("id", id);
        p.setAttribute("class", clazz);
        p.setText(text);
    }

    private static Element addList(Element body, String id) {
        Element ul = new Element("ul");
        body.addContent(ul);
        ul.setAttribute("id", id);
        return ul;
    }

    private static Element addListItem(Element ul, String... paras) {
        Element li = new Element("li");
        ul.addContent(li);
        for (String para : paras) {
            addPara(li, para);
        }
        return ul;
    }

    private static void addPara(Element li, String text) {
        if(text == null) {
            return;
        }
        Element p = new Element("p");
        li.addContent(p);
        p.setText(text);
    }

    private static Element addTable(Element body, String id) {
        Element table = new Element("table");
        body.addContent(table);
        table.setAttribute("id", id);
        return table;
    }

    private static void addTableRow(Element table, String[] cells) {
        Element tr = new Element("tr");
        table.addContent(tr);
        for (String columnName : cells) {
            Element td = new Element("td");
            tr.addContent(td);
            td.setText(columnName);
        }
    }
    //endregion

    //region > injected services

    @javax.inject.Inject
    DomainObjectContainer container;

    @javax.inject.Inject
    private PdfService pdfService;

    //endregion

}
