import type { Bet } from './types';

const BASE = import.meta.env.VITE_API_URL ?? '';

export async function fetchBets(): Promise<Bet[]> {
  const res = await fetch(`${BASE}/api/bets`);
  if (!res.ok) throw new Error(`GET /api/bets failed: ${res.status}`);
  return res.json() as Promise<Bet[]>;
}

export async function announceWinner(
  eventId: string,
  eventName: string,
  winnerId: string,
): Promise<void> {
  const res = await fetch(`${BASE}/api/event-outcomes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ eventId, eventName, winnerId }),
  });
  if (!res.ok) throw new Error(`POST /api/event-outcomes failed: ${res.status}`);
}
