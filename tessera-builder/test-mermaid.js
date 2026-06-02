const fs = require('fs');
const jsdom = require("jsdom");
const { JSDOM } = jsdom;

const dom = new JSDOM(`<!DOCTYPE html><p>Hello world</p>`);
global.DOMParser = dom.window.DOMParser;
global.document = dom.window.document;

const xmlString = `
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/MT103">
    <Document>
      <FIToFICstmrCdtTrf>
        <GrpHdr>
          <MsgId><xsl:value-of select="BasicHeader/MessageReference"/></MsgId>
        </GrpHdr>
        <xsl:for-each select="Transaction">
          <CdtTrfTxInf>
            <xsl:choose>
              <xsl:when test="Amount &gt; 100000">
                <Priority>HIGH</Priority>
              </xsl:when>
              <xsl:otherwise>
                <Priority>NORM</Priority>
              </xsl:otherwise>
            </xsl:choose>
            <InstdAmt><xsl:value-of select="Amount"/></InstdAmt>
            <xsl:if test="Currency != 'USD'">
              <FxApplied>TRUE</FxApplied>
            </xsl:if>
          </CdtTrfTxInf>
        </xsl:for-each>
      </FIToFICstmrCdtTrf>
    </Document>
  </xsl:template>
</xsl:stylesheet>
`;

const themes = {
  dark: {
    mermaid: `
      classDef default fill:#1E293B,stroke:#475569,stroke-width:2px,color:#F8FAFC;
      classDef template fill:#334155,stroke:#0F172A,stroke-width:3px,color:#F8FAFC;
      classDef loop fill:#D97706,stroke:#B45309,stroke-width:2px,color:#fff;
      classDef condition fill:#0284C7,stroke:#0369A1,stroke-width:2px,color:#fff;
      classDef extract fill:#059669,stroke:#047857,stroke-width:2px,color:#fff;
    `
  }
};

function convertToMermaid(rootNode, layout, themeKey) {
  let graphCode = `graph ${layout}\n`;
  let nodeIdCounter = 0;
  
  let classGroups = { template: [], loop: [], condition: [], extract: [], default: [] };

  function traverse(node, parentId) {
    if (node.nodeType !== 1) return;

    let tagName = node.localName || node.nodeName;
    tagName = tagName.replace('xsl:', ''); 

    let currentId = "N" + (nodeIdCounter++);
    let label = tagName;
    let shapeOpen = "["; let shapeClose = "]";
    let group = "default";

    if (tagName === "stylesheet" || tagName === "transform") {
      return Array.from(node.children).forEach(child => traverse(child, parentId));
    }
    else if (tagName === "template") {
      label = `Template: ${node.getAttribute('match') || node.getAttribute('name') || ''}`;
      shapeOpen = "(["; shapeClose = "])"; group = "template";
    } 
    else if (tagName === "for-each") {
      label = `Loop: ${node.getAttribute('select') || ''}`;
      shapeOpen = "[["; shapeClose = "]]"; group = "loop";
    } 
    else if (tagName === "choose" || tagName === "when" || tagName === "if") {
      let testAttr = node.getAttribute('test') ? `: ${node.getAttribute('test')}` : '';
      label = tagName === "choose" ? "Decision Block" : `If${testAttr}`;
      shapeOpen = "{"; shapeClose = "}"; group = "condition";
    } 
    else if (tagName === "otherwise") {
      label = "Default (Otherwise)"; group = "condition";
    } 
    else if (tagName === "value-of") {
      label = `Extract: ${node.getAttribute('select') || ''}`;
      shapeOpen = ">"; shapeClose = "]"; group = "extract";
    }
    else {
      label = `<${tagName}>`; shapeOpen = "(["; shapeClose = "])";
    }

    classGroups[group].push(currentId);
    label = label.replace(/["']/g, "").replace(/</g, "&lt;").replace(/>/g, "&gt;"); 

    graphCode += `    ${currentId}${shapeOpen}"${label}"${shapeClose}\n`;
    if (parentId) graphCode += `    ${parentId} --> ${currentId}\n`;

    Array.from(node.children).forEach(child => traverse(child, currentId));
  }

  traverse(rootNode, null);
  
  graphCode += `\n${themes[themeKey].mermaid}\n`;

  for (const [className, nodeIds] of Object.entries(classGroups)) {
    if (nodeIds.length > 0) graphCode += `    class ${nodeIds.join(',')} ${className};\n`;
  }

  return graphCode;
}

const parser = new DOMParser();
const xmlDoc = parser.parseFromString(xmlString, "text/xml");
console.log(convertToMermaid(xmlDoc.documentElement, "TD", "dark"));
