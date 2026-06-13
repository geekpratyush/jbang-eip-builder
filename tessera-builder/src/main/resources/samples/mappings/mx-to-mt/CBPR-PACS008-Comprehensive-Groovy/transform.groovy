import groovy.xml.XmlSlurper

try {
    // Parse XML
    def doc = new XmlSlurper().parseText(body)

    // Handle potential Document wrapper
    def root = doc.name() == 'Document' ? doc.FIToFICstmrCdtTrf : doc
    if (root.name() != 'FIToFICstmrCdtTrf') {
        // Fallback for namespaced search if root wasn't found directly
        root = doc.'**'.find { it.name() == 'FIToFICstmrCdtTrf' }
    }
    
    def hdr = root.GrpHdr
    def tx = root.CdtTrfTxInf[0]

    // Helpers
    def slice = { str, len -> 
        def s = str?.toString() ?: ""
        return s.length() > len ? s.take(len) : s 
    }
    def padBIC = { bic -> 
        def b = bic?.toString() ?: ""
        return b.padRight(12, 'X').take(12) 
    }

    // Field extraction
    def instgBIC = padBIC(hdr.InstgAgt.FinInstnId.BICFI.text() ?: tx.InstgAgt.FinInstnId.BICFI.text())
    def instdBIC = padBIC(hdr.InstdAgt.FinInstnId.BICFI.text() ?: tx.InstdAgt.FinInstnId.BICFI.text())
    def msgId = slice(hdr.MsgId.text(), 16)
    def uetr = tx.PmtId.UETR.text()

    def instrId = slice(tx.PmtId.InstrId.text() ?: tx.PmtId.EndToEndId.text(), 16)
    def sttlmDt = tx.IntrBkSttlmDt.text().replaceAll('-', '')
    if (sttlmDt.length() >= 8) sttlmDt = sttlmDt[2..7]
    
    def amtNode = tx.IntrBkSttlmAmt
    def amtVal = amtNode.text().replace('.', ',')
    def amtCcy = amtNode.@Ccy.text()

    def dbtrAcct = tx.DbtrAcct.Id.IBAN.text() ?: tx.DbtrAcct.Id.Othr.Id.text()
    def dbtrNm = slice(tx.Dbtr.Nm.text(), 35)

    def intrmyBIC = tx.IntrmyAgt1.FinInstnId.BICFI.text()
    def cdtrAgtBIC = tx.CdtrAgt.FinInstnId.BICFI.text()

    def cdtrAcct = tx.CdtrAcct.Id.IBAN.text() ?: tx.CdtrAcct.Id.Othr.Id.text()
    def cdtrNm = slice(tx.Cdtr.Nm.text(), 35)

    def rmtLines = tx.RmtInf.Ustrd.collect { slice(it.text(), 35) }
    def rmtInf = rmtLines.join('\n')

    def chrgBr = tx.ChrgBr.text()
    def mtChrg = (chrgBr == "DEBT") ? "OUR" : (chrgBr == "CRED") ? "BEN" : "SHA"

    // Assemble MT103
    def mt = new StringBuilder()
    mt.append("{1:F01${instgBIC}0000000000}")
    mt.append("{2:I103${instdBIC}N}")
    mt.append("{3:{108:${msgId}}")
    if (uetr) mt.append("{121:${uetr}}")
    mt.append("}{4:\n")
    mt.append(":20:${instrId}\n")
    mt.append(":23B:CRED\n")
    mt.append(":32A:${sttlmDt}${amtCcy}${amtVal}\n")
    mt.append(":50A:${dbtrAcct ? '/' + dbtrAcct + '\n' : ''}${dbtrNm}\n")
    if (intrmyBIC) mt.append(":56A:${intrmyBIC}\n")
    if (cdtrAgtBIC) mt.append(":57A:${cdtrAgtBIC}\n")
    mt.append(":59:${cdtrAcct ? '/' + cdtrAcct + '\n' : ''}${cdtrNm}\n")
    if (rmtInf) mt.append(":70:${rmtInf}\n")
    mt.append(":71A:${mtChrg}\n")
    mt.append("-}")

    return mt.toString()
} catch (Exception e) {
    return "Error: " + e.toString()
}
