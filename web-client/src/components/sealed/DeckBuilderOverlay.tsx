import { useState, useMemo } from 'react'
import { useGameStore, type DeckBuildingState } from '../../store/gameStore'
import type { SealedCardInfo } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

/**
 * Deck Builder overlay for sealed draft mode.
 */
export function DeckBuilderOverlay() {
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)

  if (!deckBuildingState) return null

  if (deckBuildingState.phase === 'waiting') {
    return <WaitingForOpponent setName={deckBuildingState.setName} />
  }

  return <DeckBuilder state={deckBuildingState} />
}

/**
 * Waiting screen shown to the first player while waiting for opponent.
 */
function WaitingForOpponent({ setName }: { setName: string }) {
  const sessionId = useGameStore((state) => state.sessionId)
  const responsive = useResponsive()

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 24,
        padding: responsive.containerPadding,
        zIndex: 1000,
      }}
    >
      <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 20 : 28 }}>
        Sealed Draft - {setName}
      </h2>
      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.large }}>
        Waiting for opponent to join...
      </p>
      <div
        style={{
          backgroundColor: '#222',
          padding: '16px 24px',
          borderRadius: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 8,
        }}
      >
        <p style={{ color: '#aaa', margin: 0, fontSize: responsive.fontSize.normal }}>
          Session ID:
        </p>
        <code
          style={{
            color: '#4fc3f7',
            fontSize: responsive.isMobile ? 14 : 18,
            backgroundColor: '#333',
            padding: '8px 16px',
            borderRadius: 4,
            fontFamily: 'monospace',
            userSelect: 'all',
          }}
        >
          {sessionId}
        </code>
        <p style={{ color: '#666', margin: 0, fontSize: responsive.fontSize.small }}>
          Share this ID with your opponent
        </p>
      </div>
    </div>
  )
}

/**
 * Main deck builder interface.
 */
