/**
 * Shared chrome and primitives for the admin dashboard's sub-screens (hub, stats, players). The frame
 * is {@link DevPage} — the app's page shell over the card art, scrolling itself because `#root` is
 * `overflow:hidden` for the game board. Panels here are the same glass as the rest of the app.
 */
import type React from 'react'
import { DevPage } from './DevPage'

export const adminTheme = {
  /** An inset well inside a panel (map box, inputs) — the art never shows through it. */
  bg: 'rgba(0, 0, 0, 0.3)',
  panel: 'rgba(10, 12, 20, 0.72)',
  /** Opaque enough to read over a chart (tooltips, chips). */
  panelAlt: 'rgba(22, 25, 36, 0.94)',
  border: 'rgba(255, 255, 255, 0.1)',
  borderSoft: 'rgba(255, 255, 255, 0.06)',
  accent: '#f2c97a',
  accentSolid: '#f2b45c',
  /** Text on an `accentSolid` fill. */
  onAccent: '#1d1405',
  text: '#f1f3f9',
  textSecondary: '#b4bccd',
  textMuted: '#8f98ac',
  good: '#5bd16e',
  bad: '#e15b6e',
} as const

/**
 * A full-height, self-scrolling admin screen with a sticky header (back affordance + title + optional
 * subtitle + right-aligned slot) and a centered content column.
 */
export function AdminScreen({
  title,
  subtitle,
  onBack,
  backLabel = '← Back',
  right,
  children,
}: {
  title: string
  subtitle?: string | undefined
  onBack?: (() => void) | undefined
  backLabel?: string
  right?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <DevPage section="Admin" title={title} subtitle={subtitle} onBack={onBack} backLabel={backLabel} right={right}>
      {children}
    </DevPage>
  )
}

export function Panel({
  title,
  subtitle,
  action,
  children,
  span2,
}: {
  title?: string
  subtitle?: string
  action?: React.ReactNode
  children: React.ReactNode
  span2?: boolean
}) {
  return (
    <div style={span2 ? { ...panelStyle.panel, gridColumn: '1 / -1' } : panelStyle.panel}>
      {(title || action) && (
        <div style={panelStyle.panelHead}>
          <div style={panelStyle.panelTitleBlock}>
            {title && <h2 style={panelStyle.panelTitle}>{title}</h2>}
            {subtitle && <p style={panelStyle.panelSubtitle}>{subtitle}</p>}
          </div>
          {action}
        </div>
      )}
      {children}
    </div>
  )
}

export function StatCard({ label, value, accent }: { label: string; value: React.ReactNode; accent?: boolean }) {
  return (
    <div style={panelStyle.metric}>
      <div style={accent ? { ...panelStyle.metricValue, color: adminTheme.accent } : panelStyle.metricValue}>
        {value ?? '—'}
      </div>
      <div style={panelStyle.metricLabel}>{label}</div>
    </div>
  )
}

/** A table whose first [leftColumns] headers read left-aligned (text); the rest right-aligned (numbers). */
export function Table({
  head,
  children,
  leftColumns = 1,
}: {
  head: React.ReactNode[]
  children: React.ReactNode
  leftColumns?: number
}) {
  return (
    <div style={tableStyle.wrap}>
      <table style={tableStyle.table}>
        <thead>
          <tr>
            {head.map((h, i) => (
              <th key={i} style={i < leftColumns ? tableStyle.th : tableStyle.thNum}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}

export const chartTooltipStyle: React.CSSProperties = {
  backgroundColor: adminTheme.panelAlt,
  border: `1px solid ${adminTheme.border}`,
  borderRadius: 8,
  color: adminTheme.text,
  fontSize: 12,
}

const panelStyle: Record<string, React.CSSProperties> = {
  panel: {
    minWidth: 0,
    backgroundColor: adminTheme.panel,
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 16,
    padding: 18,
    backdropFilter: 'blur(18px) saturate(140%)',
    WebkitBackdropFilter: 'blur(18px) saturate(140%)',
  },
  panelHead: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    marginBottom: 14,
  },
  panelTitleBlock: { display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0 },
  panelTitle: { margin: 0, color: adminTheme.text, fontFamily: 'var(--font-display)', fontSize: 17, fontWeight: 600 },
  panelSubtitle: { margin: 0, color: adminTheme.textMuted, fontSize: 12 },
  metric: {
    flex: '1 1 130px',
    backgroundColor: adminTheme.panel,
    border: `1px solid ${adminTheme.border}`,
    backdropFilter: 'blur(18px)',
    WebkitBackdropFilter: 'blur(18px)',
    borderRadius: 14,
    padding: '16px 14px',
    textAlign: 'center',
  },
  metricValue: { color: adminTheme.text, fontFamily: 'var(--font-display)', fontSize: 28, fontWeight: 600, lineHeight: 1.1 },
  metricLabel: {
    color: adminTheme.textMuted,
    fontSize: 11,
    marginTop: 6,
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
}

const tableStyle: Record<string, React.CSSProperties> = {
  wrap: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: {
    textAlign: 'left',
    color: adminTheme.textMuted,
    fontWeight: 600,
    padding: '8px 10px',
    borderBottom: `1px solid ${adminTheme.border}`,
    whiteSpace: 'nowrap',
  },
  thNum: {
    textAlign: 'right',
    color: adminTheme.textMuted,
    fontWeight: 600,
    padding: '8px 10px',
    borderBottom: `1px solid ${adminTheme.border}`,
    whiteSpace: 'nowrap',
  },
}

export const cellStyle: Record<string, React.CSSProperties> = {
  td: { textAlign: 'left', color: adminTheme.textSecondary, padding: '8px 10px', borderBottom: `1px solid ${adminTheme.borderSoft}` },
  tdNum: { textAlign: 'right', color: adminTheme.textSecondary, padding: '8px 10px', borderBottom: `1px solid ${adminTheme.borderSoft}` },
  muted: { color: adminTheme.textMuted, fontSize: 13, margin: 0 },
}
