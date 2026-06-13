try {
    javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
    org.w3c.dom.Document doc = db.parse(new java.io.ByteArrayInputStream(((String) body).getBytes(java.nio.charset.StandardCharsets.UTF_8)));

    org.w3c.dom.Element envelope = doc.getDocumentElement();
    org.w3c.dom.Node originalWrapper = envelope.getElementsByTagName("original").item(0);
    org.w3c.dom.Node truncatedWrapper = envelope.getElementsByTagName("truncated").item(0);

    org.w3c.dom.Element originalDoc = null;
    org.w3c.dom.Element truncatedDoc = null;

    for (int i = 0; i < originalWrapper.getChildNodes().getLength(); i++) {
        if (originalWrapper.getChildNodes().item(i) instanceof org.w3c.dom.Element) {
            originalDoc = (org.w3c.dom.Element) originalWrapper.getChildNodes().item(i);
            break;
        }
    }
    for (int i = 0; i < truncatedWrapper.getChildNodes().getLength(); i++) {
        if (truncatedWrapper.getChildNodes().item(i) instanceof org.w3c.dom.Element) {
            truncatedDoc = (org.w3c.dom.Element) truncatedWrapper.getChildNodes().item(i);
            break;
        }
    }

    // Recursive merge function
    java.util.function.BiConsumer<org.w3c.dom.Element, org.w3c.dom.Element> merge = new java.util.function.BiConsumer<org.w3c.dom.Element, org.w3c.dom.Element>() {
        @Override
        public void accept(org.w3c.dom.Element orig, org.w3c.dom.Element trunc) {
            if (orig == null || trunc == null) return;

            org.w3c.dom.NodeList origChildren = orig.getChildNodes();
            boolean hasChildren = false;
            for (int i = 0; i < origChildren.getLength(); i++) {
                if (origChildren.item(i) instanceof org.w3c.dom.Element) {
                    hasChildren = true;
                    org.w3c.dom.Element child = (org.w3c.dom.Element) origChildren.item(i);
                    String nodeName = child.getLocalName();
                    
                    // Find matching child in truncated by name and position
                    int pos = 0;
                    for (int j = 0; j < i; j++) {
                        if (origChildren.item(j) instanceof org.w3c.dom.Element && 
                            origChildren.item(j).getLocalName().equals(nodeName)) {
                            pos++;
                        }
                    }

                    org.w3c.dom.NodeList truncChildren = trunc.getChildNodes();
                    org.w3c.dom.Element truncMatch = null;
                    int tPos = 0;
                    for (int j = 0; j < truncChildren.getLength(); j++) {
                        if (truncChildren.item(j) instanceof org.w3c.dom.Element && 
                            truncChildren.item(j).getLocalName().equals(nodeName)) {
                            if (tPos == pos) {
                                truncMatch = (org.w3c.dom.Element) truncChildren.item(j);
                                break;
                            }
                            tPos++;
                        }
                    }
                    accept(child, truncMatch);
                }
            }

            if (!hasChildren) {
                String truncText = trunc.getTextContent();
                if (truncText != null && !truncText.trim().isEmpty()) {
                    orig.setTextContent(truncText);
                }
            }
        }
    };

    merge.accept(originalDoc, truncatedDoc);

    // Serialize back to String
    javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
    javax.xml.transform.Transformer t = tf.newTransformer();
    t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
    t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    java.io.StringWriter sw = new java.io.StringWriter();
    t.transform(new javax.xml.transform.dom.DOMSource(originalDoc), new javax.xml.transform.stream.StreamResult(sw));
    
    // Remove blank lines to match Groovy/XSLT clean output
    return sw.toString().replaceAll("(?m)^[ \\t]*\\r?\\n", "");

} catch (Exception e) {
    java.io.StringWriter sw = new java.io.StringWriter();
    e.printStackTrace(new java.io.PrintWriter(sw));
    return "Error: " + e.toString() + "\n" + sw.toString();
}
