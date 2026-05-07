import { useEffect, useRef, useState } from 'react';
import { fetchBets } from '../api';
import type { Bet } from '../types';

const STATUS_CLASSES: Record<string, string> = {
  PENDING: 'bg-gray-700 text-gray-300',
  WON:     'bg-green-900/50 text-green-400',
  LOST:    'bg-red-900/50 text-red-400',
};

interface Props {
  pollTrigger: number;
}

export function BetsTable({ pollTrigger }: Props) {
  const [bets, setBets] = useState<Bet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  function stopPolling() {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }

  async function load() {
    try {
      const data = await fetchBets();
      setBets(data);
      if (data.length > 0 && data.every(b => b.status !== 'PENDING')) {
        stopPolling();
      }
    } catch {
      setError('Failed to load bets');
      stopPolling();
    }
  }

  useEffect(() => {
    load();
    intervalRef.current = setInterval(load, 2000);
    return stopPolling;
  }, [pollTrigger]);

  if (error) return <p className="text-red-400 text-sm">{error}</p>;

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="text-left border-b border-gray-800">
            {['ID', 'User', 'Event', 'Picked', 'Amount', 'Status', 'Payout'].map(h => (
              <th key={h} className="px-4 py-2 font-semibold text-gray-500 uppercase tracking-wider text-xs">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {bets.map(bet => (
            <tr key={bet.id} className="border-b border-gray-800 hover:bg-gray-800/50 transition-colors">
              <td className="px-4 py-3 text-gray-600">{bet.id}</td>
              <td className="px-4 py-3 text-gray-300">{bet.userId}</td>
              <td className="px-4 py-3 text-gray-300">{bet.eventId}</td>
              <td className="px-4 py-3 text-gray-300">{bet.marketId}</td>
              <td className="px-4 py-3 text-gray-300">${Number(bet.amount).toFixed(2)}</td>
              <td className="px-4 py-3">
                <span className={`px-2 py-0.5 rounded text-xs font-semibold ${STATUS_CLASSES[bet.status]}`}>
                  {bet.status}
                </span>
              </td>
              <td className="px-4 py-3 text-gray-300">
                {bet.payout != null ? `$${Number(bet.payout).toFixed(2)}` : '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
