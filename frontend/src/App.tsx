import { Navigate, Route, Routes } from 'react-router-dom'
import { Login } from './pages/Login'
import { Register } from './pages/Register'
import { JobsPage } from './pages/JobsPage'
import { TrackerPage } from './pages/TrackerPage'
import { ResumesPage } from './pages/ResumesPage'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { WorkspaceProvider } from './workspace/WorkspaceContext'
import { AppLayout } from './components/AppLayout'

function ProtectedLayout() {
  return (
    <ProtectedRoute>
      <WorkspaceProvider>
        <AppLayout />
      </WorkspaceProvider>
    </ProtectedRoute>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/jobs" element={<JobsPage />} />
        <Route path="/tracker" element={<TrackerPage />} />
        <Route path="/resumes" element={<ResumesPage />} />
      </Route>
      <Route path="/" element={<Navigate to="/jobs" replace />} />
      <Route path="/dashboard" element={<Navigate to="/jobs" replace />} />
      <Route path="*" element={<Navigate to="/jobs" replace />} />
    </Routes>
  )
}
