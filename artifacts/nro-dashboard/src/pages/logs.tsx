import { Badge } from '@/components/ui/badge';
import { Link } from 'wouter';

type LogLevel = 'INFO' | 'WARN' | 'ERROR';

const MOCK_LOGS: { timestamp: string; level: LogLevel; message: string }[] = [
  { timestamp: '2024-01-15 14:32:01', level: 'INFO', message: 'Server started successfully on port 5816' },
  { timestamp: '2024-01-15 14:32:05', level: 'INFO', message: 'Database connection established' },
  { timestamp: '2024-01-15 14:33:12', level: 'INFO', message: 'Player ShadowBlade logged in from 192.168.1.42' },
  { timestamp: '2024-01-15 14:34:00', level: 'WARN', message: 'High memory usage detected: 78% of allocated heap' },
  { timestamp: '2024-01-15 14:34:18', level: 'INFO', message: 'Player FireFist logged in from 10.0.0.7' },
  { timestamp: '2024-01-15 14:35:22', level: 'ERROR', message: 'Failed to load map chunk for sector 7-C: timeout' },
  { timestamp: '2024-01-15 14:35:23', level: 'INFO', message: 'Map chunk retry successful for sector 7-C' },
  { timestamp: '2024-01-15 14:36:00', level: 'INFO', message: 'Auto-save completed: 142 player records written' },
  { timestamp: '2024-01-15 14:37:14', level: 'WARN', message: 'Player MoonWalker connection timed out, disconnecting' },
  { timestamp: '2024-01-15 14:37:15', level: 'INFO', message: 'Player MoonWalker session ended and saved' },
  { timestamp: '2024-01-15 14:38:44', level: 'ERROR', message: 'Auth service returned 503 — retrying in 5s' },
  { timestamp: '2024-01-15 14:38:49', level: 'INFO', message: 'Auth service reconnected successfully' },
  { timestamp: '2024-01-15 14:40:00', level: 'INFO', message: 'Scheduled maintenance reminder broadcast sent' },
  { timestamp: '2024-01-15 14:41:03', level: 'WARN', message: 'Unusual login attempt for account "GoldenFist" — IP flagged' },
  { timestamp: '2024-01-15 14:42:30', level: 'INFO', message: 'Anti-cheat scan completed: 0 violations found' },
];

const levelVariant: Record<LogLevel, 'default' | 'secondary' | 'destructive'> = {
  INFO: 'secondary',
  WARN: 'default',
  ERROR: 'destructive',
};

const levelTextColor: Record<LogLevel, string> = {
  INFO: 'text-green-400',
  WARN: 'text-yellow-400',
  ERROR: 'text-red-400',
};

export default function Logs() {
  return (
    <div className="min-h-screen bg-background p-6">
      <div className="max-w-6xl mx-auto">
        <div className="mb-6 flex items-center gap-4">
          <Link href="/" className="text-muted-foreground hover:text-primary text-sm transition-colors">
            ← Back to Dashboard
          </Link>
        </div>

        <div className="mb-6">
          <h1 className="text-3xl font-bold text-foreground tracking-tight">Logs</h1>
          <p className="text-muted-foreground mt-1">
            {MOCK_LOGS.length} recent log entries
          </p>
        </div>

        <div className="rounded-xl border border-border bg-card overflow-hidden">
          <div className="p-1 font-mono text-sm">
            {MOCK_LOGS.map((log, i) => (
              <div
                key={i}
                className="flex items-start gap-3 px-4 py-2 hover:bg-muted/30 transition-colors border-b border-border last:border-b-0"
              >
                <span className="text-muted-foreground shrink-0 text-xs pt-0.5">
                  {log.timestamp}
                </span>
                <Badge
                  variant={levelVariant[log.level]}
                  className={`shrink-0 text-xs font-bold w-12 justify-center ${
                    log.level === 'WARN'
                      ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30 hover:bg-yellow-500/20'
                      : log.level === 'INFO'
                      ? 'bg-green-500/20 text-green-400 border-green-500/30 hover:bg-green-500/20'
                      : ''
                  }`}
                >
                  {log.level}
                </Badge>
                <span className={`${levelTextColor[log.level]} flex-1`}>
                  {log.message}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
