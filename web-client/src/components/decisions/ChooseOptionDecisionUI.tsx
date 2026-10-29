import { useState, useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, ChooseOptionDecision } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { optionIcon } from '@/assets/icons/options'
import { DecisionCardPreview } from './DecisionComponents'
import styles from './DecisionUI.module.css'

/** Lists this long or shorter are shown without a search box. */
const SEARCH_THRESHOLD = 8

/**
 * Choose option decision - select from a list of string options.
 * Uses a searchable scrollable list for large option sets (e.g., creature types).
 */

export function ChooseOptionDecisionUI({
  decision,
}: {
  decision: ChooseOptionDecision
}) {
  // Prefill the search box from defaultSearch only for long lists (e.g. ~280 creature types),
  // where narrowing is essential. Short lists (color words / land types) stay fully visible so
  // the player sees every option — defaultSearch still pre-selects one via initialIndex below.
  const [filter, setFilter] = useState(
    decision.options.length > 20 ? (decision.defaultSearch ?? '') : '',
  )
  const [minimized, setMinimized] = useState(false)
  const [hoveredPreviewCard, setHoveredPreviewCard] = useState<{ name: string; imageUri: string | null | undefined } | null>(null)

  // Auto-select: defaultSearch match, or the option with the most cards
  const initialIndex = useMemo(() => {
    if (decision.defaultSearch) {
      const idx = decision.options.findIndex((opt) => opt.toLowerCase() === decision.defaultSearch!.toLowerCase())
      if (idx >= 0) return idx
    }
    if (decision.optionCardIds) {
      let bestIndex = -1
      let bestCount = 0
      for (let i = 0; i < decision.options.length; i++) {
        const count = decision.optionCardIds[i]?.length ?? 0
        if (count > bestCount) {
          bestCount = count
          bestIndex = i
        }
      }
      if (bestIndex >= 0) return bestIndex
    }
    return null
  }, [decision.defaultSearch, decision.options, decision.optionCardIds])

  const [selectedIndex, setSelectedIndex] = useState<number | null>(initialIndex)
  const [isHoveringSource, setIsHoveringSource] = useState(false)
  const submitOptionDecision = useGameStore((s) => s.submitOptionDecision)
  const submitCancelDecision = useGameStore((s) => s.submitCancelDecision)
  const gameState = useGameStore((s) => s.gameState)
  const responsive = useResponsive()

  // Source card image for context
  const sourceCard = decision.context.sourceId ? gameState?.cards[decision.context.sourceId] : undefined
  const sourceCardName = decision.context.sourceName ?? sourceCard?.name
  const sourceCardImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  const hasCardIds = !!decision.optionCardIds
  // A "tiled" choice has per-option metadata with at least one icon. This is the
  // visual fork in the road for card-defined mode choices (e.g. Sieges).
  const hasOptionMetadata = (decision.optionMetadata?.length ?? 0) === decision.options.length
    && (decision.optionMetadata?.length ?? 0) > 0
  const useTiledLayout = hasOptionMetadata
    && decision.optionMetadata!.some((m) => !!optionIcon(m.iconKey))

  const filteredOptions = useMemo(() => {
    const mapped = decision.options.map((opt, i) => {
      const cardCount = decision.optionCardIds?.[i]?.length
      const label = cardCount != null ? `${opt} (${cardCount})` : opt
      return { label, index: i }
    })
    if (!filter) return mapped
    const lower = filter.toLowerCase()
    return mapped.filter((opt) => opt.label.toLowerCase().includes(lower))
  }, [decision.options, decision.optionCardIds, filter])

  // Get card previews for the selected option
  const previewCards = useMemo(() => {
    if (selectedIndex === null || !decision.optionCardIds || !gameState) return []
    const cardIds = decision.optionCardIds[selectedIndex] ?? []
    const results: { id: EntityId; name: string; imageUri: string | null | undefined }[] = []
    for (const id of cardIds) {
      const card = gameState.cards[id]
      if (card) {
        results.push({ id, name: card.name, imageUri: card.imageUri })
      }
    }
    return results
  }, [selectedIndex, decision.optionCardIds, gameState])

  const handleConfirm = () => {
    if (selectedIndex !== null) {
      submitOptionDecision(decision.id, selectedIndex)
    }
  }

  if (minimized) {
    return (
      <button
        className={styles.floatingReturnButton}
        onClick={() => setMinimized(false)}
      >
        Return to {decision.prompt}
      </button>
    )
  }

  return (
    <div className={styles.overlay}>
      {/* Source card image */}
      {sourceCardImageUrl && (
        <img
          src={sourceCardImageUrl}
          alt={`Source: ${sourceCardName ?? 'card'}`}
          className={styles.bannerCardImage}
          onMouseEnter={() => setIsHoveringSource(true)}
          onMouseLeave={() => setIsHoveringSource(false)}
        />
      )}

      <h2 className={styles.title}>
        {decision.prompt}
      </h2>

      {sourceCardName && (
        <p className={styles.sourceLabel}>
          {sourceCardName}
        </p>
      )}

      {/* Search filter — only for long lists (creature types, card names); a handful of options
          (a vote, a pile, a mode) reads at a glance. Hidden for the tiled (icon-backed) layout. */}
      {!useTiledLayout && decision.options.length > SEARCH_THRESHOLD && (
        <input
          type="text"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Search..."
          className={styles.optionSearchInput}
          autoFocus
        />
      )}

      {/* Visual tile layout for card-defined mode choices that supply icons. */}
      {useTiledLayout ? (
        <div className={styles.optionTiles}>
          {decision.options.map((label, idx) => {
            const meta = decision.optionMetadata![idx]
            const iconUrl = optionIcon(meta?.iconKey)
            const isSelected = selectedIndex === idx
            return (
              <button
                key={idx}
                onClick={() => setSelectedIndex(idx)}
                className={`${styles.optionTile} ${isSelected ? styles.optionTileSelected : ''}`}
              >
                {iconUrl && (
                  <img src={iconUrl} alt="" className={styles.optionTileIcon} />
                )}
                <span className={styles.optionTileLabel}>{label}</span>
                {meta?.description && (
                  <span className={styles.optionTileDescription}>{meta.description}</span>
                )}
              </button>
            )
          })}
        </div>
      ) : (
        <div className={styles.optionList}>
          {filteredOptions.map((opt) => {
            const description = decision.optionMetadata?.[opt.index]?.description
            return (
              <button
                key={opt.index}
                onClick={() => setSelectedIndex(opt.index)}
                className={`${styles.optionItem} ${selectedIndex === opt.index ? styles.optionItemSelected : ''}`}
              >
                <span>{opt.label}</span>
                {description && (
                  <span className={styles.optionTileDescription}> — {description}</span>
                )}
              </button>
            )
          })}
          {filteredOptions.length === 0 && (
            <p className={styles.noCardsMessage}>No matching options</p>
          )}
        </div>
      )}

      {/* Card previews for selected option */}
      {hasCardIds && previewCards.length > 0 && (
        <div className={styles.optionCardPreview}>
          {previewCards.map((card) => {
            const imgUrl = getCardImageUrl(card.name, card.imageUri)
            return (
              <div
                key={card.id}
                className={styles.optionPreviewCard}
                onMouseEnter={() => setHoveredPreviewCard({ name: card.name, imageUri: card.imageUri })}
                onMouseLeave={() => setHoveredPreviewCard(null)}
              >
                <img
                  src={imgUrl}
                  alt={card.name}
                  className={styles.optionPreviewImage}
                  onError={(e) => {
                    e.currentTarget.style.display = 'none'
                    const fallback = e.currentTarget.nextElementSibling as HTMLElement
                    if (fallback) fallback.style.display = 'flex'
                  }}
                />
                <div className={styles.optionPreviewFallback}>
                  <span className={styles.cardFallbackName}>{card.name}</span>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Hover-to-zoom preview (source card or option card) */}
      {isHoveringSource && sourceCardName && !responsive.isMobile && (
        <DecisionCardPreview cardName={sourceCardName} imageUri={sourceCard?.imageUri} />
      )}
      {!isHoveringSource && hoveredPreviewCard && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredPreviewCard.name} imageUri={hoveredPreviewCard.imageUri} />
      )}

      {/* Action buttons */}
      <div className={styles.optionButtonRow}>
        <button
          onClick={() => setMinimized(true)}
          className={styles.viewBattlefieldButton}
        >
          View Battlefield
        </button>
        {decision.canCancel && (
          <button
            onClick={() => submitCancelDecision(decision.id)}
            className={styles.confirmButton}
          >
            Cancel
          </button>
        )}
        <button
          onClick={handleConfirm}
          disabled={selectedIndex === null}
          className={styles.confirmButton}
        >
          Confirm
        </button>
      </div>
    </div>
  )
}
