<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pacs="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14"
    exclude-result-prefixes="pacs">

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">
        <xsl:apply-templates select="//pacs:FIToFICstmrCdtTrf"/>
    </xsl:template>

    <xsl:template match="pacs:FIToFICstmrCdtTrf">
        <xsl:text>{1:F01</xsl:text>
        <xsl:value-of select="(pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI | pacs:CdtTrfTxInf/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI)[1]"/>
        <xsl:text>0000000000}{2:I103</xsl:text>
        <xsl:value-of select="(pacs:GrpHdr/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI | pacs:CdtTrfTxInf/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI)[1]"/>
        <xsl:text>N2}{3:{108:</xsl:text>
        <xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/>
        <xsl:text>}}{4:
:20:</xsl:text>
        <xsl:choose>
            <xsl:when test="pacs:CdtTrfTxInf/pacs:PmtId/pacs:InstrId">
                <xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:PmtId/pacs:InstrId, 1, 16)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:text>
:23B:CRED</xsl:text>
        <xsl:if test="pacs:CdtTrfTxInf/pacs:InstrForCdtrAgt/pacs:Cd">
            <xsl:text>
:23E:</xsl:text>
            <xsl:value-of select="pacs:CdtTrfTxInf/pacs:InstrForCdtrAgt/pacs:Cd"/>
            <xsl:if test="pacs:CdtTrfTxInf/pacs:InstrForCdtrAgt/pacs:InstrInf">
                <xsl:text>/</xsl:text>
                <xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:InstrForCdtrAgt/pacs:InstrInf, 1, 30)"/>
            </xsl:if>
        </xsl:if>
        <xsl:text>
:32A:</xsl:text>
        <xsl:value-of select="substring(translate(pacs:CdtTrfTxInf/pacs:IntrBkSttlmDt, '-', ''), 3, 6)"/>
        <xsl:value-of select="pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt/@Ccy"/>
        <xsl:value-of select="translate(pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt, '.', ',')"/>
        
        <xsl:if test="pacs:CdtTrfTxInf/pacs:InstdAmt">
            <xsl:text>
:33B:</xsl:text>
            <xsl:value-of select="pacs:CdtTrfTxInf/pacs:InstdAmt/@Ccy"/>
            <xsl:value-of select="translate(pacs:CdtTrfTxInf/pacs:InstdAmt, '.', ',')"/>
        </xsl:if>
        
        <xsl:if test="pacs:CdtTrfTxInf/pacs:XchgRate">
            <xsl:text>
:36:</xsl:text>
            <xsl:value-of select="translate(pacs:CdtTrfTxInf/pacs:XchgRate, '.', ',')"/>
        </xsl:if>
        
        <xsl:apply-templates select="pacs:CdtTrfTxInf/pacs:Dbtr"/>
        
        <xsl:if test="pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI">
            <xsl:text>
:51A:</xsl:text>
            <xsl:if test="pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:ClrSysMmbId/pacs:MmbId">
                <xsl:text>/</xsl:text>
                <xsl:value-of select="pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:ClrSysMmbId/pacs:MmbId"/>
                <xsl:text>
</xsl:text>
            </xsl:if>
            <xsl:value-of select="pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI"/>
        </xsl:if>
        
        <xsl:apply-templates select="pacs:CdtTrfTxInf/pacs:DbtrAgt" mode="field52"/>
        
        <xsl:apply-templates select="pacs:CdtTrfTxInf/pacs:IntrmyAgt1" mode="field56"/>
        <xsl:apply-templates select="pacs:CdtTrfTxInf/pacs:CdtrAgt" mode="field57"/>
        <xsl:apply-templates select="pacs:CdtTrfTxInf/pacs:Cdtr"/>
        
        <xsl:apply-templates select="pacs:CdtTrfTxInf/pacs:RmtInf"/>
        
        <xsl:text>
:71A:</xsl:text>
        <xsl:choose>
            <xsl:when test="pacs:CdtTrfTxInf/pacs:ChrgBr = 'DEBT'">OUR</xsl:when>
            <xsl:when test="pacs:CdtTrfTxInf/pacs:ChrgBr = 'CRED'">BEN</xsl:when>
            <xsl:otherwise>SHA</xsl:otherwise>
        </xsl:choose>
        
        <xsl:for-each select="pacs:CdtTrfTxInf/pacs:ChrgsInf">
            <xsl:text>
:71F:</xsl:text>
            <xsl:value-of select="pacs:Amt/@Ccy"/>
            <xsl:value-of select="translate(pacs:Amt, '.', ',')"/>
        </xsl:for-each>
        
        <xsl:call-template name="field72"/>
        
        <xsl:text>
