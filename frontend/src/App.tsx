import { useState } from 'react';
import { AnnounceWinnerForm } from './components/AnnounceWinnerForm';
import { BetsTable } from './components/BetsTable';
import { PipelineStatus } from './components/PipelineStatus';
import { resetBets } from './api';

const STAGES_COUNT = 4;

export default function App() {
  const [pipelineActive, setPipelineActive] = useState(false);
  const [pollTrigger, setPollTrigger] = useState(0);
  const [resetting, setResetting] = useState(false);

  function handleAnnounced() {
    setPipelineActive(true);
    setPollTrigger(t => t + 1);
    setTimeout(() => setPipelineActive(false), STAGES_COUNT * 500 + 200);
  }

  async function handleReset() {
    setResetting(true);
    try {
      await resetBets();
      setPollTrigger(t => t + 1);
    } finally {
      setResetting(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#030712] text-white">
      <header className="bg-gray-900 border-b border-gray-800 px-8 py-4 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <img src="/sporty-logo-white.svg" alt="Sporty Group" className="h-6" />
          <span className="text-gray-700 text-lg font-thin">|</span>
          <p className="text-xs text-gray-500">Bet Settlement Demo</p>
        </div>
        <button
          onClick={handleReset}
          disabled={resetting}
          className="text-xs px-4 py-2 rounded border border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-200 disabled:opacity-40 transition-colors uppercase tracking-wide"
        >
          {resetting ? 'Resetting…' : 'Reset Demo'}
        </button>
      </header>
      <main className="max-w-5xl mx-auto px-8 py-8 space-y-4">
        <section className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-4">
          <h2 className="text-xs font-semibold text-[#E41827] uppercase tracking-widest">Announce Winner</h2>
          <AnnounceWinnerForm onAnnounced={handleAnnounced} />
        </section>
        <section className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-4">
          <h2 className="text-xs font-semibold text-[#E41827] uppercase tracking-widest">Pipeline Status</h2>
          <PipelineStatus active={pipelineActive} />
        </section>
        <section className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-4">
          <h2 className="text-xs font-semibold text-[#E41827] uppercase tracking-widest">Bets</h2>
          <BetsTable pollTrigger={pollTrigger} />
        </section>
      </main>
    </div>
  );
}
