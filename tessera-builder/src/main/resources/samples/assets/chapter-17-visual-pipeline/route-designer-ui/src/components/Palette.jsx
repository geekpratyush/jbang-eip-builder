import React, { useState } from 'react';
import * as LucideIcons from 'lucide-react';
import { Search, ChevronDown, ChevronRight } from 'lucide-react';
import { CAMEL_COMPONENTS } from '../data/camelComponents';

const Palette = () => {
  const [search, setSearch] = useState('');
  const [expandedCategories, setExpandedCategories] = useState(
    CAMEL_COMPONENTS.map(c => c.category)
  );

  const onDragStart = (event, nodeType, component) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.setData('application/component', JSON.stringify(component));
    event.dataTransfer.effectAllowed = 'move';
  };

  const toggleCategory = (category) => {
    setExpandedCategories(prev => 
      prev.includes(category) 
        ? prev.filter(c => c !== category) 
        : [...prev, category]
    );
  };

  const filteredComponents = CAMEL_COMPONENTS.map(cat => ({
    ...cat,
    items: cat.items.filter(item => 
      item.label.toLowerCase().includes(search.toLowerCase()) ||
      item.description.toLowerCase().includes(search.toLowerCase())
    )
  })).filter(cat => cat.items.length > 0);

  return (
    <div className="flex flex-col h-full bg-slate-900 border-r border-slate-700 w-64 text-slate-300">
      <div className="p-4 border-b border-slate-700">
        <h2 className="text-sm font-bold uppercase tracking-wider mb-4 text-blue-400">Component Palette</h2>
        <div className="relative">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder="Search components..."
            className="w-full bg-slate-800 border border-slate-700 rounded-md py-2 pl-8 pr-4 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 transition-all"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-2">
        {filteredComponents.map((category) => (
          <div key={category.category} className="mb-2">
            <button
              onClick={() => toggleCategory(category.category)}
              className="flex items-center w-full p-2 text-xs font-bold uppercase text-slate-500 hover:text-slate-300 transition-colors"
            >
              {expandedCategories.includes(category.category) ? (
                <ChevronDown className="h-3 w-3 mr-1" />
              ) : (
                <ChevronRight className="h-3 w-3 mr-1" />
              )}
              {category.category}
            </button>

            {expandedCategories.includes(category.category) && (
              <div className="grid grid-cols-1 gap-1 mt-1 px-1">
                {category.items.map((item) => {
                  const Icon = LucideIcons[item.icon] || LucideIcons.Box;
                  return (
                    <div
                      key={item.id}
                      className="flex items-center gap-3 p-2 bg-slate-800/50 border border-slate-700 rounded-lg cursor-grab hover:bg-slate-700 hover:border-blue-500/50 transition-all group"
                      onDragStart={(event) => onDragStart(event, item.id, item)}
                      draggable
                    >
                      <div className="p-1.5 bg-slate-700 rounded group-hover:bg-blue-900/50 group-hover:text-blue-400 transition-colors">
                        <Icon className="h-4 w-4" />
                      </div>
                      <div className="flex flex-col overflow-hidden">
                        <span className="text-sm font-medium truncate">{item.label}</span>
                        <span className="text-[10px] text-slate-500 truncate">{item.description}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default Palette;
