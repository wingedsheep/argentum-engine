import { useGameStore } from '../../store/gameStore'
import { useViewingPlayer, useOpponent, useZoneCards, useBattlefieldCards, useHasLegalActions } from '../../store/selectors'
import { hand, graveyard } from '../../types'
import type { ClientCard, ZoneId, ClientPlayer } from '../../types'
import { PhaseIndicator } from '../ui/PhaseIndicator'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import React, { createContext, useContext } from 'react'

// Context to pass responsive sizes down the component tree
const ResponsiveContext = createContext<ResponsiveSizes | null>(null)

function useResponsiveContext(): ResponsiveSizes {
  const ctx = useContext(ResponsiveContext)
  if (!ctx) throw new Error('ResponsiveContext not provided')
  return ctx
}

/**
 * 2D Game board layout - MTG Arena style.
 */
export function GameBoard() {
  const gameState = useGameStore((state) => state.gameState)
  const playerId = useGameStore((state) => state.playerId)
  const submitAction = useGameStore((state) => state.submitAction)
  const responsive = useResponsive()

  const viewingPlayer = useViewingPlayer()
  const opponent = useOpponent()

  if (!gameState || !playerId || !viewingPlayer) {
    return null
  }

  const hasPriority = gameState.priorityPlayerId === viewingPlayer.playerId

  return (
    <ResponsiveContext.Provider value={responsive}>
    <div style={{
      ...styles.container,
      padding: responsive.containerPadding,
      gap: responsive.sectionGap,
    }}>
      {/* Opponent area (top) */}
      <div style={styles.opponentArea}>
        <div style={styles.playerRowWithZones}>
          <div style={styles.playerMainArea}>
            {/* Opponent info */}
            <div style={styles.playerInfo}>
              <span style={{ ...styles.playerName, fontSize: responsive.fontSize.normal }}>{opponent?.name ?? 'Opponent'}</span>
              <LifeDisplay life={opponent?.life ?? 0} />
            </div>

            {/* Opponent hand (face down) */}
            {opponent && (
              <CardRow
                zoneId={hand(opponent.playerId)}
                faceDown
                small
              />
            )}

            {/* Opponent battlefield - lands first (closer to opponent), then creatures */}
            <BattlefieldArea isOpponent />
          </div>

          {/* Opponent deck/graveyard (right side) */}
          {opponent && <ZonePile player={opponent} />}
        </div>
      </div>

      {/* Center - Phase indicator and stack */}
      <div style={styles.centerArea}>
        <PhaseIndicator
          phase={gameState.currentPhase}
          step={gameState.currentStep}
          turnNumber={gameState.turnNumber}
          isActivePlayer={gameState.activePlayerId === viewingPlayer.playerId}
          hasPriority={hasPriority}
        />
      </div>

      {/* Player area (bottom) */}
      <div style={styles.playerArea}>
        <div style={styles.playerRowWithZones}>
          <div style={styles.playerMainArea}>
            {/* Player battlefield - creatures first (closer to center), then lands */}
            <BattlefieldArea isOpponent={false} />

            {/* Player hand */}
            <CardRow
              zoneId={hand(playerId)}
              faceDown={false}
              interactive
            />

            {/* Player info and actions */}
            <div style={styles.playerControls}>
              <div style={styles.playerInfo}>
                <span style={{ ...styles.playerName, fontSize: responsive.fontSize.normal }}>{viewingPlayer.name}</span>
                <LifeDisplay life={viewingPlayer.life} isPlayer />
              </div>

              {hasPriority && (
                <button
                  onClick={() => {
                    submitAction({
                      type: 'PassPriority',
                      playerId: viewingPlayer.playerId,
                    })
                  }}
                  style={{
                    ...styles.passButton,
                    padding: responsive.isMobile ? '8px 16px' : '10px 24px',
                    fontSize: responsive.fontSize.normal,
                  }}
                >
                  Pass
                </button>
              )}
            </div>
          </div>

          {/* Player deck/graveyard (right side) */}
          <ZonePile player={viewingPlayer} />
        </div>
      </div>

      {/* Action menu for selected card */}
      <ActionMenu />
    </div>
    </ResponsiveContext.Provider>
  )
}