function DeckBuilder({ state }: { state: DeckBuildingState }) {
  const responsive = useResponsive()
  const addCardToDeck = useGameStore((s) => s.addCardToDeck)
  const removeCardFromDeck = useGameStore((s) => s.removeCardFromDeck)
  const setLandCount = useGameStore((s) => s.setLandCount)
  const submitSealedDeck = useGameStore((s) => s.submitSealedDeck)

  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [sortBy, setSortBy] = useState<'color' | 'cmc' | 'rarity'>('color')

  // Count cards in deck (non-land cards + lands)
  const nonLandCount = state.deck.length
  const landCount = Object.values(state.landCounts).reduce((a, b) => a + b, 0)
  const totalCount = nonLandCount + landCount
  const isValidDeck = totalCount >= 40

  // Sort and organize pool cards
  const sortedPool = useMemo(() => {
    // Cards not in deck (available to add)
    const deckCardCounts = state.deck.reduce<Record<string, number>>((acc, name) => {
      acc[name] = (acc[name] || 0) + 1
      return acc
    }, {})

    const availableCards = state.cardPool.filter((card) => {
      const inDeckCount = deckCardCounts[card.name] || 0
      const inPoolCount = state.cardPool.filter((c) => c.name === card.name).length
      // Show card if there are more copies in pool than in deck
      return inPoolCount > inDeckCount || !deckCardCounts[card.name]
    })

    return [...availableCards].sort((a, b) => {
      if (sortBy === 'color') {
        return getColorOrder(a) - getColorOrder(b) || getCmc(a) - getCmc(b)
      } else if (sortBy === 'cmc') {
        return getCmc(a) - getCmc(b)
      } else {
        return getRarityOrder(a) - getRarityOrder(b)
      }
    })
  }, [state.cardPool, state.deck, sortBy])

  // Group deck cards by name
  const deckCardGroups = useMemo(() => {
    const groups: Record<string, { card: SealedCardInfo; count: number }> = {}
    for (const cardName of state.deck) {
      if (!groups[cardName]) {
        const cardInfo = state.cardPool.find((c) => c.name === cardName)
        if (cardInfo) {
          groups[cardName] = { card: cardInfo, count: 0 }
        }
      }
      if (groups[cardName]) {
        groups[cardName].count++
      }
    }
    return Object.values(groups).sort((a, b) => getColorOrder(a.card) - getColorOrder(b.card) || getCmc(a.card) - getCmc(b.card))
  }, [state.deck, state.cardPool])

  const isSubmitted = state.phase === 'submitted'

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: '#1a1a1a',
        display: 'flex',
        flexDirection: 'column',
        zIndex: 1000,
        overflow: 'hidden',
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: responsive.isMobile ? '8px 12px' : '12px 24px',
          backgroundColor: '#222',
          borderBottom: '1px solid #444',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div>
          <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 16 : 22 }}>
            Deck Builder - {state.setName}
          </h2>
          <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.small }}>
            Build a deck with at least 40 cards
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* Status indicators */}
          {state.opponentReady && (
            <span style={{ color: '#4caf50', fontSize: responsive.fontSize.small }}>
              Opponent ready
            </span>
          )}

          {/* Deck count */}
          <div
            style={{
              padding: '8px 16px',
              backgroundColor: isValidDeck ? '#2e7d32' : '#555',
              borderRadius: 6,
              color: 'white',
              fontWeight: 600,
              fontSize: responsive.fontSize.normal,
            }}
          >
            {totalCount} / 40
          </div>

          {/* Submit button */}
          <button
            onClick={submitSealedDeck}
            disabled={!isValidDeck || isSubmitted}
            style={{
              padding: responsive.isMobile ? '8px 16px' : '10px 24px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: isValidDeck && !isSubmitted ? '#4caf50' : '#555',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              cursor: isValidDeck && !isSubmitted ? 'pointer' : 'not-allowed',
              fontWeight: 600,
            }}
          >
            {isSubmitted ? 'Submitted' : 'Submit Deck'}
          </button>
        </div>
      </div>

      {/* Main content area */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: responsive.isMobile ? 'column' : 'row',
          overflow: 'hidden',
        }}
      >
        {/* Card Pool (left/top) */}
        <div
          style={{
            flex: responsive.isMobile ? 1 : 0.6,
            display: 'flex',
            flexDirection: 'column',
            borderRight: responsive.isMobile ? 'none' : '1px solid #444',
            borderBottom: responsive.isMobile ? '1px solid #444' : 'none',
          }}
        >
          {/* Sort controls */}
          <div
            style={{
              padding: '8px 12px',
              backgroundColor: '#2a2a2a',
              borderBottom: '1px solid #333',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            <span style={{ color: '#888', fontSize: responsive.fontSize.small }}>Sort:</span>
            {(['color', 'cmc', 'rarity'] as const).map((option) => (
              <button
                key={option}
                onClick={() => setSortBy(option)}
                style={{
                  padding: '4px 12px',
                  fontSize: responsive.fontSize.small,
                  backgroundColor: sortBy === option ? '#4fc3f7' : '#444',
                  color: sortBy === option ? '#000' : '#ccc',
                  border: 'none',
                  borderRadius: 4,
                  cursor: 'pointer',
                  textTransform: 'capitalize',
                }}
              >
                {option}
              </button>
            ))}
            <span style={{ color: '#666', fontSize: responsive.fontSize.small, marginLeft: 'auto' }}>
              Pool: {sortedPool.length} cards
            </span>
          </div>

          {/* Pool cards */}
          <div
            style={{
              flex: 1,
              overflow: 'auto',
              padding: 8,
            }}
          >
            <div
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: 6,
                justifyContent: 'flex-start',
              }}
            >
              {sortedPool.map((card, index) => (
                <PoolCard
                  key={`${card.name}-${index}`}
                  card={card}
                  onClick={() => !isSubmitted && addCardToDeck(card.name)}
                  onHover={setHoveredCard}
                  disabled={isSubmitted}
                />
              ))}
            </div>
          </div>
        </div>

        {/* Deck (right/bottom) */}
        <div
          style={{
            flex: responsive.isMobile ? 1 : 0.4,
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: '#222',
          }}
        >
          {/* Deck header */}
          <div
            style={{
              padding: '8px 12px',
              backgroundColor: '#2a2a2a',
              borderBottom: '1px solid #333',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            <span style={{ color: '#ccc', fontSize: responsive.fontSize.normal, fontWeight: 600 }}>
              Your Deck
            </span>
            <span style={{ color: '#888', fontSize: responsive.fontSize.small }}>
              ({nonLandCount} spells + {landCount} lands)
            </span>
          </div>

          {/* Deck cards */}
          <div
            style={{
              flex: 1,
              overflow: 'auto',
              padding: 8,
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {deckCardGroups.map(({ card, count }) => (
                <DeckCard
                  key={card.name}
                  card={card}
                  count={count}
                  onClick={() => !isSubmitted && removeCardFromDeck(card.name)}
                  onHover={setHoveredCard}
                  disabled={isSubmitted}
                />
              ))}
            </div>

            {/* Basic lands */}
            <div style={{ marginTop: 16, paddingTop: 16, borderTop: '1px solid #444' }}>
              <h4 style={{ color: '#888', margin: '0 0 12px 0', fontSize: responsive.fontSize.small }}>
                Basic Lands
              </h4>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {state.basicLands.map((land) => (
                  <LandCounter
                    key={land.name}
                    land={land}
                    count={state.landCounts[land.name] || 0}
                    onIncrement={() => !isSubmitted && setLandCount(land.name, (state.landCounts[land.name] || 0) + 1)}
                    onDecrement={() => !isSubmitted && setLandCount(land.name, (state.landCounts[land.name] || 0) - 1)}
                    onHover={setHoveredCard}
                    disabled={isSubmitted}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Card preview on hover (desktop only) */}
      {hoveredCard && !responsive.isMobile && (
        <CardPreview card={hoveredCard} />
      )}

      {/* Submitted overlay */}
      {isSubmitted && (
        <div
          style={{
            position: 'absolute',
            bottom: 24,
            left: '50%',
            transform: 'translateX(-50%)',
            backgroundColor: 'rgba(76, 175, 80, 0.9)',
            color: 'white',
            padding: '12px 24px',
            borderRadius: 8,
            fontWeight: 600,
            fontSize: responsive.fontSize.large,
          }}
        >
          Deck submitted! Waiting for opponent...
        </div>
      )}
    </div>
  )
}

/**
 * Card in the pool (available to add to deck).
 */
function PoolCard({
  card,
  onClick,
  onHover,
  disabled,
}: {
  card: SealedCardInfo
  onClick: () => void
  onHover: (card: SealedCardInfo | null) => void
  disabled: boolean
}) {
  const cardWidth = 80
  const cardHeight = Math.round(cardWidth * 1.4)
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'small')

  return (
    <div
      onClick={disabled ? undefined : onClick}
      onMouseEnter={() => onHover(card)}
      onMouseLeave={() => onHover(null)}
      style={{
        width: cardWidth,
        height: cardHeight,
        borderRadius: 6,
        overflow: 'hidden',
        cursor: disabled ? 'default' : 'pointer',
        border: '2px solid #444',
        opacity: disabled ? 0.6 : 1,
        transition: 'all 0.15s',
      }}
      onMouseOver={(e) => {
        if (!disabled) {
          e.currentTarget.style.border = '2px solid #4fc3f7'
          e.currentTarget.style.transform = 'scale(1.05)'
        }
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.border = '2px solid #444'
        e.currentTarget.style.transform = 'scale(1)'
      }}
    >
      <img
        src={imageUrl}
        alt={card.name}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
        }}
      />
    </div>
  )
}

/**
 * Card entry in the deck list.
 */
function DeckCard({
  card,
  count,
  onClick,
  onHover,
  disabled,
}: {
  card: SealedCardInfo
  count: number
  onClick: () => void
  onHover: (card: SealedCardInfo | null) => void
  disabled: boolean
}) {
  return (
    <div
      onClick={disabled ? undefined : onClick}
      onMouseEnter={() => onHover(card)}
      onMouseLeave={() => onHover(null)}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '6px 8px',
        backgroundColor: '#333',
        borderRadius: 4,
        cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.8 : 1,
      }}
      onMouseOver={(e) => {
        if (!disabled) {
          e.currentTarget.style.backgroundColor = '#444'
        }
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.backgroundColor = '#333'
      }}
    >
      <span
        style={{
          width: 24,
          textAlign: 'center',
          color: '#4fc3f7',
          fontWeight: 600,
          fontSize: 14,
        }}
      >
        {count}x
      </span>
      <span style={{ color: '#ccc', fontSize: 13, flex: 1 }}>{card.name}</span>
      <span style={{ color: '#888', fontSize: 11 }}>{card.manaCost || ''}</span>
    </div>
  )
}

