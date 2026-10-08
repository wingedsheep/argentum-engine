/**
 * The admin dashboard landing page: a hub that routes to the admin areas (Stats, Live games,
 * Activity, Players). It's the starting point a signed-in admin lands on; the bootstrap-password login also
 * arrives here after authenticating.
 */
import type React from 'react'
import { AdminScreen, adminTheme } from './adminUi'

export type AdminArea = 'stats' | 'live' | 'activity' | 'players'

interface HubItem {
  area: AdminArea
  icon: string
  title: string
  description: string
}

const ITEMS: HubItem[] = [
  { area: 'stats', icon: '📊', title: 'Stats', description: 'Global activity, decks, cards, win rates and geography.' },
  { area: 'live', icon: '🟢', title: 'Live games', description: 'Who is online and what they are doing, plus every game and tournament running — check before maintenance, or watch any game.' },
  { area: 'activity', icon: '🏆', title: 'Activity', description: 'Recent games and tournaments across every player — click through to replays and standings.' },
  { area: 'players', icon: '👥', title: 'Players', description: 'Registered accounts, their games, and admin access.' },
]

export function AdminHub({
  onNavigate,
  authLabel,
}: {
  onNavigate: (area: AdminArea) => void
  authLabel: string
}) {
  return (
    <AdminScreen
      title="Admin dashboard"
      subtitle={authLabel}
    >
      <div style={styles.grid}>
        {ITEMS.map((item) => (
          <button key={item.area} type="button" style={styles.card} onClick={() => onNavigate(item.area)}>
            <span style={styles.icon} aria-hidden>
              {item.icon}
            </span>
            <span style={styles.cardTitle}>{item.title}</span>
            <span style={styles.cardDesc}>{item.description}</span>
            <span style={styles.cardArrow}>Open →</span>
          </button>
        ))}
      </div>
    </AdminScreen>
  )
}

const styles: Record<string, React.CSSProperties> = {
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
    gap: 18,
  },
  card: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    gap: 8,
    textAlign: 'left',
    backgroundColor: adminTheme.panel,
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 16,
    backdropFilter: 'blur(18px) saturate(140%)',
    WebkitBackdropFilter: 'blur(18px) saturate(140%)',
    padding: '22px 20px 18px',
    font: 'inherit',
    color: adminTheme.text,
    cursor: 'pointer',
    transition: 'border-color 120ms ease, transform 120ms ease',
  },
  icon: { fontSize: 30, lineHeight: 1 },
  cardTitle: { fontFamily: 'var(--font-display)', fontSize: 21, fontWeight: 600, marginTop: 4 },
  cardDesc: { fontSize: 13, color: adminTheme.textSecondary, lineHeight: 1.45 },
  cardArrow: { marginTop: 6, fontSize: 13, fontWeight: 600, color: adminTheme.accent },
}
