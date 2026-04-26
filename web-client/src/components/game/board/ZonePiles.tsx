import React, { useState, useEffect, useLayoutEffect, useRef } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useZoneCards, useStackCards, useZone, selectGameState } from '@/store/selectors.ts'
import { graveyard, exile, library } from '@/types'
import type { ClientCard, ClientPlayer, EntityId } from '@/types'
import { CARD_BACK_IMAGE_URL } from '@/utils/cardImages.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { useResponsiveContext, handleImageError } from './shared'
import { styles } from './styles'

const CARD_RATIO = 1.4
const LABEL_HEIGHT = 14
const PILE_COUNT = 3
// Reserve room above the opponent's pile column for the absolutely-positioned
// Concede button so the top pile doesn't render under it.
const OPPONENT_TOP_RESERVED = 52
const MIN_PILE_WIDTH = 28

/**
 * Deck, graveyard, and exile pile display.
 */
export function ZonePile({ player, isOpponent = false }: { player: ClientPlayer; isOpponent?: boolean }) {
  const graveyardCards = useZoneCards(graveyard(player.playerId))
  const topGraveyardCard = graveyardCards[graveyardCards.length - 1]
  const exileCards = useZoneCards(exile(player.playerId))
  const topExileCard = exileCards[exileCards.length - 1]
  const libraryZone = useZone(library(player.playerId))
  const libraryEntityIds = libraryZone?.cardIds ?? []
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [browsingGraveyard, setBrowsingGraveyard] = useState(false)
  const [browsingExile, setBrowsingExile] = useState(false)
  const [browsingLibrary, setBrowsingLibrary] = useState(false)
  const stackCards = useStackCards()

  // Shrink piles to fit the column's actual height. The viewport-derived
  // pileWidth doesn't know about (a) the opponent's Concede button, which
  // overlays the top of grid row 2, or (b) tighter row heights from short
  // viewports. Without this, the third pile (Exile) overflows and is clipped
  // by the opponentArea/playerArea overflow:hidden.
  const containerRef = useRef<HTMLDivElement | null>(null)
  const [fittedPileWidth, setFittedPileWidth] = useState(responsive.pileWidth)

  useLayoutEffect(() => {
    const el = containerRef.current
    const parent = el?.parentElement
    if (!parent) return

    const reservedTop = isOpponent ? OPPONENT_TOP_RESERVED : 0
    const reservedBottom = isOpponent ? 0 : responsive.sectionGap * 2
    const totalGap = responsive.cardGap * (PILE_COUNT - 1)
    const totalLabel = LABEL_HEIGHT * PILE_COUNT
    const fixedOverhead = reservedTop + reservedBottom + totalGap + totalLabel

    const compute = (availableHeight: number) => {
      const heightForPiles = Math.max(0, availableHeight - fixedOverhead)
      const maxPileHeight = heightForPiles / PILE_COUNT
      const widthFromHeight = Math.floor(maxPileHeight / CARD_RATIO)
      const next = Math.max(MIN_PILE_WIDTH, Math.min(responsive.pileWidth, widthFromHeight))
      setFittedPileWidth(next)
    }

    compute(parent.clientHeight)
    const obs = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry) compute(entry.contentRect.height)
    })
    obs.observe(parent)
    return () => obs.disconnect()
  }, [isOpponent, responsive.pileWidth, responsive.cardGap, responsive.sectionGap])

  const effectivePileWidth = fittedPileWidth
  const effectivePileHeight = Math.round(effectivePileWidth * CARD_RATIO)

  // Find any graveyard cards that are being targeted by spells on the stack
  const targetedGraveyardCards = React.useMemo(() => {
    const targeted: ClientCard[] = []
    const seenIds = new Set<string>()
    for (const stackCard of stackCards) {
      if (!stackCard.targets) continue
      for (const target of stackCard.targets) {
        if (target.type === 'Card') {
          const card = graveyardCards.find((c) => c.id === target.cardId)
          if (card && !seenIds.has(card.id)) {
            seenIds.add(card.id)
            targeted.push(card)
          }
        }
      }
    }
    return targeted
  }, [stackCards, graveyardCards])

  const pileStyle = {
    width: effectivePileWidth,
    height: effectivePileHeight,
    borderRadius: responsive.isMobile ? 4 : 6,
  }

  // Position piles at the far end of each player's battlefield row (opponent:
  // top of row 2, below the Concede button; player: bottom of row 4, above
  // row 5's hand reservation and the Pass button). This keeps them clear of
  // the center HUD in row 3 — previously a margin offset pulled them toward
  // the center "to clear the buttons", but that intruded into row 3 and the
  // HUD visibly overlapped the deck/graveyard/exile.
  const verticalOffset = isOpponent
    ? { alignSelf: 'flex-start' as const }
    : { alignSelf: 'flex-end' as const, marginBottom: responsive.sectionGap * 2 }

  return (
    <div ref={containerRef} style={{ ...styles.zonePile, gap: responsive.cardGap, minWidth: effectivePileWidth + 10, ...verticalOffset }}>
      {/* Library/Deck */}
      <div style={styles.zoneStack}>
        <div
          data-zone={isOpponent ? 'opponent-library' : 'player-library'}
          style={{ ...styles.deckPile, ...pileStyle, cursor: player.librarySize > 0 ? 'pointer' : 'default' }}
          onClick={() => { if (player.librarySize > 0) setBrowsingLibrary(true) }}
        >
          {player.librarySize > 0 ? (
            <img
              src={CARD_BACK_IMAGE_URL}
              alt="Library"
              style={styles.pileImage}
            />
          ) : (
            <div style={styles.emptyPile} />
          )}
          <div style={{ ...styles.pileCount, fontSize: responsive.fontSize.small }}>{player.librarySize}</div>
        </div>
        <span style={{ ...styles.zoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>Deck</span>
      </div>

      {/* Graveyard */}
      <div style={styles.zoneStack}>
        <div
          data-graveyard-id={player.playerId}
          style={{ ...styles.graveyardPile, ...pileStyle, cursor: graveyardCards.length > 0 ? 'pointer' : 'default' }}
          onClick={() => { if (graveyardCards.length > 0) setBrowsingGraveyard(true) }}
          onMouseEnter={(e) => { if (topGraveyardCard) hoverCard(topGraveyardCard.id, { x: e.clientX, y: e.clientY }) }}
          onMouseLeave={() => hoverCard(null)}
        >
          {topGraveyardCard ? (
            <img
              src={getCardImageUrl(topGraveyardCard.name, topGraveyardCard.imageUri, 'normal')}
              alt={topGraveyardCard.name}
              style={{ ...styles.pileImage, opacity: 0.8 }}
              onError={(e) => handleImageError(e, topGraveyardCard.name, 'normal')}
            />
          ) : (
            <div style={styles.emptyPile} />
          )}
          {player.graveyardSize > 0 && (
            <div style={{ ...styles.pileCount, fontSize: responsive.fontSize.small }}>{player.graveyardSize}</div>
          )}
          {/* Show targeted cards fanned out when a spell is targeting cards in this graveyard */}
          {targetedGraveyardCards.map((card, index) => {
            const fanOffset = targetedGraveyardCards.length > 1
              ? (index - (targetedGraveyardCards.length - 1) / 2) * (responsive.isMobile ? 14 : 20)
              : 0
            return (
              <div
                key={card.id}
                data-card-id={card.id}
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: '100%',
                  zIndex: 10 + index,
                  boxShadow: '0 0 12px 4px rgba(255, 136, 0, 0.8)',
                  borderRadius: responsive.isMobile ? 4 : 6,
                  transform: `translateX(${fanOffset}px)`,
                }}
              >
                <img
                  src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                  alt={card.name}
                  style={{ ...styles.pileImage, borderRadius: responsive.isMobile ? 4 : 6 }}
                  onError={(e) => handleImageError(e, card.name, 'normal')}
                />
              </div>
            )
          })}
        </div>
        <span style={{ ...styles.zoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>Graveyard</span>
      </div>

      {/* Exile */}
      <div style={styles.zoneStack}>
        <div
          data-exile-id={player.playerId}
          style={{ ...styles.exilePile, ...pileStyle, cursor: exileCards.length > 0 ? 'pointer' : 'default' }}
          onClick={() => { if (exileCards.length > 0) setBrowsingExile(true) }}
          onMouseEnter={(e) => { if (topExileCard) hoverCard(topExileCard.id, { x: e.clientX, y: e.clientY }) }}
          onMouseLeave={() => hoverCard(null)}
        >
          {topExileCard ? (
            <img
              src={getCardImageUrl(topExileCard.name, topExileCard.imageUri, 'normal')}
              alt={topExileCard.name}
              style={{ ...styles.pileImage, opacity: 0.7 }}
              onError={(e) => handleImageError(e, topExileCard.name, 'normal')}
            />
          ) : (
            <div style={styles.emptyPile} />
          )}
          {player.exileSize > 0 && (
            <div style={{ ...styles.pileCount, fontSize: responsive.fontSize.small }}>{player.exileSize}</div>
          )}
        </div>
        <span style={{ ...styles.zoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>Exile</span>
      </div>

      {browsingGraveyard && (
        <GraveyardBrowser cards={graveyardCards} onClose={() => setBrowsingGraveyard(false)} />
      )}
      {browsingExile && (
        <ExileBrowser cards={exileCards} onClose={() => setBrowsingExile(false)} />
      )}
      {browsingLibrary && (
        <LibraryBrowser
          ownerLabel={isOpponent ? "Opponent's Library" : 'Your Library'}
          entityIds={libraryEntityIds}
          onClose={() => setBrowsingLibrary(false)}
        />
      )}
    </div>
  )
}

/**
 * Full-screen overlay for browsing graveyard cards.
 */
function GraveyardBrowser({ cards, onClose }: { cards: readonly ClientCard[], onClose: () => void }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [minimized, setMinimized] = useState(false)

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (minimized) {
          setMinimized(false)
        } else {
          onClose()
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, minimized])

  // When minimized, show floating button to restore
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#1e40af',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        ↑ Return to Graveyard
      </button>
    )
  }

  return (
    <div style={styles.graveyardOverlay} onClick={onClose}>
      <div style={styles.graveyardBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.graveyardBrowserHeader}>
          <h2 style={styles.graveyardBrowserTitle}>Graveyard ({cards.length})</h2>
          <button style={styles.graveyardCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.graveyardCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{ width: cardWidth, height: cardHeight, borderRadius: 6, overflow: 'hidden', flexShrink: 0 }}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
            </div>
          ))}
        </div>
        {/* Action buttons */}
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#1e40af',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            View Battlefield
          </button>
          <button
            onClick={onClose}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#333',
              color: '#aaa',
              border: '1px solid #555',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Full-screen overlay for browsing exile cards.
 */
