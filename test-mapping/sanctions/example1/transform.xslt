<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/message">
    <ISO20022>
       <MsgId><xsl:value-of select="//tag[name='20']/value"/></MsgId>
    </ISO20022>
  </xsl:template>
</xsl:stylesheet>