-}</xsl:text>
    </xsl:template>

    <xsl:template match="pacs:Dbtr">
        <xsl:choose>
            <xsl:when test="../pacs:DbtrAcct/pacs:Id/pacs:IBAN">
                <xsl:text>
:50K:</xsl:text>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="../pacs:DbtrAcct/pacs:Id/pacs:IBAN"/>
                <xsl:text>
</xsl:text>
                <xsl:value-of select="substring(pacs:Nm, 1, 35)"/>
                <xsl:for-each select="pacs:PstlAdr/pacs:AdrLine">
                    <xsl:text>
</xsl:text>
                    <xsl:value-of select="substring(., 1, 35)"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>
:50K:</xsl:text>
                <xsl:if test="../pacs:DbtrAcct/pacs:Id/pacs:Othr/pacs:Id">
                    <xsl:text>/</xsl:text>
                    <xsl:value-of select="../pacs:DbtrAcct/pacs:Id/pacs:Othr/pacs:Id"/>
                    <xsl:text>
</xsl:text>
                </xsl:if>
                <xsl:value-of select="substring(pacs:Nm, 1, 35)"/>
                <xsl:for-each select="pacs:PstlAdr/pacs:AdrLine">
                    <xsl:text>
</xsl:text>
                    <xsl:value-of select="substring(., 1, 35)"/>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="pacs:Cdtr">
        <xsl:choose>
            <xsl:when test="../pacs:CdtrAcct/pacs:Id/pacs:IBAN">
                <xsl:text>
:59:</xsl:text>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="../pacs:CdtrAcct/pacs:Id/pacs:IBAN"/>
                <xsl:text>
</xsl:text>
                <xsl:value-of select="substring(pacs:Nm, 1, 35)"/>
                <xsl:for-each select="pacs:PstlAdr/pacs:AdrLine">
                    <xsl:text>
</xsl:text>
                    <xsl:value-of select="substring(., 1, 35)"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>
:59:</xsl:text>
                <xsl:if test="../pacs:CdtrAcct/pacs:Id/pacs:Othr/pacs:Id">
                    <xsl:text>/</xsl:text>
                    <xsl:value-of select="../pacs:CdtrAcct/pacs:Id/pacs:Othr/pacs:Id"/>
                    <xsl:text>
</xsl:text>
                </xsl:if>
                <xsl:value-of select="substring(pacs:Nm, 1, 35)"/>
                <xsl:for-each select="pacs:PstlAdr/pacs:AdrLine">
                    <xsl:text>
</xsl:text>
                    <xsl:value-of select="substring(., 1, 35)"/>
                </xsl:for-each>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="pacs:RmtInf">
        <xsl:text>
:70:</xsl:text>
        <xsl:choose>
            <xsl:when test="pacs:Ustrd">
                <xsl:for-each select="pacs:Ustrd">
                    <xsl:if test="position() > 1">
                        <xsl:text>
</xsl:text>
                    </xsl:if>
                    <xsl:value-of select="substring(., 1, 35)"/>
                    <xsl:if test="string-length(.) > 35">
                        <xsl:text>
</xsl:text><xsl:value-of select="substring(., 36, 35)"/>
                    </xsl:if>
                    <xsl:if test="string-length(.) > 70">
                        <xsl:text>
</xsl:text><xsl:value-of select="substring(., 71, 35)"/>
                    </xsl:if>
                    <xsl:if test="string-length(.) > 105">
                        <xsl:text>
</xsl:text><xsl:value-of select="substring(., 106, 35)"/>
                    </xsl:if>
                </xsl:for-each>
            </xsl:when>
            </xsl:choose>
    </xsl:template>

    <xsl:template match="pacs:DbtrAgt" mode="field52">
        <xsl:text>
:52A:</xsl:text>
        <xsl:value-of select="pacs:FinInstnId/pacs:BICFI"/>
    </xsl:template>

    <xsl:template match="pacs:IntrmyAgt1" mode="field56">
        <xsl:text>
:56A:</xsl:text>
        <xsl:value-of select="pacs:FinInstnId/pacs:BICFI"/>
    </xsl:template>

    <xsl:template match="pacs:CdtrAgt" mode="field57">
        <xsl:text>
:57A:</xsl:text>
        <xsl:value-of select="pacs:FinInstnId/pacs:BICFI"/>
    </xsl:template>

    <xsl:template name="field72">
        </xsl:template>

</xsl:stylesheet>