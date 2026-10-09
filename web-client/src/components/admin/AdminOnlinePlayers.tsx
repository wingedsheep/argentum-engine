/**
 * The "who is doing what" half of the admin Live overview: every online player with the server's
 * one-line account of their activity (playing, drafting, waiting in a lobby, searching, editing decks,
 * browsing a page), a breakdown bar over those buckets, and a feed of recent moves — lobbies opened,
 * queues joined, decks submitted. All of it comes from the same `/api/admin/live-overview` poll.
 */
import type React from 'react'
import type { ActivityFeedEntry, ActivityKind, OnlinePlayer } from '@/api/adminLiveOverview'
import { formatAgo, formatClock } from './statFormat'
import { Panel, Table, adminTheme, cellStyle } from './adminUi'

/** Players with no input for this long read as away — the tab is open, nobody is at it. */
const AWAY_AFTER_MS = 10 * 60_000

/** The dashboard's buckets: several server kinds fold into one where the difference is detail. */
const GROUPS: readonly { key: string; label: string; color: string; kinds: readonly ActivityKind[] }[] = [
  { key: 'playing', label: 'Playing', color: adminTheme.good, kinds: ['PLAYING'] },
  { key: 'limited', label: 'Drafting / building', color: adminTheme.accentSolid, kinds: ['DRAFTING', 'BUILDING_LIMITED', 'TOURNAMENT'] },
  { key: 'lobby', label: 'In a lobby', color: '#6fb3ff', kinds: ['LOBBY'] },
  { key: 'search', label: 'Searching', color: '#4fd1c5', kinds: ['SEARCHING'] },
  { key: 'decks', label: 'Deckbuilder', color: '#c58bff', kinds: ['DECKBUILDER'] },
  { key: 'watching', label: 'Watching', color: '#f78fb3', kinds: ['SPECTATING'] },
  { key: 'browsing', label: 'Browsing', color: adminTheme.textMuted, kinds: ['BROWSING', 'HOME'] },
]

function groupOf(kind: ActivityKind) {
  return GROUPS.find((g) => g.kinds.includes(kind)) ?? GROUPS[GROUPS.length - 1]!
}

export function OnlinePlayersPanel({ players, now }: { players: readonly OnlinePlayer[]; now: number }) {
  const counts = GROUPS.map((g) => ({ ...g, count: players.filter((p) => g.kinds.includes(p.activity)).length }))
  const away = players.filter((p) => isAway(p, now)).length

  return (
    <Panel
      title="Online players"
      subtitle={`${players.length} connected${away > 0 ? ` · ${away} with no input for ${AWAY_AFTER_MS / 60_000}+ min` : ''}`}
    >
      {players.length === 0 ? (
        <p style={cellStyle.muted}>Nobody is online.</p>
      ) : (
        <>
          <div style={styles.bar} role="img" aria-label={counts.filter((c) => c.count > 0).map((c) => `${c.count} ${c.label}`).join(', ')}>
            {counts
              .filter((c) => c.count > 0)
              .map((c) => (
                <span key={c.key} style={{ ...styles.barSegment, flexGrow: c.count, backgroundColor: c.color }} />
              ))}
          </div>
          <div style={styles.legend}>
            {counts
              .filter((c) => c.count > 0)
              .map((c) => (
                <span key={c.key} style={styles.legendItem}>
                  <span style={{ ...styles.dot, backgroundColor: c.color }} aria-hidden />
                  <strong style={styles.legendCount}>{c.count}</strong> {c.label}
                </span>
              ))}
          </div>
          <Table head={['Player', 'Doing', 'Last input', '']} leftColumns={2}>
            {players.map((p, i) => (
              <PlayerRow key={`${p.name}-${i}`} player={p} now={now} />
            ))}
          </Table>
        </>
      )}
    </Panel>
  )
}

