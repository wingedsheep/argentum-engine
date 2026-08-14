/**
 * Shared, polished renderer for a deck — the single deck UI reused by the player's recent-games
 * deck modal, the admin player view, and the in-game deck tracker. It takes registry-enriched card
 * lines ({@link GameDeckCard}: copies + cmc + types + colours) and renders them the way the
 * deckbuilder does: cards grouped by type with running counts, a stacked-by-colour mana curve and
 * colour pips (both from the deckbuilder's {@link DeckSummary} helpers), and a cursor-following card
 * image on hover ({@link HoverCardPreview}). Nothing here is admin- or profile-specific, so every
 * surface shares exactly one look.
 *
 * Live games pass {@link DeckViewCard}s carrying a `remaining` count, which switches rows into
 * tracker mode: `remaining/copies` instead of a bare copy count, fully-drawn rows dimmed, and — when
 * `drawPoolSize` is given — the chance the next card drawn is that one.
 */
import { useState } from 'react'
import type React from 'react'
import type { GameDeckCard, GameDeckParticipant } from '@/api/account'
import {
  COLOR_DOT,
  ManaCurveBars,
  computeDeckStats,
  type DeckStatsCard,
} from '@/components/ui/DeckSummary'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'
import { colorLabel } from '@/components/admin/statFormat'

// Primary-type buckets, in the order the deckbuilder lists them. A card is filed under the first of
// these its type set matches (so an artifact creature counts as a Creature); anything else is Other.
const TYPE_ORDER = ['CREATURE', 'PLANESWALKER', 'INSTANT', 'SORCERY', 'ARTIFACT', 'ENCHANTMENT', 'LAND'] as const
const TYPE_LABEL: Record<string, string> = {
  CREATURE: 'Creatures',
  PLANESWALKER: 'Planeswalkers',
  INSTANT: 'Instants',
  SORCERY: 'Sorceries',
  ARTIFACT: 'Artifacts',
  ENCHANTMENT: 'Enchantments',
  LAND: 'Lands',
  OTHER: 'Other',
}

/**
 * A deck line, optionally annotated with how many copies are still unaccounted for. `remaining` is
 * absent for a recorded deck (nothing to track) and present for a live game.
 */
export type DeckViewCard = GameDeckCard & { readonly remaining?: number }

interface TypeGroup {
  key: string
  label: string
  cards: DeckViewCard[]
  count: number
  remaining: number
}

function primaryType(card: DeckViewCard): string {
  for (const t of TYPE_ORDER) if (card.cardTypes.includes(t)) return t
  return 'OTHER'
}

function groupByType(cards: readonly DeckViewCard[]): TypeGroup[] {
  const buckets = new Map<string, DeckViewCard[]>()
  for (const card of cards) {
    const key = primaryType(card)
    const list = buckets.get(key)
    if (list) list.push(card)
    else buckets.set(key, [card])
  }
  const order = [...TYPE_ORDER, 'OTHER']
  return order
    .filter((k) => buckets.has(k))
    .map((k) => {
      const groupCards = buckets
        .get(k)!
        .slice()
        .sort((a, b) => a.cmc - b.cmc || a.cardName.localeCompare(b.cardName))
      return {
        key: k,
        label: TYPE_LABEL[k] ?? k,
        cards: groupCards,
        count: groupCards.reduce((sum, c) => sum + c.copies, 0),
        remaining: groupCards.reduce((sum, c) => sum + (c.remaining ?? c.copies), 0),
      }
    })
}

type HoverState = { name: string; imageUri: string | null; pos: { x: number; y: number } } | null

/**
 * The body of a single deck: colour pips, mana curve, and the cards grouped by type. Used standalone
 * for a saved deck, inside {@link SeatDeckColumn} for a game seat, and by the in-game deck tracker.
 * Manages its own hover preview.
 *
 * The curve and colour pips always describe the *whole* deck — that's the decklist you built, and
 * it shouldn't reshape itself as you draw. Only the per-card rows track what's left.
 *
 * @param drawPoolSize Library size, when known. Turns on the per-row "chance your next draw is this
 *   card" column. Omit for a recorded deck.
 * @param emptyLabel What to show when there are no cards at all.
 */
