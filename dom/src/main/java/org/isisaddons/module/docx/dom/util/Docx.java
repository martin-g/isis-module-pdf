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
package org.isisaddons.module.docx.dom.util;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.fdf.FDFDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.isisaddons.module.docx.dom.MergeException;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.Body;
import org.docx4j.wml.R;
import org.docx4j.wml.SdtElement;
import org.docx4j.wml.Tag;

public final class Docx {
    private Docx() {
    }

    public static Function<SdtElement, String> tagToValue() {
        return  new Function<SdtElement, String>(){
            public String apply(SdtElement input) {
                return input.getSdtPr().getTag().getVal();
            }
        };
    }

    public static Predicate<Object> withAnyTag() {
        return new Predicate<Object>(){
            public boolean apply(Object object) {
                if(!(object instanceof SdtElement)) {
                    return false;
                }
                SdtElement sdtBlock = (SdtElement) object;
                Tag tag = sdtBlock.getSdtPr().getTag();
                return tag != null;
            }
        };
    }

    public static Predicate<Object> withTagVal(final String tagVal) {
        return new Predicate<Object>(){
            public boolean apply(Object object) {
                if(!(object instanceof SdtElement)) {
                    return false;
                }
                SdtElement sdtBlock = (SdtElement) object;
                Tag tag = sdtBlock.getSdtPr().getTag();
                return tag != null && Objects.equal(tagVal, tag.getVal());
            }
            
        };
    }

    @SuppressWarnings({ "rawtypes", "restriction" })
    public
    static boolean setText(R run, String value) {
        List<Object> runContent = run.getContent();
        if(runContent.isEmpty()) {
            return false;
        }
        Object jaxbElObj = runContent.get(0);
        
        if(!(jaxbElObj instanceof javax.xml.bind.JAXBElement)) {
            return false;
        } 
        javax.xml.bind.JAXBElement jaxbElement = (javax.xml.bind.JAXBElement) jaxbElObj;
        Object textObj = jaxbElement.getValue();
        if(!(textObj instanceof org.docx4j.wml.Text)) {
            return false;
        }
        org.docx4j.wml.Text text = (org.docx4j.wml.Text) textObj;
        text.setValue(value);
        return true;
    }

    public static Body docxBodyFor(WordprocessingMLPackage docxPkg) {
        MainDocumentPart docxMdp = docxPkg.getMainDocumentPart();
        
        org.docx4j.wml.Document docxDoc = docxMdp.getJaxbElement();
        return docxDoc.getBody();
    }

    public static PDAcroForm clone(PDAcroForm pdfTemplate) throws MergeException {
        PDAcroForm copy;
        try {
            PDDocument document = pdfTemplate.getDocument();
            FDFDocument fdfDocument = pdfTemplate.exportFDF();
            copy = new PDAcroForm(document);
            copy.importFDF(fdfDocument);
        } catch (IOException e) {
            throw new MergeException("unable to defensive copy (problem exporting)", e);
        }
        return copy;
    }

}
