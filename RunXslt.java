//DEPS net.sf.saxon:Saxon-HE:12.4
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RunXslt {
    public static void main(String[] args) throws Exception {
        String xsltFile = args[0];
        String sourceFile = args[1];
        
        // Since XSLT expects the SWIFT-XML format, we need to mock the /message root
        // But wait, the XSLT uses 'block1/logicalTerminal' etc.
        // That means it expects the output of Camel's unmarshal().swiftMt().
        
        // Let's create a dummy SWIFT XML based on the source.txt
        String swiftXml = "<message>" +
            "<block1><logicalTerminal>SENDERBKAXXX0000000000</logicalTerminal></block1>" +
            "<block2><receiverAddress>RECEIVERBKAXXXN</receiverAddress></block2>" +
            "<block3><tag><name>108</name><value>CBPR-PROD-001</value></tag><tag><name>121</name><value>eb5f1a7d-2b4e-4f7d-8f1a-7d2b4e4f7d8f</value></tag></block3>" +
            "<block4>" +
            "<field><name>20</name><component number='1'>SENDER-REF-CBPR</component></field>" +
            "<field><name>23B</name><component number='1'>CRED</component></field>" +
            "<field><name>32A</name><component number='1'>240609</component><component number='2'>USD</component><component number='3'>50000,00</component></field>" +
            "<field><name>50F</name><component number='1'>DEBTOR-ID-123</component><component number='2'>1/SOPHISTICATED CORP</component><component number='3'>2/123 FINANCIAL PLAZA</component><component number='4'>3/US/NEW YORK</component></field>" +
            "<field><name>52A</name><component number='3'>ORDERINGBKAXXX</component></field>" +
            "<field><name>57A</name><component number='3'>ACCOUNTBKAXXX</component></field>" +
            "<field><name>59</name><component number='1'>/4433221100</component><component number='2'>CREDITOR SERVICES LTD</component><component number='3'>456 COMMERCE WAY</component><component number='4'>LONDON, UK</component></field>" +
            "<field><name>70</name><component number='1'>CBPR+ MULTI-LINE REMITTANCE INVOICE INV-999-PROD</component></field>" +
            "<field><name>71A</name><component number='1'>SHA</component></field>" +
            "<field><name>71F</name><component number='1'>USD</component><component number='2'>15,00</component></field>" +
            "<field><name>77B</name><component number='1'>REGULATORY DATA TYPE 1 /COUNTRY/US/RESIDENT</component></field>" +
            "</block4></message>";
            
        TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
        Source xslt = new StreamSource(new File(xsltFile));
        Transformer transformer = factory.newTransformer(xslt);
        
        Source text = new StreamSource(new StringReader(swiftXml));
        transformer.transform(text, new StreamResult(System.out));
    }
}
