import React, { useState, useEffect, useRef } from 'react';
import {
  Globe,
  Building,
  DollarSign,
  AlertTriangle,
  CheckCircle,
  XCircle,
  FileText,
  Activity,
  ArrowRight,
  TrendingUp,
  UserCheck,
  Search,
  RotateCcw,
  Sliders,
  Play,
  Lock,
  Layers,
  Database,
  BarChart,
  ShieldAlert,
  Loader2,
  Clock,
  ExternalLink
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart as RechartsBarChart,
  Bar,
  Legend
} from 'recharts';

// ================= MOCK DATA SEED =================

const INITIAL_FACILITIES = [
  { id: 'FAC-1001', name: 'AeroCorp International', limit: 50000000, utilized: 12000000, rating: 'A+' },
  { id: 'FAC-1002', name: 'BioPharma Global', limit: 30000000, utilized: 15000000, rating: 'A-' },
  { id: 'FAC-1003', name: 'TechPrime Systems', limit: 80000000, utilized: 32000000, rating: 'AA' },
  { id: 'FAC-1004', name: 'Apex Energy Corp', limit: 20000000, utilized: 18500000, rating: 'BBB' },
  { id: 'FAC-1005', name: 'Global Retail Group', limit: 40000000, utilized: 15000000, rating: 'BBB+' }
];

const BUSINESS_SUBLIMITS_METADATA = [
  { type: 'Trade Finance', share: 0.4, utilizedShare: 0.5 },
  { type: 'Foreign Exchange (FX)', share: 0.3, utilizedShare: 0.3 },
  { type: 'Working Capital', share: 0.2, utilizedShare: 0.1 },
  { type: 'Treasury Settlement', share: 0.1, utilizedShare: 0.05 }
];

const COUNTRIES = [
  { code: 'US', name: 'United States', region: 'NAMER', riskScore: 1 },
  { code: 'CA', name: 'Canada', region: 'NAMER', riskScore: 1 },
  { code: 'GB', name: 'United Kingdom', region: 'EMEA', riskScore: 2 },
  { code: 'DE', name: 'Germany', region: 'EMEA', riskScore: 2 },
  { code: 'CH', name: 'Switzerland', region: 'EMEA', riskScore: 1 },
  { code: 'SG', name: 'Singapore', region: 'APAC', riskScore: 1 },
  { code: 'AU', name: 'Australia', region: 'APAC', riskScore: 1 },
  { code: 'JP', name: 'Japan', region: 'APAC', riskScore: 2 },
  { code: 'BR', name: 'Brazil', region: 'LATAM', riskScore: 4 },
  { code: 'MX', name: 'Mexico', region: 'LATAM', riskScore: 3 },
  { code: 'IR', name: 'Iran (Restricted)', region: 'EMEA', riskScore: 10, sanctioned: true },
  { code: 'KP', name: 'North Korea (Restricted)', region: 'APAC', riskScore: 10, sanctioned: true },
  { code: 'SY', name: 'Syria (Restricted)', region: 'EMEA', riskScore: 10, sanctioned: true }
];

const INITIAL_HISTORY = [
  { id: 'TXN-9001', facilityName: 'AeroCorp International', facilityId: 'FAC-1001', sublimitType: 'Trade Finance', amount: 1500000, finalEarmark: 1575000, origin: 'NAMER', destination: 'DE', type: 'Trade Financing', status: 'APPROVED', timestamp: '08:14 AM', rulesTriggered: ['Cross-Border Cushion (+5%)'] },
  { id: 'TXN-9002', facilityName: 'BioPharma Global', facilityId: 'FAC-1002', sublimitType: 'Foreign Exchange (FX)', amount: 4000000, finalEarmark: 4200000, origin: 'EMEA', destination: 'US', type: 'FX Spot', status: 'APPROVED', timestamp: '08:45 AM', rulesTriggered: ['Cross-Border Cushion (+5%)'] },
  { id: 'TXN-9003', facilityName: 'Apex Energy Corp', facilityId: 'FAC-1004', sublimitType: 'Working Capital', amount: 3500000, finalEarmark: 3570000, origin: 'NAMER', destination: 'BR', type: 'Loan draw', status: 'BLOCKED', timestamp: '09:02 AM', blockReason: 'Capital Control Limit exceeded for LATAM region (> $3M)', rulesTriggered: ['Capital Control Rule Triggered', 'Risk Cushion (+2%)'] },
  { id: 'TXN-9004', facilityName: 'TechPrime Systems', facilityId: 'FAC-1003', sublimitType: 'Treasury Settlement', amount: 800000, finalEarmark: 800000, origin: 'APAC', destination: 'SG', type: 'Payment', status: 'APPROVED', timestamp: '09:30 AM', rulesTriggered: [] },
  { id: 'TXN-9005', facilityName: 'Global Retail Group', facilityId: 'FAC-1005', sublimitType: 'Trade Finance', amount: 12000000, finalEarmark: 12840000, origin: 'LATAM', destination: 'IR', type: 'Trade Financing', status: 'BLOCKED', timestamp: '10:15 AM', blockReason: 'Sanctioned Country Alert: Transaction to Iran is blocked.', rulesTriggered: ['Sanctions List Match'] },
  { id: 'TXN-9006', facilityName: 'Apex Energy Corp', facilityId: 'FAC-1004', sublimitType: 'Trade Finance', amount: 2500000, finalEarmark: 2675000, origin: 'NAMER', destination: 'GB', type: 'Trade Financing', status: 'APPROVED', timestamp: '11:05 AM', rulesTriggered: ['Cross-Border Cushion (+5%)', 'Risk Cushion (+2%)'], dolApproved: true },
  { id: 'TXN-9007', facilityName: 'AeroCorp International', facilityId: 'FAC-1001', sublimitType: 'Working Capital', amount: 500000, finalEarmark: 500000, origin: 'NAMER', destination: 'US', type: 'Loan draw', status: 'APPROVED', timestamp: '11:50 AM', rulesTriggered: [] },
  { id: 'TXN-9008', facilityName: 'BioPharma Global', facilityId: 'FAC-1002', sublimitType: 'Trade Finance', amount: 6500000, finalEarmark: 6825000, origin: 'EMEA', destination: 'SG', type: 'Trade Financing', status: 'APPROVED', timestamp: '12:20 PM', rulesTriggered: ['Cross-Border Cushion (+5%)'] },
  { id: 'TXN-9009', facilityName: 'TechPrime Systems', facilityId: 'FAC-1003', sublimitType: 'Foreign Exchange (FX)', amount: 20000000, finalEarmark: 21000000, origin: 'APAC', destination: 'CH', type: 'FX Spot', status: 'APPROVED', timestamp: '01:40 PM', rulesTriggered: ['Cross-Border Cushion (+5%)'] },
  { id: 'TXN-9010', facilityName: 'Global Retail Group', facilityId: 'FAC-1005', sublimitType: 'Working Capital', amount: 9000000, finalEarmark: 9180000, origin: 'LATAM', destination: 'MX', type: 'Loan draw', status: 'APPROVED', timestamp: '03:10 PM', rulesTriggered: ['Risk Cushion (+2%)'] }
];