export function DeckCardBody({
  cards,
  drawPoolSize,
  emptyLabel = 'Deck not recorded.',
}: {
  cards: readonly DeckViewCard[]
  drawPoolSize?: number
  emptyLabel?: string
}) {
  const [hover, setHover] = useState<HoverState>(null)

  if (cards.length === 0) return <p style={styles.muted}>{emptyLabel}</p>

  // Reuse the deckbuilder's stats helper — GameDeckCard satisfies DeckStatsCard structurally.
  const deckRecord: Record<string, number> = {}
  const cardMeta: Record<string, DeckStatsCard> = {}
  for (const c of cards) {
    deckRecord[c.cardName] = c.copies
    cardMeta[c.cardName] = { cmc: c.cmc, colors: c.colors, cardTypes: c.cardTypes }
  }
  const stats = computeDeckStats(deckRecord, cardMeta)
  const groups = groupByType(cards)
  const tracking = cards.some((c) => c.remaining !== undefined)

  return (
    <div style={styles.body}>
      {stats.colorCounts.length > 0 && (
        <div style={styles.pips}>
          {stats.colorCounts.map(([color, n]) => (
            <span key={color} style={styles.pip} title={`${color}: ${Math.round(n)}`}>
              <span style={{ ...styles.pipDot, background: COLOR_DOT[color] ?? '#888' }} />
              {Math.round(n)}
            </span>
          ))}
        </div>
      )}

      {stats.curve.some((n) => n > 0) && (
        <div style={styles.curve}>
          <ManaCurveBars curve={stats.curve} curveByColor={stats.curveByColor} />
        </div>
      )}

      <div style={styles.groups}>
        {groups.map((g) => (
          <div key={g.key}>
            <div style={styles.groupTitle}>
              {g.label}{' '}
              <span style={styles.groupCount}>{tracking ? `${g.remaining}/${g.count}` : g.count}</span>
            </div>
            <ul style={styles.cardList}>
              {g.cards.map((c) => {
                const remaining = c.remaining ?? c.copies
                const drawn = tracking && remaining === 0
                // Chance the very next card drawn is this one. Capped at 100% because a copy
                // hidden outside the library (a face-down exile) still counts as "remaining".
                const odds =
                  drawPoolSize && drawPoolSize > 0 && remaining > 0
                    ? Math.min(100, Math.round((remaining / drawPoolSize) * 100))
                    : null
                return (
                  <li
                    key={c.cardName}
                    style={drawn ? { ...styles.cardRow, ...styles.cardRowDrawn } : styles.cardRow}
                    onMouseEnter={(e) => setHover({ name: c.cardName, imageUri: c.imageUri, pos: { x: e.clientX, y: e.clientY } })}
                    onMouseMove={(e) => setHover({ name: c.cardName, imageUri: c.imageUri, pos: { x: e.clientX, y: e.clientY } })}
                    onMouseLeave={() => setHover(null)}
                  >
                    <span style={tracking ? { ...styles.copies, ...styles.copiesTracking } : styles.copies}>
                      {tracking ? remaining : c.copies}
                      {tracking && <span style={styles.ofCopies}>/{c.copies}</span>}
                    </span>
                    <span style={styles.cardName}>{c.cardName}</span>
                    {odds !== null && <span style={styles.odds}>{odds}%</span>}
                  </li>
                )
              })}
            </ul>
          </div>
        ))}
      </div>

      {hover && <HoverCardPreview name={hover.name} imageUri={hover.imageUri} pos={hover.pos} />}
    </div>
  )
}

/**
 * One seat in a game's deck view: a header (name / result / colours) above its {@link DeckCardBody}.
 * `renderActions` is an optional slot for per-seat controls (e.g. a "Save deck" button) — the
 * recent-games modal supplies one; the admin viewer leaves it unset. Keeping it a render prop lets
 * this component stay a pure renderer with no save-store dependency.
 */
