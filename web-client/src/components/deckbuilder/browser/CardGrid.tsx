/**
 * Catalog card grid — lazy-loaded card images with a count badge, click to add,
 * shift/right-click to remove, drag to any drop zone, and a cursor-following hover preview.
 *
 * Shared between the deckbuilder page and the scenario builder's card browser; the CSS lives in
 * the deckbuilder stylesheet, which both surfaces import.
 */
import { memo, useCallback, useEffect, useRef, useState } from 'react'
import { ManaCost } from '@/components/ui/ManaSymbols'
import { useDfcHoverFlip } from '@/components/ui/useDfcHoverFlip'
import { getCardImageUrl, landscapeImageRotateDeg } from '@/utils/cardImages'
import type { CardSummary } from '../cardFilter'
import styles from '../deckbuilder.module.css'
import { setCardDragData } from './cardDrag'
import { HoverFollowPreview } from './HoverFollowPreview'

export function CardGrid({
  cards,
  deckCards,
  onAdd,
  onRemove,
  hasMore,
  onShowMore,
  compact = false,
}: {
  cards: CardSummary[]
  /** Copies already placed, keyed by card name — rendered as a badge on the tile. */
  deckCards: Record<string, number>
  onAdd: (c: CardSummary) => void
  onRemove: (name: string) => void
  hasMore: boolean
  onShowMore: () => void
  /** Denser tiles, for narrow side panes. */
  compact?: boolean
}) {
  const [hoverCard, setHoverCard] = useState<CardSummary | null>(null)
  const dfc = useDfcHoverFlip(
    hoverCard
      ? {
          name: hoverCard.name,
          imageUri: hoverCard.imageUri ?? null,
          isDoubleFaced: hoverCard.isDoubleFaced ?? false,
          backFaceName: hoverCard.backFaceName ?? null,
          backFaceImageUri: hoverCard.backFaceImageUri ?? null,
        }
      : null,
  )
  const resetDfcFlip = dfc.resetFlip

  // Stable hover handlers so memoized CardTiles don't re-render every time the
  // hovered card changes — without this, swapping hovered card replaces both
  // closures' identity for all ~100 visible tiles.
  const handleTileHover = useCallback(
    (c: CardSummary) => {
      setHoverCard((prev) => {
        if (prev?.name !== c.name) resetDfcFlip()
        return c
      })
    },
    [resetDfcFlip],
  )
  const handleTileLeave = useCallback(() => setHoverCard(null), [])

  const gridClass = compact ? styles.gridCompact : styles.grid

  if (cards.length === 0) {
    return (
      <div className={styles.gridScroll}>
        <div className={gridClass}>
          <div className={styles.emptyState}>No cards match the current filters.</div>
        </div>
      </div>
    )
  }

  return (
    <>
      <div className={styles.gridScroll}>
        <div className={gridClass}>
          {cards.map((card) => (
            <CardTile
              key={card.name}
              card={card}
              count={deckCards[card.name] ?? 0}
              onAdd={onAdd}
              onRemove={onRemove}
              onHover={handleTileHover}
              onLeave={handleTileLeave}
            />
          ))}
        </div>
        {hasMore && (
          <div className={styles.showMoreRow}>
            <button className={styles.secondaryButton} onClick={onShowMore} type="button">
              Show more
            </button>
          </div>
        )}
      </div>
      <HoverFollowPreview
        name={hoverCard ? (dfc.displayName ?? hoverCard.name) : null}
        imageUri={hoverCard ? (dfc.displayImageUri ?? hoverCard.imageUri ?? null) : null}
        overlay={dfc.hint}
        imageRotateDeg={landscapeImageRotateDeg(hoverCard)}
      />
    </>
  )
}

// Memoized: hover state is owned by the parent CardGrid, and a hovered-card
// change re-renders CardGrid. Without memo, every visible tile re-renders on
// every hover transition — the visible cause of the deckbuilder hover lag.
const CardTile = memo(function CardTile({
  card,
  count,
  onAdd,
  onRemove,
  onHover,
  onLeave,
}: {
  card: CardSummary
  count: number
  onAdd: (c: CardSummary) => void
  onRemove: (name: string) => void
  onHover: (c: CardSummary) => void
  onLeave: () => void
}) {
  // Lazy-load the image once the tile scrolls into view.
  const ref = useRef<HTMLDivElement | null>(null)
  const [visible, setVisible] = useState(false)
  useEffect(() => {
    if (!ref.current) return
    const obs = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setVisible(true)
            obs.disconnect()
            return
          }
        }
      },
      { rootMargin: '200px' }
    )
    obs.observe(ref.current)
    return () => obs.disconnect()
  }, [])

  const handleClick = (e: React.MouseEvent) => {
    if (e.shiftKey) {
      onRemove(card.name)
    } else {
      onAdd(card)
    }
  }

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault()
    onRemove(card.name)
  }

  return (
    <div
      ref={ref}
      className={styles.cardTile}
      draggable
      onDragStart={(e) => setCardDragData(e, card.name, 'catalog')}
      onClick={handleClick}
      onContextMenu={handleContextMenu}
      onMouseEnter={() => onHover(card)}
      onMouseLeave={onLeave}
    >
      {visible ? (
        <img
          className={styles.cardImage}
          src={resolveImageUrl(card)}
          alt={card.name}
          loading="lazy"
          onError={(e) => {
            // Fall back to the text tile if the image 404s.
            ;(e.currentTarget as HTMLImageElement).style.display = 'none'
          }}
        />
      ) : (
        <CardTextFallback card={card} />
      )}
      {count > 0 && <span className={styles.cardCountBadge}>{count}</span>}
    </div>
  )
})

function CardTextFallback({ card }: { card: CardSummary }) {
  const typeLabel = [card.supertypes, card.cardTypes, card.subtypes]
    .flat()
    .filter(Boolean)
    .map((t) => t[0]! + t.slice(1).toLowerCase())
    .join(' ')
  return (
    <div className={styles.cardTextFallback}>
      <span className={styles.cardFallbackName}>{card.name}</span>
      <ManaCost cost={card.manaCost || null} size={12} />
      <span className={styles.cardFallbackType}>{typeLabel}</span>
    </div>
  )
}

export function resolveImageUrl(card: CardSummary): string {
  return getCardImageUrl(card.name, card.imageUri ?? null, 'normal')
}
