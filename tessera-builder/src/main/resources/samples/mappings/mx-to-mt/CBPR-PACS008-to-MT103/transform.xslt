<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:pacs="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08"
    exclude-result-prefixes="xs pacs">

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates select="//pacs:FIToFICstmrCdtTrf"/>
    </xsl:template>

    <xsl:template match="pacs:FIToFICstmrCdtTrf">
        <xsl:text>{1:F01</xsl:text>
        <xsl:variable name="instgBIC" select="(pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI | pacs:CdtTrfTxInf/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI)[1]"/>
        <xsl:value-of select="substring(concat($instgBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>0000000000}{2:I103</xsl:text>
        <xsl:variable name="instdBIC" select="(pacs:GrpHdr/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI | pacs:CdtTrfTxInf/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI)[1]"/>
        <xsl:value-of select="substring(concat($instdBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>N}{3:{108:</xsl:text>
        <xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="pacs:CdtTrfTxInf/pacs:PmtId/pacs:UETR">
            <xsl:text>{121:</xsl:text><xsl:value-of select="pacs:CdtTrfTxInf/pacs:PmtId/pacs:UETR"/><xsl:text>}</xsl:text>
        </xsl:if>
        <xsl:text>}{4:
:20:</xsl:text>
        <xsl:value-of select="substring((pacs:CdtTrfTxInf/pacs:PmtId/pacs:InstrId | pacs:CdtTrfTxInf/pacs:PmtId/pacs:EndToEndId)[1], 1, 16)"/>
        <xsl:text>
:23B:CRED</xsl:text>
        
        <xsl:if test="pacs:CdtTrfTxInf/pacs:PmtTpInf/pacs:LclInstrm/pacs:Prtry">
            <xsl:text>
:23E:</xsl:text><xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:PmtTpInf/pacs:LclInstrm/pacs:Prtry, 1, 30)"/>
        </xsl:if>

        <xsl:variable name="sttlmDt" select="pacs:CdtTrfTxInf/pacs:IntrBkSttlmDt"/>
        <xsl:text>
:32A:</xsl:text>
        <xsl:value-of select="concat(substring($sttlmDt, 3, 2), substring($sttlmDt, 6, 2), substring($sttlmDt, 9, 2))"/>
        <xsl:value-of select="pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt/@Ccy"/>
        <xsl:value-of select="translate(format-number(pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt, '#.00'), '.', ',')"/>

        <!-- Field 50A or 50K -->
        <xsl:text>
:50A:</xsl:text>
        <xsl:variable name="dbtrAcct" select="(pacs:CdtTrfTxInf/pacs:DbtrAcct/pacs:Id/IBAN | pacs:CdtTrfTxInf/pacs:DbtrAcct/pacs:Id/Othr/Id)[1]"/>
        <xsl:if test="$dbtrAcct"><xsl:text>/</xsl:text><xsl:value-of select="$dbtrAcct"/><xsl:text>
</xsl:text></xsl:if>
        <xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:Dbtr/pacs:Nm, 1, 35)"/>

        <xsl:if test="pacs:CdtTrfTxInf/pacs:DbtrAgt/pacs:FinInstnId/pacs:BICFI">
            <xsl:text>
:52A:</xsl:text><xsl:value-of select="pacs:CdtTrfTxInf/pacs:DbtrAgt/pacs:FinInstnId/pacs:BICFI"/>
        </xsl:if>
        
        <xsl:if test="pacs:CdtTrfTxInf/pacs:IntrmyAgt1/pacs:FinInstnId/pacs:BICFI">
            <xsl:text>
:56A:</xsl:text><xsl:value-of select="pacs:CdtTrfTxInf/pacs:IntrmyAgt1/pacs:FinInstnId/pacs:BICFI"/>
        </xsl:if>

        <xsl:if test="pacs:CdtTrfTxInf/pacs:CdtrAgt/pacs:FinInstnId/pacs:BICFI">
            <xsl:text>
:57A:</xsl:text><xsl:value-of select="pacs:CdtTrfTxInf/pacs:CdtrAgt/pacs:FinInstnId/pacs:BICFI"/>
        </xsl:if>

        <xsl:text>
:59:</xsl:text>
        <xsl:variable name="cdtrAcct" select="(pacs:CdtTrfTxInf/pacs:CdtrAcct/pacs:Id/IBAN | pacs:CdtTrfTxInf/pacs:CdtrAcct/pacs:Id/Othr/Id)[1]"/>
        <xsl:if test="$cdtrAcct"><xsl:text>/</xsl:text><xsl:value-of select="$cdtrAcct"/><xsl:text>
</xsl:text></xsl:if>
        <xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:Cdtr/pacs:Nm, 1, 35)"/>

        <xsl:if test="pacs:CdtTrfTxInf/pacs:RmtInf/pacs:Ustrd">
            <xsl:text>
:70:</xsl:text>
            <xsl:for-each select="pacs:CdtTrfTxInf/pacs:RmtInf/pacs:Ustrd">
                <xsl:value-of select="substring(., 1, 35)"/>
                <xsl:if test="position() &lt; last() and position() &lt; 4">
                    <xsl:text>
</xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
        
        <xsl:text>
:71A:</xsl:text>
        <xsl:choose>
            <xsl:when test="pacs:CdtTrfTxInf/pacs:ChrgBr = 'DEBT'">OUR</xsl:when>
            <xsl:when test="pacs:CdtTrfTxInf/pacs:ChrgBr = 'CRED'">BEN</xsl:when>
            <xsl:otherwise>SHA</xsl:otherwise>
        </xsl:choose>

        <xsl:if test="pacs:CdtTrfTxInf/pacs:RgltryRptg/pacs:Dtls/pacs:Inf">
            <xsl:text>
:77B:</xsl:text><xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:RgltryRptg/pacs:Dtls/pacs:Inf, 1, 35)"/>
        </xsl:if>

        <xsl:text>
-}</xsl:text>
    </xsl:template>
</xsl:stylesheet>
