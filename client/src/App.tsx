import { Compass, FileText, Heart, KeyRound, Mail, ShieldCheck } from 'lucide-react'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { RootLayout } from './layouts/RootLayout'
import { LandingPage } from './pages/LandingPage'
import { PlaceholderPage } from './pages/PlaceholderPage'

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { index: true, element: <LandingPage /> },
      {
        path: 'join',
        element: (
          <PlaceholderPage
            icon={Heart}
            badge="Sign-up opens soon"
            title="Your story starts here — very soon."
            description="We’re still building accounts for KehDoNa. Sign-up isn’t available yet, but this is where you’ll create your profile when it opens."
          />
        ),
      },
      {
        path: 'login',
        element: (
          <PlaceholderPage
            icon={KeyRound}
            badge="Log-in opens soon"
            title="Welcome back, almost."
            description="Logging in isn’t available yet. Once accounts launch, you’ll be able to sign in right here."
          />
        ),
      },
      {
        path: 'privacy',
        element: (
          <PlaceholderPage
            icon={ShieldCheck}
            badge="Being written"
            title="Privacy"
            description="Our privacy policy will be published here before KehDoNa collects any personal information."
          />
        ),
      },
      {
        path: 'guidelines',
        element: (
          <PlaceholderPage
            icon={FileText}
            badge="Being written"
            title="Community Guidelines"
            description="Guidelines for keeping KehDoNa kind, safe, and respectful will be shared here before launch."
          />
        ),
      },
      {
        path: 'contact',
        element: (
          <PlaceholderPage
            icon={Mail}
            badge="Coming soon"
            title="Contact"
            description="Contact details will be added here closer to launch."
          />
        ),
      },
      {
        path: '*',
        element: (
          <PlaceholderPage
            icon={Compass}
            badge="404"
            title="This page wandered off."
            description="We couldn’t find what you were looking for. Let’s get you back somewhere familiar."
          />
        ),
      },
    ],
  },
])

export function App() {
  return <RouterProvider router={router} />
}
