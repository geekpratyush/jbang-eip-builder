import React, { useState, useEffect } from 'react';
import { Settings, Code, Copy, Check } from 'lucide-react';
import yaml from 'js-yaml';

const PropertiesPanel = ({ selectedNode }) => {
  const [copied, setCopied] = useState(false);
  const [activeTab, setActiveTab] = useState('properties');

  const handleCopy = () => {
    const yamlString = generateYaml();
    navigator.clipboard.writeText(yamlString);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const generateYaml = () => {
    if (!selectedNode) return '# No node selected';
    
    const nodeData = selectedNode.data;
    const camelObj = {};

    if (nodeData.type === 'source') {
      camelObj.from = {
        uri: nodeData.uri,
        steps: []
      };
    } else if (nodeData.id === 'choice') {
      camelObj.choice = {
        when: [
          { expression: { simple: '${header.type} == "A"' }, steps: [{ to: 'log:A' }] },
          { expression: { simple: '${header.type} == "B"' }, steps: [{ to: 'log:B' }] }
        ],
        otherwise: { steps: [{ to: 'log:otherwise' }] }
      };
    } else if (nodeData.id === 'split') {
      camelObj.split = {
        expression: nodeData.expression,
        steps: [{ to: 'log:split-item' }]
      };
    } else if (nodeData.uri) {
      camelObj.to = nodeData.uri;
    } else if (nodeData.expression) {
      camelObj[nodeData.id] = { expression: nodeData.expression };
    } else {
      camelObj[nodeData.id] = nodeData;
    }

    return yaml.dump(camelObj, { indent: 2 });
  };

  if (!selectedNode) {
    return (
      <div className="w-80 bg-slate-900 border-l border-slate-700 flex flex-col items-center justify-center p-8 text-center text-slate-500">
        <Settings className="h-12 w-12 mb-4 opacity-20" />
        <p className="text-sm italic">Select a node on the canvas to edit its properties or view its DSL.</p>
      </div>
    );
  }

  return (
    <div className="w-80 bg-slate-900 border-l border-slate-700 flex flex-col text-slate-300">
      <div className="flex border-b border-slate-700">
        <button
          onClick={() => setActiveTab('properties')}
          className={`flex-1 py-3 text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 transition-colors ${
            activeTab === 'properties' ? 'text-blue-400 border-b-2 border-blue-500 bg-blue-500/5' : 'text-slate-500 hover:text-slate-300'
          }`}
        >
          <Settings className="h-3.5 w-3.5" />
          Properties
        </button>
        <button
          onClick={() => setActiveTab('preview')}
          className={`flex-1 py-3 text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 transition-colors ${
            activeTab === 'preview' ? 'text-blue-400 border-b-2 border-blue-500 bg-blue-500/5' : 'text-slate-500 hover:text-slate-300'
          }`}
        >
          <Code className="h-3.5 w-3.5" />
          YAML Preview
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {activeTab === 'properties' ? (
          <div className="space-y-6">
            <div>
              <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1.5 tracking-widest">Node ID</label>
              <input 
                type="text" 
                readOnly 
                value={selectedNode.id}
                className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-sm text-slate-400 font-mono"
              />
            </div>
            
            {selectedNode.data.uri !== undefined && (
              <div>
                <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1.5 tracking-widest">Endpoint URI</label>
                <input 
                  type="text" 
                  defaultValue={selectedNode.data.uri}
                  className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-sm focus:ring-1 focus:ring-blue-500 outline-none font-mono"
                />
              </div>
            )}

            {selectedNode.data.expression && (
              <div className="space-y-4">
                <label className="block text-[10px] font-bold uppercase text-slate-500 mb-[-8px] tracking-widest">Expression</label>
                {Object.keys(selectedNode.data.expression).map(lang => (
                  <div key={lang}>
                    <span className="text-[10px] text-blue-400 font-mono uppercase mb-1 block">{lang}</span>
                    <textarea 
                      defaultValue={selectedNode.data.expression[lang]}
                      rows={3}
                      className="w-full bg-slate-800 border border-slate-700 rounded p-2 text-sm focus:ring-1 focus:ring-blue-500 outline-none font-mono resize-none"
                    />
                  </div>
                ))}
              </div>
            )}

            {selectedNode.data.description && (
              <p className="text-xs text-slate-500 bg-slate-800/50 p-3 rounded-lg border border-slate-700/50 italic">
                {selectedNode.data.description}
              </p>
            )}
          </div>
        ) : (
          <div className="relative h-full">
            <button
              onClick={handleCopy}
              className="absolute right-2 top-2 p-1.5 bg-slate-800 border border-slate-700 rounded hover:bg-slate-700 transition-colors z-10"
              title="Copy to clipboard"
            >
              {copied ? <Check className="h-4 w-4 text-emerald-500" /> : <Copy className="h-4 w-4 text-slate-400" />}
            </button>
            <pre className="text-xs font-mono text-blue-300 bg-slate-950 p-4 rounded-lg border border-slate-800 overflow-x-auto h-full">
              {generateYaml()}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
};

export default PropertiesPanel;
