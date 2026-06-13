<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs">

    <xsl:output method="text" encoding="UTF-8"/>

    <!-- Match the root regardless of the pacs.008 version number (.08, .14, etc.) -->
    <xsl:template match="*[local-name()='Document']">
        <xsl:apply-templates select=".//*[local-name()='FIToFICstmrCdtTrf']"/>
    </xsl:template>

    <xsl:template match="*[local-name()='FIToFICstmrCdtTrf']">
        <xsl:variable name="hdr" select="*[local-name()='GrpHdr']"/>
        <xsl:variable name="tx" select="*[local-name()='CdtTrfTxInf'][1]"/>
        
        <xsl:text>{1:F01</xsl:text>
        <!-- GrpHdr/InstgAgt/FinInstnId/BICFI -->
        <xsl:variable name="instgBIC" select="($hdr/*:InstgAgt/*:FinInstnId/*:BICFI | $tx/*:InstgAgt/*:FinInstnId/*:BICFI)[1]"/>
        <xsl:value-of select="substring(concat($instgBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>0000000000}{2:I103</xsl:text>
        
        <!-- GrpHdr/InstdAgt/FinInstnId/BICFI -->
        <xsl:variable name="instdBIC" select="($hdr/*:InstdAgt/*:FinInstnId/*:BICFI | $tx/*:InstdAgt/*:FinInstnId/*:BICFI)[1]"/>
        <xsl:value-of select="substring(concat($instdBIC, 'XXXXXXXXXXXX'), 1, 12)"/>
        <xsl:text>N}{3:{108:</xsl:text>
        <xsl:value-of select="substring($hdr/*:MsgId, 1, 16)"/>
        <xsl:text>}</xsl:text>
        
        <!-- Field 121: UETR -->
        <xsl:if test="$tx/*:PmtId/*:UETR">
            <xsl:text>{121:</xsl:text><xsl:value-of select="$tx/*:PmtId/*:UETR"/><xsl:text>}</xsl:text>
        </xsl:if>
        
        <xsl:text>}{4:
:20:</xsl:text>
        <xsl:value-of select="substring(($tx/*:PmtId/*:InstrId | $tx/*:PmtId/*:EndToEndId)[1], 1, 16)"/>
        <xsl:text>
:23B:CRED</xsl:text>

        <!-- Amount and Date -->
        <xsl:variable name="sttlmDt" select="$tx/*:IntrBkSttlmDt"/>
        <xsl:text>
:32A:</xsl:text>
        <xsl:value-of select="concat(substring($sttlmDt, 3, 2), substring($sttlmDt, 6, 2), substring($sttlmDt, 9, 2))"/>
        <xsl:value-of select="$tx/*:IntrBkSttlmAmt/@Ccy"/>
        <xsl:value-of select="translate(format-number($tx/*:IntrBkSttlmAmt, '#.00'), '.', ',')"/>

        <!-- Debtor (50A) -->
        <xsl:text>
:50A:</xsl:text>
        <xsl:variable name="dbtrAcct" select="($tx/*:DbtrAcct/*:Id/*:IBAN | $tx/*:DbtrAcct/*:Id/*:Othr/*:Id)[1]"/>
        <xsl:if test="$dbtrAcct"><xsl:text>/</xsl:text><xsl:value-of select="$dbtrAcct"/><xsl:text>
</xsl:text></xsl:if>
        <xsl:value-of select="substring($tx/*:Dbtr/*:Nm, 1, 35)"/>

        <!-- Intermediary Agent (56A) -->
        <xsl:if test="$tx/*:IntrmyAgt1/*:FinInstnId/*:BICFI">
            <xsl:text>
:56A:</xsl:text><xsl:value-of select="$tx/*:IntrmyAgt1/*:FinInstnId/*:BICFI"/>
        </xsl:if>

        <!-- Account With Institution (57A) -->
        <xsl:if test="$tx/*:CdtrAgt/*:FinInstnId/*:BICFI">
            <xsl:text>
:57A:</xsl:text><xsl:value-of select="$tx/*:CdtrAgt/*:FinInstnId/*:BICFI"/>
        </xsl:if>

        <!-- Creditor (59) -->
        <xsl:text>
:59:</xsl:text>
        <xsl:variable name="cdtrIban" select="($tx/*:CdtrAcct/*:Id/*:IBAN | $tx/*:CdtrAcct/*:Id/*:Othr/*:Id)[1]"/>
        <xsl:if test="$cdtrIban"><xsl:text>/</xsl:text><xsl:value-of select="$cdtrIban"/><xsl:text>
</xsl:text></xsl:if>
        <xsl:value-of select="substring($tx/*:Cdtr/*:Nm, 1, 35)"/>

        <!-- Remittance Information (70) -->
        <xsl:if test="$tx/*:RmtInf/*:Ustrd">
            <xsl:text>
:70:</xsl:text>
            <xsl:for-each select="$tx/*:RmtInf/*:Ustrd">
                <xsl:value-of select="substring(., 1, 35)"/>
                <xsl:if test="position() &lt; last() and position() &lt; 4">
                    <xsl:text>
</xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:if>

        <!-- Details of Charges (71A) -->
        <xsl:text>
:71A:</xsl:text>
        <xsl:choose>
            <xsl:when test="$tx/*:ChrgBr = 'DEBT'">OUR</xsl:when>
            <xsl:when test="$tx/*:ChrgBr = 'CRED'">BEN</xsl:when>
            <xsl:otherwise>SHA</xsl:otherwise>
        </xsl:choose>

        <xsl:text>
-}</xsl:text>
    </xsl:template>
</xsl:stylesheet>