export function SeatDeckColumn({
  p,
  renderActions,
}: {
  p: GameDeckParticipant
  renderActions?: (p: GameDeckParticipant) => React.ReactNode
}) {
  const total = p.cards.reduce((sum, c) => sum + c.copies, 0)
  const actions = renderActions?.(p)
  return (
    <div style={styles.col}>
      <div style={styles.colHead}>
        <span style={styles.colName}>
          {p.isSelf ? 'You' : p.playerName}
          {p.isAi ? <span style={styles.aiTag}> AI</span> : null}
        </span>
        <span style={{ ...styles.resultTag, color: p.won ? '#5bd16e' : '#e15b6e' }}>{p.won ? 'Win' : 'Loss'}</span>
      </div>
      <div style={styles.colMeta}>
        <span>{p.colors ? colorLabel(p.colors) : 'Colourless'}</span>
        <span style={styles.dim}>· {total} cards</span>
      </div>
      {actions ? <div style={styles.actions}>{actions}</div> : null}
      <DeckCardBody cards={p.cards} />
    </div>
  )
}

/**
 * Every seat's deck side by side — the body of the recent-games / admin deck modal. Seat count is
 * whatever the game had: two for a duel, three to six for a multiplayer pod. The grid keeps every
 * column the same width, so a pod that doesn't divide evenly into the row wraps into aligned columns
 * instead of stretching its last seat across the full width.
 */
export function GameDeckColumns({
  participants,
  renderActions,
}: {
  participants: GameDeckParticipant[]
  renderActions?: (p: GameDeckParticipant) => React.ReactNode
}) {
  return (
    <div style={styles.columns}>
      {participants.map((p, i) => (
        <SeatDeckColumn key={`${p.playerName}-${i}`} p={p} {...(renderActions ? { renderActions } : {})} />
      ))}
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  muted: { margin: '6px 0 0', color: '#888', fontSize: 13 },
  columns: { display: 'grid', gap: 16, gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))' },
  col: {
    minWidth: 0,
    backgroundColor: '#171723',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '14px 16px',
  },
  colHead: { display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 8 },
  colName: { color: '#fff', fontSize: 15, fontWeight: 600 },
  resultTag: { fontSize: 12, fontWeight: 700 },
  aiTag: { color: '#888', fontSize: 11 },
  colMeta: { display: 'flex', alignItems: 'center', gap: 6, color: '#bbb', fontSize: 12, margin: '6px 0 4px' },
  actions: { margin: '2px 0 4px' },
  dim: { color: '#777' },
  body: { display: 'flex', flexDirection: 'column', gap: 12, marginTop: 8 },
  pips: { display: 'flex', flexWrap: 'wrap', gap: 10 },
  pip: { display: 'inline-flex', alignItems: 'center', gap: 5, color: '#cdd', fontSize: 12, fontVariantNumeric: 'tabular-nums' },
  pipDot: { width: 11, height: 11, borderRadius: 999, display: 'inline-block', border: '1px solid #00000055' },
  curve: { marginTop: 2 },
  groups: { display: 'flex', flexDirection: 'column', gap: 12 },
  groupTitle: {
    display: 'flex',
    alignItems: 'baseline',
    gap: 8,
    color: '#9aa0b5',
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  groupCount: { color: '#666', fontWeight: 600 },
  cardList: { listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 1 },
  cardRow: {
    display: 'flex',
    gap: 8,
    alignItems: 'baseline',
    fontSize: 13,
    color: '#d6d9e6',
    padding: '2px 6px',
    borderRadius: 6,
    cursor: 'default',
  },
  cardRowDrawn: { opacity: 0.35 },
  copies: { color: '#8b9bff', fontVariantNumeric: 'tabular-nums', minWidth: 18, textAlign: 'right', fontWeight: 600 },
  copiesTracking: { minWidth: 34 },
  ofCopies: { color: '#5a6180', fontWeight: 500 },
  cardName: { color: '#d6d9e6', flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  odds: { color: '#6b7394', fontSize: 11, fontVariantNumeric: 'tabular-nums', flexShrink: 0 },
}
