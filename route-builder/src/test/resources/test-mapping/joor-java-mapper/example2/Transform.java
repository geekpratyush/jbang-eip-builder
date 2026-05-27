package com.routebuilder.dynamic;

import org.json.JSONObject;
import org.json.JSONArray;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class Mapper {
    public static String map(String body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            
            Element root = doc.getDocumentElement();
            String storeName = root.getAttribute("name");
            String location = root.getAttribute("location");
            
            JSONObject result = new JSONObject();
            result.put("store", storeName);
            result.put("city", location);
            
            JSONArray booksArray = new JSONArray();
            NodeList books = root.getElementsByTagName("book");
            for (int i = 0; i < books.getLength(); i++) {
                Element book = (Element) books.item(i);
                JSONObject bookJson = new JSONObject();
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
    }
}