function ExileBrowser({ cards, onClose }: { cards: readonly ClientCard[], onClose: () => void }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [minimized, setMinimized] = useState(false)

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (minimized) {
          setMinimized(false)
        } else {
          onClose()
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, minimized])

  // When minimized, show floating button to restore
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#7c3aed',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        ↑ Return to Exile
      </button>
    )
  }

  return (
    <div style={styles.exileOverlay} onClick={onClose}>
      <div style={styles.exileBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.exileBrowserHeader}>
          <h2 style={styles.exileBrowserTitle}>Exile ({cards.length})</h2>
          <button style={styles.exileCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.exileCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{ width: cardWidth, height: cardHeight, borderRadius: 6, overflow: 'hidden', flexShrink: 0 }}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
            </div>
          ))}
        </div>
        {/* Action buttons */}
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#7c3aed',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            View Battlefield
          </button>
          <button
            onClick={onClose}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#333',
              color: '#aaa',
              border: '1px solid #555',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Full-screen overlay for browsing a library.
 *
 * Cards that have been revealed to the viewer (Scry, Surveil, look-at-top-N, etc.)
 * appear face-up in their library positions. All other slots render as card backs.
 * The order matches the actual library order — top of deck first. A shuffle on the
 * server clears all reveals, so a freshly shuffled library shows entirely face-down.
 */
