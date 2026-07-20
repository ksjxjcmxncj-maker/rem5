import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Link } from 'wouter';
import { Map, Users, Layers } from 'lucide-react';

const MOCK_MAPS = [
  { name: 'Twin City', zones: 4, players: 58, description: 'The central hub of the realm' },
  { name: 'Desert City', zones: 3, players: 21, description: 'A scorching trade outpost' },
  { name: 'Phoenix Castle', zones: 6, players: 34, description: 'Ancient fortress of legend' },
  { name: 'Bird Island', zones: 2, players: 12, description: 'Serene training grounds' },
  { name: 'Ape Mountain', zones: 5, players: 8, description: 'Treacherous peak dungeon' },
  { name: 'Mystic Castle', zones: 7, players: 17, description: 'Haunted stronghold' },
  { name: 'Market', zones: 1, players: 45, description: 'Bustling merchant district' },
  { name: 'Prison', zones: 2, players: 3, description: 'For those who break the rules' },
];

export default function Maps() {
  return (
    <div className="min-h-screen bg-background p-6">
      <div className="max-w-6xl mx-auto">
        <div className="mb-6 flex items-center gap-4">
          <Link href="/" className="text-muted-foreground hover:text-primary text-sm transition-colors">
            ← Back to Dashboard
          </Link>
        </div>

        <div className="mb-6">
          <h1 className="text-3xl font-bold text-foreground tracking-tight">Maps</h1>
          <p className="text-muted-foreground mt-1">
            {MOCK_MAPS.length} active game zones
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {MOCK_MAPS.map((map) => (
            <Card key={map.name} className="border-border hover:border-primary/50 transition-colors">
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2">
                  <Map className="h-4 w-4 text-primary flex-shrink-0" />
                  <CardTitle className="text-base text-foreground">{map.name}</CardTitle>
                </div>
                <p className="text-xs text-muted-foreground">{map.description}</p>
              </CardHeader>
              <CardContent className="space-y-2">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Layers className="h-3.5 w-3.5 text-primary/70" />
                  <span>{map.zones} zone{map.zones !== 1 ? 's' : ''}</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Users className="h-3.5 w-3.5 text-primary/70" />
                  <span>{map.players} player{map.players !== 1 ? 's' : ''}</span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
