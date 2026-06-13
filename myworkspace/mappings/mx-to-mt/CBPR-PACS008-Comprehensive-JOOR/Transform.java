try {
    String xml = (String) body;
    org.json.JSONObject json = org.json.XML.toJSONObject(xml);
    
    org.json.JSONObject doc = json.optJSONObject("Document");
    org.json.JSONObject root = (doc != null) ? doc.optJSONObject("FIToFICstmrCdtTrf") : json.optJSONObject("FIToFICstmrCdtTrf");
    org.json.JSONObject hdr = root.optJSONObject("GrpHdr");
    
    Object txRaw = root.get("CdtTrfTxInf");
    org.json.JSONObject tx = (txRaw instanceof org.json.JSONArray) ? ((org.json.JSONArray)txRaw).getJSONObject(0) : (org.json.JSONObject)txRaw;
    
    // Helpers
    java.util.function.BiFunction<String, Integer, String> slice = (s, l) -> (s == null) ? "" : (s.length() > l ? s.substring(0, l) : s);
    java.util.function.Function<String, String> padBIC = s -> {
        String b = (s == null) ? "" : s;
        while(b.length() < 12) b += "X";
        return b.substring(0, 12);
    };

    // Extraction
    String instgBIC = padBIC.apply(hdr.optJSONObject("InstgAgt") != null ? hdr.optJSONObject("InstgAgt").optJSONObject("FinInstnId").optString("BICFI") : tx.optJSONObject("InstgAgt").optJSONObject("FinInstnId").optString("BICFI"));
    String instdBIC = padBIC.apply(hdr.optJSONObject("InstdAgt") != null ? hdr.optJSONObject("InstdAgt").optJSONObject("FinInstnId").optString("BICFI") : tx.optJSONObject("InstdAgt").optJSONObject("FinInstnId").optString("BICFI"));
    String msgId = slice.apply(hdr.optString("MsgId"), 16);
    String uetr = tx.optJSONObject("PmtId").optString("UETR");
    
    String instrId = slice.apply(tx.optJSONObject("PmtId").optString("InstrId", tx.optJSONObject("PmtId").optString("EndToEndId")), 16);
    String sttlmDt = tx.optString("IntrBkSttlmDt").replace("-", "").substring(2);
    
    org.json.JSONObject amtObj = tx.optJSONObject("IntrBkSttlmAmt");
    String amtVal = String.valueOf(amtObj.get("content")).replace(".", ",");
    String amtCcy = amtObj.optString("Ccy");
    
    org.json.JSONObject dbtrAcctObj = tx.optJSONObject("DbtrAcct");
    String dbtrAcct = "";
    if (dbtrAcctObj != null) {
        org.json.JSONObject idObj = dbtrAcctObj.optJSONObject("Id");
        if (idObj != null) {
            dbtrAcct = idObj.optString("IBAN", idObj.optJSONObject("Othr") != null ? idObj.optJSONObject("Othr").optString("Id") : "");
        }
    }
    String dbtrNm = slice.apply(tx.optJSONObject("Dbtr").optString("Nm"), 35);
    
    String intrmyBIC = tx.optJSONObject("IntrmyAgt1") != null ? tx.optJSONObject("IntrmyAgt1").optJSONObject("FinInstnId").optString("BICFI") : "";
    String cdtrAgtBIC = tx.optJSONObject("CdtrAgt") != null ? tx.optJSONObject("CdtrAgt").optJSONObject("FinInstnId").optString("BICFI") : "";
    
    org.json.JSONObject cdtrAcctObj = tx.optJSONObject("CdtrAcct");
    String cdtrAcct = "";
    if (cdtrAcctObj != null) {
        org.json.JSONObject idObj = cdtrAcctObj.optJSONObject("Id");
        if (idObj != null) {
            cdtrAcct = idObj.optString("IBAN", idObj.optJSONObject("Othr") != null ? idObj.optJSONObject("Othr").optString("Id") : "");
        }
    }
    String cdtrNm = slice.apply(tx.optJSONObject("Cdtr").optString("Nm"), 35);
    
    org.json.JSONObject rmtObj = tx.optJSONObject("RmtInf");
    String rmtInf = "";
    if (rmtObj != null && rmtObj.has("Ustrd")) {
        Object ustrd = rmtObj.get("Ustrd");
        if (ustrd instanceof org.json.JSONArray) {
            org.json.JSONArray arr = (org.json.JSONArray) ustrd;
            java.util.List<String> lines = new java.util.ArrayList<>();
            for(int i=0; i<arr.length(); i++) lines.add(slice.apply(arr.getString(i), 35));
            rmtInf = String.join("\n", lines);
        } else {
            rmtInf = slice.apply(ustrd.toString(), 35);
        }
    }
    
    String chrgBr = tx.optString("ChrgBr");
    String mtChrg = "DEBT".equals(chrgBr) ? "OUR" : "CRED".equals(chrgBr) ? "BEN" : "SHA";
    
    StringBuilder mt = new StringBuilder();
    mt.append("{1:F01").append(instgBIC).append("0000000000}");
    mt.append("{2:I103").append(instdBIC).append("N}");
    mt.append("{3:{108:").append(msgId).append("}");
    if (uetr != null && !uetr.isEmpty()) mt.append("{121:").append(uetr).append("}");
    mt.append("}{4:\n");
    mt.append(":20:").append(instrId).append("\n");
    mt.append(":23B:CRED\n");
    mt.append(":32A:").append(sttlmDt).append(amtCcy).append(amtVal).append("\n");
    mt.append(":50A:").append(!dbtrAcct.isEmpty() ? "/" + dbtrAcct + "\n" : "").append(dbtrNm).append("\n");
    if (!intrmyBIC.isEmpty()) mt.append(":56A:").append(intrmyBIC).append("\n");
    if (!cdtrAgtBIC.isEmpty()) mt.append(":57A:").append(cdtrAgtBIC).append("\n");
    mt.append(":59:").append(!cdtrAcct.isEmpty() ? "/" + cdtrAcct + "\n" : "").append(cdtrNm).append("\n");
    if (!rmtInf.isEmpty()) mt.append(":70:").append(rmtInf).append("\n");
    mt.append(":71A:").append(mtChrg).append("\n");
    mt.append("-}");
    
    return mt.toString();
} catch (Exception e) {
    return "Error: " + e.toString();
}
