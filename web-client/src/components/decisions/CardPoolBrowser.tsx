import { useMemo, useState } from 'react'
import type { EntityId } from '@/types'
import { DecisionCard } from './DecisionComponents'
import styles from './DecisionUI.module.css'

/** Above this many cards a selection switches from "fit everything on one line" to the browser. */
export const LARGE_POOL_THRESHOLD = 14

/** One option as the browser sees it. */
export interface PoolCard {
  id: EntityId
  name: string
  typeLine: string
  imageUri?: string | null | undefined
  manaValue: number
  /** Shown but never selectable ("look at" cards). */
  nonSelectable: boolean
}

/** Identical cards (same name, same selectability) collapsed into one tile. */
interface PoolGroup {
  key: string
  name: string
  imageUri?: string | null | undefined
  typeLine: string
  ids: EntityId[]
  nonSelectable: boolean
}

/** Card types offered as filter chips, in display order. */
const FILTER_TYPES = [
  'Creature', 'Planeswalker', 'Instant', 'Sorcery', 'Artifact', 'Enchantment', 'Battle', 'Land',
] as const

/** Sort rank: creatures and spells first, lands last, like a decklist. */
function typeRank(typeLine: string): number {
  const idx = FILTER_TYPES.findIndex((t) => typeLine.includes(t))
  return idx === -1 ? FILTER_TYPES.length : idx
}

/**
 * Browser for a large selection pool (a sideboard, "a card from outside the game", a big
 * graveyard): readable cards in a scrolling grid, duplicates stacked with a ×N badge, a name/type
 * search, type filter chips, and a tray of the current picks so a choice stays visible (and
 * removable) while the grid is filtered.
 */
export function CardPoolBrowser({
  cards,
  selectedCards,
  isDisabled,
  onAdd,
  onRemove,
  cardWidth,
  isMobile,
  onHover,
}: {
  cards: readonly PoolCard[]
  selectedCards: readonly EntityId[]
  isDisabled: (id: EntityId) => boolean
  /** Try to select this card; the parent applies the selection rules. */
  onAdd: (id: EntityId) => void
  onRemove: (id: EntityId) => void
  cardWidth: number
  isMobile: boolean
  onHover: (id: EntityId | null) => void
}) {
  const [query, setQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState<string | null>(null)

  const groups = useMemo(() => {
    const byKey = new Map<string, PoolGroup>()
    const sorted = [...cards].sort(
      (a, b) =>
        Number(a.nonSelectable) - Number(b.nonSelectable) ||
        typeRank(a.typeLine) - typeRank(b.typeLine) ||
        a.manaValue - b.manaValue ||
        a.name.localeCompare(b.name),
    )
    for (const card of sorted) {
      const key = `${card.nonSelectable ? 'ns' : 's'}:${card.name}`
      const group = byKey.get(key)
      if (group) group.ids.push(card.id)
      else byKey.set(key, { key, name: card.name, imageUri: card.imageUri, typeLine: card.typeLine, ids: [card.id], nonSelectable: card.nonSelectable })
    }
    return [...byKey.values()]
  }, [cards])

  const typeCounts = useMemo(() => {
    const counts = new Map<string, number>()
    for (const card of cards) {
      for (const t of FILTER_TYPES) if (card.typeLine.includes(t)) counts.set(t, (counts.get(t) ?? 0) + 1)
    }
    return FILTER_TYPES.filter((t) => counts.has(t)).map((t) => ({ type: t, count: counts.get(t)! }))
  }, [cards])

  const needle = query.trim().toLowerCase()
  const visibleGroups = groups.filter(
    (g) =>
      (typeFilter == null || g.typeLine.includes(typeFilter)) &&
      (needle === '' || g.name.toLowerCase().includes(needle) || g.typeLine.toLowerCase().includes(needle)),
  )

  const nameOf = (id: EntityId) => cards.find((c) => c.id === id)?.name ?? 'Unknown Card'

  const clickGroup = (group: PoolGroup) => {
    if (group.nonSelectable) return
    const available = group.ids.find((id) => !selectedCards.includes(id) && !isDisabled(id))
    const selectedInGroup = group.ids.filter((id) => selectedCards.includes(id))
    // Clicking adds the next free copy; once none can be added, it takes one back off. A single
    // copy is a plain toggle, like any other card.
    if (available) {
      onAdd(available)
    } else if (selectedInGroup.length > 0) {
      onRemove(selectedInGroup[selectedInGroup.length - 1]!)
    }
  }

  return (
    <div className={styles.poolBrowser}>
      <div className={styles.poolToolbar}>
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={`Search ${cards.length} cards…`}
          aria-label="Search cards"
          className={styles.poolSearch}
          autoFocus={!isMobile}
        />
        {typeCounts.length > 1 && (
          <div className={styles.poolChips} role="group" aria-label="Filter by card type">
            <button
              type="button"
              className={typeFilter == null ? styles.poolChipActive : styles.poolChip}
              onClick={() => setTypeFilter(null)}
            >
              All <span className={styles.poolChipCount}>{cards.length}</span>
            </button>
            {typeCounts.map(({ type, count }) => (
              <button
                key={type}
                type="button"
                className={typeFilter === type ? styles.poolChipActive : styles.poolChip}
                onClick={() => setTypeFilter(typeFilter === type ? null : type)}
              >
                {type} <span className={styles.poolChipCount}>{count}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className={styles.poolGrid} style={{ gridTemplateColumns: `repeat(auto-fill, ${cardWidth}px)` }}>
        {visibleGroups.map((group) => {
          const selectedCount = group.ids.filter((id) => selectedCards.includes(id)).length
          const disabled =
            group.nonSelectable || (selectedCount === 0 && group.ids.every((id) => isDisabled(id)))
          return (
            <div
              key={group.key}
              className={styles.poolTile}
              title={group.name}
              onMouseEnter={() => onHover(group.ids[0]!)}
              onMouseLeave={() => onHover(null)}
            >
              <DecisionCard
                cardId={group.ids[0]!}
                cardName={group.name}
                imageUri={group.imageUri}
                isSelected={selectedCount > 0}
                onClick={() => clickGroup(group)}
                cardWidth={cardWidth}
                nonSelectable={disabled}
              />
              {group.ids.length > 1 && (
                <span className={styles.poolCopies}>
                  {selectedCount > 0 ? `${selectedCount}/${group.ids.length}` : `×${group.ids.length}`}
                </span>
              )}
              {group.ids.length > 1 && selectedCount > 0 && (
                <button
                  type="button"
                  className={styles.poolRemoveOne}
                  aria-label={`Remove one ${group.name}`}
                  onClick={() => onRemove(group.ids.filter((id) => selectedCards.includes(id)).pop()!)}
                >
                  −
                </button>
              )}
            </div>
          )
        })}
        {visibleGroups.length === 0 && <p className={styles.poolEmpty}>No cards match.</p>}
      </div>

      {selectedCards.length > 0 && (
        <div className={styles.poolTray}>
          <span className={styles.poolTrayLabel}>Selected</span>
          {selectedCards.map((id) => (
            <button
              key={id}
              type="button"
              className={styles.poolTrayChip}
              onClick={() => onRemove(id)}
              onMouseEnter={() => onHover(id)}
              onMouseLeave={() => onHover(null)}
              aria-label={`Deselect ${nameOf(id)}`}
            >
              {nameOf(id)} <span aria-hidden>×</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
