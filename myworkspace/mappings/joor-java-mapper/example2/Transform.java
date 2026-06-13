try {
    javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
    org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(((String) body).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    
    org.w3c.dom.Element root = doc.getDocumentElement();
    String storeName = root.getAttribute("name");
    String location = root.getAttribute("location");
    
    org.json.JSONObject result = new org.json.JSONObject();
    result.put("store", storeName);
    result.put("city", location);
    
    org.json.JSONArray booksArray = new org.json.JSONArray();
    org.w3c.dom.NodeList books = root.getElementsByTagName("book");
    for (int i = 0; i < books.getLength(); i++) {
        org.w3c.dom.Element book = (org.w3c.dom.Element) books.item(i);
        org.json.JSONObject bookJson = new org.json.JSONObject();
        bookJson.put("category", book.getAttribute("category"));
        bookJson.put("author", book.getElementsByTagName("author").item(0).getTextContent());
        bookJson.put("title", book.getElementsByTagName("title").item(0).getTextContent());
        bookJson.put("price", Double.parseDouble(book.getElementsByTagName("price").item(0).getTextContent()));
        booksArray.put(bookJson);
    }
    result.put("catalog", booksArray);
    return result.toString(2);
} catch (Exception e) {
    return "{\"error\": \"" + e.getMessage() + "\"}";
}
