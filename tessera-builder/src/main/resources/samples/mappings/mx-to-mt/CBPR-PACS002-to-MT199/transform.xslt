<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:pacs="urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10"
    exclude-result-prefixes="xs pacs">

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates select="//pacs:FIToFIPmtStsRpt"/>
    </xsl:template>

    <xsl:template match="pacs:FIToFIPmtStsRpt">
        <xsl:text>{1:F01</xsl:text>
        <xsl:variable name="instgBIC" select="pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI"/>
        <xsl:value-of select="substring(concat($instgBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>0000000000}{2:I199</xsl:text>
        <xsl:variable name="instdBIC" select="pacs:GrpHdr/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI"/>
        <xsl:value-of select="substring(concat($instdBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>N}{3:{108:</xsl:text>
        <xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="pacs:TxInfAndSts/pacs:OrgnlUETR">
            <xsl:text>{121:</xsl:text><xsl:value-of select="pacs:TxInfAndSts/pacs:OrgnlUETR"/><xsl:text>}</xsl:text>
        </xsl:if>
        <xsl:text>}{4:
:20:</xsl:text>
        <xsl:value-of select="substring(pacs:TxInfAndSts/pacs:StsId, 1, 16)"/>
        <xsl:text>
:21:</xsl:text>
        <xsl:value-of select="substring(pacs:TxInfAndSts/pacs:OrgnlTxId, 1, 16)"/>
        <xsl:text>
:79:STATUS REPORT
ORIGINAL MSG ID: </xsl:text><xsl:value-of select="pacs:TxInfAndSts/pacs:OrgnlGrpInf/pacs:OrgnlMsgId"/><xsl:text>
STATUS: </xsl:text><xsl:value-of select="pacs:TxInfAndSts/pacs:TxSts"/><xsl:text>
REASON: </xsl:text><xsl:value-of select="pacs:TxInfAndSts/pacs:StsRsnInf/pacs:Rsn/pacs:Cd"/><xsl:text>
INFO: </xsl:text><xsl:value-of select="substring(pacs:TxInfAndSts/pacs:StsRsnInf/pacs:AddtlInf, 1, 35)"/>
        <xsl:text>
-}</xsl:text>
    </xsl:template>
</xsl:stylesheet>