function LibraryBrowser({
  ownerLabel,
  entityIds,
  onClose,
}: {
  ownerLabel: string
  entityIds: readonly EntityId[]
  onClose: () => void
}) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const cardsMap = useGameStore((state) => selectGameState(state)?.cards)
  const responsive = useResponsiveContext()
  const [minimized, setMinimized] = useState(false)

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (minimized) {
          setMinimized(false)
        } else {
          onClose()
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, minimized])

  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#1e3a8a',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        ↑ Return to Library
      </button>
    )
  }

  const revealedCount = entityIds.reduce((acc, id) => acc + (cardsMap?.[id] ? 1 : 0), 0)

  return (
    <div style={styles.libraryOverlay} onClick={onClose}>
      <div style={styles.libraryBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.libraryBrowserHeader}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <h2 style={styles.libraryBrowserTitle}>
              {ownerLabel} ({entityIds.length}
              {revealedCount > 0 ? ` · ${revealedCount} known` : ''})
            </h2>
            <span style={{ color: '#64748b', fontSize: 11, letterSpacing: 0.5 }}>
              Reading order: top → bottom, left to right
            </span>
          </div>
          <button style={styles.libraryCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.libraryCardGrid}>
          {entityIds.map((id, index) => {
            const card = cardsMap?.[id]
            const isTop = index === 0
            const isBottom = index === entityIds.length - 1 && entityIds.length > 1
            const accent = isTop ? '#fde68a' : isBottom ? '#fb923c' : null
            return (
              <div
                key={id}
                style={{
                  width: cardWidth,
                  height: cardHeight,
                  borderRadius: 6,
                  overflow: 'hidden',
                  flexShrink: 0,
                  position: 'relative',
                  boxShadow: accent ? `0 0 0 2px ${accent}, 0 0 14px ${accent}66` : 'none',
                }}
                onMouseEnter={(e) => { if (card) hoverCard(card.id, { x: e.clientX, y: e.clientY }) }}
                onMouseLeave={() => hoverCard(null)}
              >
                {card ? (
                  <img
                    src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                    alt={card.name}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    onError={(e) => handleImageError(e, card.name, 'normal')}
                  />
                ) : (
                  <img
                    src={CARD_BACK_IMAGE_URL}
                    alt="Card back"
                    style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: 0.85 }}
                  />
                )}
                {/* Position badge — shown on every card */}
                <div
                  style={{
                    position: 'absolute',
                    top: 4,
                    left: 4,
                    fontSize: 10,
                    color: '#bfdbfe',
                    backgroundColor: 'rgba(0,0,0,0.65)',
                    padding: '2px 6px',
                    borderRadius: 3,
                    pointerEvents: 'none',
                  }}
                >
                  #{index + 1}
                </div>
                {/* Anchor banner across the bottom of the first/last card so orientation
                    is unambiguous regardless of how the grid wraps */}
                {accent && (
                  <div
                    style={{
                      position: 'absolute',
                      bottom: 0,
                      left: 0,
                      right: 0,
                      backgroundColor: accent,
                      color: '#1c1917',
                      fontSize: 11,
                      fontWeight: 700,
                      letterSpacing: 1,
                      textAlign: 'center',
                      padding: '4px 0',
                      textTransform: 'uppercase',
                      pointerEvents: 'none',
                    }}
                  >
                    {isTop ? 'Top · Next draw' : 'Bottom'}
                  </div>
                )}
              </div>
            )
          })}
        </div>
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#1e3a8a',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            View Battlefield
          </button>
          <button
            onClick={onClose}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#333',
              color: '#aaa',
              border: '1px solid #555',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}
