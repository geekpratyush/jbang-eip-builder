<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:pacs="urn:iso:std:iso:20022:tech:xsd:pacs.004.001.09"
    exclude-result-prefixes="xs pacs">

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates select="//pacs:PmtRtr"/>
    </xsl:template>

    <xsl:template match="pacs:PmtRtr">
        <xsl:text>{1:F01</xsl:text>
        <xsl:variable name="instgBIC" select="pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI"/>
        <xsl:value-of select="substring(concat($instgBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>0000000000}{2:I202</xsl:text>
        <xsl:variable name="instdBIC" select="pacs:GrpHdr/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI"/>
        <xsl:value-of select="substring(concat($instdBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>N}{3:{108:</xsl:text>
        <xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="pacs:TxInf/pacs:OrgnlUETR">
            <xsl:text>{121:</xsl:text><xsl:value-of select="pacs:TxInf/pacs:OrgnlUETR"/><xsl:text>}</xsl:text>
        </xsl:if>
        <xsl:text>}{4:
:20:</xsl:text>
        <xsl:value-of select="substring(pacs:TxInf/pacs:RtrId, 1, 16)"/>
        <xsl:text>
:21:</xsl:text>
        <xsl:value-of select="substring(pacs:TxInf/pacs:OrgnlTxId, 1, 16)"/>
        <xsl:variable name="sttlmDt" select="pacs:TxInf/pacs:IntrBkSttlmDt"/>
        <xsl:text>
:32A:</xsl:text>
        <xsl:value-of select="concat(substring($sttlmDt, 3, 2), substring($sttlmDt, 6, 2), substring($sttlmDt, 9, 2))"/>
        <xsl:value-of select="pacs:TxInf/pacs:RtrdIntrBkSttlmAmt/@Ccy"/>
        <xsl:value-of select="translate(format-number(pacs:TxInf/pacs:RtrdIntrBkSttlmAmt, '#.00'), '.', ',')"/>

        <xsl:text>
:72:/RETN/</xsl:text>
        <xsl:value-of select="pacs:TxInf/pacs:RtrRsnInf/pacs:Rsn/pacs:Cd"/>
        <xsl:if test="pacs:TxInf/pacs:RtrRsnInf/pacs:AddtlInf">
            <xsl:text>/</xsl:text><xsl:value-of select="substring(pacs:TxInf/pacs:RtrRsnInf/pacs:AddtlInf, 1, 30)"/>
        </xsl:if>
        <xsl:text>
-}</xsl:text>
    </xsl:template>
</xsl:stylesheet>
