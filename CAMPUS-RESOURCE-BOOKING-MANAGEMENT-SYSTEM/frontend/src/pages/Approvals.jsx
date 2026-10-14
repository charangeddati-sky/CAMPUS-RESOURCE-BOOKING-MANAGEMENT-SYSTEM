import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { CheckCircle2, XCircle, Clock, Calendar, MapPin, User, FileText, AlertCircle, ShieldCheck } from 'lucide-react'
import client from '../api/client'
import { errorMessage, formatDate, formatTime, statusBadge } from '../utils/status'
import { PageHeader, EmptyState, ConfirmDialog, Spinner } from '../components/ui'

export default function Approvals() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [processingId, setProcessingId] = useState(null)
  const [approveTarget, setApproveTarget] = useState(null)
  const [rejectTarget, setRejectTarget] = useState(null)
  const [rejectReason, setRejectReason] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const res = await client.get('/approvals')
      setRows(res.data || [])
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const confirmApprove = async () => {
    if (!approveTarget) return
    const id = approveTarget.id
    setProcessingId(id)
    try {
      await client.post(`/bookings/${id}/approve`, { comment: '' })
      toast.success(`Booking #${id} approved successfully`)
      setApproveTarget(null)
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setProcessingId(null)
    }
  }

  const confirmReject = async () => {
    if (!rejectTarget) return
    if (!rejectReason.trim()) {
      toast.error('Please provide a reason for rejection.')
      return
    }
    const id = rejectTarget.id
    setProcessingId(id)
    try {
      await client.post(`/bookings/${id}/reject`, { comment: rejectReason.trim() })
      toast.success(`Booking #${id} rejected`)
      setRejectTarget(null)
      setRejectReason('')
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setProcessingId(null)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        kicker="Workflow Administration"
        title="Pending Approvals"
        subtitle="Review, approve, or reject reservation requests requiring your authorization."
        actions={
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-gold-400/20 px-3 py-1 text-xs font-semibold text-gold-600 dark:text-gold-300">
              {rows.length} pending
            </span>
            <button onClick={load} className="btn-ghost text-xs">
              Refresh
            </button>
          </div>
        }
      />

      {loading ? (
        <div className="flex h-48 items-center justify-center">
          <Spinner size={32} className="text-grove-600" />
        </div>
      ) : rows.length === 0 ? (
        <EmptyState
          title="All caught up!"
          icon={<ShieldCheck className="h-8 w-8 text-grove-600" />}
        >
          No pending bookings currently require your review or authorization.
        </EmptyState>
      ) : (
        <div className="space-y-4">
          {rows.map((b) => {
            const badge = statusBadge(b.status)
            const resName = b.resources?.[0]?.name || 'Resource'
            const building = b.resources?.[0]?.buildingName || ''
            const floor = b.resources?.[0]?.floorName || ''

            return (
              <div
                key={b.id}
                className="soft-card p-5 transition hover:shadow-md"
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="space-y-1.5">
                    <div className="flex flex-wrap items-center gap-2.5">
                      <h3 className="font-display text-xl font-bold">{resName}</h3>
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${badge.bg} ${badge.color}`}>
                        {badge.label}
                      </span>
                      <span className="text-xs text-grove-400">#{b.id}</span>
                    </div>

                    <div className="flex flex-wrap items-center gap-4 text-xs text-grove-600 dark:text-grove-300">
                      {(building || floor) && (
                        <span className="flex items-center gap-1">
                          <MapPin size={13} className="text-grove-400" />
                          {[building, floor].filter(Boolean).join(' · ')}
                        </span>
                      )}
                      <span className="flex items-center gap-1">
                        <Calendar size={13} className="text-grove-400" />
                        {formatDate(b.date)}
                      </span>
                      <span className="flex items-center gap-1">
                        <Clock size={13} className="text-grove-400" />
                        {formatTime(b.startTime)} – {formatTime(b.endTime)}
                      </span>
                      <span className="flex items-center gap-1">
                        <User size={13} className="text-grove-400" />
                        <span className="font-medium text-grove-800 dark:text-grove-100">{b.userName}</span>
                        {b.department && ` (${b.department})`}
                      </span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    <Link
                      to={`/bookings/${b.id}`}
                      className="rounded-full border border-grove-200 px-3 py-1.5 text-xs font-medium transition hover:bg-cream-100 dark:border-white/10 dark:hover:bg-grove-800"
                    >
                      View Details
                    </Link>
                    <button
                      onClick={() => setApproveTarget(b)}
                      disabled={processingId === b.id}
                      className="flex items-center gap-1.5 rounded-full bg-grove-700 px-4 py-1.5 text-xs font-semibold text-white transition hover:bg-grove-800 disabled:opacity-50"
                    >
                      {processingId === b.id ? <Spinner size={13} /> : <CheckCircle2 size={14} />}
                      Approve
                    </button>
                    <button
                      onClick={() => { setRejectTarget(b); setRejectReason('') }}
                      disabled={processingId === b.id}
                      className="flex items-center gap-1.5 rounded-full border border-brick-300 px-3.5 py-1.5 text-xs font-semibold text-brick-600 transition hover:bg-brick-50 dark:border-brick-800 dark:text-brick-400 dark:hover:bg-brick-950/30 disabled:opacity-50"
                    >
                      <XCircle size={14} />
                      Reject
                    </button>
                  </div>
                </div>

                {/* Purpose & attendee details */}
                <div className="mt-3.5 rounded-xl bg-cream-50/70 p-3 text-sm text-grove-700 dark:bg-grove-850/60 dark:text-grove-200">
                  <div className="flex items-start gap-2">
                    <FileText size={15} className="mt-0.5 shrink-0 text-grove-400" />
                    <div>
                      <span className="font-semibold text-grove-900 dark:text-grove-100">Purpose: </span>
                      {b.purpose || 'No stated purpose'}
                      {b.attendees && (
                        <span className="ml-3 inline-block text-xs text-grove-500">
                          (Expected attendees: {b.attendees})
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Approve Confirmation Dialog */}
      <ConfirmDialog
        open={!!approveTarget}
        title="Approve Booking Request"
        message={`Are you sure you want to approve booking #${approveTarget?.id} for ${approveTarget?.resources?.[0]?.name || 'this venue'} on ${approveTarget?.date ? formatDate(approveTarget.date) : ''}?`}
        confirmText="Yes, Approve"
        cancelText="Cancel"
        onConfirm={confirmApprove}
        onCancel={() => setApproveTarget(null)}
      />

      {/* Custom Reject Modal with Reason Input */}
      {rejectTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 animate-fade-in">
          <div className="soft-card w-full max-w-md p-6 shadow-2xl animate-modal-enter">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brick-100 text-brick-600 dark:bg-brick-900/50 dark:text-brick-400">
                <AlertCircle size={20} />
              </div>
              <div>
                <h3 className="font-display text-xl font-bold text-brick-600 dark:text-brick-400">
                  Reject Booking Request
                </h3>
                <p className="mt-0.5 text-xs text-grove-500">
                  Booking #{rejectTarget.id} · {rejectTarget.resources?.[0]?.name}
                </p>
              </div>
            </div>

            <div className="mt-4">
              <label className="text-xs font-semibold text-grove-700 dark:text-grove-300">
                Reason for Rejection <span className="text-brick-500">*</span>
              </label>
              <textarea
                className="field mt-1.5 w-full min-h-[96px] rounded-xl p-3 text-sm focus:ring-brick-400"
                placeholder="Explain why this request is being rejected (e.g., room under maintenance, priority institutional event, capacity issue)..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                autoFocus
              />
              <div className="mt-2 flex flex-wrap gap-1.5">
                {['Room under maintenance', 'Priority departmental event', 'Capacity mismatch', 'Alternative venue assigned'].map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setRejectReason(preset)}
                    className="rounded-full bg-cream-100 px-2.5 py-0.5 text-[11px] text-grove-600 hover:bg-cream-200 dark:bg-grove-800 dark:text-grove-300"
                  >
                    {preset}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => { setRejectTarget(null); setRejectReason('') }}
                className="btn-ghost text-xs"
                disabled={processingId !== null}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={confirmReject}
                className="rounded-full bg-brick-600 px-4 py-2 text-xs font-semibold text-white transition hover:bg-brick-700 disabled:opacity-50"
                disabled={processingId !== null || !rejectReason.trim()}
              >
                {processingId !== null ? 'Rejecting...' : 'Confirm Rejection'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
