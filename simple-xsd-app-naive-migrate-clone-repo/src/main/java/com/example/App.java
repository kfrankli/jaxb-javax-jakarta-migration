package com.example;

// Swap javax to jakarta
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBElement;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Marshaller;
//import javax.xml.bind.Unmarshaller;
//import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import java.io.StringReader;
import java.io.StringWriter;

public class App {
    public static void main(String[] args) {
        try {
            com.northpolesouthern.ObjectFactory factory = new com.northpolesouthern.ObjectFactory();
            
            // Create the raw data object (now called TrainType)
            com.northpolesouthern.TrainType myTrainData = factory.createTrainType();
            myTrainData.setId(1045);
            myTrainData.setOrigin("Chicago");
            myTrainData.setDestination("Seattle");
            myTrainData.setAxles(44);

            // Wrap it in a formerly javax.xml.bind, now jakarta.xml.bind JAXBElement using the ObjectFactory
            //      but this is going to fail due to the transative dependency on javax.xml.bind that isn't available 
            //      from java 11+
            JAXBElement<com.northpolesouthern.TrainType> trainElement = factory.createTrain(myTrainData);

            // Notice we initialize the context with the ObjectFactory class now, 
            //      since TrainType doesn't have an @XmlRootElement annotation.
            JAXBContext jaxbContext = JAXBContext.newInstance(com.northpolesouthern.ObjectFactory.class);

            // --- Marshalling ---
            System.out.println("--- Marshalling (Java to XML) ---");
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            StringWriter xmlWriter = new StringWriter();
            
            // Pass the JAXBElement to the marshaller
            marshaller.marshal(trainElement, xmlWriter);
            
            String xmlOutput = xmlWriter.toString();
            System.out.println(xmlOutput);

            // --- Unmarshalling ---
            System.out.println("--- Unmarshalling (XML to Java) ---");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            StringReader xmlReader = new StringReader(xmlOutput);
            StreamSource source = new StreamSource(xmlReader);
            
            // Unmarshal directly into a JAXBElement by specifying the expected type
            JAXBElement<com.northpolesouthern.TrainType> unmarshalledElement = 
                unmarshaller.unmarshal(source, com.northpolesouthern.TrainType.class);
            
            // Extract the underlying TrainType data payload
            com.northpolesouthern.TrainType parsedTrain = unmarshalledElement.getValue();
             
            System.out.println("Successfully parsed XML back into Java:");
            System.out.println("Train ID : " + parsedTrain.getId());
            System.out.println("Route    : " + parsedTrain.getOrigin() + " -> " + parsedTrain.getDestination());

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
