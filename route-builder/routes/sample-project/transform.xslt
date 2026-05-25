<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/MTMessage">
        <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
            <FIToFICstmrCdtTrf>
                <GrpHdr>
                    <MsgId><xsl:value-of select="Header/MessageID"/></MsgId>
                    <CreDtTm><xsl:value-of select="Header/CreationDateTime"/></CreDtTm>
                </GrpHdr>
                <CdtTrfTxInf>
                    <PmtId>
                        <InstrId><xsl:value-of select="Body/TransactionID"/></InstrId>
                    </PmtId>
                    <!-- Merged enrichment details -->
                    <EnrichedData>
                        <xsl:value-of select="Enrichment/DBStatus"/>
                    </EnrichedData>
                </CdtTrfTxInf>
            </FIToFICstmrCdtTrf>
        </Document>
    </xsl:template>

</xsl:stylesheet>
