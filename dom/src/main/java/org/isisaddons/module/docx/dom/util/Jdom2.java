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
import java.io.StringReader;
import java.util.List;

import org.isisaddons.module.docx.dom.LoadInputException;
import org.isisaddons.module.docx.dom.MergeException;
import com.google.common.base.Function;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;

public final class Jdom2 {
    
    private Jdom2(){}

    public static String textValueOf(Element htmlElement) {
        List<Content> htmlContent = htmlElement.getContent();
        if(htmlContent.isEmpty()) {
            return null;
        }
        Content content = htmlContent.get(0);
        if(!(content instanceof Text)) {
            return null;
        }
        Text htmlText = (Text) content;
        return normalized(htmlText.getValue());
    }
    

    private static String normalized(String value) {
        String replaceAll = value.replaceAll("\\s+", " ");
        return replaceAll;
    }

    public static Function<Element, String> textValue() {
        return  new Function<Element, String>(){
        public String apply(Element input) {
            return textValueOf(input);
        }};
    }

    public static String attrOf(Element input, String attname) {
        Attribute attribute = input.getAttribute(attname);
        if(attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    public static org.jdom2.Document loadInput(String html) throws LoadInputException {
        try {
            return new SAXBuilder().build(new StringReader(html));
        } catch (JDOMException e) {
            throw new LoadInputException("Unable to parse input", e);
        } catch (IOException e) {
            throw new LoadInputException("Unable to parse input", e);
        }
    }

    public static Element htmlBodyFor(Document htmlDoc) throws MergeException {
        Element htmlEl = htmlDoc.getRootElement();
        Element bodyEl = htmlEl.getChild("body");
        if (bodyEl == null) {
            throw new MergeException("cannot locate body element within the input HTML");
        }
        return bodyEl;
    }

}
