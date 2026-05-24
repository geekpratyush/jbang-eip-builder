<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:pacs="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14" exclude-result-prefixes="pacs">
<xsl:output method="text" encoding="UTF-8"/>
<xsl:template match="/"><xsl:apply-templates select="//pacs:FIToFICstmrCdtTrf"/></xsl:template>
<xsl:template match="pacs:FIToFICstmrCdtTrf">{1:F01<xsl:value-of select="(pacs:GrpHdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI | pacs:CdtTrfTxInf/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI)[1]"/>0000000000}{2:I103<xsl:value-of select="(pacs:GrpHdr/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI | pacs:CdtTrfTxInf/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI)[1]"/>N2}{3:{108:<xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/>}}{4:
:20:<xsl:choose><xsl:when test="pacs:CdtTrfTxInf/pacs:PmtId/pacs:InstrId"><xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:PmtId/pacs:InstrId, 1, 16)"/></xsl:when><xsl:otherwise><xsl:value-of select="substring(pacs:GrpHdr/pacs:MsgId, 1, 16)"/></xsl:otherwise></xsl:choose>
:23B:CRED
:32A:<xsl:value-of select="substring(translate(pacs:CdtTrfTxInf/pacs:IntrBkSttlmDt, '-', ''), 3, 6)"/><xsl:value-of select="pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt/@Ccy"/><xsl:value-of select="translate(pacs:CdtTrfTxInf/pacs:IntrBkSttlmAmt, '.', ',')"/>
:50K:/<xsl:value-of select="pacs:CdtTrfTxInf/pacs:DbtrAcct/pacs:Id/pacs:IBAN"/>
<xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:Dbtr/pacs:Nm, 1, 35)"/>
:59:/<xsl:value-of select="pacs:CdtTrfTxInf/pacs:CdtrAcct/pacs:Id/pacs:IBAN"/>
<xsl:value-of select="substring(pacs:CdtTrfTxInf/pacs:Cdtr/pacs:Nm, 1, 35)"/>
:71A:SHA
-}
</xsl:template></xsl:stylesheet>