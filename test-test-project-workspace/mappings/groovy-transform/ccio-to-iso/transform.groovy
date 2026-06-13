import java.util.regex.Pattern
import java.time.LocalDate
import java.time.format.DateTimeFormatter

String input = body

Pattern HEADER_PATTERN = Pattern.compile(/\[HEADER_START\](.*?)\[HEADER_END\]/, Pattern.DOTALL)
Pattern N1_PE_PATTERN = Pattern.compile(/<N1_PE>(.*?)<\/N1_PE>/, Pattern.DOTALL)
Pattern DTM_PATTERN = Pattern.compile(/<DTM>(.*?)<\/DTM>/, Pattern.DOTALL)
Pattern RMR_PATTERN = Pattern.compile(/<RMR>(.*?)<\/RMR>/, Pattern.DOTALL)
Pattern REF_PATTERN = Pattern.compile(/<REF>(.*?)<\/REF>/, Pattern.DOTALL)

Pattern N101_PATTERN = Pattern.compile(/<N101>(.*?)<\/N101>/)
Pattern N102_PATTERN = Pattern.compile(/<N102>(.*?)<\/N102>/)
Pattern N103_PATTERN = Pattern.compile(/<N103>(.*?)<\/N103>/)
Pattern N104_PATTERN = Pattern.compile(/<N104>(.*?)<\/N104>/)
Pattern DTM01_PATTERN = Pattern.compile(/<DTM01>(.*?)<\/DTM01>/)
Pattern DTM02_PATTERN = Pattern.compile(/<DTM02>(.*?)<\/DTM02>/)
Pattern RMR01_PATTERN = Pattern.compile(/<RMR01>(.*?)<\/RMR01>/)
Pattern RMR02_PATTERN = Pattern.compile(/<RMR02>(.*?)<\/RMR02>/)
Pattern RMR04_PATTERN = Pattern.compile(/<RMR04>(.*?)<\/RMR04>/)
Pattern RMR05_PATTERN = Pattern.compile(/<RMR05>(.*?)<\/RMR05>/)
Pattern RMR06_PATTERN = Pattern.compile(/<RMR06>(.*?)<\/RMR06>/)
Pattern REF02_PATTERN = Pattern.compile(/<REF02>(.*?)<\/REF02>/)

def headerMatcher = HEADER_PATTERN.matcher(input)
if (!headerMatcher.find()) {
    throw new IllegalArgumentException("Header not found")
}

String header = headerMatcher.group(0)
String xml = input.substring(headerMatcher.end()).trim()

def extract = { String block, Pattern pattern ->
    if (!block) return ""
    def matcher = pattern.matcher(block)
    return matcher.find() ? matcher.group(1) : ""
}

def extractBlock = { Pattern pattern ->
    def matcher = pattern.matcher(xml)
    return matcher.find() ? matcher.group(1) : null
}

def n1peBlock = extractBlock(N1_PE_PATTERN)
def dtmBlock = extractBlock(DTM_PATTERN)
def rmrBlock = extractBlock(RMR_PATTERN)
def refBlock = extractBlock(REF_PATTERN)

def cdtrNm = extract(n1peBlock, N102_PATTERN)
def cdtrAcct = extract(n1peBlock, N104_PATTERN)
def cdtrSchme1 = extract(n1peBlock, N103_PATTERN)
def cdtrSchme2 = extract(n1peBlock, N101_PATTERN)
def rgltryTp = extract(dtmBlock, DTM01_PATTERN)
def rgltryDt = extract(dtmBlock, DTM02_PATTERN)
def refPrtry = extract(rmrBlock, RMR01_PATTERN)
def refNb = extract(rmrBlock, RMR02_PATTERN)
def amtRmtd = extract(rmrBlock, RMR04_PATTERN)
def amtDue = extract(rmrBlock, RMR05_PATTERN)
def amtDsct = extract(rmrBlock, RMR06_PATTERN)
def cdtrRef = extract(refBlock, REF02_PATTERN)

def IN_FMT = DateTimeFormatter.ofPattern("yyyyMMdd")
def OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
def isoDate = ""
if (rgltryDt && rgltryDt.length() == 8) {
    isoDate = LocalDate.parse(rgltryDt, IN_FMT).format(OUT_FMT)
}

def escapeXml = { text ->
    if (!text) return ""
    return text.replace('&', '&amp;')
               .replace('<', '&lt;')
               .replace('>', '&gt;')
               .replace('"', '&quot;')
               .replace("'", '&apos;')
}

def isoXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><TransactionMessage xmlns=\"http://www.citi.com/ISO/ICG/CS/1.0\"><OrigCdtrNm>" + escapeXml(cdtrNm) + "</OrigCdtrNm><OrigCdtrPrvAcctID>" + cdtrAcct + "</OrigCdtrPrvAcctID><OrigCdtractSchmeNm>" + cdtrSchme1 + "</OrigCdtractSchmeNm><OrigCdtractSchmeNm>" + cdtrSchme2 + "</OrigCdtractSchmeNm><OrigRgltryRptgDtIsTp>" + rgltryTp + "</OrigRgltryRptgDtIsTp><OrigStrdRefDILineDtlsIdRltdDt>" + isoDate + "</OrigStrdRefDILineDtlsIdRltdDt><OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry>" + refPrtry + "</OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry><OrigStrdRefDILineDtlsIdNb>" + refNb + "</OrigStrdRefDILineDtlsIdNb><OrigStrdRefDILineDtlsAmtDuePyblAmt>" + amtDue + "</OrigStrdRefDILineDtlsAmtDuePyblAmt><OrigStrdRefDILineDtlsAmtDsctApldAmtAmt>" + amtDsct + "</OrigStrdRefDILineDtlsAmtDsctApldAmtAmt><OrigStrdRefDILineDtlsAmtRmtdAmt>" + amtRmtd + "</OrigStrdRefDILineDtlsAmtRmtdAmt><OrigStrdCdtrRefInfRef>" + cdtrRef + "</OrigStrdCdtrRefInfRef></TransactionMessage>"

return header + "\n" + isoXml
