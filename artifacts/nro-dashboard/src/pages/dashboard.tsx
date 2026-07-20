import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Users, Clock, Map, BookUser } from 'lucide-react';

const MOCK_STATS = {
  playersOnline: 142,
  serverUptime: '14d 7h 22m',
  mapsActive: 18,
  totalAccounts: 9347,
};

const statCards = [
  {
    title: 'Players Online',
    value: MOCK_STATS.playersOnline,
    icon: Users,
    description: 'Currently connected',
  },
  {
    title: 'Server Uptime',
    value: MOCK_STATS.serverUptime,
    icon: Clock,
    description: 'Since last restart',
  },
  {
    title: 'Maps Active',
    value: MOCK_STATS.mapsActive,
    icon: Map,
    description: 'Zones loaded',
  },
  {
    title: 'Total Accounts',
    value: MOCK_STATS.totalAccounts.toLocaleString(),
    icon: BookUser,
    description: 'Registered accounts',
  },
];

export default function Dashboard() {
  return (
    <div className="min-h-screen bg-background p-6">
      <div className="max-w-6xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-foreground tracking-tight">
            NRO Dashboard
          </h1>
          <p className="text-muted-foreground mt-1">
            Ninja Realm Online — Server Overview
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {statCards.map(({ title, value, icon: Icon, description }) => (
            <Card key={title} className="border-border">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {title}
                </CardTitle>
                <Icon className="h-4 w-4 text-primary" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">{value}</div>
                <p className="text-xs text-muted-foreground mt-1">{description}</p>
              </CardContent>
            </Card>
          ))}
        </div>

        <div className="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Card className="border-border">
            <CardHeader>
              <CardTitle className="text-foreground">Quick Navigation</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <a
                href="/players"
                className="block p-3 rounded-lg bg-secondary hover:bg-accent hover:text-accent-foreground transition-colors text-secondary-foreground"
              >
                → Manage Players
              </a>
              <a
                href="/maps"
                className="block p-3 rounded-lg bg-secondary hover:bg-accent hover:text-accent-foreground transition-colors text-secondary-foreground"
              >
                → Browse Maps
              </a>
              <a
                href="/logs"
                className="block p-3 rounded-lg bg-secondary hover:bg-accent hover:text-accent-foreground transition-colors text-secondary-foreground"
              >
                → View Logs
              </a>
            </CardContent>
          </Card>

          <Card className="border-border">
            <CardHeader>
              <CardTitle className="text-foreground">Server Status</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {[
                { label: 'Game Server', status: 'Online' },
                { label: 'Database', status: 'Online' },
                { label: 'Auth Service', status: 'Online' },
                { label: 'Chat Server', status: 'Online' },
              ].map(({ label, status }) => (
                <div key={label} className="flex items-center justify-between">
                  <span className="text-sm text-muted-foreground">{label}</span>
                  <span className="flex items-center gap-1.5 text-sm text-green-400 font-medium">
                    <span className="h-2 w-2 rounded-full bg-green-400 inline-block" />
                    {status}
                  </span>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
