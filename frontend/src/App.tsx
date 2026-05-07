import { useState } from 'react';
import { AnnounceWinnerForm } from './components/AnnounceWinnerForm';
import { BetsTable } from './components/BetsTable';
import { PipelineStatus } from './components/PipelineStatus';

const STAGES_COUNT = 4;

export default function App() {
  const [pipelineActive, setPipelineActive] = useState(false);
  const [pollTrigger, setPollTrigger] = useState(0);

  function handleAnnounced() {
    setPipelineActive(true);
    setPollTrigger(t => t + 1);
    setTimeout(() => setPipelineActive(false), STAGES_COUNT * 500 + 200);
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b px-8 py-4 shadow-sm">
        <h1 className="text-xl font-bold text-gray-900">Sporty Home — Bet Settlement Demo</h1>
        <p className="text-sm text-gray-500 mt-0.5">Spring Boot · Kafka · jOOQ · RocketMQ</p>
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
