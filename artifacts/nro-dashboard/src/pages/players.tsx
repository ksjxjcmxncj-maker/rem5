import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Link } from 'wouter';

const MOCK_PLAYERS = [
  { name: 'ShadowBlade', level: 87, class: 'Ninja', map: 'Twin City', status: 'Online' },
  { name: 'FireFist', level: 130, class: 'Warrior', map: 'Desert City', status: 'Online' },
  { name: 'IceDagger', level: 45, class: 'Archer', map: 'Bird Island', status: 'Idle' },
  { name: 'StormRider', level: 110, class: 'Trojan', map: 'Phoenix Castle', status: 'Online' },
  { name: 'MoonWalker', level: 68, class: 'Taoist', map: 'Market', status: 'Offline' },
  { name: 'DarkSpirit', level: 99, class: 'Ninja', map: 'Twin City', status: 'Online' },
  { name: 'GoldenFist', level: 121, class: 'Warrior', map: 'Ape Mountain', status: 'Idle' },
  { name: 'NightHawk', level: 55, class: 'Archer', map: 'Mystic Castle', status: 'Online' },
];

const statusColor: Record<string, string> = {
  Online: 'text-green-400',
  Idle: 'text-yellow-400',
  Offline: 'text-muted-foreground',
};

export default function Players() {
  return (
    <div className="min-h-screen bg-background p-6">
      <div className="max-w-6xl mx-auto">
        <div className="mb-6 flex items-center gap-4">
          <Link href="/" className="text-muted-foreground hover:text-primary text-sm transition-colors">
            ← Back to Dashboard
          </Link>
        </div>

        <div className="mb-6">
          <h1 className="text-3xl font-bold text-foreground tracking-tight">Players</h1>
          <p className="text-muted-foreground mt-1">
            {MOCK_PLAYERS.filter((p) => p.status === 'Online').length} players currently online
          </p>
        </div>

        <div className="rounded-xl border border-border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="border-border bg-muted/50">
                <TableHead className="text-muted-foreground">Name</TableHead>
                <TableHead className="text-muted-foreground">Level</TableHead>
                <TableHead className="text-muted-foreground">Class</TableHead>
                <TableHead className="text-muted-foreground">Map</TableHead>
                <TableHead className="text-muted-foreground">Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {MOCK_PLAYERS.map((player) => (
                <TableRow key={player.name} className="border-border hover:bg-muted/30">
                  <TableCell className="font-medium text-foreground">{player.name}</TableCell>
                  <TableCell className="text-muted-foreground">{player.level}</TableCell>
                  <TableCell className="text-muted-foreground">{player.class}</TableCell>
                  <TableCell className="text-muted-foreground">{player.map}</TableCell>
                  <TableCell>
                    <span className={`flex items-center gap-1.5 text-sm font-medium ${statusColor[player.status]}`}>
                      <span
                        className={`h-2 w-2 rounded-full inline-block ${
                          player.status === 'Online'
                            ? 'bg-green-400'
                            : player.status === 'Idle'
                            ? 'bg-yellow-400'
                            : 'bg-muted-foreground'
                        }`}
                      />
                      {player.status}
                    </span>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  );
}
