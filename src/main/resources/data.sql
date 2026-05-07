-- Seed bets for demo. Two events, multiple bets per event.

INSERT INTO bets (user_id, event_id, market_id, winner_id, amount, status) VALUES
  ('user-1', 'evt-1', 'market-1h', 'team-real',    100.00, 'PENDING'),
  ('user-2', 'evt-1', 'market-1h', 'team-barca',   200.00, 'PENDING'),
  ('user-3', 'evt-1', 'market-1h', 'team-real',     50.00, 'PENDING'),
  ('user-4', 'evt-2', 'market-2h', 'team-arsenal', 150.00, 'PENDING'),
  ('user-5', 'evt-2', 'market-2h', 'team-chelsea',  75.00, 'PENDING');
