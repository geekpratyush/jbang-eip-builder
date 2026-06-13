//DEPS org.apache.camel:camel-joor:4.18.0
//DEPS org.apache.camel:camel-groovy:4.18.0

try {
    String mt = (String) body;
    if (mt == null) return "";

    String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

    // --- 1. Robust Block Extraction ---
    String b1="", b2="", b3="", b4="";
    int p1 = mt.indexOf("{1:");
    if (p1 >= 0) b1 = mt.substring(p1+3, mt.indexOf("}", p1));
    int p2 = mt.indexOf("{2:");
    if (p2 >= 0) b2 = mt.substring(p2+3, mt.indexOf("}", p2));
    int p3 = mt.indexOf("{3:");
    if (p3 >= 0) b3 = mt.substring(p3+3, mt.lastIndexOf("}", mt.indexOf("{4:") > 0 ? mt.indexOf("{4:") : mt.length()));
    int p4 = mt.indexOf("{4:");
    if (p4 >= 0) b4 = mt.substring(p4+3, mt.lastIndexOf("-}", mt.indexOf("{5:") > 0 ? mt.indexOf("{5:") : mt.length()));

    // --- 2. Field Parsing ---
    java.util.Map<String, java.util.List<String>> fields = new java.util.HashMap<>(32);
    java.util.regex.Pattern P_FIELD = java.util.regex.Pattern.compile(":(\\d{2}[A-Z]?):(.*?)(?=\\r?\\n:|\\r?\\n-|$)", java.util.regex.Pattern.DOTALL);
    java.util.regex.Matcher fm = P_FIELD.matcher(b4);
    while (fm.find()) {
        fields.computeIfAbsent(fm.group(1), k -> new java.util.ArrayList<>(2)).add(fm.group(2).trim());
    }

    java.util.function.Function<String, String> get = (tag) -> {
        java.util.List<String> list = fields.get(tag);
        return list != null && !list.isEmpty() ? list.get(0) : "";
    };

    // --- 3. Tag Extraction ---
    String msgId = "", uetr = "";
    if (b3.contains("{108:")) msgId = b3.substring(b3.indexOf("{108:")+5, b3.indexOf("}", b3.indexOf("{108:")));
    if (b3.contains("{121:")) uetr = b3.substring(b3.indexOf("{121:")+5, b3.indexOf("}", b3.indexOf("{121:")));

    String sender = b1.length() >= 11 ? b1.substring(0, 11) : "UNKNOWN";
    if (b1.length() >= 14) sender = b1.substring(3, 14);
    
    String receiver = b2.length() >= 12 ? b2.substring(b2.length()-12, b2.length()-1) : "UNKNOWN";
    if (b2.length() >= 15) receiver = b2.substring(4, 15);

    String f20 = get.apply("20");
    if (msgId.isEmpty()) msgId = f20;

    String sttlmDt = "", currency = "", amount = "";
    String f32A = get.apply("32A");
    if (f32A.length() >= 12) {
        sttlmDt = "20" + f32A.substring(0, 2) + "-" + f32A.substring(2, 4) + "-" + f32A.substring(4, 6);
        currency = f32A.substring(6, 9);
        amount = f32A.substring(9).replace(",", ".");
    }

    String f71A = get.apply("71A");
    String mxChrg = "OUR".equals(f71A) ? "DEBT" : "BEN".equals(f71A) ? "CRED" : "SHAR";

    java.util.function.Function<String, String> esc = (text) -> {
        if (text == null || text.isEmpty()) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace(String.valueOf((char)39), "&apos;");
    };

    // --- 4. XML Construction ---
    StringBuilder sb = new StringBuilder(4096);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<Document xmlns=\"").append(NS).append("\">");
    sb.append("<FIToFICstmrCdtTrf><GrpHdr>");
    sb.append("<MsgId>").append(esc.apply(msgId)).append("</MsgId>");
    sb.append("<CreDtTm>TIMESTAMP</CreDtTm>");
    sb.append("<NbOfTxs>1</NbOfTxs><SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf>");
    sb.append("<InstgAgt><FinInstnId><BICFI>").append(sender).append("</BICFI></FinInstnId></InstgAgt>");
    sb.append("<InstdAgt><FinInstnId><BICFI>").append(receiver).append("</BICFI></FinInstnId></InstdAgt></GrpHdr>");
    
    sb.append("<CdtTrfTxInf><PmtId>");
    sb.append("<InstrId>").append(esc.apply(f20)).append("</InstrId>");
    sb.append("<EndToEndId>").append(esc.apply(f20)).append("</EndToEndId>");
    if (!uetr.isEmpty()) sb.append("<UETR>").append(uetr.toLowerCase()).append("</UETR>");
    sb.append("</PmtId>");
    
    sb.append("<PmtTpInf><SvcLvl><Cd>NURG</Cd></SvcLvl>");
    String f23B = get.apply("23B");
    if (!f23B.isEmpty()) sb.append("<LclInstrm><Prtry>").append(esc.apply(f23B)).append("</Prtry></LclInstrm>");
    sb.append("</PmtTpInf>");

    sb.append("<IntrBkSttlmAmt Ccy=\"").append(currency).append("\">").append(amount).append("</IntrBkSttlmAmt>");
    sb.append("<IntrBkSttlmDt>").append(sttlmDt).append("</IntrBkSttlmDt>");
    
    if (get.apply("23E").equals("SDVA")) sb.append("<InstrForCdtrAgt><Cd>SDVA</Cd></InstrForCdtrAgt>");

    sb.append("<ChrgBr>").append(mxChrg).append("</ChrgBr>");

    String f71F = get.apply("71F");
    if (!f71F.isEmpty()) {
        sb.append("<ChrgsInf><Amt Ccy=\"").append(f71F.substring(0,3)).append("\">").append(f71F.substring(3).replace(",", ".")).append("</Amt>");
        sb.append("<Agt><FinInstnId><BICFI>").append(sender).append("</BICFI></FinInstnId></Agt></ChrgsInf>");
    }

    sb.append("<Dbtr><Nm></Nm><PstlAdr><Ctry></Ctry><AdrLine></AdrLine></PstlAdr></Dbtr>");
    sb.append("<DbtrAcct><Id><Othr><Id>");
    String f50F = get.apply("50F");
    sb.append(esc.apply(f50F.split("\n")[0]));
    sb.append("</Id></Othr></Id></DbtrAcct>");

    String f52A = get.apply("52A");
    if (!f52A.isEmpty()) sb.append("<DbtrAgt><FinInstnId><BICFI>").append(f52A.substring(f52A.lastIndexOf("\n")+1).trim()).append("</BICFI></FinInstnId></DbtrAgt>");
    String f57A = get.apply("57A");
    if (!f57A.isEmpty()) sb.append("<CdtrAgt><FinInstnId><BICFI>").append(f57A.substring(f57A.lastIndexOf("\n")+1).trim()).append("</BICFI></FinInstnId></CdtrAgt>");

    String f59 = get.apply("59");
    String[] f59Lines = f59.split("\n");
    sb.append("<Cdtr><Nm>").append(f59Lines.length > 1 ? esc.apply(f59Lines[1]) : "").append("</Nm>");
    if (f59Lines.length > 2) sb.append("<PstlAdr><AdrLine>").append(esc.apply(f59Lines[2])).append("</AdrLine></PstlAdr>");
    sb.append("</Cdtr>");

    sb.append("<CdtrAcct><Id><Othr><Id>").append(esc.apply(f59Lines[0].replace("/", ""))).append("</Id></Othr></Id></CdtrAcct>");

    String f70 = get.apply("70");
    if (!f70.isEmpty()) sb.append("<RmtInf><Ustrd>").append(esc.apply(f70)).append("</Ustrd></RmtInf>");
    String f77B = get.apply("77B");
    if (!f77B.isEmpty()) sb.append("<RgltryRptg><Dtls><Inf>").append(esc.apply(f77B)).append("</Inf></Dtls></RgltryRptg>");

    sb.append("</CdtTrfTxInf></FIToFICstmrCdtTrf></Document>");

    String rawXml = sb.toString();
    try {
        javax.xml.transform.Transformer t = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
        t.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
        java.io.StringWriter sw = new java.io.StringWriter();
        t.transform(new javax.xml.transform.stream.StreamSource(new java.io.StringReader(rawXml)), new javax.xml.transform.stream.StreamResult(sw));
        return sw.toString().replace("standalone=\"no\" ", "");
    } catch (Exception e) {
        return rawXml;
    }
} catch (Exception e) {
    return "Error: " + e.getMessage();
}
