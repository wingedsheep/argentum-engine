/**
 * A small colored pill for a tournament's lifecycle status (in progress / completed / abandoned).
 * Shared by the admin dashboard's Tournaments table, the profile Recent tournaments table, and the
 * tournament detail modal. Accepts a plain string so callers don't have to share a status type.
 */
import type React from 'react'

const STYLES: Record<string, { label: string; color: string; bg: string; border: string }> = {
  IN_PROGRESS: { label: 'In progress', color: '#7fd1ff', bg: 'rgba(88,166,255,0.14)', border: '#3d6fa855' },
  COMPLETED: { label: 'Completed', color: '#5bd16e', bg: 'rgba(91,209,110,0.12)', border: '#3a8a4a55' },
  ABANDONED: { label: 'Abandoned', color: '#e0a15b', bg: 'rgba(224,161,91,0.12)', border: '#8a6a3a55' },
}

export function TournamentStatusBadge({ status }: { status: string }) {
  const s = STYLES[status] ?? {
    label: status,
    color: '#aaa',
    bg: 'rgba(160,160,160,0.12)',
    border: '#55555555',
  }
  return (
    <span
      style={{
        ...badge,
        color: s.color,
        backgroundColor: s.bg,
        border: `1px solid ${s.border}`,
      }}
    >
      {s.label}
    </span>
  )
}

const badge: React.CSSProperties = {
  display: 'inline-block',
  fontSize: 11,
  fontWeight: 600,
  letterSpacing: 0.3,
  borderRadius: 999,
  padding: '2px 8px',
  whiteSpace: 'nowrap',
}