/**
 * Basic land counter with +/- buttons.
 */
function LandCounter({
  land,
  count,
  onIncrement,
  onDecrement,
  onHover,
  disabled,
}: {
  land: SealedCardInfo
  count: number
  onIncrement: () => void
  onDecrement: () => void
  onHover: (card: SealedCardInfo | null) => void
  disabled: boolean
}) {
  const imageUrl = getCardImageUrl(land.name, land.imageUri, 'small')

  return (
    <div
      onMouseEnter={() => onHover(land)}
      onMouseLeave={() => onHover(null)}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        padding: 8,
        backgroundColor: '#333',
        borderRadius: 6,
        opacity: disabled ? 0.8 : 1,
      }}
    >
      <div
        style={{
          width: 50,
          height: 70,
          borderRadius: 4,
          overflow: 'hidden',
        }}
      >
        <img
          src={imageUrl}
          alt={land.name}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      </div>
      <span style={{ color: '#aaa', fontSize: 11 }}>{land.name}</span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        <button
          onClick={disabled ? undefined : onDecrement}
          disabled={disabled || count <= 0}
          style={{
            width: 24,
            height: 24,
            borderRadius: 4,
            border: 'none',
            backgroundColor: count > 0 && !disabled ? '#666' : '#444',
            color: count > 0 && !disabled ? 'white' : '#666',
            cursor: count > 0 && !disabled ? 'pointer' : 'not-allowed',
            fontWeight: 600,
            fontSize: 14,
          }}
        >
          -
        </button>
        <span
          style={{
            width: 24,
            textAlign: 'center',
            color: 'white',
            fontWeight: 600,
            fontSize: 14,
          }}
        >
          {count}
        </span>
        <button
          onClick={disabled ? undefined : onIncrement}
          disabled={disabled}
          style={{
            width: 24,
            height: 24,
            borderRadius: 4,
            border: 'none',
            backgroundColor: disabled ? '#444' : '#4caf50',
            color: disabled ? '#666' : 'white',
            cursor: disabled ? 'not-allowed' : 'pointer',
            fontWeight: 600,
            fontSize: 14,
          }}
        >
          +
        </button>
      </div>
    </div>
  )
}

