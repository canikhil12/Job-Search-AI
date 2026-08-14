import { Outlet } from 'react-router-dom'
import { Navbar } from './Navbar'
import { JobDetailDrawer } from '../jobs/JobDetailDrawer'
import { useWorkspace } from '../workspace/WorkspaceContext'

export function AppLayout() {
  const { selectedJob, activeResumeId, statuses, updateStatus, closeJob } = useWorkspace()

  return (
    <div className="app-shell">
      <Navbar />
      <Outlet />

      {selectedJob && (
        <JobDetailDrawer
          job={selectedJob}
          activeResumeId={activeResumeId}
          status={statuses[selectedJob.id]}
          onSetStatus={(s) => updateStatus(selectedJob.id, s)}
          onClose={closeJob}
        />
      )}
    </div>
  )
}