/**
 * Life total display.
 */
function LifeDisplay({ life, isPlayer = false }: { life: number; isPlayer?: boolean }) {
  const responsive = useResponsiveContext()
  const bgColor = isPlayer ? '#1a3a5a' : '#3a1a4a'
  const borderColor = isPlayer ? '#3a7aba' : '#7a3a9a'
  const size = responsive.isMobile ? 36 : responsive.isTablet ? 42 : 48

  return (
    <div
      style={{
        ...styles.lifeDisplay,
        width: size,
        height: size,
        fontSize: responsive.fontSize.large,
        backgroundColor: bgColor,
        borderColor: borderColor,
      }}
    >
      <span style={{ color: life <= 5 ? '#ff4444' : '#ffffff' }}>{life}</span>
    </div>
  )
}

/**
 * Row of cards (hand or other horizontal zone).
 */
function CardRow({
  zoneId,
  faceDown = false,
  interactive = false,
  small = false,
}: {
  zoneId: ZoneId
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
}) {
  const cards = useZoneCards(zoneId)
  const responsive = useResponsiveContext()

  if (cards.length === 0) {
    return <div style={{ ...styles.emptyZone, fontSize: responsive.fontSize.small }}>No cards</div>
  }

  // Calculate available width for the hand (viewport - padding - zone piles on sides)
  const sideZoneWidth = responsive.pileWidth + 20 // pile + margin
  const availableWidth = responsive.viewportWidth - (responsive.containerPadding * 2) - (sideZoneWidth * 2)

  // Calculate card width that fits all cards
  const baseWidth = small ? responsive.smallCardWidth : responsive.cardWidth
  const minWidth = small ? 30 : 45
  const fittingWidth = calculateFittingCardWidth(
    cards.length,
    availableWidth,
    responsive.cardGap,
    baseWidth,
    minWidth
  )

  return (
    <div style={{ ...styles.cardRow, gap: responsive.cardGap, padding: responsive.cardGap }}>
      {cards.map((card) => (
        <GameCard
          key={card.id}
          card={card}
          faceDown={faceDown}
          interactive={interactive}
          small={small}
          overrideWidth={fittingWidth}
        />
      ))}
    </div>
  )
}

/**
 * Deck and graveyard pile display.
 */
