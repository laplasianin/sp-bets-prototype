import { useState } from 'react';
import { announceWinner } from '../api';

const EVENTS = [
  { id: 'evt-1', name: 'Real Madrid vs Barcelona' },
  { id: 'evt-2', name: 'Arsenal vs Chelsea' },
];

const WINNERS: Record<string, { id: string; label: string }[]> = {
  'evt-1': [
    { id: 'team-real',  label: 'Real Madrid' },
    { id: 'team-barca', label: 'Barcelona' },
  ],
  'evt-2': [
    { id: 'team-arsenal', label: 'Arsenal' },
    { id: 'team-chelsea', label: 'Chelsea' },
  ],
};

interface Props {
  onAnnounced: () => void;
}

export function AnnounceWinnerForm({ onAnnounced }: Props) {
  const [eventId, setEventId]   = useState('evt-1');
  const [winnerId, setWinnerId] = useState('team-real');
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState<string | null>(null);

  const event   = EVENTS.find(e => e.id === eventId)!;
  const winners = WINNERS[eventId];

  function handleEventChange(newEventId: string) {
    setEventId(newEventId);
    setWinnerId(WINNERS[newEventId][0].id);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await announceWinner(eventId, event.name, winnerId);
      onAnnounced();
    } catch {
      setError('Failed to announce winner. Is the backend running?');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4">
      <div>
        <label className="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5">Event</label>
        <select
          value={eventId}
          onChange={e => handleEventChange(e.target.value)}
          className="bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          {EVENTS.map(ev => (
            <option key={ev.id} value={ev.id}>{ev.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5">Winner</label>
        <select
          value={winnerId}
          onChange={e => setWinnerId(e.target.value)}
          className="bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          {winners.map(w => (
            <option key={w.id} value={w.id}>{w.label}</option>
          ))}
        </select>
      </div>
      <button
        type="submit"
        disabled={loading}
        className="bg-blue-600 text-white px-6 py-2 rounded font-semibold text-sm hover:bg-blue-700 disabled:opacity-50 transition-colors"
      >
        {loading ? 'Sending…' : 'GO'}
      </button>
      {error && <span className="text-red-400 text-sm">{error}</span>}
    </form>
  );
}
