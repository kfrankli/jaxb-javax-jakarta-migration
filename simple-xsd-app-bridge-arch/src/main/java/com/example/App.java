package com.example;

// Import Jakarta types for your application code logic
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;

// Import explicit Javax types for handling the legacy library objects
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class App {
    public static void main(String[] args) {
        try {
            com.northpolesouthern.ObjectFactory factory = new com.northpolesouthern.ObjectFactory();
            
            // 1. Create the raw data object (TrainType)
            com.northpolesouthern.TrainType myTrainData = factory.createTrainType();
            myTrainData.setId(1045);
            myTrainData.setOrigin("Chicago");
            myTrainData.setDestination("Seattle");
            myTrainData.setAxles(44);

            // 2. Receive the legacy object from the factory
            javax.xml.bind.JAXBElement<com.northpolesouthern.TrainType> legacyElem = factory.createTrain(myTrainData);

            // --- Marshalling (Using Legacy Javax Context) ---
            System.out.println("--- Marshalling (Java to XML) ---");
            
            // Explicitly build a Javax context so it reads the legacy annotations correctly
            javax.xml.bind.JAXBContext javaxContext = javax.xml.bind.JAXBContext.newInstance(com.northpolesouthern.ObjectFactory.class);
            javax.xml.bind.Marshaller marshaller = javaxContext.createMarshaller();
            
            // Explicitly use the Javax property string key
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            StringWriter xmlWriter = new StringWriter();
            
            // Marshal the legacy element directly using the legacy marshaller
            marshaller.marshal(legacyElem, xmlWriter);
            
            String xmlOutput = xmlWriter.toString();
            System.out.println(xmlOutput);


            // --- Unmarshalling (Using Modern Jakarta Context) ---
            System.out.println("--- Unmarshalling (XML to Java) ---");

            // 1. Set up the modern Jakarta context
            jakarta.xml.bind.JAXBContext jakartaContext = jakarta.xml.bind.JAXBContext.newInstance(com.northpolesouthern.TrainType.class);
            jakarta.xml.bind.Unmarshaller unmarshaller = jakartaContext.createUnmarshaller();

            // 2. Create the raw XML readers
            StringReader xmlReader = new StringReader(xmlOutput);
            org.xml.sax.InputSource inputSource = new org.xml.sax.InputSource(xmlReader);

            // 3. ✅ THE FIX: Create a SAX filter that ignores the legacy XML namespace
            org.xml.sax.helpers.XMLFilterImpl namespaceFilter = new org.xml.sax.helpers.XMLFilterImpl() {
                @Override
                public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws org.xml.sax.SAXException {
                    // Force the namespace URI to be blank so Jakarta matches the fields structurally
                    super.startElement("", localName, qName, atts);
                }
            };

            // 4. Connect the filter to a SAX reader engine
            javax.xml.transform.sax.SAXSource saxSource = new javax.xml.transform.sax.SAXSource(
                org.xml.sax.helpers.XMLReaderFactory.createXMLReader(), 
                inputSource
            );
            namespaceFilter.setParent(saxSource.getXMLReader());
            saxSource.setXMLReader(namespaceFilter);

            // 5. Unmarshal using the filtered SAXSource directly into the TrainType wrapper
            jakarta.xml.bind.JAXBElement<com.northpolesouthern.TrainType> unmarshalledElement = 
                unmarshaller.unmarshal(saxSource, com.northpolesouthern.TrainType.class);

            // 6. Extract the underlying TrainType data payload
            com.northpolesouthern.TrainType parsedTrain = unmarshalledElement.getValue();
            
            System.out.println("Successfully parsed XML back into Java:");
            System.out.println("Train ID : " + parsedTrain.getId());
            System.out.println("Route    : " + parsedTrain.getOrigin() + " -> " + parsedTrain.getDestination());



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
