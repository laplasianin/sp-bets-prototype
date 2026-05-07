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
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4 shadow-sm flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Sporty Home — Bet Settlement Demo</h1>
          <p className="text-sm text-gray-500 mt-0.5">Spring Boot · Kafka · jOOQ · RocketMQ</p>
        </div>
        <button
          onClick={handleReset}
          disabled={resetting}
          className="text-sm px-4 py-2 rounded border border-gray-300 text-gray-600 hover:bg-gray-100 disabled:opacity-50 transition-colors"
        >
          {resetting ? 'Resetting…' : 'Reset Demo'}
        </button>
      </header>
      <main className="max-w-5xl mx-auto px-8 py-8 space-y-6">
        <section className="bg-white rounded-lg border p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Announce Winner</h2>
          <AnnounceWinnerForm onAnnounced={handleAnnounced} />
        </section>
        <section className="bg-white rounded-lg border p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Pipeline Status</h2>
          <PipelineStatus active={pipelineActive} />
        </section>
        <section className="bg-white rounded-lg border p-6 space-y-4">
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Bets</h2>
          <BetsTable pollTrigger={pollTrigger} />
        </section>
      </main>
    </div>
  );
}
