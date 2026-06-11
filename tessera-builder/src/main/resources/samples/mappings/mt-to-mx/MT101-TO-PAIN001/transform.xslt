<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03"
    exclude-result-prefixes="xs">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/message">
        <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
            <CstmrCdtTrfInitn>
                <GrpHdr>
                    <MsgId><xsl:value-of select="block3/tag[name='108']/value"/></MsgId>
                    <CreDtTm><xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]Z')"/></CreDtTm>
                    <NbOfTxs>1</NbOfTxs>
                    <InitgPty>
                        <Nm><xsl:value-of select="block4/field[name='50L']/component[@number='1']"/></Nm>
                    </InitgPty>
                </GrpHdr>
                <PmtInf>
                    <PmtInfId><xsl:value-of select="block4/field[name='20']/component[@number='1']"/></PmtInfId>
                    <PmtMtd>TRF</PmtMtd>
                    <ReqdExctnDt>
                        <Dt>20<xsl:value-of select="substring(block4/field[name='30']/component[@number='1'], 1, 2)"/>-<xsl:value-of select="substring(block4/field[name='30']/component[@number='1'], 3, 2)"/>-<xsl:value-of select="substring(block4/field[name='30']/component[@number='1'], 5, 2)"/></Dt>
                    </ReqdExctnDt>
                    <Dbtr>
                        <Nm><xsl:value-of select="block4/field[name='50L']/component[@number='1']"/></Nm>
                    </Dbtr>
                    <DbtrAgt>
                        <FinInstnId><BICFI><xsl:value-of select="substring(block1/logicalTerminal, 1, 11)"/></BICFI></FinInstnId>
                    </DbtrAgt>
                    <CdtTrfTxInf>
                        <PmtId>
                            <EndToEndId><xsl:value-of select="block4/field[name='21']/component[@number='1']"/></EndToEndId>
                        </PmtId>
                        <Amt>
                            <InstdAmt>
                                <xsl:attribute name="Ccy"><xsl:value-of select="block4/field[name='32B']/component[@number='1']"/></xsl:attribute>
                                <xsl:value-of select="translate(block4/field[name='32B']/component[@number='2'], ',', '.')"/>
                            </InstdAmt>
                        </Amt>
                        <Cdtr>
                            <Nm><xsl:value-of select="block4/field[name='59']/component[@number='2']"/></Nm>
                        </Cdtr>
                        <CdtrAcct>
                            <Id><IBAN><xsl:value-of select="substring(block4/field[name='59']/component[@number='1'], 2)"/></IBAN></Id>
                        </CdtrAcct>
                    </CdtTrfTxInf>
                </PmtInf>
            </CstmrCdtTrfInitn>
        </Document>
    </xsl:template>
</xsl:stylesheet>
