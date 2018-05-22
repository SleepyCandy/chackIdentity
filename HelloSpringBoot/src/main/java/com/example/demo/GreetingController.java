package com.example.demo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.soap.*;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Controller
@RestController
public class GreetingController {

    public static Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @GetMapping("/Id/{id}")
    public String greeting(@PathVariable(name="id") String Id) {

    String soapEndpointUrl = "https://rdws.rd.go.th/serviceRD3/checktinpinservice.asmx?WSDL";
    String soapAction = "https://rdws.rd.go.th/serviceRD3/checktinpinservice/ServiceTIN";
    System.out.println(Id);
    return callSoapWebService(soapEndpointUrl, soapAction,Id);
}

    private static void createSoapEnvelope(SOAPMessage soapMessage,String Id) throws SOAPException {
        SOAPPart soapPart = soapMessage.getSOAPPart();

        String myNamespace = "ServiceTIN";
        String myNamespaceURI = "https://rdws.rd.go.th/serviceRD3/checktinpinservice";

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration(myNamespace, myNamespaceURI);


            /*
            Constructed SOAP Request Message:
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:myNamespace="http://www.webserviceX.NET">
                <SOAP-ENV:Header/>
                <SOAP-ENV:Body>
                    <myNamespace:GetInfoByCity>
                        <myNamespace:USCity>New York</myNamespace:USCity>
                    </myNamespace:GetInfoByCity>
                </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
            */

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement elementServiceTin_ = soapBody.addChildElement("ServiceTIN",myNamespace);
        SOAPElement elementUsername_ = elementServiceTin_.addChildElement("username",myNamespace);
        elementUsername_.addTextNode("anonymous");
        SOAPElement elementPassword_ = elementServiceTin_.addChildElement("password",myNamespace);
        elementPassword_.addTextNode("anonymous");
        SOAPElement elementTin = elementServiceTin_.addChildElement("TIN",myNamespace);
        elementTin.addTextNode(Id);
    }

    private String callSoapWebService(String soapEndpointUrl, String soapAction, String Id) {
        try {
            // Create SOAP Connection

            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Set trust all certificates context to HttpsURLConnection
            // Send SOAP Message to SOAP Server

            // hack certificate method
            SOAPMessage soapResponse = sendSoapRequest(soapEndpointUrl,createSOAPRequest(soapAction,Id));
//            SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(soapAction), soapEndpointUrl);

            // Print the SOAP Response
            System.out.println("Response SOAP Message:");
            soapResponse.writeTo(System.out);
            //any element you want ^^
            try{
                //Element you choose
                NodeList nodes = soapResponse.getSOAPBody().getElementsByTagName("vDigitOk");
                logger.info(getNode(nodes));
                soapConnection.close();
                return getNode(nodes);
            }catch (Exception e)
            {
                NodeList nodes = soapResponse.getSOAPBody().getElementsByTagName("faultstring");
                logger.info(getNode(nodes));
                soapConnection.close();
                return getNode(nodes);
            }



        } catch (Exception e) {
            System.err.println("\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
            e.printStackTrace();
        }

        return "no Element";
    }

    public String getNode(NodeList nodes){
        String someMsgContent = null;
        Node node = nodes.item(0);
        someMsgContent = node != null ? node.getTextContent() : "";
//        someMsgContent = someMsgContent.substring(42);
        System.out.println(someMsgContent);
        return someMsgContent;
    }

    private static SOAPMessage createSOAPRequest(String soapAction,String Id) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();

        createSoapEnvelope(soapMessage,Id);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", soapAction);

        soapMessage.saveChanges();

        /* Print the request message, just for debugging purposes */
        System.out.println("Request SOAP Message:");
        soapMessage.writeTo(System.out);
        System.out.println("\n");

        return soapMessage;
    }

    public static SOAPMessage sendSoapRequest(String endpointUrl, SOAPMessage request) {
        try {
            final boolean isHttps = endpointUrl.toLowerCase().startsWith("https");
            HttpsURLConnection httpsConnection = null;
            // Open HTTPS connection
            if (isHttps) {
                // Create SSL context and trust all certificates
                SSLContext sslContext = SSLContext.getInstance("SSL");
                TrustManager[] trustAll
                        = new TrustManager[] {new TrustAllCertificates()};
                sslContext.init(null, trustAll, new java.security.SecureRandom());
                // Set trust all certificates context to HttpsURLConnection
                HttpsURLConnection
                        .setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                // Open HTTPS connection
                URL url = new URL(endpointUrl);
                httpsConnection = (HttpsURLConnection) url.openConnection();
                // Trust all hosts
                httpsConnection.setHostnameVerifier(new TrustAllHosts());
                // Connect
                httpsConnection.connect();
            }
            // Send HTTP SOAP request and get response
            SOAPConnection soapConnection
                    = SOAPConnectionFactory.newInstance().createConnection();
            SOAPMessage response = soapConnection.call(request, endpointUrl);
            // Close connection
            soapConnection.close();
            // Close HTTPS connection
            if (isHttps) {
                System.out.println("isHttps");
                httpsConnection.disconnect();
            }
            return response;
        } catch (SOAPException | IOException | NoSuchAlgorithmException | KeyManagementException ex) {
            // Do Something
            System.err.println("\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
        }
        return null;
    }
}