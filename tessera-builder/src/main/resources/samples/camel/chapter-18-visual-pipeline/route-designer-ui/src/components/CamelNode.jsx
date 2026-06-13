import React, { memo } from 'react';
import { Handle, Position } from 'reactflow';
import * as LucideIcons from 'lucide-react';

const CamelNode = ({ data, selected }) => {
  const Icon = LucideIcons[data.icon] || LucideIcons.Box;

  return (
    <div className={`px-4 py-3 rounded-lg border-2 transition-all min-w-[150px] shadow-lg ${
      selected 
        ? 'border-blue-500 bg-blue-900/40 ring-4 ring-blue-500/20' 
        : 'border-slate-700 bg-slate-800 hover:border-slate-500'
    }`}>
      {data.type !== 'source' && (
        <Handle type="target" position={Position.Top} className="w-3 h-3 bg-blue-500 border-2 border-slate-900" />
      )}
      
      <div className="flex items-center gap-3">
        <div className={`p-2 rounded-md ${selected ? 'bg-blue-500 text-white' : 'bg-slate-700 text-slate-300'}`}>
          <Icon className="h-5 w-5" />
        </div>
        <div className="flex flex-col">
          <span className={`text-sm font-bold ${selected ? 'text-white' : 'text-slate-200'}`}>{data.label}</span>
          <span className="text-[10px] text-slate-500 font-mono truncate max-w-[120px]">
            {data.uri || (data.expression ? 'expression' : 'processor')}
          </span>
        </div>
      </div>

      <Handle type="source" position={Position.Bottom} className="w-3 h-3 bg-blue-500 border-2 border-slate-900" />
    </div>
  );
};

export default memo(CamelNode);
