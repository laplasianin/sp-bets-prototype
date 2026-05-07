export type BetStatus = 'PENDING' | 'WON' | 'LOST';

export interface Bet {
  id: number;
  userId: string;
  eventId: string;
  marketId: string;
  winnerId: string | null;
  amount: number;
  status: BetStatus;
  payout: number | null;
  settledAt: string | null;
  createdAt: string;
}
