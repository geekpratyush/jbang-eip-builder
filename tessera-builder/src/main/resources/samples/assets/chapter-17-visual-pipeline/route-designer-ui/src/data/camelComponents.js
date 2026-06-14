export const CAMEL_COMPONENTS = [
  {
    category: "Core Components",
    items: [
      { id: 'timer', label: 'Timer', icon: 'Clock', type: 'source', description: 'Generate periodic messages', defaultData: { uri: 'timer:tick?period=5000' } },
      { id: 'log', label: 'Log', icon: 'FileText', type: 'step', description: 'Log message to console', defaultData: { uri: 'log:info' } },
      { id: 'direct', label: 'Direct', icon: 'ArrowRight', type: 'both', description: 'Synchronous invocation', defaultData: { uri: 'direct:start' } },
      { id: 'file', label: 'File', icon: 'Folder', type: 'both', description: 'Read/Write files', defaultData: { uri: 'file:data/input' } },
      { id: 'set-body', label: 'Set Body', icon: 'Type', type: 'step', description: 'Set message body', defaultData: { expression: { simple: 'Hello World' } } },
      { id: 'set-header', label: 'Set Header', icon: 'Tag', type: 'step', description: 'Set message header', defaultData: { name: 'myHeader', expression: { simple: 'myValue' } } },
    ]
  },
  {
    category: "EIP Patterns",
    items: [
      { 
        id: 'choice', 
        label: 'Choice', 
        icon: 'Split', 
        type: 'step', 
        description: 'Content-based router', 
        isComplex: true,
        scaffold: {
          nodes: [
            { id: 'when-1', label: 'When', data: { expression: { simple: '${header.type} == "A"' } } },
            { id: 'when-2', label: 'When', data: { expression: { simple: '${header.type} == "B"' } } },
            { id: 'otherwise', label: 'Otherwise', data: {} }
          ]
        }
      },
      { 
        id: 'split', 
        label: 'Split', 
        icon: 'Layers', 
        type: 'step', 
        description: 'Split message into parts',
        isComplex: true,
        scaffold: {
          nodes: [
            { id: 'split-step-1', label: 'Processor 1', data: { uri: 'log:split-item' } }
          ],
          data: { expression: { simple: '${body}' } }
        }
      },
      { id: 'aggregate', label: 'Aggregate', icon: 'Combine', type: 'step', description: 'Combine multiple messages', defaultData: { strategyRef: 'myStrategy', completionSize: 5 } },
      { id: 'wire-tap', label: 'Wire Tap', icon: 'Trello', type: 'step', description: 'Send a copy to another endpoint', defaultData: { uri: 'direct:tap' } },
    ]
  },
  {
    category: "Transformation",
    items: [
      { id: 'xslt-saxon', label: 'XSLT (Saxon)', icon: 'Zap', type: 'step', description: 'XML transformation using XSLT', defaultData: { uri: 'xslt-saxon:transform.xsl' } },
      { id: 'jslt', label: 'JSLT', icon: 'Code', type: 'step', description: 'JSON transformation', defaultData: { uri: 'jslt:transform.jslt' } },
      { id: 'groovy', label: 'Groovy', icon: 'ScrollText', type: 'step', description: 'Execute Groovy script', defaultData: { expression: { groovy: 'request.body = "Fixed"' } } },
      { id: 'joor', label: 'jOOR', icon: 'Coffee', type: 'step', description: 'Java runtime expression', defaultData: { expression: { joor: 'body.toUpperCase()' } } },
      { id: 'smooks', label: 'Smooks', icon: 'BoxSelect', type: 'step', description: 'Data mapping and processing', defaultData: { configuration: 'smooks-config.xml' } },
      { id: 'flatpack', label: 'Flatpack', icon: 'AlignJustify', type: 'step', description: 'Fixed width/delimited files', defaultData: { uri: 'flatpack:fixed:mapping.pzmap.xml' } },
    ]
  },
  {
    category: "SWIFT / Financial",
    items: [
      { id: 'unmarshal-swift-mt', label: 'Unmarshal MT', icon: 'UnfoldVertical', type: 'step', description: 'Decode SWIFT MT message', defaultData: { format: 'swift-mt' } },
      { id: 'unmarshal-swift-mx', label: 'Unmarshal MX', icon: 'UnfoldVertical', type: 'step', description: 'Decode SWIFT MX (ISO20022)', defaultData: { format: 'swift-mx' } },
      { id: 'marshal-swift-mt', label: 'Marshal MT', icon: 'FoldVertical', type: 'step', description: 'Encode SWIFT MT message', defaultData: { format: 'swift-mt' } },
      { id: 'marshal-swift-mx', label: 'Marshal MX', icon: 'FoldVertical', type: 'step', description: 'Encode SWIFT MX (ISO20022)', defaultData: { format: 'swift-mx' } },
    ]
  }
];
