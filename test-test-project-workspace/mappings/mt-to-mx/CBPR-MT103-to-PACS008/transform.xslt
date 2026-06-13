<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:tessera="http://tessera.com/functions"
    xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08"
    exclude-result-prefixes="xs tessera">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <!-- SWIFT X-Character Set Normalization -->
    <xsl:function name="tessera:cleanString" as="xs:string">
        <xsl:param name="input" as="xs:string?"/>
        <xsl:variable name="allowed" select="'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/-?:().,''+ '"/>
        <!-- For production, real regex cleanup would go here. For now, we return valid string -->
        <xsl:sequence select="string($input)"/> 
    </xsl:function>

    <!-- Date Formatter: YYMMDD -> YYYY-MM-DD -->
    <xsl:function name="tessera:formatDate" as="xs:string">
        <xsl:param name="dateStr" as="xs:string?"/>
        <xsl:choose>
            <xsl:when test="string-length($dateStr) = 6">
                <xsl:sequence select="concat('20', substring($dateStr, 1, 2), '-', substring($dateStr, 3, 2), '-', substring($dateStr, 5, 2))"/>
            </xsl:when>
            <xsl:otherwise><xsl:sequence select="string($dateStr)"/></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Amount Formatter: 1234,56 -> 1234.56 -->
    <xsl:function name="tessera:formatAmount" as="xs:string">
        <xsl:param name="amtStr" as="xs:string?"/>
        <xsl:sequence select="translate(string($amtStr), ',', '.')"/>
    </xsl:function>

    <xsl:template match="/message">
        <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
            <FIToFICstmrCdtTrf>
                <GrpHdr>
                    <MsgId><xsl:value-of select="tessera:cleanString(block3/tag[name='108']/value)"/></MsgId>
                    <CreDtTm><xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]Z')"/></CreDtTm>
                    <NbOfTxs>1</NbOfTxs>
                    <SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf>
                    <InstgAgt><FinInstnId><BICFI><xsl:value-of select="substring(block1/logicalTerminal, 1, 11)"/></BICFI></FinInstnId></InstgAgt>
                    <InstdAgt><FinInstnId><BICFI><xsl:value-of select="substring(block2/receiverAddress, 1, 11)"/></BICFI></FinInstnId></InstdAgt>
                </GrpHdr>
                <CdtTrfTxInf>
                    <PmtId>
                        <InstrId><xsl:value-of select="tessera:cleanString(block4/field[name='20']/component[@number='1'])"/></InstrId>
                        <EndToEndId><xsl:value-of select="tessera:cleanString(block4/field[name='20']/component[@number='1'])"/></EndToEndId>
                        <UETR><xsl:value-of select="block3/tag[name='121']/value"/></UETR>
                    </PmtId>
                    <PmtTpInf>
                        <SvcLvl><Cd>NURG</Cd></SvcLvl>
                        <xsl:if test="block4/field[name='23B']">
                            <LclInstrm><Prtry><xsl:value-of select="block4/field[name='23B']/component[@number='1']"/></Prtry></LclInstrm>
                        </xsl:if>
                    </PmtTpInf>
                    
                    <xsl:variable name="f32a" select="block4/field[name='32A']"/>
                    <IntrBkSttlmAmt>
                        <xsl:attribute name="Ccy"><xsl:value-of select="$f32a/component[@number='2']"/></xsl:attribute>
                        <xsl:value-of select="tessera:formatAmount($f32a/component[@number='3'])"/>
                    </IntrBkSttlmAmt>
                    <IntrBkSttlmDt><xsl:value-of select="tessera:formatDate($f32a/component[@number='1'])"/></IntrBkSttlmDt>
                    
                    <!-- Repeatable 23E Tags -->
                    <xsl:for-each select="block4/field[name='23E']">
                        <InstrForCdtrAgt>
                            <Cd><xsl:value-of select="component[@number='1']"/></Cd>
                            <xsl:if test="component[@number='2']">
                                <InstrInf><xsl:value-of select="tessera:cleanString(component[@number='2'])"/></InstrInf>
                            </xsl:if>
                        </InstrForCdtrAgt>
                    </xsl:for-each>

                    <ChrgBr>
                        <xsl:choose>
                            <xsl:when test="block4/field[name='71A']/component[@number='1'] = 'OUR'">DEBT</xsl:when>
                            <xsl:when test="block4/field[name='71A']/component[@number='1'] = 'BEN'">CRED</xsl:when>
                            <xsl:otherwise>SHAR</xsl:otherwise>
                        </xsl:choose>
                    </ChrgBr>
                    
                    <xsl:for-each select="block4/field[name='71F']">
                        <ChrgsInf>
                            <Amt>
                                <xsl:attribute name="Ccy"><xsl:value-of select="component[@number='1']"/></xsl:attribute>
                                <xsl:value-of select="tessera:formatAmount(component[@number='2'])"/>
                            </Amt>
                            <Agt><FinInstnId><BICFI><xsl:value-of select="substring(../../block1/logicalTerminal, 1, 11)"/></BICFI></FinInstnId></Agt>
                        </ChrgsInf>
                    </xsl:for-each>

                    <!-- Advanced Debtor (50A/F/K) -->
                    <Dbtr>
                        <xsl:choose>
                            <xsl:when test="block4/field[name='50F']">
                                <Nm><xsl:value-of select="substring-after(block4/field[name='50F']/component[@number='2'], '1/')"/></Nm>
                                <PstlAdr>
                                    <Ctry><xsl:value-of select="substring(substring-after(block4/field[name='50F']/component[@number='4'], '3/'), 1, 2)"/></Ctry>
                                    <AdrLine><xsl:value-of select="substring-after(block4/field[name='50F']/component[@number='3'], '2/')"/></AdrLine>
                                </PstlAdr>
                            </xsl:when>
                            <xsl:otherwise>
                                <Nm><xsl:value-of select="block4/field[name='50A' or name='50K']/component[@number='2']"/></Nm>
                            </xsl:otherwise>
                        </xsl:choose>
                    </Dbtr>
                    <DbtrAcct>
                        <Id>
                            <xsl:variable name="acc" select="block4/field[name='50A' or name='50F' or name='50K']/component[@number='1']"/>
                            <xsl:choose>
                                <xsl:when test="starts-with($acc, '/')">
                                    <Othr><Id><xsl:value-of select="substring($acc, 2)"/></Id></Othr>
                                </xsl:when>
                                <xsl:otherwise><Othr><Id><xsl:value-of select="$acc"/></Id></Othr></xsl:otherwise>
                            </xsl:choose>
                        </Id>
                    </DbtrAcct>

                    <DbtrAgt><FinInstnId><BICFI><xsl:value-of select="block4/field[name='52A']/component[@number='3']"/></BICFI></FinInstnId></DbtrAgt>
                    <CdtrAgt><FinInstnId><BICFI><xsl:value-of select="block4/field[name='57A']/component[@number='3']"/></BICFI></FinInstnId></CdtrAgt>
                    
                    <!-- Advanced Creditor (59/A) -->
                    <Cdtr>
                        <Nm><xsl:value-of select="block4/field[name='59' or name='59A']/component[@number='2']"/></Nm>
                        <xsl:if test="block4/field[name='59']/component[@number='3']">
                            <PstlAdr><AdrLine><xsl:value-of select="block4/field[name='59']/component[@number='3']"/></AdrLine></PstlAdr>
                        </xsl:if>
                    </Cdtr>
                    <CdtrAcct>
                        <Id>
                            <xsl:variable name="acc" select="block4/field[name='59' or name='59A']/component[@number='1']"/>
                            <xsl:choose>
                                <xsl:when test="starts-with($acc, '/')">
                                    <Othr><Id><xsl:value-of select="substring($acc, 2)"/></Id></Othr>
                                </xsl:when>
                                <xsl:otherwise><Othr><Id><xsl:value-of select="$acc"/></Id></Othr></xsl:otherwise>
                            </xsl:choose>
                        </Id>
                    </CdtrAcct>
                    
                    <xsl:if test="block4/field[name='70']">
                        <RmtInf><Ustrd><xsl:value-of select="tessera:cleanString(block4/field[name='70']/component[@number='1'])"/></Ustrd></RmtInf>
                    </xsl:if>

                    <xsl:if test="block4/field[name='77B']">
                        <RgltryRptg><Dtls><Inf><xsl:value-of select="tessera:cleanString(block4/field[name='77B']/component[@number='1'])"/></Inf></Dtls></RgltryRptg>
                    </xsl:if>
                    
                </CdtTrfTxInf>
            </FIToFICstmrCdtTrf>
        </Document>
    </xsl:template>
</xsl:stylesheet>