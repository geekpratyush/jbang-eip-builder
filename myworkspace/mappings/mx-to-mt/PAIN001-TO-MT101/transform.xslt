<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:pain="urn:iso:std:iso:20022:tech:xsd:pain.001.001.13"
    exclude-result-prefixes="xs pain">

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates select="//pain:CstmrCdtTrfInitn"/>
    </xsl:template>

    <xsl:template match="pain:CstmrCdtTrfInitn">
        <xsl:text>{1:F01</xsl:text>
        <xsl:variable name="instgBIC" select="pain:PmtInf/pain:DbtrAgt/pain:FinInstnId/pain:BICFI"/>
        <xsl:value-of select="substring(concat($instgBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>0000000000}{2:I101</xsl:text>
        <xsl:variable name="instdBIC" select="pain:PmtInf/pain:CdtTrfTxInf/pain:CdtrAgt/pain:FinInstnId/pain:BICFI"/>
        <xsl:value-of select="substring(concat($instdBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>N}{3:{108:</xsl:text>
        <xsl:value-of select="substring(pain:GrpHdr/pain:MsgId, 1, 16)"/>
        <xsl:text>}</xsl:text>
        <xsl:text>}{4:
:20:</xsl:text>
        <xsl:value-of select="substring(pain:PmtInf/pain:PmtInfId, 1, 16)"/>
        <xsl:text>
:28D:1/1
:50L:</xsl:text>
        <xsl:variable name="dbtrNm" select="pain:PmtInf/pain:Dbtr/pain:Nm"/>
        <xsl:value-of select="substring($dbtrNm, 1, 35)"/>
        <xsl:variable name="execDt" select="pain:PmtInf/pain:ReqdExctnDt/pain:Dt"/>
        <xsl:text>
:30:</xsl:text>
        <xsl:value-of select="concat(substring($execDt, 3, 2), substring($execDt, 6, 2), substring($execDt, 9, 2))"/>
        
        <xsl:for-each select="pain:PmtInf/pain:CdtTrfTxInf">
            <xsl:text>
:21:</xsl:text>
            <xsl:value-of select="substring(pain:PmtId/pain:EndToEndId, 1, 16)"/>
            <xsl:text>
:32B:</xsl:text>
            <xsl:value-of select="pain:Amt/pain:InstdAmt/@Ccy"/>
            <xsl:value-of select="translate(format-number(pain:Amt/pain:InstdAmt, '#.00'), '.', ',')"/>
            <xsl:text>
:59:</xsl:text>
            <xsl:variable name="cdtrAcct" select="pain:CdtrAcct/pain:Id/pain:IBAN"/>
            <xsl:if test="$cdtrAcct"><xsl:text>/</xsl:text><xsl:value-of select="$cdtrAcct"/><xsl:text>
</xsl:text></xsl:if>
            <xsl:value-of select="substring(pain:Cdtr/pain:Nm, 1, 35)"/>
        </xsl:for-each>
        <xsl:text>
-}</xsl:text>
    </xsl:template>
</xsl:stylesheet>