function App() {
  // --- State Setup ---
  const [facilities, setFacilities] = useState(INITIAL_FACILITIES);
  const [globalPool, setGlobalPool] = useState({ limit: 500000000, utilized: 110000000 });
  const [regions, setRegions] = useState({
    NAMER: { limit: 200000000, utilized: 60000000 },
    EMEA: { limit: 150000000, utilized: 41000000 },
    APAC: { limit: 100000000, utilized: 22000000 },
    LATAM: { limit: 50000000, utilized: 8000000 }
  });

  const [history, setHistory] = useState(INITIAL_HISTORY);
  const [logs, setLogs] = useState([
    { time: '08:00 AM', type: 'system', message: 'Context-Aware Credit Earmarking Engine initialized successfully.' },
    { time: '08:05 AM', type: 'system', message: 'Hierarchical credit limit tree loaded: 5 Facilities, 20 sublimit branches.' },
    { time: '08:14 AM', type: 'transaction', message: 'TXN-9001 Processed. AeroCorp Trade Finance - Earmarked $1,575,000 (included 5% FX cushion).' }
  ]);

  // --- Active Inputs ---
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFacility, setSelectedFacility] = useState(INITIAL_FACILITIES[0]);
  const [selectedSublimitType, setSelectedSublimitType] = useState('Trade Finance');
  const [originRegion, setOriginRegion] = useState('NAMER');
  const [destCountryCode, setDestCountryCode] = useState('DE');
  const [txnType, setTxnType] = useState('Trade Financing');
  const [txnAmount, setTxnAmount] = useState(2500000);

  // --- Pipeline Simulation States ---
  const [simState, setSimState] = useState('idle'); // idle, upstream, compliance, limits, dol_approval, earmarking, downstream, completed, blocked
  const [currentPayload, setCurrentPayload] = useState(null);
  const [complianceLogs, setComplianceLogs] = useState([]);
  const [limitCheckDetails, setLimitCheckDetails] = useState(null);
  const [pendingDOL, setPendingDOL] = useState(null); // Active transaction waiting for DOL approve/reject
  const [dolQueue, setDolQueue] = useState([]); // List of past/active override requests
  const [highlightedRule, setHighlightedRule] = useState(null);

  const consoleEndRef = useRef(null);

  // Auto scroll console logs
  useEffect(() => {
    if (consoleEndRef.current) {
      consoleEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [logs]);

  // Sync Dynamic Account & Sublimit options when facility changes
  const getSublimitData = (facility, type) => {
    const meta = BUSINESS_SUBLIMITS_METADATA.find(m => m.type === type);
    const limit = facility.limit * meta.share;
    const utilized = limit * meta.utilizedShare;
    return { limit, utilized, available: limit - utilized };
  };

  const addLog = (message, type = 'info') => {
    const timeString = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    setLogs(prev => [...prev, { time: timeString, type, message }]);
  };

  // --- Running the Simulation Pipeline ---
  const handleStartSimulation = () => {
    if (simState !== 'idle') return;

    const dest = COUNTRIES.find(c => c.code === destCountryCode);
    const sublimitData = getSublimitData(selectedFacility, selectedSublimitType);
    
    const txnId = 'TXN-' + Math.floor(1000 + Math.random() * 9000);
    const payload = {
      id: txnId,
      facilityId: selectedFacility.id,
      facilityName: selectedFacility.name,
      rating: selectedFacility.rating,
      sublimitType: selectedSublimitType,
      amount: Number(txnAmount),
      origin: originRegion,
      destination: dest.name,
      destCountryCode: dest.code,
      destRegion: dest.region,
      destSanctioned: dest.sanctioned || false,
      destRiskScore: dest.riskScore,
      type: txnType,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setCurrentPayload(payload);
    setSimState('upstream');
    setComplianceLogs([]);
    setLimitCheckDetails(null);
    
    addLog(`Initiating Upstream request ${txnId} for ${payload.facilityName} | Amount: $${Number(txnAmount).toLocaleString()}`, 'system');

    // Step 1: Upstream initiation delay
    setTimeout(() => {
      // Step 2: Move to Compliance checking (Rules Engine)
      setSimState('compliance');
      executeComplianceCheck(payload, sublimitData);
    }, 1200);
  };

  // --- Validation 2: Pre-Earmarking Rules Engine ---
  const executeComplianceCheck = (payload, sublimitData) => {
    addLog(`Evaluating transaction ${payload.id} against pre-earmarking jurisdictional rules...`, 'rules');
    
    const checkLogs = [];
    let isBlocked = false;
    let blockReason = '';
    let cushionPercent = 0;
    const rulesApplied = [];

    // 1. Sanctions Check
    checkLogs.push({ step: 'Sanctions Check', status: 'running', detail: `Verifying destination [${payload.destination}] against OFAC/SDN database...` });
    
    if (payload.destSanctioned) {
      isBlocked = true;
      blockReason = `Sanction Alert: Destination country ${payload.destination} matches restricted sanctions registry.`;
      checkLogs.push({ step: 'Sanctions Check', status: 'failed', detail: blockReason });
    } else {
      checkLogs.push({ step: 'Sanctions Check', status: 'passed', detail: 'Country clear. No matches found on OFAC lists.' });
    }

    // 2. Capital Controls Check
    if (!isBlocked) {
      checkLogs.push({ step: 'Capital Controls Check', status: 'running', detail: 'Verifying cross-border capital compliance limits...' });
      // Example rule: If destination is LATAM and amount > 3M, trigger block
      if (payload.destRegion === 'LATAM' && payload.amount > 3000000) {
        isBlocked = true;
        blockReason = `Capital Control Violation: Cross-border transfer to LATAM region exceeds the $3,000,000 regulatory ceiling.`;
        checkLogs.push({ step: 'Capital Controls Check', status: 'failed', detail: blockReason });
      } else {
        checkLogs.push({ step: 'Capital Controls Check', status: 'passed', detail: 'Capital limits validated.' });
      }
    }

    // 3. Cushion & Buffers Calculation
    if (!isBlocked) {
      checkLogs.push({ step: 'Cushion Calculation', status: 'running', detail: 'Calculating context-aware liquidity margins...' });
      
      // Rule 3A: Cross-border transaction cushion (+5%)
      if (payload.origin !== payload.destRegion) {
        cushionPercent += 0.05;
        rulesApplied.push('Cross-Border Cushion (+5%)');
      }

      // Rule 3B: Lower credit rating buffer (+2% for BBB and below)
      if (payload.rating.startsWith('BBB')) {
        cushionPercent += 0.02;
        rulesApplied.push('Risk Rating Cushion (+2%)');
      }

      const calculatedCushion = payload.amount * cushionPercent;
      const finalEarmark = payload.amount + calculatedCushion;
      payload.cushionPercent = cushionPercent;
      payload.cushionAmount = calculatedCushion;
      payload.finalEarmark = finalEarmark;

      checkLogs.push({ 
        step: 'Cushion Calculation', 
        status: 'passed', 
        detail: cushionPercent > 0 
          ? `Applied cushions: ${rulesApplied.join(', ')}. Base: $${payload.amount.toLocaleString()} -> Required Earmark: $${finalEarmark.toLocaleString()}`
          : 'Domestic clear rating. Earmark matches base amount.'
      });
    } else {
      payload.finalEarmark = payload.amount;
    }

    payload.rulesTriggered = rulesApplied;
    setComplianceLogs(checkLogs);

    // Timeout delay before switching state
    setTimeout(() => {
      if (isBlocked) {
        setSimState('blocked');
        payload.status = 'BLOCKED';
        payload.blockReason = blockReason;
        setHistory(prev => [payload, ...prev]);
        addLog(`Transaction ${payload.id} BLOCKED: ${blockReason}`, 'error');
      } else {
        // Step 3: Move to Limits Check
        setSimState('limits');
        executeLimitsCheck(payload, sublimitData);
      }
    }, 1800);
  };

  // --- Validation 3: Limits Verification ---
  const executeLimitsCheck = (payload, sublimitData) => {
    addLog(`Evaluating required earmark $${payload.finalEarmark.toLocaleString()} against limit hierarchies...`, 'limits');
    
    const required = payload.finalEarmark;
    const sublimitType = payload.sublimitType;

    const baseFacility = facilities.find(f => f.id === payload.facilityId);
    const facilityAvailable = baseFacility.limit - baseFacility.utilized;
    const globalAvailable = globalPool.limit - globalPool.utilized;
    
    const details = {
      sublimit: { name: `${sublimitType} Sublimit`, limit: sublimitData.limit, utilized: sublimitData.utilized, available: sublimitData.available, passed: sublimitData.available >= required },
      facility: { name: `Facility Pool (${baseFacility.id})`, limit: baseFacility.limit, utilized: baseFacility.utilized, available: facilityAvailable, passed: facilityAvailable >= required },
      global: { name: 'Institution Global Pool', limit: globalPool.limit, utilized: globalPool.utilized, available: globalAvailable, passed: globalAvailable >= required }
    };

    setLimitCheckDetails(details);

    const isSublimitFail = !details.sublimit.passed;
    const isFacilityFail = !details.facility.passed;
    const isGlobalFail = !details.global.passed;

    const hasExcess = isSublimitFail || isFacilityFail || isGlobalFail;

    setTimeout(() => {
      if (hasExcess) {
        // Determine DOL Authority required based on the excess amount
        let excessAmount = 0;
        if (isSublimitFail) excessAmount = Math.max(excessAmount, required - sublimitData.available);
        if (isFacilityFail) excessAmount = Math.max(excessAmount, required - facilityAvailable);
        if (isGlobalFail) excessAmount = Math.max(excessAmount, required - globalAvailable);

        let dolLevel = 'Tier 1: Local Credit Officer';
        if (excessAmount > 5000000) {
          dolLevel = 'Tier 3: Chief Risk Officer';
        } else if (excessAmount > 1000000) {
          dolLevel = 'Tier 2: Regional Credit Committee';
        }

        payload.dolLevel = dolLevel;
        payload.excessAmount = excessAmount;
        
        setSimState('dol_approval');
        setPendingDOL({ ...payload, excessAmount, dolLevel });
        setDolQueue(prev => [{ ...payload, excessAmount, dolLevel, status: 'PENDING' }, ...prev]);
        addLog(`Limit breach of $${excessAmount.toLocaleString()} detected! Routed to DOL: [${dolLevel}] for override consideration.`, 'warning');
      } else {
        // Proceed to Earmarking
        setSimState('earmarking');
        commitEarmark(payload);
      }
    }, 1800);
  };

  // --- Action 2: Earmark Funds ---
  const commitEarmark = (payload) => {
    addLog(`Reserving & placing dynamic hold on $${payload.finalEarmark.toLocaleString()} in client accounts...`, 'system');

    setTimeout(() => {
      // Step 4: Downstream Balance clearing
      setSimState('downstream');
      commitDownstreamClear(payload);
    }, 1500);
  };

  // --- Downstream Clearing & finalization ---
  const commitDownstreamClear = (payload) => {
    addLog(`Routing to Downstream: Regional Balance Clearing System. Settling balances...`, 'downstream');

    setTimeout(() => {
      // Update limits in memory
      const earmarkVal = payload.finalEarmark;

      // Update Facility utilization
      setFacilities(prev => prev.map(f => {
        if (f.id === payload.facilityId) {
          return { ...f, utilized: f.utilized + earmarkVal };
        }
        return f;
      }));

      // Update Global Pool
      setGlobalPool(prev => ({ ...prev, utilized: prev.utilized + earmarkVal }));

      // Update Regional Pool (using destination region)
      const destRegion = COUNTRIES.find(c => c.code === payload.destCountryCode).region;
      setRegions(prev => {
        const current = prev[destRegion] || { limit: 50000000, utilized: 0 };
        return {
          ...prev,
          [destRegion]: { ...current, utilized: current.utilized + earmarkVal }
        };
      });

      // Update history
      payload.status = 'APPROVED';
      setHistory(prev => [payload, ...prev]);
      
      setSimState('completed');
      addLog(`DOWNSTREAM SUCCESS: Transaction ${payload.id} fully settled. Earmark converted to utilization. Compliance reporting triggered.`, 'success');

      // Return simulator back to idle after a few seconds
      setTimeout(() => {
        setSimState('idle');
        setCurrentPayload(null);
        setLimitCheckDetails(null);
        setComplianceLogs([]);
      }, 3500);
    }, 1600);
  };

  // --- DOL Manual Approvals ---
  const handleDOLDecision = (approved) => {
    if (!pendingDOL) return;

    const payload = { ...pendingDOL };
    setPendingDOL(null);

    // Update state inside DOL queue list
    setDolQueue(prev => prev.map(item => {
      if (item.id === payload.id) {
        return { ...item, status: approved ? 'APPROVED' : 'REJECTED' };
      }
      return item;
    }));

    if (approved) {
      addLog(`DOL Override APPROVED by senior authority for ${payload.id}. Resuming transaction pipeline...`, 'success');
      payload.dolApproved = true;
      setSimState('earmarking');
      commitEarmark(payload);
    } else {
      addLog(`DOL Override REJECTED by senior authority for ${payload.id}. Transaction canceled.`, 'error');
      payload.status = 'BLOCKED';
      payload.blockReason = `DOL Override Rejected: Excess authority request denied for [${payload.dolLevel}].`;
      setHistory(prev => [payload, ...prev]);
      setSimState('blocked');

      setTimeout(() => {
        setSimState('idle');
        setCurrentPayload(null);
        setLimitCheckDetails(null);
        setComplianceLogs([]);
      }, 3000);
    }
  };

  // --- Reset Simulator to Idle manually ---
  const handleReset = () => {
    setSimState('idle');
    setCurrentPayload(null);
    setLimitCheckDetails(null);
    setComplianceLogs([]);
    setPendingDOL(null);
    addLog('Simulation reset manually. Standby for new requests.');
  };

  // --- Search & Filter facility lists ---
  const filteredFacilities = INITIAL_FACILITIES.filter(f => 
    f.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    f.id.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // --- Charts Computations ---
  // Chart 1: Hourly transaction volumes
  const getHourlyData = () => {
    // Generate simple cumulative data from morning
    const hours = ['08:00 AM', '09:00 AM', '10:00 AM', '11:00 AM', '12:00 PM', '01:00 PM', '02:00 PM', '03:00 PM', '04:00 PM', 'Active'];
    let cumulative = 45000000;
    
    return hours.map((hour, idx) => {
      if (hour === 'Active') {
        const activeAdd = history
          .filter(h => h.status === 'APPROVED')
          .reduce((sum, current) => sum + (current.finalEarmark || current.amount), 0);
        return { name: hour, Volume: (cumulative + activeAdd) / 1000000 };
      }
      // static increase matching seed history
      if (idx === 1) cumulative += 2300000;
      if (idx === 2) cumulative += 1500000;
      if (idx === 3) cumulative += 800000;
      if (idx === 4) cumulative += 500000;
      if (idx === 5) cumulative += 6500000;
      if (idx === 6) cumulative += 20000000;
      if (idx === 7) cumulative += 9000000;
      return { name: hour, Volume: cumulative / 1000000 };
    });
  };

  // Chart 2: Transaction Status Pie
  const getStatusPieData = () => {
    const approved = history.filter(h => h.status === 'APPROVED').length;
    const blocked = history.filter(h => h.status === 'BLOCKED').length;
    const pending = dolQueue.filter(h => h.status === 'PENDING').length;
    return [
      { name: 'Approved', value: approved, color: '#10b981' }, // emerald-500
      { name: 'Blocked', value: blocked, color: '#ef4444' }, // rose-500
      { name: 'Pending DOL', value: pending, color: '#f59e0b' } // amber-500
    ];
  };

  // Chart 3: Rules Triggered Frequency
  const getRulesTriggerData = () => {
    const counts = {
      'Sanction Matched': 0,
      'Capital Limit Trigger': 0,
      'Cross-Border Cushion': 0,
      'Rating Buffer': 0
    };

    history.forEach(h => {
      if (h.rulesTriggered) {
        h.rulesTriggered.forEach(r => {
          if (r.includes('Sanction')) counts['Sanction Matched'] += 1;
          if (r.includes('Capital')) counts['Capital Limit Trigger'] += 1;
          if (r.includes('Cross-Border')) counts['Cross-Border Cushion'] += 1;
          if (r.includes('Rating')) counts['Rating Buffer'] += 1;
        });
      }
    });

    return Object.keys(counts).map(key => ({
      name: key,
      Triggers: counts[key]
    }));
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex flex-col antialiased">
      
      {/* --- Top Navbar --- */}
      <header className="bg-white border-b border-slate-200 px-6 py-4 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4 sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-blue-600 rounded-xl text-white shadow-md shadow-blue-500/20">
            <Layers className="h-6 w-6 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded-md text-[11px] font-bold tracking-wide uppercase bg-blue-100 text-blue-800 border border-blue-200">Enterprise</span>
              <span className="text-xs text-slate-400 font-mono">v4.2.1-Prod</span>
            </div>
            <h1 className="text-xl font-extrabold text-slate-900 tracking-tight leading-none mt-1">Context-Aware Credit Earmarking Engine</h1>
          </div>
        </div>

        {/* Real-time Telemetry Health status */}
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2 bg-slate-100 px-3 py-1.5 rounded-lg border border-slate-200">
            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping" />
            <span className="text-xs font-semibold text-slate-700">Clearing Network: ONLINE</span>
          </div>
          <button 
            onClick={handleReset} 
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 hover:text-slate-800 transition-colors shadow-sm"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            Reset State
          </button>
        </div>
      </header>

      {/* --- Top Header: Regional Limit Telemetry --- */}
      <section className="bg-white border-b border-slate-200 px-6 py-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-5 shadow-sm">
        
        {/* Global Pool */}
        <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex flex-col justify-between shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Institution Global Pool</span>
            <Building className="h-4.5 w-4.5 text-blue-600" />
          </div>
          <div className="my-2">
            <div className="text-2xl font-bold text-slate-900 font-mono">
              ${(globalPool.limit / 1000000).toFixed(0)}M
            </div>
            <div className="text-xs text-slate-400 flex items-center justify-between mt-1">
              <span>Utilized: ${(globalPool.utilized / 1000000).toFixed(1)}M</span>
              <span className="font-semibold text-slate-600">
                {((globalPool.utilized / globalPool.limit) * 100).toFixed(1)}%
              </span>
            </div>
          </div>
          <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
            <div 
              className="bg-blue-600 h-2 rounded-full transition-all duration-1000" 
              style={{ width: `${(globalPool.utilized / globalPool.limit) * 100}%` }}
            />
          </div>
        </div>

        {/* Region Gauges */}
        {Object.keys(regions).map(key => {
          const region = regions[key];
          const pct = ((region.utilized / region.limit) * 100).toFixed(1);
          return (
            <div key={key} className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col justify-between shadow-xs">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">{key} Sub-Pool</span>
                <Globe className="h-4.5 w-4.5 text-slate-400" />
              </div>
              <div className="my-2">
                <div className="text-xl font-bold text-slate-900 font-mono">
                  ${(region.limit / 1000000).toFixed(0)}M
                </div>
                <div className="text-xs text-slate-400 flex items-center justify-between mt-1">
                  <span>Util: ${(region.utilized / 1000000).toFixed(1)}M</span>
                  <span className="font-semibold text-slate-600">{pct}%</span>
                </div>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                <div 
                  className="bg-emerald-500 h-1.5 rounded-full transition-all duration-1000" 
                  style={{ width: `${pct}%` }}
                />
              </div>
            </div>
          );
        })}

      </section>

      {/* --- Main Contents Area --- */}
      <main className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6 p-6">
        
        {/* === Left Panel: Upstream Simulation Controls === */}
        <section className="lg:col-span-4 bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col gap-4">
          <div className="flex items-center gap-2 pb-2 border-b border-slate-100">
            <Sliders className="h-5 w-5 text-blue-600" />
            <h2 className="text-base font-bold text-slate-900">Upstream Simulation Panel</h2>
          </div>

          {/* Search Facility Input */}
          <div className="relative">
            <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-4 w-4 text-slate-400" />
            </span>
            <input
              type="text"
              placeholder="Filter facility by client name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
            />
          </div>

          {/* Facility Selection List */}
          <div className="flex flex-col gap-2">
            <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Select Facility (Base ID)</label>
            <div className="max-h-36 overflow-y-auto border border-slate-200 rounded-xl divide-y divide-slate-100 bg-white">
              {filteredFacilities.map(f => {
                const isSelected = selectedFacility.id === f.id;
                return (
                  <button
                    key={f.id}
                    onClick={() => {
                      if (simState === 'idle') setSelectedFacility(f);
                    }}
                    disabled={simState !== 'idle'}
                    className={`w-full text-left px-3.5 py-2.5 text-xs flex items-center justify-between transition-colors ${
                      isSelected ? 'bg-blue-50 text-blue-700 font-semibold' : 'hover:bg-slate-50 text-slate-600'
                    }`}
                  >
                    <div>
                      <div className="font-semibold">{f.name}</div>
                      <div className="text-[10px] text-slate-400 mt-0.5">{f.id} • Rating: {f.rating}</div>
                    </div>
                    <div className="text-right font-mono">
                      <div>${(f.limit / 1000000).toFixed(0)}M</div>
                      <div className="text-[10px] text-slate-400">Avail: ${((f.limit - f.utilized) / 1000000).toFixed(1)}M</div>
                    </div>
                  </button>
                );
              })}
              {filteredFacilities.length === 0 && (
                <div className="p-3 text-center text-xs text-slate-400">No facilities match query</div>
              )}
            </div>
          </div>

          {/* Sublimit / Business ID Selection */}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Business Sublimit</label>
              <select
                value={selectedSublimitType}
                onChange={(e) => setSelectedSublimitType(e.target.value)}
                disabled={simState !== 'idle'}
                className="w-full text-xs py-2 px-3 border border-slate-200 rounded-xl bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
              >
                {BUSINESS_SUBLIMITS_METADATA.map(m => (
                  <option key={m.type} value={m.type}>{m.type}</option>
                ))}
              </select>
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Account ID</label>
              <select
                disabled={simState !== 'idle'}
                className="w-full text-xs py-2 px-3 border border-slate-200 rounded-xl bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
              >
                <option>{selectedFacility.id}-SETTLE-01</option>
                <option>{selectedFacility.id}-SETTLE-02</option>
              </select>
            </div>
          </div>

          {/* Jurisdictional Context */}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Origin Region</label>
              <select
                value={originRegion}
                onChange={(e) => setOriginRegion(e.target.value)}
                disabled={simState !== 'idle'}
                className="w-full text-xs py-2 px-3 border border-slate-200 rounded-xl bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
              >
                <option value="NAMER">NAMER (North America)</option>
                <option value="EMEA">EMEA (Europe/ME/Africa)</option>
                <option value="APAC">APAC (Asia-Pacific)</option>
                <option value="LATAM">LATAM (Latin America)</option>
              </select>
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Destination Country</label>
              <select
                value={destCountryCode}
                onChange={(e) => setDestCountryCode(e.target.value)}
                disabled={simState !== 'idle'}
                className="w-full text-xs py-2 px-3 border border-slate-200 rounded-xl bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white font-medium"
              >
                {COUNTRIES.map(c => (
                  <option key={c.code} value={c.code} className={c.sanctioned ? 'text-red-600 font-bold' : ''}>
                    {c.name} {c.sanctioned ? '⚠️' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Transaction Type */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-slate-400 uppercase tracking-wider">Transaction Type</label>
            <div className="grid grid-cols-2 gap-2">
              {['Trade Financing', 'FX Spot', 'Payment', 'Loan draw'].map(type => (
                <button
                  key={type}
                  type="button"
                  onClick={() => {
                    if (simState === 'idle') setTxnType(type);
                  }}
                  disabled={simState !== 'idle'}
                  className={`py-2 px-3 text-xs border rounded-xl font-medium transition-all ${
                    txnType === type 
                      ? 'bg-blue-600 border-blue-600 text-white shadow-sm' 
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border-slate-200'
                  }`}
                >
                  {type}
                </button>
              ))}
            </div>
          </div>

          {/* Slider amount */}
          <div className="flex flex-col gap-1.5 mt-2 bg-slate-50 p-4 rounded-xl border border-slate-100">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Amount (USD)</label>
              <input
                type="number"
                value={txnAmount}
                min={10000}
                max={40000000}
                step={50000}
                onChange={(e) => {
                  if (simState === 'idle') setTxnAmount(Number(e.target.value));
                }}
                disabled={simState !== 'idle'}
                className="w-32 text-right border border-slate-200 rounded-lg px-2 py-0.5 text-xs font-mono font-bold focus:outline-none focus:ring-1 focus:ring-blue-500 focus:bg-white"
              />
            </div>
            
            <input
              type="range"
              min="100000"
              max="20000000"
              step="100000"
              value={txnAmount}
              onChange={(e) => {
                if (simState === 'idle') setTxnAmount(Number(e.target.value));
              }}
              disabled={simState !== 'idle'}
              className="w-full h-1.5 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600 mt-2"
            />
            
            <div className="flex items-center justify-between text-[10px] text-slate-400 mt-1">
              <span>Min: $100K</span>
              <span>Max: $20M</span>
            </div>
          </div>

          {/* Launch Simulator Button */}
          <button
            onClick={handleStartSimulation}
            disabled={simState !== 'idle'}
            className={`w-full py-3.5 rounded-xl font-bold flex items-center justify-center gap-2 shadow-md transition-all ${
              simState === 'idle'
                ? 'bg-blue-600 hover:bg-blue-700 text-white shadow-blue-500/20 hover:shadow-blue-500/30'
                : 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200 shadow-none'
            }`}
          >
            <Play className={`h-4 w-4 ${simState === 'idle' ? 'fill-current' : ''}`} />
            Run Orchestration Pipeline
          </button>
        </section>

        {/* === Center Panel: The Earmarking Pipeline Diagram === */}
        <section className="lg:col-span-5 bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <Activity className="h-5 w-5 text-blue-600" />
                <h2 className="text-base font-bold text-slate-900">Pipeline Flow Visualizer</h2>
              </div>
              <div className="text-xs text-slate-400 font-mono">
                State: <span className="font-bold text-blue-600 uppercase">{simState}</span>
              </div>
            </div>

            {/* FLOW NODES CONTAINER */}
            <div className="relative flex flex-col gap-6 py-6 items-center">
              
              {/* Node 1: Upstream */}
              <div className={`w-full max-w-sm rounded-xl p-4 border transition-all duration-300 relative ${
                simState === 'upstream'
                  ? 'bg-blue-50 border-blue-500 shadow-md ring-2 ring-blue-500/20 scale-102'
                  : simState !== 'idle' ? 'bg-slate-50 border-slate-200 opacity-60' : 'bg-slate-50 border-slate-200'
              }`}>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Upstream System</span>
                  <ExternalLink className="h-4 w-4 text-slate-400" />
                </div>
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-blue-100 rounded-lg text-blue-700">
                    <Database className="h-4 w-4" />
                  </div>
                  <div>
                    <h3 className="text-xs font-semibold text-slate-800">Core Settlement Gateway</h3>
                    {currentPayload && (
                      <p className="text-[10px] font-mono text-slate-500 mt-0.5">Payload: {currentPayload.id} (${(currentPayload.amount / 1000000).toFixed(2)}M)</p>
                    )}
                  </div>
                </div>
              </div>

              {/* Connecting line 1 */}
              <div className="h-4 w-0.5 bg-slate-200 relative">
                {simState === 'compliance' && (
                  <div className="absolute top-0 left-0 w-0.5 bg-blue-500 h-full animate-bounce" />
                )}
              </div>

              {/* Node 2: Rules Engine */}
              <div className={`w-full max-w-sm rounded-xl p-4 border transition-all duration-300 relative ${
                simState === 'compliance'
                  ? 'bg-blue-50 border-blue-500 shadow-md ring-2 ring-blue-500/20 scale-102'
                  : simState === 'blocked' ? 'bg-red-50 border-red-300 shadow-md shadow-red-100'
                  : ['limits', 'dol_approval', 'earmarking', 'downstream', 'completed'].includes(simState)
                    ? 'bg-emerald-50/50 border-emerald-200 opacity-90'
                    : 'bg-slate-50 border-slate-200'
              }`}>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Jurisdictional Rules Engine</span>
                  {simState === 'compliance' ? (
                    <Loader2 className="h-4.5 w-4.5 text-blue-600 animate-spin" />
                  ) : simState === 'blocked' && currentPayload?.blockReason?.includes('Sanction') ? (
                    <ShieldAlert className="h-4.5 w-4.5 text-red-600 animate-bounce" />
                  ) : ['limits', 'dol_approval', 'earmarking', 'downstream', 'completed'].includes(simState) ? (
                    <CheckCircle className="h-4.5 w-4.5 text-emerald-500" />
                  ) : (
                    <Lock className="h-4 w-4 text-slate-400" />
                  )}
                </div>

                <div className="flex items-start gap-3">
                  <div className={`p-2 rounded-lg ${
                    simState === 'blocked' ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-700'
                  }`}>
                    <Sliders className="h-4 w-4" />
                  </div>
                  <div className="flex-1">
                    <h3 className="text-xs font-semibold text-slate-800">Pre-Earmark Compliance</h3>
                    
                    {/* Microchecklist */}
                    <div className="mt-2.5 flex flex-col gap-1">
                      {complianceLogs.map((cl, i) => (
                        <div key={i} className="flex items-center justify-between text-[10px] py-0.5 border-b border-dashed border-slate-100 last:border-b-0">
                          <span className="font-medium text-slate-600">{cl.step}</span>
                          <span className={`font-mono text-[9px] font-bold ${
                            cl.status === 'passed' ? 'text-emerald-600' : cl.status === 'failed' ? 'text-red-600' : 'text-blue-600 animate-pulse'
                          }`}>
                            {cl.status.toUpperCase()}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

              {/* Connecting line 2 */}
              <div className="h-4 w-0.5 bg-slate-200 relative">
                {simState === 'limits' && (
                  <div className="absolute top-0 left-0 w-0.5 bg-blue-500 h-full animate-bounce" />
                )}
              </div>

              {/* Node 3: Limit Verification */}
              <div className={`w-full max-w-sm rounded-xl p-4 border transition-all duration-300 relative ${
                simState === 'limits'
                  ? 'bg-blue-50 border-blue-500 shadow-md ring-2 ring-blue-500/20 scale-102'
                  : simState === 'dol_approval' ? 'bg-amber-50 border-amber-300 shadow-md ring-2 ring-amber-400/20 animate-pulse'
                  : ['earmarking', 'downstream', 'completed'].includes(simState)
                    ? 'bg-emerald-50/50 border-emerald-200 opacity-90'
                    : 'bg-slate-50 border-slate-200'
              }`}>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Credit Limit Check</span>
                  {simState === 'limits' ? (
                    <Loader2 className="h-4.5 w-4.5 text-blue-600 animate-spin" />
                  ) : simState === 'dol_approval' ? (
                    <AlertTriangle className="h-4.5 w-4.5 text-amber-500" />
                  ) : ['earmarking', 'downstream', 'completed'].includes(simState) ? (
                    <CheckCircle className="h-4.5 w-4.5 text-emerald-500" />
                  ) : (
                    <Layers className="h-4 w-4 text-slate-400" />
                  )}
                </div>

                <div className="flex items-start gap-3">
                  <div className="p-2 bg-slate-100 rounded-lg text-slate-700">
                    <TrendingUp className="h-4 w-4" />
                  </div>
                  <div className="flex-1">
                    <h3 className="text-xs font-semibold text-slate-800">Hierarchical limit verification</h3>
                    
                    {/* Limit Details checklist */}
                    {limitCheckDetails && (
                      <div className="mt-2.5 flex flex-col gap-1.5">
                        {Object.keys(limitCheckDetails).map(key => {
                          const det = limitCheckDetails[key];
                          return (
                            <div key={key} className="text-[10px]">
                              <div className="flex items-center justify-between font-semibold text-slate-700">
                                <span>{det.name}</span>
                                <span className={det.passed ? 'text-emerald-600' : 'text-red-500'}>
                                  {det.passed ? 'PASSED' : 'EXCESS'}
                                </span>
                              </div>
                              <div className="w-full bg-slate-100 rounded-full h-1 mt-0.5">
                                <div 
                                  className={`h-1 rounded-full ${det.passed ? 'bg-emerald-400' : 'bg-red-500'}`}
                                  style={{ width: `${Math.min(100, (det.utilized / det.limit) * 100)}%` }}
                                />
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Connecting line 3 */}
              <div className="h-4 w-0.5 bg-slate-200 relative">
                {['earmarking', 'downstream'].includes(simState) && (
                  <div className="absolute top-0 left-0 w-0.5 bg-blue-500 h-full animate-bounce" />
                )}
              </div>

              {/* Node 4: Downstream Settlement */}
              <div className={`w-full max-w-sm rounded-xl p-4 border transition-all duration-300 relative ${
                simState === 'downstream' || simState === 'completed'
                  ? 'bg-blue-50 border-blue-500 shadow-md ring-2 ring-blue-500/20 scale-102 font-medium'
                  : 'bg-slate-50 border-slate-200'
              }`}>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Downstream Clearing</span>
                  {simState === 'downstream' ? (
                    <Loader2 className="h-4.5 w-4.5 text-blue-600 animate-spin" />
                  ) : simState === 'completed' ? (
                    <CheckCircle className="h-4.5 w-4.5 text-emerald-500" />
                  ) : (
                    <UserCheck className="h-4 w-4 text-slate-400" />
                  )}
                </div>

                <div className="flex items-center gap-3">
                  <div className="p-2 bg-slate-100 rounded-lg text-slate-700">
                    <Globe className="h-4 w-4" />
                  </div>
                  <div>
                    <h3 className="text-xs font-semibold text-slate-800">Regional Balance Clearing System</h3>
                    <p className="text-[10px] text-slate-500 mt-0.5">Clearing node, FX reconciliation, ledger update.</p>
                  </div>
                </div>
              </div>

            </div>
          </div>

          {/* Active Flow Payload Card */}
          <div className="bg-slate-50 border border-slate-200 p-4 rounded-xl">
            <h3 className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <FileText className="h-3.5 w-3.5" />
              Active Transaction Pipeline State
            </h3>
            {currentPayload ? (
              <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                <div>
                  <span className="text-slate-400">Transaction ID:</span> {currentPayload.id}
                </div>
                <div>
                  <span className="text-slate-400">Final Earmark:</span> <span className="font-bold text-blue-600">${(currentPayload.finalEarmark || currentPayload.amount).toLocaleString()}</span>
                </div>
                <div>
                  <span className="text-slate-400">Target Region:</span> {currentPayload.destRegion}
                </div>
                <div>
                  <span className="text-slate-400">Status:</span> 
                  <span className={`ml-1 font-bold ${
                    simState === 'completed' ? 'text-emerald-600' : simState === 'blocked' ? 'text-red-500' : 'text-blue-600'
                  }`}>{simState === 'completed' ? 'APPROVED' : simState === 'blocked' ? 'BLOCKED' : 'PROCESSING'}</span>
                </div>
              </div>
            ) : (
              <p className="text-xs text-slate-400 italic">No transaction running. Adjust controls on the left and run pipeline.</p>
            )}
          </div>
        </section>

        {/* === Right Panel: Event Log & DOL Actions === */}
        <section className="lg:col-span-3 flex flex-col gap-6">
          
          {/* DOL Authorization Panel */}
          <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col gap-3">
            <div className="flex items-center gap-2 pb-2 border-b border-slate-100">
              <UserCheck className="h-5 w-5 text-amber-500 animate-pulse" />
              <h2 className="text-base font-bold text-slate-900">DOL Override Actions</h2>
            </div>
            
            {pendingDOL ? (
              <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 flex flex-col gap-3">
                <div>
                  <div className="flex items-center justify-between text-xs text-amber-800 font-bold">
                    <span>LIMIT EXCESS DETECTED</span>
                    <span className="bg-amber-200 px-1.5 py-0.5 rounded text-[9px]">{pendingDOL.id}</span>
                  </div>
                  <p className="text-[11px] text-amber-700 mt-1">
                    Client: <span className="font-semibold">{pendingDOL.facilityName}</span>
                  </p>
                  <p className="text-[11px] text-amber-700 mt-0.5">
                    Excess Amount: <span className="font-bold font-mono text-xs">${pendingDOL.excessAmount.toLocaleString()}</span>
                  </p>
                  <p className="text-[10px] text-slate-500 font-mono mt-1 flex items-center gap-1">
                    <Clock className="h-3 w-3" />
                    Auth Path: {pendingDOL.dolLevel}
                  </p>
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={() => handleDOLDecision(true)}
                    className="flex-1 bg-amber-500 hover:bg-amber-600 text-white font-bold text-xs py-2 px-3 rounded-lg shadow-sm hover:shadow transition-all"
                  >
                    Approve Override
                  </button>
                  <button
                    onClick={() => handleDOLDecision(false)}
                    className="flex-1 bg-white hover:bg-slate-100 border border-amber-300 text-slate-700 font-bold text-xs py-2 px-3 rounded-lg transition-all"
                  >
                    Decline Earmark
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-4 text-center border border-dashed border-slate-200 rounded-xl text-xs text-slate-400 italic">
                No override requests currently pending.
              </div>
            )}
          </div>

          {/* Scrolling Event Terminal */}
          <div className="bg-slate-900 text-slate-200 rounded-2xl p-4 shadow-lg flex-1 flex flex-col min-h-64 justify-between">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-1.5">
                <div className="w-2.5 h-2.5 rounded-full bg-red-500" />
                <div className="w-2.5 h-2.5 rounded-full bg-yellow-500" />
                <div className="w-2.5 h-2.5 rounded-full bg-green-500" />
                <span className="text-xs font-bold font-mono text-slate-400 ml-2">sys_orchestrator_log</span>
              </div>
              <Terminal className="h-4.5 w-4.5 text-slate-500" />
            </div>

            {/* Scroll Container */}
            <div className="flex-1 overflow-y-auto max-h-[30rem] py-3 flex flex-col gap-2 font-mono text-[10px] leading-relaxed">
              {logs.map((log, index) => {
                let colorClass = 'text-slate-300';
                if (log.type === 'error') colorClass = 'text-red-400 font-semibold';
                if (log.type === 'success') colorClass = 'text-emerald-400 font-semibold';
                if (log.type === 'warning') colorClass = 'text-amber-400 font-semibold';
                if (log.type === 'rules') colorClass = 'text-purple-300';
                if (log.type === 'limits') colorClass = 'text-blue-300';
                return (
                  <div key={index} className={`border-b border-slate-800/35 pb-1.5 ${colorClass}`}>
                    <span className="text-slate-500">[{log.time}]</span> {log.message}
                  </div>
                );
              })}
              <div ref={consoleEndRef} />
            </div>
          </div>
        </section>

      </main>

      {/* --- Bottom Section: Graphs & Charts (Metrics since Morning) --- */}
      <section className="bg-white border-t border-slate-200 p-6 grid grid-cols-1 md:grid-cols-12 gap-6 shadow-inner">
        
        {/* Cumulative Volume Chart */}
        <div className="md:col-span-5 bg-slate-50 border border-slate-200 rounded-xl p-4 shadow-xs">
          <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-1.5">
            <TrendingUp className="h-4 w-4 text-blue-500" />
            Cumulative Settled Volume (USD Millions)
          </h3>
          <div className="h-48">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={getHourlyData()} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2563eb" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#2563eb" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="name" stroke="#94a3b8" fontSize={9} />
                <YAxis stroke="#94a3b8" fontSize={9} />
                <Tooltip formatter={(value) => [`$${value.toFixed(1)}M`, 'Volume']} />
                <Area type="monotone" dataKey="Volume" stroke="#2563eb" strokeWidth={2} fillOpacity={1} fill="url(#colorVolume)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Transaction Outcome Pie */}
        <div className="md:col-span-3 bg-slate-50 border border-slate-200 rounded-xl p-4 shadow-xs flex flex-col justify-between">
          <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
            <Activity className="h-4 w-4 text-emerald-500" />
            Outcome Distribution
          </h3>
          
          <div className="h-40 flex items-center justify-center relative">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={getStatusPieData()}
                  cx="50%"
                  cy="50%"
                  innerRadius={45}
                  outerRadius={60}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {getStatusPieData().map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
            
            {/* Center Summary Label */}
            <div className="absolute flex flex-col items-center justify-center">
              <span className="text-xl font-bold text-slate-800">{history.length}</span>
              <span className="text-[9px] uppercase font-bold text-slate-400">Total Txns</span>
            </div>
          </div>

          <div className="flex justify-center gap-4 text-[10px] font-semibold text-slate-600 mt-2">
            {getStatusPieData().map(entry => (
              <div key={entry.name} className="flex items-center gap-1">
                <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: entry.color }} />
                <span>{entry.name}: {entry.value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Rules Triggered Bar Chart */}
        <div className="md:col-span-4 bg-slate-50 border border-slate-200 rounded-xl p-4 shadow-xs">
          <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-1.5">
            <Sliders className="h-4 w-4 text-purple-500" />
            Rules Engine Triggers (Hits)
          </h3>
          <div className="h-48">
            <ResponsiveContainer width="100%" height="100%">
              <RechartsBarChart data={getRulesTriggerData()} margin={{ top: 10, right: 10, left: -30, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="name" stroke="#94a3b8" fontSize={9} />
                <YAxis stroke="#94a3b8" fontSize={9} />
                <Tooltip />
                <Bar dataKey="Triggers" fill="#a855f7" radius={[4, 4, 0, 0]}>
                  {getRulesTriggerData().map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={index % 2 === 0 ? '#8b5cf6' : '#a855f7'} />
                  ))}
                </Bar>
              </RechartsBarChart>
            </ResponsiveContainer>
          </div>
        </div>

      </section>

      {/* --- Footer / Architecture Summary --- */}
      <footer className="bg-slate-900 border-t border-slate-800 text-slate-400 px-6 py-6 text-center text-xs">
        <div className="max-w-4xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Database className="h-4 w-4 text-blue-400" />
            <span className="font-semibold text-slate-300">Downstream Node ID: balance-clearing-namer-prod-02</span>
          </div>
          <p className="text-[11px] leading-relaxed max-w-xl text-left md:text-right">
            This dashboard simulates the end-to-end **Context-Aware Credit Earmarking Pipeline**. 
            It intercepts transactions, runs regional sanction and control checks, applies rating-based liquidity buffers, 
            verifies limits hierarchically, and clears settlement to downstream balance clearing nodes.
          </p>
        </div>
      </footer>

    </div>
  );
}

export default App;
