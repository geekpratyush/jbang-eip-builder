<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:tessera="http://tessera.com/functions"
    xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02"
    exclude-result-prefixes="xs tessera">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:function name="tessera:formatDate" as="xs:string">
        <xsl:param name="dateStr" as="xs:string?"/>
        <xsl:sequence select="concat('20', substring($dateStr, 1, 2), '-', substring($dateStr, 3, 2), '-', substring($dateStr, 5, 2))"/>
    </xsl:function>

    <xsl:function name="tessera:formatAmount" as="xs:string">
        <xsl:param name="amtStr" as="xs:string?"/>
        <xsl:sequence select="translate(string($amtStr), ',', '.')"/>
    </xsl:function>

    <xsl:template match="/message">
        <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02">
            <FIToFICstmrCdtTrf>
                <GrpHdr>
                    <MsgId><xsl:value-of select="block3/tag[name='108']/value"/></MsgId>
                    <CreDtTm><xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]Z')"/></CreDtTm>
                    <NbOfTxs>1</NbOfTxs>
                    <SttlmInf><SttlmMtd>CLRG</SttlmMtd><ClrSys><Prtry>SEPA</Prtry></ClrSys></SttlmInf>
                </GrpHdr>
                <CdtTrfTxInf>
                    <PmtId>
                        <InstrId><xsl:value-of select="block4/field[name='20']/component[@number='1']"/></InstrId>
                        <EndToEndId><xsl:value-of select="block4/field[name='20']/component[@number='1']"/></EndToEndId>
                    </PmtId>
                    <PmtTpInf>
                        <SvcLvl><Cd>SEPA</Cd></SvcLvl>
                    </PmtTpInf>
                    <IntrBkSttlmAmt Ccy="EUR">
                        <xsl:value-of select="tessera:formatAmount(block4/field[name='32A']/component[@number='3'])"/>
                    </IntrBkSttlmAmt>
                    <IntrBkSttlmDt><xsl:value-of select="tessera:formatDate(block4/field[name='32A']/component[@number='1'])"/></IntrBkSttlmDt>
                    <ChrgBr>SLEV</ChrgBr>
                    <Dbtr><Nm><xsl:value-of select="block4/field[name='50A']/component[@number='2']"/></Nm></Dbtr>
                    <DbtrAcct><Id><IBAN><xsl:value-of select="substring-after(block4/field[name='50A']/component[@number='1'], '/')"/></IBAN></Id></DbtrAcct>
                    <DbtrAgt><FinInstnId><BIC><xsl:value-of select="block4/field[name='52A']/component[@number='1']"/></BIC></FinInstnId></DbtrAgt>
                    <CdtrAgt><FinInstnId><BIC><xsl:value-of select="block4/field[name='57A']/component[@number='1']"/></BIC></FinInstnId></CdtrAgt>
                    <Cdtr><Nm><xsl:value-of select="block4/field[name='59']/component[@number='2']"/></Nm></Cdtr>
                    <CdtrAcct><Id><IBAN><xsl:value-of select="substring-after(block4/field[name='59']/component[@number='1'], '/')"/></IBAN></Id></CdtrAcct>
                    <xsl:if test="block4/field[name='70']">
                        <RmtInf><Ustrd><xsl:value-of select="block4/field[name='70']/component[@number='1']"/></Ustrd></RmtInf>
                    </xsl:if>
                </CdtTrfTxInf>
            </FIToFICstmrCdtTrf>
        </Document>
    </xsl:template>
</xsl:stylesheet>