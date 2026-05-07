import { useEffect, useRef, useState } from 'react';
import { fetchBets } from '../api';
import type { Bet } from '../types';

const STATUS_CLASSES: Record<string, string> = {
  PENDING: 'bg-gray-100 text-gray-600',
  WON:     'bg-green-100 text-green-700',
  LOST:    'bg-red-100 text-red-700',
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

  if (error) return <p className="text-red-600 text-sm">{error}</p>;

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="bg-gray-50 text-left">
            {['ID', 'User', 'Event', 'Picked', 'Amount', 'Status', 'Payout'].map(h => (
              <th key={h} className="px-4 py-2 border-b font-semibold text-gray-600">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {bets.map(bet => (
            <tr key={bet.id} className="border-b hover:bg-gray-50 transition-colors">
              <td className="px-4 py-2 text-gray-500">{bet.id}</td>
              <td className="px-4 py-2">{bet.userId}</td>
              <td className="px-4 py-2">{bet.eventId}</td>
              <td className="px-4 py-2">{bet.marketId}</td>
              <td className="px-4 py-2">${Number(bet.amount).toFixed(2)}</td>
              <td className="px-4 py-2">
                <span className={`px-2 py-0.5 rounded text-xs font-semibold ${STATUS_CLASSES[bet.status]}`}>
                  {bet.status}
                </span>
              </td>
              <td className="px-4 py-2">
                {bet.payout != null ? `$${Number(bet.payout).toFixed(2)}` : '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
