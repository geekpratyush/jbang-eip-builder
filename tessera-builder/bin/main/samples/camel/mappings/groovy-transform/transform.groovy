// Groovy Mapper
def order = new XmlSlurper().parseText(body)
def total = order.item.collect { it.@price.toInteger() * it.@qty.toInteger() }.sum()

return "<result><total>${total}</total></result>"
