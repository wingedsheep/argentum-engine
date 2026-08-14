import React, { useState, useEffect, useLayoutEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import { useGameStore } from '@/store/gameStore.ts'
import { useZoneCards, useStackCards, useZone, useCard, selectGameState } from '@/store/selectors.ts'
import { graveyard, exile, library } from '@/types'
import type { ClientCard, ClientDeckCard, ClientPlayer } from '@/types'
import { CARD_BACK_IMAGE_URL } from '@/utils/cardImages.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { useResponsiveContext, handleImageError, getStashCounters, getTimeCounters } from './shared'
import { DeckBrowser } from './DeckBrowser'
import { counterManaClass } from '@/assets/icons/keywords'
import { styles } from './styles'

/** Stable empty array so the `ownDeck` selector doesn't hand Zustand a new reference each render. */
const EMPTY_DECK: readonly ClientDeckCard[] = []

const CARD_RATIO = 1.4
const LABEL_HEIGHT = 14
// Deck + Graveyard + Exile are always present; extra "Plotted", "Paradigm", and "Suspended"
// piles appear only when the player has those cards in exile (see `pileCount` below).
const BASE_PILE_COUNT = 3
// Reserve room above the opponent's pile column for the absolutely-positioned
// Concede button so the top pile doesn't render under it.
const OPPONENT_TOP_RESERVED = 52
const MIN_PILE_WIDTH = 28

/**
 * Native tooltip for the Deck pile. When the top card is face up, lead with its name — that's
 * the reason the pile looks different, and it doubles as the accessible label for the image.
 */
function deckPileTitle(canBrowseDeck: boolean, isOwnDeck: boolean, topCard: ClientCard | null): string | undefined {
  if (!canBrowseDeck) return topCard ? `Top card: ${topCard.name}` : undefined
  const browse = isOwnDeck ? 'Your deck list and library (D)' : 'Library'
  return topCard ? `Top card: ${topCard.name} · ${browse}` : browse
}

/**
 * Deck, graveyard, exile — and, when present, a dedicated Plotted pile — display.
 */
export function ZonePile({ player, isOpponent = false }: { player: ClientPlayer; isOpponent?: boolean }) {
  const graveyardCards = useZoneCards(graveyard(player.playerId))
  const topGraveyardCard = graveyardCards[graveyardCards.length - 1]
  const exileCards = useZoneCards(exile(player.playerId))
  const topExileCard = exileCards[exileCards.length - 1]
  // Plotted cards (CR 718) are a public subset of exile: face-up, castable for free on a
  // later turn. Surface them in their own pile so both players — crucially the opponent —
  // can see at a glance which spells are waiting, instead of digging through the exile pile.
  const plottedCards = exileCards.filter((c) => c.isPlotted)
  const topPlottedCard = plottedCards[plottedCards.length - 1]
  // Active paradigm cards (Secrets of Strixhaven) are another public subset of exile: they stay
  // exiled and recast a free copy of themselves each precombat main. Surface them in their own pile
  // so both players can see the recurring threat without digging through the exile pile.
  const paradigmCards = exileCards.filter((c) => c.isParadigm)
  const topParadigmCard = paradigmCards[paradigmCards.length - 1]
  // Suspended cards (CR 702.62) are another public subset of exile: face-up, counting down at their
  // owner's upkeep toward a free cast. Surface them in their own pile so both players can see the
  // countdown without digging through the exile pile.
  const suspendedCards = exileCards.filter((c) => c.isSuspended)
  const topSuspendedCard = suspendedCards[suspendedCards.length - 1]
  const libraryZone = useZone(library(player.playerId))
  const libraryEntityIds = libraryZone?.cardIds ?? []
  // A library card only carries details when the server decided its identity is legitimately
  // known to this viewer: a public "play with the top card revealed" (Future Sight, Goblin Spy),
  // a private "you may look at the top card of your library any time" (Glarb, Lens of Clarity),
  // or a scry/surveil the viewer just performed. Whenever that holds for the *top* card, show it
  // face up on the pile rather than making the player open the browser to read it. Index 0 is the
  // top of the library — the same ordering the Library-order tab renders.
  const topLibraryCard = useCard(libraryEntityIds[0] ?? null)
  // The server sends `deck` only for the viewing player, so this is empty on every other seat's
  // pile — which is exactly what suppresses the deck-list tab there.
  const ownDeck = useGameStore((state) => {
    const gameState = selectGameState(state)
    if (!gameState || gameState.viewingPlayerId !== player.playerId) return EMPTY_DECK
    return gameState.deck ?? EMPTY_DECK
  })
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [browsingGraveyard, setBrowsingGraveyard] = useState(false)
  const [browsingExile, setBrowsingExile] = useState(false)
  const [browsingPlotted, setBrowsingPlotted] = useState(false)
  const [browsingParadigm, setBrowsingParadigm] = useState(false)
  const [browsingSuspended, setBrowsingSuspended] = useState(false)
  const [browsingLibrary, setBrowsingLibrary] = useState(false)
  const stackCards = useStackCards()

  const hasPlotted = plottedCards.length > 0
  const hasParadigm = paradigmCards.length > 0
  const hasSuspended = suspendedCards.length > 0
  const pileCount = BASE_PILE_COUNT + (hasPlotted ? 1 : 0) + (hasParadigm ? 1 : 0) + (hasSuspended ? 1 : 0)

  // Browser titles carry the owner's name — "Opponent's Graveyard" is ambiguous at a
  // multiplayer table.
  const ownerLabel = (zone: string) => (isOpponent ? `${player.name}'s ${zone}` : `Your ${zone}`)

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
    const totalGap = responsive.cardGap * (pileCount - 1)
    const totalLabel = LABEL_HEIGHT * pileCount
    const fixedOverhead = reservedTop + reservedBottom + totalGap + totalLabel

    const compute = (availableHeight: number) => {
      const heightForPiles = Math.max(0, availableHeight - fixedOverhead)
      const maxPileHeight = heightForPiles / pileCount
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
  }, [isOpponent, pileCount, responsive.pileWidth, responsive.cardGap, responsive.sectionGap])

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

  // The deck list is worth opening even on an empty library (you can still read what you played).
  const isOwnDeck = ownDeck.length > 0
  const canBrowseDeck = player.librarySize > 0 || isOwnDeck
  const showsTopLibraryCard = player.librarySize > 0 && topLibraryCard !== null

  // `D` opens your own deck — the shortcut lives here rather than in a global handler because
  // exactly one ZonePile on the table is the viewer's, and `ownDeck` is how we know which.
  useEffect(() => {
    if (!isOwnDeck) return
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'd' && e.key !== 'D') return
      if (e.metaKey || e.ctrlKey || e.altKey) return
      const active = document.activeElement
      if (active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement) return
      if (active instanceof HTMLElement && active.isContentEditable) return
      setBrowsingLibrary((open) => !open)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [isOwnDeck])

  return (
    <div ref={containerRef} style={{ ...styles.zonePile, gap: responsive.cardGap, minWidth: effectivePileWidth + 10, ...verticalOffset }}>
      {/* Library/Deck */}
      <div style={styles.zoneStack}>
        <div
          data-zone={isOpponent ? 'opponent-library' : 'player-library'}
          title={deckPileTitle(canBrowseDeck, isOwnDeck, showsTopLibraryCard ? topLibraryCard : null)}
          style={{
            ...styles.deckPile,
            ...pileStyle,
            cursor: canBrowseDeck ? 'pointer' : 'default',
            ...(showsTopLibraryCard ? styles.deckPileTopRevealed : null),
          }}
          onClick={() => { if (canBrowseDeck) setBrowsingLibrary(true) }}
          onMouseEnter={(e) => { if (showsTopLibraryCard) hoverCard(topLibraryCard.id, { x: e.clientX, y: e.clientY }) }}
          onMouseLeave={() => hoverCard(null)}
        >
          {player.librarySize > 0 ? (
            showsTopLibraryCard ? (
              <img
                src={getCardImageUrl(topLibraryCard.name, topLibraryCard.imageUri, 'normal')}
                alt={topLibraryCard.name}
                style={styles.pileImage}
                onError={(e) => handleImageError(e, topLibraryCard.name, 'normal')}
              />
            ) : (
              <img
                src={CARD_BACK_IMAGE_URL}
                alt="Library"
                style={styles.pileImage}
              />
            )
          ) : (
            <div style={styles.emptyPile} />
          )}
          {/* Badge the face-up top card so it doesn't read as "the whole deck is public" — the
              rest of the library is still a card back's worth of unknown. */}
          {showsTopLibraryCard && <div style={styles.deckTopRevealedBadge}>👁</div>}
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

      {/* Plotted (CR 718) — only present when this player has plotted spells waiting. A
          first-class, always-visible zone (for both players) so the opponent can read the
          threat without opening the exile pile. */}
      {hasPlotted && (
        <div style={styles.zoneStack}>
          <div
            data-plotted-id={player.playerId}
            style={{ ...styles.plottedPile, ...pileStyle, cursor: 'pointer' }}
            onClick={() => setBrowsingPlotted(true)}
            onMouseEnter={(e) => { if (topPlottedCard) hoverCard(topPlottedCard.id, { x: e.clientX, y: e.clientY }) }}
            onMouseLeave={() => hoverCard(null)}
          >
            {topPlottedCard && (
              <img
                src={getCardImageUrl(topPlottedCard.name, topPlottedCard.imageUri, 'normal')}
                alt={topPlottedCard.name}
                style={styles.pileImage}
                onError={(e) => handleImageError(e, topPlottedCard.name, 'normal')}
              />
            )}
            <div style={{ ...styles.pileCount, ...styles.plottedPileCount, fontSize: responsive.fontSize.small }}>
              {plottedCards.length}
            </div>
          </div>
          <span style={{ ...styles.zoneLabel, ...styles.plottedZoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>
            ⚐ Plotted
          </span>
        </div>
      )}

      {/* Paradigm (Secrets of Strixhaven) — only present when this player has active paradigm cards
          in exile. A first-class, always-visible zone (for both players) so the recurring free-cast
          threat can be read without opening the exile pile. */}
      {hasParadigm && (
        <div style={styles.zoneStack}>
          <div
            data-paradigm-id={player.playerId}
            style={{ ...styles.paradigmPile, ...pileStyle, cursor: 'pointer' }}
            onClick={() => setBrowsingParadigm(true)}
            onMouseEnter={(e) => { if (topParadigmCard) hoverCard(topParadigmCard.id, { x: e.clientX, y: e.clientY }) }}
            onMouseLeave={() => hoverCard(null)}
          >
            {topParadigmCard && (
              <img
                src={getCardImageUrl(topParadigmCard.name, topParadigmCard.imageUri, 'normal')}
                alt={topParadigmCard.name}
                style={styles.pileImage}
                onError={(e) => handleImageError(e, topParadigmCard.name, 'normal')}
              />
            )}
            <div style={{ ...styles.pileCount, ...styles.paradigmPileCount, fontSize: responsive.fontSize.small }}>
              {paradigmCards.length}
            </div>
          </div>
          <span style={{ ...styles.zoneLabel, ...styles.paradigmZoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>
            ◈ Paradigm
          </span>
        </div>
      )}

      {/* Suspended (CR 702.62) — only present when this player has actively-suspended cards in
          exile (>=1 time counter). A first-class, always-visible zone (for both players) so the
          countdown toward a free cast can be read without opening the exile pile. */}
      {hasSuspended && (
        <div style={styles.zoneStack}>
          <div
            data-suspended-id={player.playerId}
            style={{ ...styles.suspendedPile, ...pileStyle, cursor: 'pointer' }}
            onClick={() => setBrowsingSuspended(true)}
            onMouseEnter={(e) => { if (topSuspendedCard) hoverCard(topSuspendedCard.id, { x: e.clientX, y: e.clientY }) }}
            onMouseLeave={() => hoverCard(null)}
          >
            {topSuspendedCard && (
              <img
                src={getCardImageUrl(topSuspendedCard.name, topSuspendedCard.imageUri, 'normal')}
                alt={topSuspendedCard.name}
                style={styles.pileImage}
                onError={(e) => handleImageError(e, topSuspendedCard.name, 'normal')}
              />
            )}
            {/* Live time-counter countdown for the top card — same info Impending shows
                continuously on the battlefield, kept visible here without opening the pile. */}
            {topSuspendedCard && getTimeCounters(topSuspendedCard) > 0 && (
              <div style={{
                position: 'absolute',
                top: 2,
                left: 2,
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                backgroundColor: 'rgba(40, 30, 70, 0.95)',
                border: '1px solid rgba(170, 140, 230, 0.7)',
                borderRadius: 4,
                padding: '1px 3px',
                color: '#cbb6f0',
                fontWeight: 700,
                fontSize: 10,
                lineHeight: 1,
                textShadow: '0 0 4px rgba(170, 140, 230, 0.8)',
              }}>
                <i className={`ms ms-${counterManaClass.TIME}`} style={{ fontSize: 9 }} />
                {getTimeCounters(topSuspendedCard)}
              </div>
            )}
            <div style={{ ...styles.pileCount, ...styles.suspendedPileCount, fontSize: responsive.fontSize.small }}>
              {suspendedCards.length}
            </div>
          </div>
          <span style={{ ...styles.zoneLabel, ...styles.suspendedZoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>
            ⏳ Suspended
          </span>
        </div>
      )}

      {/* The browsers are position:fixed full-screen overlays, but an opponent's
          ZonePile sits inside the multiplayer board strip whose translateX transform
          makes `fixed` resolve against the (overflow-hidden, possibly off-screen)
          strip cell instead of the viewport. Portal them to <body> so browsing an
          opponent's graveyard/exile/library works identically in every layout. */}
      {browsingGraveyard && createPortal(
        <GraveyardBrowser
          cards={graveyardCards}
          ownerLabel={ownerLabel('Graveyard')}
          onClose={() => setBrowsingGraveyard(false)}
        />,
        document.body,
      )}
      {browsingExile && createPortal(
        <ExileBrowser
          cards={exileCards}
          ownerLabel={ownerLabel('Exile')}
          onClose={() => setBrowsingExile(false)}
        />,
        document.body,
      )}
      {browsingPlotted && createPortal(
        <PlottedBrowser
          cards={plottedCards}
          ownerLabel={ownerLabel('Plotted Spells')}
          onClose={() => setBrowsingPlotted(false)}
        />,
        document.body,
      )}
      {browsingParadigm && createPortal(
        <ParadigmBrowser
          cards={paradigmCards}
          ownerLabel={ownerLabel('Paradigm Cards')}
          onClose={() => setBrowsingParadigm(false)}
        />,
        document.body,
      )}
      {browsingSuspended && createPortal(
        <SuspendedBrowser
          cards={suspendedCards}
          ownerLabel={ownerLabel('Suspended Cards')}
          onClose={() => setBrowsingSuspended(false)}
        />,
        document.body,
      )}
      {browsingLibrary && createPortal(
        <DeckBrowser
          ownerLabel={isOpponent ? `${player.name}'s` : 'Your'}
          entityIds={libraryEntityIds}
          deck={ownDeck}
          onClose={() => setBrowsingLibrary(false)}
        />,
        document.body,
      )}
    </div>
  )
}

/**
 * Full-screen overlay for browsing graveyard cards.
 */
function GraveyardBrowser({ cards, ownerLabel, onClose }: { cards: readonly ClientCard[], ownerLabel: string, onClose: () => void }) {
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
          <h2 style={styles.graveyardBrowserTitle}>{ownerLabel} ({cards.length})</h2>
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
function ExileBrowser({ cards, ownerLabel, onClose }: { cards: readonly ClientCard[], ownerLabel: string, onClose: () => void }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [minimized, setMinimized] = useState(false)

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)

  // Plotted cards (CR 718) are still part of exile, but they carry future value — float
  // them to the front and badge them so they stand out among morphs, impulse-draws, etc.
  const plottedCount = cards.filter((c) => c.isPlotted).length
  const orderedCards = plottedCount > 0
    ? [...cards].sort((a, b) => Number(b.isPlotted ?? false) - Number(a.isPlotted ?? false))
    : cards

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
          <h2 style={styles.exileBrowserTitle}>
            {ownerLabel} ({cards.length})
            {plottedCount > 0 && (
              <span style={styles.exilePlottedCount}> · {plottedCount} plotted</span>
            )}
          </h2>
          <button style={styles.exileCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.exileCardGrid}>
          {orderedCards.map((card) => (
            <div
              key={card.id}
              style={{
                width: cardWidth,
                height: cardHeight,
                borderRadius: 6,
                overflow: 'hidden',
                flexShrink: 0,
                position: 'relative',
                boxShadow: card.isPlotted ? '0 0 0 2px #f5d76e, 0 0 14px rgba(245, 215, 110, 0.6)' : 'none',
              }}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
              {card.isPlotted && <div style={styles.plottedGridBadge}>Plotted</div>}
              {getStashCounters(card) > 0 && (
                <div style={styles.stashGridBadge}>
                  <i className={`ms ms-${counterManaClass.STASH}`} /> {getStashCounters(card)}
                </div>
              )}
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
 * Full-screen overlay for browsing plotted spells (CR 718) — the public, face-up subset of
 * exile that can be cast for free on a later turn. Used for both the viewer's and the
 * opponent's Plotted pile, so the threat of an incoming free spell is always inspectable.
 */
function PlottedBrowser({
  cards,
  ownerLabel,
  onClose,
}: {
  cards: readonly ClientCard[]
  ownerLabel: string
  onClose: () => void
}) {
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
          backgroundColor: '#a07b16',
          color: '#fff8e1',
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
        ↑ Return to Plotted
      </button>
    )
  }

  return (
    <div style={styles.plottedOverlay} onClick={onClose}>
      <div style={styles.plottedBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.exileBrowserHeader}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <h2 style={styles.plottedBrowserTitle}>⚐ {ownerLabel} ({cards.length})</h2>
            <span style={{ color: '#bfa14a', fontSize: 11, letterSpacing: 0.5 }}>
              Castable for free on a later turn (CR 718)
            </span>
          </div>
          <button style={styles.plottedCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.exileCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{
                width: cardWidth,
                height: cardHeight,
                borderRadius: 6,
                overflow: 'hidden',
                flexShrink: 0,
                position: 'relative',
                boxShadow: '0 0 0 2px #f5d76e, 0 0 14px rgba(245, 215, 110, 0.6)',
              }}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
              <div style={styles.plottedGridBadge}>Plotted</div>
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#a07b16',
              color: '#fff8e1',
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
 * Full-screen overlay for browsing active paradigm cards (Secrets of Strixhaven).
 *
 * Paradigm cards stay face-up in exile and recast a free copy of themselves at the start of each of
 * their owner's precombat main phases. They're public knowledge, so both players can open this view.
 */
function ParadigmBrowser({
  cards,
  ownerLabel,
  onClose,
}: {
  cards: readonly ClientCard[]
  ownerLabel: string
  onClose: () => void
}) {
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
          backgroundColor: '#15756d',
          color: '#e6fffb',
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
        ↑ Return to Paradigm
      </button>
    )
  }

  return (
    <div style={styles.paradigmOverlay} onClick={onClose}>
      <div style={styles.paradigmBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.exileBrowserHeader}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <h2 style={styles.paradigmBrowserTitle}>◈ {ownerLabel} ({cards.length})</h2>
            <span style={{ color: '#4ec4b8', fontSize: 11, letterSpacing: 0.5 }}>
              Casts a free copy of itself each precombat main phase (Paradigm)
            </span>
          </div>
          <button style={styles.paradigmCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.exileCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{
                width: cardWidth,
                height: cardHeight,
                borderRadius: 6,
                overflow: 'hidden',
                flexShrink: 0,
                position: 'relative',
                boxShadow: '0 0 0 2px #5fded0, 0 0 14px rgba(95, 222, 208, 0.6)',
              }}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
              <div style={styles.paradigmGridBadge}>Paradigm</div>
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#15756d',
              color: '#e6fffb',
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
 * Full-screen overlay for browsing actively-suspended cards (CR 702.62).
 *
 * Suspended cards sit face-up in exile, counting down a time counter at their owner's upkeep
 * until the last is removed and they may be cast for free. They're public knowledge, so both
 * players can open this view.
 */
function SuspendedBrowser({
  cards,
  ownerLabel,
  onClose,
}: {
  cards: readonly ClientCard[]
  ownerLabel: string
  onClose: () => void
}) {
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
          backgroundColor: '#5a4fb3',
          color: '#e9e6ff',
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
        ↑ Return to Suspended
      </button>
    )
  }

  return (
    <div style={styles.suspendedOverlay} onClick={onClose}>
      <div style={styles.suspendedBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.exileBrowserHeader}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <h2 style={styles.suspendedBrowserTitle}>⏳ {ownerLabel} ({cards.length})</h2>
            <span style={{ color: '#a89aef', fontSize: 11, letterSpacing: 0.5 }}>
              Removes a time counter each upkeep; cast for free when the last one is gone (Suspend)
            </span>
          </div>
          <button style={styles.suspendedCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.exileCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{
                width: cardWidth,
                height: cardHeight,
                borderRadius: 6,
                overflow: 'hidden',
                flexShrink: 0,
                position: 'relative',
                boxShadow: '0 0 0 2px #9a8cef, 0 0 14px rgba(154, 140, 239, 0.6)',
              }}
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
              <div style={styles.suspendedGridBadge}>Suspended</div>
              {/* Same time-counter badge Impending permanents show on the battlefield —
                  icon + remaining count, so the countdown reads identically everywhere. */}
              {getTimeCounters(card) > 0 && (
                <div style={{
                  ...styles.timeCounterBadge,
                  fontSize: responsive.badges.counterTextFontSize,
                  padding: responsive.badges.badgePadding,
                }}>
                  <i className={`ms ms-${counterManaClass.TIME}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
                  <span style={{ fontWeight: 700 }}>{getTimeCounters(card)}</span>
                </div>
              )}
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#5a4fb3',
              color: '#e9e6ff',
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
