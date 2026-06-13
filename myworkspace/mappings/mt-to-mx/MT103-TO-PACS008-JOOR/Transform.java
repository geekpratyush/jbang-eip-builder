//DEPS org.apache.camel:camel-joor:4.18.0
//DEPS org.apache.camel:camel-groovy:4.18.0

try {
    String mt = (String) body;
    if (mt == null) return "";

    String NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

    // --- 1. Pre-compile Patterns (once per execution) ---
    java.util.regex.Pattern P_BLOCK = java.util.regex.Pattern.compile("\\{(\\d+):(.*?)\\}", java.util.regex.Pattern.DOTALL);
    java.util.regex.Pattern P_FIELD = java.util.regex.Pattern.compile(":(\\d{2}[A-Z]?):(.*?)(?=\\r?\\n:|\\r?\\n-}|$)", java.util.regex.Pattern.DOTALL);
    java.util.regex.Pattern P_108 = java.util.regex.Pattern.compile("\\{108:(.*?)\\}");
    java.util.regex.Pattern P_121 = java.util.regex.Pattern.compile("\\{121:(.*?)\\}");
    java.util.regex.Pattern P_50F_NAME = java.util.regex.Pattern.compile("1/(.*?)(?=\\n2/|\\n3/|$)");
    java.util.regex.Pattern P_71F = java.util.regex.Pattern.compile("([A-Z]{3})([\\d,]+)");

    // --- 2. Data Extraction (Single Pass) ---
    String b1 = "", b2 = "", b3 = "";
    java.util.regex.Matcher bm = P_BLOCK.matcher(mt);
    while (bm.find()) {
        String tag = bm.group(1);
        if ("1".equals(tag)) b1 = bm.group(2);
        else if ("2".equals(tag)) b2 = bm.group(2);
        else if ("3".equals(tag)) b3 = bm.group(2);
    }

    java.util.Map<String, java.util.List<String>> fields = new java.util.HashMap<>(32);
    java.util.regex.Matcher fm = P_FIELD.matcher(mt);
    while (fm.find()) {
        fields.computeIfAbsent(fm.group(1), k -> new java.util.ArrayList<>(2)).add(fm.group(2).trim());
    }

    java.util.function.Function<String, String> get = (tag) -> {
        java.util.List<String> list = fields.get(tag);
        return list != null && !list.isEmpty() ? list.get(0) : "";
    };

    String sender = b1.length() >= 14 ? b1.substring(3, 14).trim() : "UNKNOWN";
    String receiver = b2.length() >= 15 ? b2.substring(4, 15).trim() : "UNKNOWN";
    
    String msgId = "", uetr = "";
    if (!b3.isEmpty()) {
        java.util.regex.Matcher m108 = P_108.matcher(b3);
        if (m108.find()) msgId = m108.group(1);
        java.util.regex.Matcher m121 = P_121.matcher(b3);
        if (m121.find()) uetr = m121.group(1);
    }
    String f20 = get.apply("20");
    if (msgId.isEmpty()) msgId = f20;

    String f32A = get.apply("32A");
    String f59 = get.apply("59");
    if (f59.isEmpty()) f59 = get.apply("59A");
    String f71A = get.apply("71A");

    String sttlmDt = "", currency = "", amount = "";
    if (f32A.length() >= 12) {
        sttlmDt = "20" + f32A.substring(0, 2) + "-" + f32A.substring(2, 4) + "-" + f32A.substring(4, 6);
        currency = f32A.substring(6, 9);
        amount = f32A.substring(9).replace(",", ".");
    }
    String mxChrg = "OUR".equals(f71A) ? "DEBT" : "BEN".equals(f71A) ? "CRED" : "SHAR";

    // --- 3. Optimized XML Generation ---
    StringBuilder sb = new StringBuilder(4096);
    sb.append("<Document xmlns=\"").append(NS).append("\"><FIToFICstmrCdtTrf><GrpHdr>");
    sb.append("<MsgId>").append(msgId).append("</MsgId>");
    sb.append("<CreDtTm>").append(java.time.OffsetDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()).append("</CreDtTm>");
    sb.append("<NbOfTxs>1</NbOfTxs><SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf>");
    sb.append("<InstgAgt><FinInstnId><BICFI>").append(sender).append("</BICFI></FinInstnId></InstgAgt>");
    sb.append("<InstdAgt><FinInstnId><BICFI>").append(receiver).append("</BICFI></FinInstnId></InstdAgt></GrpHdr>");
    
    sb.append("<CdtTrfTxInf><PmtId><InstrId>").append(f20).append("</InstrId><EndToEndId>").append(f20).append("</EndToEndId>");
    if (!uetr.isEmpty()) sb.append("<UETR>").append(uetr.toLowerCase()).append("</UETR>");
    sb.append("</PmtId>");
    
    sb.append("<IntrBkSttlmAmt Ccy=\"").append(currency).append("\">").append(amount).append("</IntrBkSttlmAmt>");
    sb.append("<IntrBkSttlmDt>").append(sttlmDt).append("</IntrBkSttlmDt>");
    sb.append("<ChrgBr>").append(mxChrg).append("</ChrgBr>");

    sb.append("</CdtTrfTxInf></FIToFICstmrCdtTrf></Document>");

    return sb.toString();

} catch (Exception e) {
    java.io.StringWriter sw = new java.io.StringWriter();
    e.printStackTrace(new java.io.PrintWriter(sw));
    return "Error: " + e.toString() + "\n" + sw.toString();
}
