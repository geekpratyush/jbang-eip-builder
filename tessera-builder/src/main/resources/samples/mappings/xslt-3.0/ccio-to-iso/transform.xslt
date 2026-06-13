<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns="http://www.citi.com/ISO/ICG/CS/1.0"
    exclude-result-prefixes="xs">

  <xsl:output method="text" encoding="UTF-8"/>

  <xsl:param name="payload" select="''"/>

  <xsl:template match="/">
    <!-- Extract header section using regex -->
    <xsl:variable name="header-regex" select="'\[HEADER_START\](.*?)\[HEADER_END\]'" as="xs:string"/>
    <xsl:variable name="header-content" select="replace($payload, $header-regex, '$1', 's')" as="xs:string"/>

    <!-- Extract XML payload using regex -->
    <xsl:variable name="xml-regex" select="'\[HEADER_END\]\s*(.*)'" as="xs:string"/>
    <xsl:variable name="xml-string" select="replace($payload, $xml-regex, '$1', 's')" as="xs:string"/>

    <!-- Parse XML payload -->
    <xsl:variable name="payload-doc" select="parse-xml($xml-string)" as="document-node()"/>

    <!-- Output header exactly as-is -->
    <xsl:text>[HEADER_START]</xsl:text>
    <xsl:value-of select="$header-content"/>
    <xsl:text>[HEADER_END]&#xA;</xsl:text>

    <!-- Transform and output XML as single line -->
    <xsl:variable name="iso-xml">
      <TransactionMessage xmlns="http://www.citi.com/ISO/ICG/CS/1.0">
        <xsl:apply-templates select="$payload-doc/CCIOMessage"/>
      </TransactionMessage>
    </xsl:variable>

    <!-- Serialize to single line -->
    <xsl:value-of select="serialize($iso-xml, 
      map{
        'method': 'xml',
        'omit-xml-declaration': false(),
        'indent': false(),
        'encoding': 'UTF-8'
      })"/>
  </xsl:template>

  <xsl:template match="CCIOMessage">
    <OrigCdtrNm>
      <xsl:value-of select="N1_PE/N102"/>
    </OrigCdtrNm>
    <OrigCdtrPrvAcctID>
      <xsl:value-of select="N1_PE/N104"/>
    </OrigCdtrPrvAcctID>
    <OrigCdtractSchmeNm>
      <xsl:value-of select="N1_PE/N103"/>
    </OrigCdtractSchmeNm>
    <OrigCdtractSchmeNm>
      <xsl:value-of select="N1_PE/N101"/>
    </OrigCdtractSchmeNm>
    <OrigRgltryRptgDtIsTp>
      <xsl:value-of select="DTM/DTM01"/>
    </OrigRgltryRptgDtIsTp>
    <OrigStrdRefDILineDtlsIdRltdDt>
      <xsl:variable name="d" select="DTM/DTM02"/>
      <xsl:value-of select="concat(substring($d,1,4),'-',substring($d,5,2),'-',substring($d,7,2))"/>
    </OrigStrdRefDILineDtlsIdRltdDt>
    <OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry>
      <xsl:value-of select="RMR/RMR01"/>
    </OrigStrdRefDILineDtlsIdTpCdOrPrtryPrtry>
    <OrigStrdRefDILineDtlsIdNb>
      <xsl:value-of select="RMR/RMR02"/>
    </OrigStrdRefDILineDtlsIdNb>
    <OrigStrdRefDILineDtlsAmtDuePyblAmt>
      <xsl:value-of select="RMR/RMR05"/>
    </OrigStrdRefDILineDtlsAmtDuePyblAmt>
    <OrigStrdRefDILineDtlsAmtDsctApldAmtAmt>
      <xsl:value-of select="RMR/RMR06"/>
    </OrigStrdRefDILineDtlsAmtDsctApldAmtAmt>
    <OrigStrdRefDILineDtlsAmtRmtdAmt>
      <xsl:value-of select="RMR/RMR04"/>
    </OrigStrdRefDILineDtlsAmtRmtdAmt>
    <OrigStrdCdtrRefInfRef>
      <xsl:value-of select="REF/REF02"/>
    </OrigStrdCdtrRefInfRef>
  </xsl:template>

</xsl:stylesheet>
