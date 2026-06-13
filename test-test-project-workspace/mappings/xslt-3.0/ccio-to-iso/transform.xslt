<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns="http://www.citi.com/ISO/ICG/CS/1.0"
    exclude-result-prefixes="xs">

  <xsl:output method="text" encoding="UTF-8"/>

  <xsl:param name="payload" as="xs:string" required="yes"/>

  <xsl:template match="/">
    
    <!-- Strip BOM -->
    <xsl:variable name="clean" 
                  select="if (starts-with($payload, '&#xFEFF;')) 
                          then substring($payload, 2) 
                          else $payload"/>
    
    <!-- Extract header content -->
    <xsl:variable name="header-content" 
                  select="replace($clean, '.*?\[HEADER_START\]\s*(.*?)\s*\[HEADER_END\].*', '$1', 's')"/>
    
    <!-- Extract XML payload -->
    <xsl:variable name="xml-string" 
                  select="replace($clean, '^.*?\[HEADER_END\]\s*', '', 's')"/>
    
    <!-- Parse CCIO XML -->
    <xsl:variable name="payload-doc" select="parse-xml($xml-string)" as="document-node()"/>
    
    <!-- OUTPUT: Header with proper newlines -->
    <xsl:text>[HEADER_START]&#xA;</xsl:text>
    <xsl:value-of select="$header-content"/>
    <xsl:text>[HEADER_END]&#xA;</xsl:text>
    
    <!-- OUTPUT: XML Declaration -->
    <xsl:text>&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;</xsl:text>
    
    <!-- Build ISO XML in default namespace (no prefix) -->
    <xsl:variable name="iso-xml" as="element()">
      <TransactionMessage>
        <xsl:apply-templates select="$payload-doc/CCIOMessage"/>
      </TransactionMessage>
    </xsl:variable>
    
    <!-- Serialize to string -->
    <xsl:variable name="iso-string" as="xs:string">
      <xsl:sequence select="serialize($iso-xml, 
        map{
          'method': 'xml',
          'omit-xml-declaration': true(),
          'indent': false()
        })"/>
    </xsl:variable>
    
    <!-- FIX: Remove whitespace BETWEEN tags only using xsl:analyze-string -->
    <xsl:variable name="iso-single-line" as="xs:string">
      <xsl:value-of>
        <xsl:analyze-string select="$iso-string" regex="&gt;\s+&lt;">
          <xsl:matching-substring>
            <xsl:text>&gt;&lt;</xsl:text>
          </xsl:matching-substring>
          <xsl:non-matching-substring>
            <xsl:value-of select="."/>
          </xsl:non-matching-substring>
        </xsl:analyze-string>
      </xsl:value-of>
    </xsl:variable>
    
    <xsl:value-of select="$iso-single-line"/>
    
  </xsl:template>

  <!-- Child elements in default namespace (no prefix) -->
  <xsl:template match="CCIOMessage">
    <OrigCdtrNm><xsl:value-of select="N1_PE/N102"/></OrigCdtrNm>
    <OrigCdtrPrvAcctID><xsl:value-of select="N1_PE/N104"/></OrigCdtrPrvAcctID>
    <OrigCdtractSchmeNm><xsl:value-of select="N1_PE/N103"/></OrigCdtractSchmeNm>
    <OrigCdtractSchmeNm><xsl:value-of select="N1_PE/N101"/></OrigCdtractSchmeNm>
    <OrigRgltryRptgDtIsTp><xsl:value-of select="DTM/DTM01"/></OrigRgltryRptgDtIsTp>
    <OrigStrdRefDILineDtlsIdRltdDt>
      <xsl:variable name="d" select="DTM/DTM02"/>
      <xsl:value-of select="concat(substring($d,1,4),'-',substring($d,5,2),'-',substring($d,7,2))"/>
    </OrigStrdRefDILineDtlsIdRltdDt>
    <OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry><xsl:value-of select="RMR/RMR01"/></OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry>
    <OrigStrdRefDILineDtlsIdNb><xsl:value-of select="RMR/RMR02"/></OrigStrdRefDILineDtlsIdNb>
    <OrigStrdRefDILineDtlsAmtDuePyblAmt><xsl:value-of select="RMR/RMR05"/></OrigStrdRefDILineDtlsAmtDuePyblAmt>
    <OrigStrdRefDILineDtlsAmtDsctApldAmtAmt><xsl:value-of select="RMR/RMR06"/></OrigStrdRefDILineDtlsAmtDsctApldAmtAmt>
    <OrigStrdRefDILineDtlsAmtRmtdAmt><xsl:value-of select="RMR/RMR04"/></OrigStrdRefDILineDtlsAmtRmtdAmt>
    <OrigStrdCdtrRefInfRef><xsl:value-of select="REF/REF02"/></OrigStrdCdtrRefInfRef>
  </xsl:template>

</xsl:stylesheet>