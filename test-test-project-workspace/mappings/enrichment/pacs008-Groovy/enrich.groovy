import groovy.xml.*

try {
    def envelope = new XmlSlurper().parseText(body)
    def original = envelope.original.Document
    def truncated = envelope.truncated.Document

    // Recursive merge function using name-based counters for position matching
    def mergeLogic
    mergeLogic = { orig, trunc, b ->
        if (orig.children().size() == 0) {
            // Leaf node: prefer truncated value if present
            def tText = trunc.text()
            b.mkp.yield((tText && tText != "null") ? tText : orig.text())
        } else {
            // Track how many times we've seen each tag name at this level
            def nameCounters = [:].withDefault { 0 }
            
            orig.children().each { child ->
                def cName = child.name()
                def pos = nameCounters[cName]++
                
                // Match with the N-th child of the same name in the truncated tree
                def tChild = trunc."${cName}"[pos]
                
                b."${cName}"(child.attributes()) {
                    mergeLogic(child, tChild, delegate)
                }
            }
        }
    }

    def builder = new StreamingMarkupBuilder()
    builder.encoding = "UTF-8"
    def merged = builder.bind {
        mkp.xmlDeclaration()
        // Use explicit delegate to ensure tags are routed to the builder
        delegate."Document"("xmlns": "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14") {
            mergeLogic(original, truncated, delegate)
        }
    }

    // Serialize and return
    return XmlUtil.serialize(merged)

} catch (Exception e) {
    return "Error during Groovy enrichment: " + e.toString()
}
