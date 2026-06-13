// Java Snippet (jOOR): 'body' is the input string
String input = (String) body;

java.util.regex.Pattern HEADER_PATTERN = java.util.regex.Pattern.compile("\\[HEADER_START\\](.*?)\\[HEADER_END\\]", java.util.regex.Pattern.DOTALL);
java.util.regex.Pattern N1_PE_PATTERN = java.util.regex.Pattern.compile("<N1_PE>(.*?)</N1_PE>", java.util.regex.Pattern.DOTALL);
java.util.regex.Pattern DTM_PATTERN = java.util.regex.Pattern.compile("<DTM>(.*?)</DTM>", java.util.regex.Pattern.DOTALL);
java.util.regex.Pattern RMR_PATTERN = java.util.regex.Pattern.compile("<RMR>(.*?)</RMR>", java.util.regex.Pattern.DOTALL);
java.util.regex.Pattern REF_PATTERN = java.util.regex.Pattern.compile("<REF>(.*?)</REF>", java.util.regex.Pattern.DOTALL);

java.util.regex.Pattern N101_PATTERN = java.util.regex.Pattern.compile("<N101>(.*?)</N101>");
java.util.regex.Pattern N102_PATTERN = java.util.regex.Pattern.compile("<N102>(.*?)</N102>");
java.util.regex.Pattern N103_PATTERN = java.util.regex.Pattern.compile("<N103>(.*?)</N103>");
java.util.regex.Pattern N104_PATTERN = java.util.regex.Pattern.compile("<N104>(.*?)</N104>");
java.util.regex.Pattern DTM01_PATTERN = java.util.regex.Pattern.compile("<DTM01>(.*?)</DTM01>");
java.util.regex.Pattern DTM02_PATTERN = java.util.regex.Pattern.compile("<DTM02>(.*?)</DTM02>");
java.util.regex.Pattern RMR01_PATTERN = java.util.regex.Pattern.compile("<RMR01>(.*?)</RMR01>");
java.util.regex.Pattern RMR02_PATTERN = java.util.regex.Pattern.compile("<RMR02>(.*?)</RMR02>");
java.util.regex.Pattern RMR04_PATTERN = java.util.regex.Pattern.compile("<RMR04>(.*?)</RMR04>");
java.util.regex.Pattern RMR05_PATTERN = java.util.regex.Pattern.compile("<RMR05>(.*?)</RMR05>");
java.util.regex.Pattern RMR06_PATTERN = java.util.regex.Pattern.compile("<RMR06>(.*?)</RMR06>");
java.util.regex.Pattern REF02_PATTERN = java.util.regex.Pattern.compile("<REF02>(.*?)</REF02>");

java.time.format.DateTimeFormatter IN_FMT = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
java.time.format.DateTimeFormatter OUT_FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

java.util.regex.Matcher headerMatcher = HEADER_PATTERN.matcher(input);
if (!headerMatcher.find()) {
    throw new IllegalArgumentException("Header not found");
}

String header = headerMatcher.group(0);
String xml = input.substring(headerMatcher.end()).trim();

java.util.function.BiFunction<String, java.util.regex.Pattern, String> extract = (block, pattern) -> {
    if (block == null) return "";
    java.util.regex.Matcher m = pattern.matcher(block);
    return m.find() ? m.group(1) : "";
};

String n1peBlock = null;
java.util.regex.Matcher mPe = N1_PE_PATTERN.matcher(xml);
if (mPe.find()) n1peBlock = mPe.group(1);

String dtmBlock = null;
java.util.regex.Matcher mDtm = DTM_PATTERN.matcher(xml);
if (mDtm.find()) dtmBlock = mDtm.group(1);

String rmrBlock = null;
java.util.regex.Matcher mRmr = RMR_PATTERN.matcher(xml);
if (mRmr.find()) rmrBlock = mRmr.group(1);

String refBlock = null;
java.util.regex.Matcher mRef = REF_PATTERN.matcher(xml);
if (mRef.find()) refBlock = mRef.group(1);

String cdtrNm = extract.apply(n1peBlock, N102_PATTERN);
String cdtrAcct = extract.apply(n1peBlock, N104_PATTERN);
String cdtrSchme1 = extract.apply(n1peBlock, N103_PATTERN);
String cdtrSchme2 = extract.apply(n1peBlock, N101_PATTERN);
String rgltryTp = extract.apply(dtmBlock, DTM01_PATTERN);
String rgltryDt = extract.apply(dtmBlock, DTM02_PATTERN);
String refPrtry = extract.apply(rmrBlock, RMR01_PATTERN);
String refNb = extract.apply(rmrBlock, RMR02_PATTERN);
String amtRmtd = extract.apply(rmrBlock, RMR04_PATTERN);
String amtDue = extract.apply(rmrBlock, RMR05_PATTERN);
String amtDsct = extract.apply(rmrBlock, RMR06_PATTERN);
String cdtrRef = extract.apply(refBlock, REF02_PATTERN);

String isoDate = "";
if (rgltryDt != null && rgltryDt.length() == 8) {
    isoDate = java.time.LocalDate.parse(rgltryDt, IN_FMT).format(OUT_FMT);
}

java.util.function.Function<String, String> escapeXml = (text) -> {
    if (text == null || text.isEmpty()) return "";
    return text.replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace("\"", "&quot;")
               .replace(String.valueOf((char)39), "&apos;");
};

StringBuilder iso = new StringBuilder(2048);
iso.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
   .append("<TransactionMessage xmlns=\"http://www.citi.com/ISO/ICG/CS/1.0\">")
   .append("<OrigCdtrNm>").append(escapeXml.apply(cdtrNm)).append("</OrigCdtrNm>")
   .append("<OrigCdtrPrvAcctID>").append(cdtrAcct).append("</OrigCdtrPrvAcctID>")
   .append("<OrigCdtractSchmeNm>").append(cdtrSchme1).append("</OrigCdtractSchmeNm>")
   .append("<OrigCdtractSchmeNm>").append(cdtrSchme2).append("</OrigCdtractSchmeNm>")
   .append("<OrigRgltryRptgDtIsTp>").append(rgltryTp).append("</OrigRgltryRptgDtIsTp>")
   .append("<OrigStrdRefDILineDtlsIdRltdDt>").append(isoDate).append("</OrigStrdRefDILineDtlsIdRltdDt>")
   .append("<OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry>").append(refPrtry).append("</OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry>")
   .append("<OrigStrdRefDILineDtlsIdNb>").append(refNb).append("</OrigStrdRefDILineDtlsIdNb>")
   .append("<OrigStrdRefDILineDtlsAmtDuePyblAmt>").append(amtDue).append("</OrigStrdRefDILineDtlsAmtDuePyblAmt>")
   .append("<OrigStrdRefDILineDtlsAmtDsctApldAmtAmt>").append(amtDsct).append("</OrigStrdRefDILineDtlsAmtDsctApldAmtAmt>")
   .append("<OrigStrdRefDILineDtlsAmtRmtdAmt>").append(amtRmtd).append("</OrigStrdRefDILineDtlsAmtRmtdAmt>")
   .append("<OrigStrdCdtrRefInfRef>").append(cdtrRef).append("</OrigStrdCdtrRefInfRef>")
   .append("</TransactionMessage>");

return header + "\n" + iso.toString();
