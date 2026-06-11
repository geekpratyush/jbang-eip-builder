<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:pacs="urn:iso:std:iso:20022:tech:xsd:pacs.009.001.08"
    exclude-result-prefixes="xs pacs">

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates select="//pacs:FICdtTrf"/>
    </xsl:template>

    <xsl:template match="pacs:FICdtTrf">
        <xsl:text>{1:F01</xsl:text>
        <xsl:variable name="instgBIC" select="pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI"/>
        <xsl:value-of select="substring(concat($instgBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>0000000000}{2:I202</xsl:text>
        <xsl:variable name="instdBIC" select="pacs:GrpHdr/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI"/>
        <xsl:value-of select="substring(concat($instdBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>N}{3:{108:</xsl:text>
        <xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="pacs:CdtTrfTxInf/pacs:PmtId/pacs:UETR">
            <xsl:text>{121:</xsl:text><xsl:value-of select="pacs:CdtTrfTxInf/pacs:PmtId/pacs:UETR"/><xsl:text>}</xsl:text>
        </xsl:if>
        <xsl:text>}{4:
:20:</xsl:text>
        <xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:PmtId/pacs:InstrId, 1, 16)"/>
        <xsl:text>
:21:</xsl:text>
        <xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:PmtId/pacs:EndToEndId, 1, 16)"/>
        <xsl:variable name="sttlmDt" select="pacs:CdtTrfTxInf/pacs:IntrBkSttlmDt"/>
        <xsl:text>
:32A:</xsl:text>
        <xsl:value-of select="concat(substring($sttlmDt, 3, 2), substring($sttlmDt, 6, 2), substring($sttlmDt, 9, 2))"/>
        <xsl:value-of select="pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt/@Ccy"/>
        <xsl:value-of select="translate(format-number(pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt, '#.00'), '.', ',')"/>

        <xsl:if test="pacs:CdtTrfTxInf/pacs:Dbtr/pacs:FinInstnId/pacs:BICFI">
            <xsl:text>
:52A:</xsl:text><xsl:value-of select="pacs:CdtTrfTxInf/pacs:Dbtr/pacs:FinInstnId/pacs:BICFI"/>
        </xsl:if>
        <xsl:if test="pacs:CdtTrfTxInf/pacs:Cdtr/pacs:FinInstnId/pacs:BICFI">
            <xsl:text>
:58A:</xsl:text><xsl:value-of select="pacs:CdtTrfTxInf/pacs:Cdtr/pacs:FinInstnId/pacs:BICFI"/>
        </xsl:if>
        <xsl:text>
-}</xsl:text>
    </xsl:template>
</xsl:stylesheet>
