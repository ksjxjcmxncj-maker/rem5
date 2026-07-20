import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from '@/components/ui/toaster';
import { TooltipProvider } from '@/components/ui/tooltip';
import { setBaseUrl } from '@workspace/api-client-react';
import NotFound from '@/pages/not-found';
import Dashboard from '@/pages/dashboard';
import Players from '@/pages/players';
import Maps from '@/pages/maps';
import Logs from '@/pages/logs';
import { Route, Switch, Router as WouterRouter } from 'wouter';

setBaseUrl('/api');

const queryClient = new QueryClient();

function Router() {
  return (
    <Switch>
      <Route path="/" component={Dashboard} />
      <Route path="/players" component={Players} />
      <Route path="/maps" component={Maps} />
      <Route path="/logs" component={Logs} />
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <WouterRouter base={import.meta.env.BASE_URL.replace(/\/$/, '')}>
          <Router />
        </WouterRouter>
        <Toaster />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
