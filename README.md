# isis-module-pdf #

[![Build Status](https://travis-ci.org/isisaddons/isis-module-pdf.png?branch=master)](https://travis-ci.org/isisaddons/isis-module-pdf)

This module, intended for use with [Apache Isis](http://isis.apache.org), provides a merge capability of input
data into an PDF (Portable Document Format) template.

The module consists of a single demo domain service, `org.isisaddons.module.pdf.fixture.dom.templates.CustomerConfirmation`.
It provides a single action method that loads a`.pdf` template and populates it with the details of an Order entity.

The implementation uses [Apache PDFBox](http://pdfbox.apache.org/) for reading and writing the PDF documents.

## Screenshots and Usage ##

The following screenshots and code fragments show an example app's usage of the module.

#### Installing the Fixture Data ####

Installing fixture data...

![](https://raw.github.com/martin-g/isis-module-pdf/master/images/example-app-install-fixtures.png)

... creates a single demo `Order` entity, with properties of different data types and a collection of child (`OrderLine`) entities: 

![](https://raw.github.com/martin-g/isis-module-pdf/master/images/example-app-order-entity.png)


#### The .pdf template ####

The template `.pdf` is created with Libre Office Writer by using Form controls (View > Toolbars > Form Controls).
Each form control is used as a placeholder for a detail in the Order entity class.
See an [article](http://www.maketecheasier.com/create-a-pdf-with-fillable-forms-in-libreoffice/) in the web with the steps to create the document and
to export it as a PDF with form.

![](https://raw.github.com/martin-g/isis-module-pdf/master/images/document-with-form-libreoffice.png)

![](https://raw.github.com/martin-g/isis-module-pdf/master/images/document-with-form-evince.png)

#### Generating the Document ####

In the example app's design the `CustomerConfirmation` example domain service is in essence an intelligent wrapper
around the `CustomerConfirmation.pdf` template.  It contributes one actions to `Order` - `downloadCustomerConfirmation()`.

The `.pdf` is simply loaded as a simple resource from the classpath:
  
```java
@DomainService
public class CustomerConfirmation {

    private byte[] pdfAsBytes;

    @PostConstruct
    public void init() throws IOException {
        pdfAsBytes = Resources.toByteArray(Resources.getResource(this.getClass(), "CustomerConfirmation.pdf"));
    }
    ...
}
```

Then, in `loadAndPopulateTemplate(Order)` helper method we create the PDDocument out of the template bytes and populate its form controls with
the order details:

```java
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
```

And finally in the `downloadCustomerConfirmation` contributed action the `CustomerConfirmation` just creates and returns
a `org.apache.isis.applib.value.Blob` with the PDF mime type and content:

```java
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
```

The streamed to the browser PDF document looks like:

![](https://raw.github.com/martin-g/isis-module-pdf/master/images/populated-pdf.png)

## How to run the Demo App ##

The prerequisite software is:

* Java JDK 7 (nb: Isis currently does not support JDK 8)
* [maven 3](http://maven.apache.org) (3.2.x is recommended).

To build the demo app:

    git clone https://github.com/martin-g/isis-module-pdf.git
    mvn clean install

To run the demo app:

    mvn antrun:run -P self-host
    
Then log on using user: `sven`, password: `pass`


## How to configure/use ##

You can either use this module "out-of-the-box", or you can fork this repo and extend to your own requirements. 



#### Forking the repo ####

If instead you want to extend this module's functionality, then we recommend that you fork this repo.  The repo is 
structured as follows:

* `pom.xml`    // parent pom
* `fixture`    // fixtures, holding a sample domain objects and fixture scripts
* `integtests` // integration tests for the module; depends on `fixture`
* `webapp`     // demo webapp (see above screenshots); depends on `fixture`

The project is just a demo how to generate a PDF filled with your entity's details and stream it back to the user..
The versions of the modules are purposely left at `0.0.1-SNAPSHOT` because they are not intended to be released.


## Change Log ##


## Legal Stuff ##
 
#### License ####

    Licensed under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.


#### Dependencies ####

In addition to Apache Isis, this module depends on:

* `org.apache.pdfbox:pdfbox` (ASL v2.0 License)
