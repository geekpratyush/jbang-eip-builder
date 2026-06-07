import React, { useState, useCallback, useRef } from 'react';
import ReactFlow, {
  addEdge,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  ReactFlowProvider,
} from 'reactflow';
import 'reactflow/dist/style.css';
import CamelNode from './CamelNode';

const nodeTypes = {
  camelNode: CamelNode,
};

const initialNodes = [];
const initialEdges = [];

let id = 0;
const getId = () => `node_${id++}`;

const FlowCanvas = ({ onNodeSelect }) => {
  const reactFlowWrapper = useRef(null);
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [reactFlowInstance, setReactFlowInstance] = useState(null);

  const onConnect = useCallback((params) => setEdges((eds) => addEdge(params, eds)), []);

  const onDragOver = useCallback((event) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event) => {
      event.preventDefault();

      const type = event.dataTransfer.getData('application/reactflow');
      const componentData = JSON.parse(event.dataTransfer.getData('application/component'));

      // check if the dropped element is valid
      if (typeof type === 'undefined' || !type) {
        return;
      }

      const position = reactFlowInstance.screenToFlowPosition({
        x: event.clientX,
        y: event.clientY,
      });

      const newNodeId = getId();
      const newNode = {
        id: newNodeId,
        type: 'camelNode',
        position,
        data: { 
          ...componentData,
          ...componentData.defaultData,
          label: componentData.label,
        },
      };

      setNodes((nds) => nds.concat(newNode));

      // Handle scaffolding for complex EIPs
      if (componentData.isComplex && componentData.scaffold) {
        const isChoice = componentData.id === 'choice';
        const scaffoldNodes = componentData.scaffold.nodes.map((n, index) => {
          const offsetX = isChoice ? (index - 1) * 220 : 0;
          const offsetY = isChoice ? 180 : (index + 1) * 120;
          return {
            id: `${newNodeId}_${n.id}`,
            type: 'camelNode',
            position: { x: position.x + offsetX, y: position.y + offsetY },
            data: { ...n.data, label: n.label, icon: n.id.includes('when') ? 'ArrowRight' : 'CornerDownRight' },
          };
        });

        const scaffoldEdges = componentData.scaffold.nodes.map((n) => ({
          id: `e_${newNodeId}_${newNodeId}_${n.id}`,
          source: newNodeId,
          target: `${newNodeId}_${n.id}`,
          animated: true,
          style: { stroke: '#3b82f6' },
        }));

        setNodes((nds) => nds.concat(scaffoldNodes));
        setEdges((eds) => eds.concat(scaffoldEdges));
      }
    },
    [reactFlowInstance, setNodes, setEdges]
  );

  return (
    <div className="flex-1 h-full relative" ref={reactFlowWrapper}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onInit={setReactFlowInstance}
        onDrop={onDrop}
        onDragOver={onDragOver}
        nodeTypes={nodeTypes}
        onNodeClick={(_, node) => onNodeSelect(node)}
        onPaneClick={() => onNodeSelect(null)}
        fitView
      >
        <Background color="#334155" gap={20} />
        <Controls className="bg-slate-800 border-slate-700 fill-slate-300" />
        <MiniMap 
          nodeStrokeColor={(n) => '#3b82f6'}
          nodeColor={(n) => '#1e293b'}
          maskColor="rgba(15, 23, 42, 0.6)"
          className="bg-slate-800 border-slate-700"
        />
      </ReactFlow>
    </div>
  );
};

export default ({ onNodeSelect }) => (
  <ReactFlowProvider>
    <FlowCanvas onNodeSelect={onNodeSelect} />
  </ReactFlowProvider>
);
