<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:p="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14" exclude-result-prefixes="p">
  <xsl:output method="xml" indent="yes" encoding="UTF-8" />
  <xsl:template match="/*[local-name()='envelope']">
    <xsl:variable name="mergedTree">
      <xsl:apply-templates select="*[local-name()='original']/p:Document" mode="merge">
        <xsl:with-param name="truncNode" select="*[local-name()='truncated']/p:Document" />
      </xsl:apply-templates>
    </xsl:variable>
  <xsl:apply-templates
      select="$mergedTree" mode="clean" />
  </xsl:template>
  <xsl:template match="*" mode="merge">
    <xsl:param name="truncNode" as="node()*" />
  <xsl:element name="{local-name()}"
      namespace="{namespace-uri()}">
      <xsl:copy-of select="@*" />
    <xsl:choose>
        <xsl:when test="count(*)=0">
          <xsl:choose>
            <xsl:when test="$truncNode/text()"><xsl:value-of select="$truncNode/text()" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="text()" /></xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:for-each select="*">
            <xsl:variable name="nodeName" select="local-name()" />
          <xsl:variable name="pos"
              select="count(preceding-sibling::*[local-name() = $nodeName]) + 1" />
          <xsl:apply-templates
              select="." mode="merge">
              <xsl:with-param name="truncNode" select="$truncNode/*[local-name() = $nodeName][$pos]" />
            </xsl:apply-templates>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>
  <xsl:template match="node()" mode="clean"><xsl:copy><xsl:apply-templates select="node()"
        mode="clean" /></xsl:copy></xsl:template>
</xsl:stylesheet>