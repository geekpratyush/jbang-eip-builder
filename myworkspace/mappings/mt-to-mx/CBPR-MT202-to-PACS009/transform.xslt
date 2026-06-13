<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:tessera="http://tessera.com/functions"
    xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.009.001.08"
    exclude-result-prefixes="xs tessera">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:function name="tessera:cleanString" as="xs:string">
        <xsl:param name="input" as="xs:string?"/>
        <xsl:sequence select="string($input)"/> 
    </xsl:function>

    <xsl:function name="tessera:formatDate" as="xs:string">
        <xsl:param name="dateStr" as="xs:string?"/>
        <xsl:choose>
            <xsl:when test="string-length($dateStr) = 6">
                <xsl:sequence select="concat('20', substring($dateStr, 1, 2), '-', substring($dateStr, 3, 2), '-', substring($dateStr, 5, 2))"/>
            </xsl:when>
            <xsl:otherwise><xsl:sequence select="string($dateStr)"/></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="tessera:formatAmount" as="xs:string">
        <xsl:param name="amtStr" as="xs:string?"/>
        <xsl:sequence select="translate(string($amtStr), ',', '.')"/>
    </xsl:function>

    <xsl:template match="/message">
        <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.009.001.08">
            <FICstmrCdtTrf>
                <GrpHdr>
                    <MsgId><xsl:value-of select="tessera:cleanString(block3/tag[name='108']/value)"/></MsgId>
                    <CreDtTm><xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]Z')"/></CreDtTm>
                    <NbOfTxs>1</NbOfTxs>
                    <SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf>
                    <InstgAgt><FinInstnId><BICFI><xsl:value-of select="substring(block1/logicalTerminal, 1, 11)"/></BICFI></FinInstnId></InstgAgt>
                    <InstdAgt><FinInstnId><BICFI><xsl:value-of select="substring(block2/receiverAddress, 1, 11)"/></BICFI></FinInstnId></InstdAgt>
                </GrpHdr>
                <CdtTrfTxInf>
                    <PmtId>
                        <InstrId><xsl:value-of select="tessera:cleanString(block4/field[name='20']/component[@number='1'])"/></InstrId>
                        <EndToEndId><xsl:value-of select="tessera:cleanString(block4/field[name='21']/component[@number='1'])"/></EndToEndId>
                        <UETR><xsl:value-of select="block3/tag[name='121']/value"/></UETR>
                    </PmtId>
                    
                    <xsl:variable name="f32a" select="block4/field[name='32A']"/>
                    <IntrBkSttlmAmt>
                        <xsl:attribute name="Ccy"><xsl:value-of select="$f32a/component[@number='2']"/></xsl:attribute>
                        <xsl:value-of select="tessera:formatAmount($f32a/component[@number='3'])"/>
                    </IntrBkSttlmAmt>
                    <IntrBkSttlmDt><xsl:value-of select="tessera:formatDate($f32a/component[@number='1'])"/></IntrBkSttlmDt>
                    
                    <Dbtr><FinInstnId><BICFI><xsl:value-of select="block4/field[name='52A']/component[@number='3']"/></BICFI></FinInstnId></Dbtr>
                    <DbtrAgt><FinInstnId><BICFI><xsl:value-of select="block4/field[name='53A']/component[@number='3']"/></BICFI></FinInstnId></DbtrAgt>
                    <CdtrAgt><FinInstnId><BICFI><xsl:value-of select="block4/field[name='57A']/component[@number='3']"/></BICFI></FinInstnId></CdtrAgt>
                    <Cdtr><FinInstnId><BICFI><xsl:value-of select="block4/field[name='58A']/component[@number='3']"/></BICFI></FinInstnId></Cdtr>
                </CdtTrfTxInf>
            </FICstmrCdtTrf>
        </Document>
    </xsl:template>
</xsl:stylesheet>