function PlayerRow({ player, now }: { player: OnlinePlayer; now: number }) {
  const group = groupOf(player.activity)
  const away = isAway(player, now)
  return (
    <tr style={away ? styles.awayRow : undefined}>
      <td style={{ ...cellStyle.td, whiteSpace: 'nowrap' }}>
        <span style={styles.name}>{player.name}</span>
        {!player.signedIn && <span style={styles.tag}> guest</span>}
      </td>
      <td style={{ ...cellStyle.td, whiteSpace: 'normal' }}>
        <span style={styles.doing}>
          <span style={{ ...styles.dot, backgroundColor: group.color }} aria-hidden />
          <span>{player.detail}</span>
        </span>
        {player.searching && (
          <span style={styles.searching}>
            {' '}
            · also searching {player.searching}
            {player.searchingSince ? ` (${formatAgo(now - Date.parse(player.searchingSince))})` : ''}
          </span>
        )}
      </td>
      <td style={{ ...cellStyle.td, color: away ? adminTheme.textMuted : adminTheme.textSecondary }}>
        {player.lastInputAt ? `${formatAgo(now - Date.parse(player.lastInputAt))} ago` : '—'}
      </td>
      <td style={cellStyle.tdNum}>
        {player.gameSessionId ? (
          <a style={styles.link} href={`/?spectate=${encodeURIComponent(player.gameSessionId)}`} target="_blank" rel="noreferrer">
            Watch ↗
          </a>
        ) : null}
      </td>
    </tr>
  )
}

const FEED_COLORS: Record<string, string> = {
  connect: adminTheme.textMuted,
  lobby: '#6fb3ff',
  deck: adminTheme.accentSolid,
  queue: '#4fd1c5',
  watch: '#f78fb3',
  game: adminTheme.good,
  page: '#c58bff',
}

export function ActivityFeedPanel({ feed, now }: { feed: readonly ActivityFeedEntry[]; now: number }) {
  return (
    <Panel title="Recent activity" subtitle="Since the last server restart · newest first">
      {feed.length === 0 ? (
        <p style={cellStyle.muted}>Nothing yet.</p>
      ) : (
        <ol style={styles.feed}>
          {feed.map((e, i) => (
            <li key={`${e.at}-${i}`} style={styles.feedItem}>
              <span style={styles.feedTime} title={formatClock(e.at)}>
                {formatAgo(now - Date.parse(e.at))}
              </span>
              <span style={{ ...styles.dot, backgroundColor: FEED_COLORS[e.kind] ?? adminTheme.textMuted }} aria-hidden />
              <span>
                <span style={styles.name}>{e.playerName}</span> {e.text}
              </span>
            </li>
          ))}
        </ol>
      )}
    </Panel>
  )
}

function isAway(player: OnlinePlayer, now: number): boolean {
  // A seated player waiting on their opponent is still at the table.
  if (player.activity === 'PLAYING') return false
  const last = [player.lastInputAt, player.pageSince].filter((t): t is string => t != null).map(Date.parse)
  return last.length > 0 && now - Math.max(...last) > AWAY_AFTER_MS
}

const styles: Record<string, React.CSSProperties> = {
  bar: { display: 'flex', gap: 2, height: 8, borderRadius: 4, overflow: 'hidden', marginBottom: 10 },
  barSegment: { flexBasis: 0, minWidth: 6 },
  legend: { display: 'flex', flexWrap: 'wrap', gap: '6px 16px', marginBottom: 14, fontSize: 13, color: adminTheme.textSecondary },
  legendItem: { display: 'inline-flex', alignItems: 'center', gap: 6 },
  legendCount: { color: adminTheme.text, fontVariantNumeric: 'tabular-nums' },
  dot: { width: 8, height: 8, borderRadius: '50%', flexShrink: 0, display: 'inline-block' },
  doing: { display: 'inline-flex', alignItems: 'center', gap: 8 },
  name: { color: adminTheme.text, fontWeight: 600 },
  tag: { color: adminTheme.textMuted, fontSize: 12 },
  searching: { color: '#4fd1c5', fontSize: 12 },
  awayRow: { opacity: 0.55 },
  link: { color: adminTheme.accent, fontSize: 13, textDecoration: 'none', whiteSpace: 'nowrap' },
  feed: { listStyle: 'none', margin: 0, padding: 0, maxHeight: 360, overflowY: 'auto' },
  feedItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '6px 2px',
    borderBottom: `1px solid ${adminTheme.borderSoft}`,
    fontSize: 13,
    color: adminTheme.textSecondary,
  },
  feedTime: { width: 52, flexShrink: 0, textAlign: 'right', color: adminTheme.textMuted, fontVariantNumeric: 'tabular-nums', fontSize: 12 },
}