/**
 * Card preview on hover (desktop).
 */
function CardPreview({ card }: { card: SealedCardInfo }) {
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'large')
  const previewWidth = 250
  const previewHeight = Math.round(previewWidth * 1.4)

  return (
    <div
      style={{
        position: 'fixed',
        top: 80,
        right: 20,
        pointerEvents: 'none',
        zIndex: 1001,
      }}
    >
      <div
        style={{
          width: previewWidth,
          height: previewHeight,
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.6)',
        }}
      >
        <img
          src={imageUrl}
          alt={card.name}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      </div>
    </div>
  )
}

// Helper functions

function getColorOrder(card: SealedCardInfo): number {
  const cost = card.manaCost || ''
  if (cost.includes('W')) return 1
  if (cost.includes('U')) return 2
  if (cost.includes('B')) return 3
  if (cost.includes('R')) return 4
  if (cost.includes('G')) return 5
  if (cost === '' || cost.match(/^\{[0-9X]+\}$/)) return 6 // Colorless/artifacts
  return 7 // Multi-color
}

function getCmc(card: SealedCardInfo): number {
  const cost = card.manaCost || ''
  let cmc = 0
  const matches = cost.match(/\{([^}]+)\}/g) || []
  for (const match of matches) {
    const inner = match.slice(1, -1)
    const num = parseInt(inner, 10)
    if (!isNaN(num)) {
      cmc += num
    } else if (inner !== 'X') {
      cmc += 1
    }
  }
  return cmc
}

function getRarityOrder(card: SealedCardInfo): number {
  switch (card.rarity.toUpperCase()) {
    case 'MYTHIC':
      return 1
    case 'RARE':
      return 2
    case 'UNCOMMON':
      return 3
    case 'COMMON':
      return 4
    default:
      return 5
  }
}