function ZonePile({ player }: { player: ClientPlayer }) {
  const graveyardCards = useZoneCards(graveyard(player.playerId))
  const topGraveyardCard = graveyardCards[graveyardCards.length - 1]
  const responsive = useResponsiveContext()

  const pileStyle = {
    width: responsive.pileWidth,
    height: responsive.pileHeight,
    borderRadius: responsive.isMobile ? 4 : 6,
  }

  return (
    <div style={{ ...styles.zonePile, gap: responsive.cardGap, minWidth: responsive.pileWidth + 10 }}>
      {/* Library/Deck */}
      <div style={styles.zoneStack}>
        <div style={{ ...styles.deckPile, ...pileStyle }}>
          {player.librarySize > 0 ? (
            <img
              src="https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389"
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
        <div style={{ ...styles.graveyardPile, ...pileStyle }}>
          {topGraveyardCard ? (
            <img
              src={`https://api.scryfall.com/cards/named?exact=${encodeURIComponent(topGraveyardCard.name)}&format=image&version=normal`}
              alt={topGraveyardCard.name}
              style={{ ...styles.pileImage, opacity: 0.8 }}
              onError={(e) => {
                e.currentTarget.style.display = 'none'
              }}
            />
          ) : (
            <div style={styles.emptyPile} />
          )}
          {player.graveyardSize > 0 && (
            <div style={{ ...styles.pileCount, fontSize: responsive.fontSize.small }}>{player.graveyardSize}</div>
          )}
        </div>
        <span style={{ ...styles.zoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>Graveyard</span>
      </div>
    </div>
  )
}

/**
 * Battlefield area with lands and creatures.
 * For player: creatures first (closer to center), then lands (closer to player)
 * For opponent: lands first (closer to opponent), then creatures (closer to center)
 */
function BattlefieldArea({ isOpponent }: { isOpponent: boolean }) {
  const {
    playerLands,
    playerCreatures,
    opponentLands,
    opponentCreatures,
  } = useBattlefieldCards()
  const responsive = useResponsiveContext()

  const lands = isOpponent ? opponentLands : playerLands
  const creatures = isOpponent ? opponentCreatures : playerCreatures

  // For opponent: lands first (top), creatures second (bottom/closer to center)
  // For player: creatures first (top/closer to center), lands second (bottom/closer to player)
  const firstRow = isOpponent ? lands : creatures
  const secondRow = isOpponent ? creatures : lands

  const minHeight = responsive.isMobile ? 100 : responsive.isTablet ? 130 : 160

  return (
    <div style={{ ...styles.battlefieldArea, gap: responsive.cardGap, minHeight }}>
      {/* First row */}
      <div style={{ ...styles.battlefieldRow, gap: responsive.cardGap }}>
        {firstRow.map((card) => (
          <GameCard
            key={card.id}
            card={card}
            interactive={!isOpponent}
            battlefield
          />
        ))}
      </div>

      {/* Second row */}
      <div style={{ ...styles.battlefieldRow, gap: responsive.cardGap }}>
        {secondRow.map((card) => (
          <GameCard
            key={card.id}
            card={card}
            interactive={!isOpponent}
            battlefield
          />
        ))}
      </div>
    </div>
  )
}

/**
 * Single card display.
 */
function GameCard({
  card,
  faceDown = false,
  interactive = false,
  small = false,
  battlefield = false,
  overrideWidth,
}: {
  card: ClientCard
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
  battlefield?: boolean
  overrideWidth?: number
}) {
  const selectCard = useGameStore((state) => state.selectCard)
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const targetingState = useGameStore((state) => state.targetingState)
  const responsive = useResponsiveContext()

  // Check if card has legal actions (is playable)
  const hasLegalActions = useHasLegalActions(card.id)

  const isSelected = selectedCardId === card.id
  const isInTargetingMode = targetingState !== null
  const isValidTarget = targetingState?.validTargets.includes(card.id) ?? false
  const isPlayable = interactive && hasLegalActions && !faceDown

  const cardImageUrl = faceDown
    ? 'https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389'
    : `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(card.name)}&format=image&version=normal`

  // Use responsive sizes, but allow override for fitting cards in hand
  const baseWidth = small
    ? responsive.smallCardWidth
    : battlefield
      ? responsive.battlefieldCardWidth
      : responsive.cardWidth
  const width = overrideWidth ?? baseWidth
  const cardRatio = 1.4
  const height = Math.round(width * cardRatio)

  const handleClick = () => {
    if (interactive && !isInTargetingMode) {
      // Normal selection
      selectCard(isSelected ? null : card.id)
    }
  }

  // Determine border color based on state (priority: selected > validTarget > playable > default)
  let borderStyle = '2px solid #333'
  let boxShadow = '0 2px 8px rgba(0,0,0,0.5)'

  if (isSelected) {
    borderStyle = '3px solid #ffff00'
    boxShadow = '0 8px 20px rgba(255, 255, 0, 0.4)'
  } else if (isValidTarget) {
    borderStyle = '3px solid #ff4444'
    boxShadow = '0 4px 15px rgba(255, 68, 68, 0.6)'
  } else if (isPlayable) {
    // Green highlight for playable cards
    borderStyle = '2px solid #00ff00'
    boxShadow = '0 0 12px rgba(0, 255, 0, 0.5), 0 0 24px rgba(0, 255, 0, 0.3)'
  }

  return (
    <div
      onClick={handleClick}
      style={{
        ...styles.card,
        width,
        height,
        borderRadius: responsive.isMobile ? 4 : 8,
        cursor: (interactive || isValidTarget) ? 'pointer' : 'default',
        border: borderStyle,
        transform: `${card.isTapped ? 'rotate(90deg)' : ''} ${isSelected ? 'translateY(-8px)' : ''}`,
        boxShadow,
      }}
    >
      <img
        src={cardImageUrl}
        alt={faceDown ? 'Card back' : card.name}
        style={styles.cardImage}
        onError={(e) => {
          e.currentTarget.style.display = 'none'
          const fallback = e.currentTarget.nextElementSibling as HTMLElement
          if (fallback) fallback.style.display = 'flex'
        }}
      />
      {/* Fallback when image fails */}
      <div style={styles.cardFallback}>
        <span style={{ ...styles.cardName, fontSize: responsive.fontSize.small }}>{faceDown ? '' : card.name}</span>
        {!faceDown && card.power !== null && card.toughness !== null && (
          <span style={{ ...styles.cardPT, fontSize: responsive.fontSize.normal }}>{card.power}/{card.toughness}</span>
        )}
      </div>

      {/* Tapped indicator */}
      {card.isTapped && (
        <div style={styles.tappedOverlay} />
      )}

      {/* Summoning sickness indicator */}
      {battlefield && card.hasSummoningSickness && card.cardTypes.includes('CREATURE') && (
        <div style={styles.summoningSicknessOverlay}>
          <div style={{ ...styles.summoningSicknessIcon, fontSize: responsive.isMobile ? 16 : 24 }}>💤</div>
        </div>
      )}

      {/* Playable indicator glow effect */}
      {isPlayable && !isSelected && (
        <div style={styles.playableGlow} />
      )}
    </div>
  )
}

/**
 * Action menu that appears when a card with legal actions is selected.
 * Handles targeting mode for blocking (select blocker, then click attacker).
 */
function ActionMenu() {
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const legalActions = useGameStore((state) => state.legalActions)
  const submitAction = useGameStore((state) => state.submitAction)
  const selectCard = useGameStore((state) => state.selectCard)
  const targetingState = useGameStore((state) => state.targetingState)
  const cancelTargeting = useGameStore((state) => state.cancelTargeting)
  const responsive = useResponsiveContext()

  // If in targeting mode, show targeting UI instead
  if (targetingState) {
    return (
      <div style={{
        ...styles.targetingOverlay,
        padding: responsive.isMobile ? '12px 16px' : '16px 24px',
      }}>
        <div style={{ ...styles.targetingPrompt, fontSize: responsive.fontSize.normal }}>
          Select an attacker to block
        </div>
        <button onClick={cancelTargeting} style={{
          ...styles.cancelButton,
          padding: responsive.isMobile ? '8px 12px' : '10px 16px',
          fontSize: responsive.fontSize.normal,
        }}>
          Cancel
        </button>
      </div>
    )
  }

  if (!selectedCardId) return null

  // Filter legal actions for the selected card
  const cardActions = legalActions.filter((info) => {
    const action = info.action
    switch (action.type) {
      case 'PlayLand':
        return action.cardId === selectedCardId
      case 'CastSpell':
        return action.cardId === selectedCardId
      case 'ActivateAbility':
        return action.sourceId === selectedCardId
      default:
        return false
    }
  })

  if (cardActions.length === 0) return null

  return (
    <div style={styles.actionMenuOverlay} onClick={() => selectCard(null)}>
      <div style={{
        ...styles.actionMenu,
        padding: responsive.isMobile ? 12 : 16,
        minWidth: responsive.isMobile ? 160 : 200,
      }} onClick={(e) => e.stopPropagation()}>
        <div style={{
          ...styles.actionMenuTitle,
          fontSize: responsive.fontSize.small,
        }}>Actions</div>

        {/* Card actions */}
        {cardActions.map((info, index) => (
          <button
            key={index}
            onClick={() => {
              submitAction(info.action)
              selectCard(null)
            }}
            style={{
              ...styles.actionButton,
              padding: responsive.isMobile ? '10px 12px' : '12px 16px',
              fontSize: responsive.fontSize.normal,
            }}
          >
            {info.description}
          </button>
        ))}

        <button
          onClick={() => selectCard(null)}
          style={{
            ...styles.cancelButton,
            padding: responsive.isMobile ? '8px 12px' : '10px 16px',
            fontSize: responsive.fontSize.normal,
          }}
        >
          Cancel
        </button>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: '#0a0a15',
    overflow: 'hidden',
  },
  opponentArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  centerArea: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    padding: '4px 0',
    flex: 1,
    minHeight: 40,
  },
  playerArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginTop: 'auto',
  },
  playerRowWithZones: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    width: '100%',
    justifyContent: 'center',
  },
  playerMainArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 4,
    flex: 1,
    minWidth: 0, // Allow shrinking
  },
  zonePile: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  zoneStack: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 2,
  },
  deckPile: {
    position: 'relative',
    overflow: 'hidden',
    boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
  },
  graveyardPile: {
    position: 'relative',
    overflow: 'hidden',
    boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
    backgroundColor: '#1a1a2e',
  },
  pileImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  emptyPile: {
    width: '100%',
    height: '100%',
    backgroundColor: '#1a1a2e',
    border: '2px dashed #333',
    borderRadius: 6,
  },
  pileCount: {
    position: 'absolute',
    bottom: 4,
    right: 4,
    backgroundColor: 'rgba(0,0,0,0.8)',
    color: 'white',
    fontSize: 12,
    fontWeight: 700,
    padding: '2px 6px',
    borderRadius: 4,
  },
  zoneLabel: {
    color: '#666',
    fontSize: 10,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  playerInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
  playerName: {
    color: '#888',
  },
  lifeDisplay: {
    borderRadius: '50%',
    border: '3px solid',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
  },
  playerControls: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    padding: 4,
  },
  passButton: {
    padding: '10px 24px',
    fontWeight: 600,
    backgroundColor: '#e67e22',
    color: 'white',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  cardRow: {
    display: 'flex',
    justifyContent: 'center',
    flexWrap: 'wrap',
    maxWidth: '100%',
  },
  emptyZone: {
    color: '#444',
    padding: 4,
  },
  battlefieldArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  battlefieldRow: {
    display: 'flex',
    justifyContent: 'center',
    flexWrap: 'wrap',
  },
  card: {
    position: 'relative',
    overflow: 'hidden',
    backgroundColor: '#1a1a2e',
    transition: 'transform 0.15s, box-shadow 0.15s',
    flexShrink: 0,
  },
  cardImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  cardFallback: {
    display: 'none',
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 4,
    backgroundColor: '#2a2a4e',
  },
  cardName: {
    color: 'white',
    textAlign: 'center',
    fontWeight: 500,
  },
  cardPT: {
    color: 'white',
    fontWeight: 700,
    marginTop: 4,
  },
  tappedOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
  },
  summoningSicknessOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(100, 100, 150, 0.3)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    pointerEvents: 'none',
  },
  summoningSicknessIcon: {
    opacity: 0.8,
  },
  playableGlow: {
    position: 'absolute',
    top: -2,
    left: -2,
    right: -2,
    bottom: -2,
    borderRadius: 10,
    pointerEvents: 'none',
    animation: 'playablePulse 2s ease-in-out infinite',
    background: 'transparent',
    boxShadow: '0 0 8px rgba(0, 255, 0, 0.4)',
  },
  targetingOverlay: {
    position: 'absolute',
    bottom: 20,
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    padding: '16px 24px',
    borderRadius: 12,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 12,
    zIndex: 100,
    border: '2px solid #ff4444',
  },
  targetingPrompt: {
    color: '#ff4444',
    fontSize: 16,
    fontWeight: 600,
  },
  actionMenuOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 100,
  },
  actionMenu: {
    backgroundColor: '#1a1a2e',
    border: '2px solid #444',
    borderRadius: 12,
    padding: 16,
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
    minWidth: 200,
  },
  actionMenuTitle: {
    color: '#888',
    fontSize: 12,
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 8,
    textAlign: 'center',
  },
  actionButton: {
    padding: '12px 16px',
    fontSize: 14,
    backgroundColor: '#2a5a2a',
    color: 'white',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    textAlign: 'left',
  },
  cancelButton: {
    padding: '10px 16px',
    fontSize: 14,
    backgroundColor: '#444',
    color: '#888',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    marginTop: 8,
  },
}
