<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/message">
        <Document>
            <FIToFICstmrCdtTrf>
                <GrpHdr>
                    <MsgId><xsl:value-of select="block3/tag[name='108']/value"/></MsgId>
                    <CreDtTm><xsl:value-of select="current-dateTime()"/></CreDtTm>
                    <NbOfTxs>1</NbOfTxs>
                    <SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf>
                    <InstgAgt><FinInstnId><BICFI><xsl:value-of select="substring(block1/logicalTerminal, 1, 11)"/></BICFI></FinInstnId></InstgAgt>
                    <InstdAgt><FinInstnId><BICFI><xsl:value-of select="substring(block2/receiverAddress, 1, 11)"/></BICFI></FinInstnId></InstdAgt>
                </GrpHdr>
                <CdtTrfTxInf>
                    <PmtId>
                        <InstrId><xsl:value-of select="block4/field[name='20']/component[@number='1']"/></InstrId>
                        <EndToEndId><xsl:value-of select="block4/field[name='20']/component[@number='1']"/></EndToEndId>
                    </PmtId>
                    <xsl:variable name="f32a" select="block4/field[name='32A']"/>
                    <IntrBkSttlmAmt>
                        <xsl:attribute name="Ccy"><xsl:value-of select="$f32a/component[@number='2']"/></xsl:attribute>
                        <xsl:value-of select="translate($f32a/component[@number='3'], ',', '.')"/>
                    </IntrBkSttlmAmt>
                    <IntrBkSttlmDt>
                        <xsl:variable name="dateStr" select="$f32a/component[@number='1']"/>
                        <xsl:value-of select="concat('20', substring($dateStr, 1, 2), '-', substring($dateStr, 3, 2), '-', substring($dateStr, 5, 2))"/>
                    </IntrBkSttlmDt>
                    <ChrgBr>
                        <xsl:choose>
                            <xsl:when test="block4/field[name='71A']/component[@number='1'] = 'OUR'">DEBT</xsl:when>
                            <xsl:when test="block4/field[name='71A']/component[@number='1'] = 'BEN'">CRED</xsl:when>
                            <xsl:otherwise>SHAR</xsl:otherwise>
                        </xsl:choose>
                    </ChrgBr>
                    <Dbtr>
                        <Nm><xsl:value-of select="block4/field[name='50K']/component[@number='2']"/></Nm>
                    </Dbtr>
                    <DbtrAgt>
                        <FinInstnId><BICFI><xsl:value-of select="block4/field[name='52A']/component[@number='3']"/></BICFI></FinInstnId>
                    </DbtrAgt>
                    <CdtrAgt>
                        <FinInstnId><BICFI><xsl:value-of select="block4/field[name='57A']/component[@number='3']"/></BICFI></FinInstnId>
                    </CdtrAgt>
                    <Cdtr>
                        <Nm><xsl:value-of select="block4/field[name='59']/component[@number='2']"/></Nm>
                    </Cdtr>
                </CdtTrfTxInf>
            </FIToFICstmrCdtTrf>
        </Document>
    </xsl:template>
</xsl:stylesheet>