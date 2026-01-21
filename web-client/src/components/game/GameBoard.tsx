import { useGameStore } from '../../store/gameStore'
import { useViewingPlayer, useOpponent, useZoneCards, useBattlefieldCards } from '../../store/selectors'
import { hand } from '../../types'
import type { ClientCard, ZoneId } from '../../types'
import { PhaseIndicator } from '../ui/PhaseIndicator'

/**
 * 2D Game board layout - MTG Arena style.
 */
export function GameBoard() {
  const gameState = useGameStore((state) => state.gameState)
  const playerId = useGameStore((state) => state.playerId)
  const submitAction = useGameStore((state) => state.submitAction)

  const viewingPlayer = useViewingPlayer()
  const opponent = useOpponent()

  if (!gameState || !playerId || !viewingPlayer) {
    return null
  }

  const hasPriority = gameState.priorityPlayerId === viewingPlayer.playerId

  return (
    <div style={styles.container}>
      {/* Opponent area (top) */}
      <div style={styles.opponentArea}>
        {/* Opponent info */}
        <div style={styles.playerInfo}>
          <span style={styles.playerName}>{opponent?.name ?? 'Opponent'}</span>
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

        {/* Opponent battlefield */}
        <BattlefieldArea isOpponent />
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
        {/* Player battlefield */}
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
            <span style={styles.playerName}>{viewingPlayer.name}</span>
            <LifeDisplay life={viewingPlayer.life} isPlayer />
          </div>

          {hasPriority && (
            <button
              onClick={() => {
                submitAction({
                  type: 'PassPriority',
                  playerId: viewingPlayer.playerId,
                  description: 'Pass priority',
                })
              }}
              style={styles.passButton}
            >
              Pass
            </button>
          )}
        </div>
      </div>

      {/* Action menu for selected card */}
      <ActionMenu />
    </div>
  )
}

/**
 * Life total display.
 */
function LifeDisplay({ life, isPlayer = false }: { life: number; isPlayer?: boolean }) {
  const bgColor = isPlayer ? '#1a3a5a' : '#3a1a4a'
  const borderColor = isPlayer ? '#3a7aba' : '#7a3a9a'

  return (
    <div
      style={{
        ...styles.lifeDisplay,
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

  if (cards.length === 0) {
    return <div style={styles.emptyZone}>No cards</div>
  }

  return (
    <div style={styles.cardRow}>
      {cards.map((card) => (
        <GameCard
          key={card.id}
          card={card}
          faceDown={faceDown}
          interactive={interactive}
          small={small}
        />
      ))}
    </div>
  )
}

/**
 * Battlefield area with lands and creatures.
 */
function BattlefieldArea({ isOpponent }: { isOpponent: boolean }) {
  const {
    playerLands,
    playerCreatures,
    opponentLands,
    opponentCreatures,
  } = useBattlefieldCards()

  const lands = isOpponent ? opponentLands : playerLands
  const creatures = isOpponent ? opponentCreatures : playerCreatures

  return (
    <div style={styles.battlefieldArea}>
      {/* Lands row */}
      <div style={styles.battlefieldRow}>
        {lands.map((card) => (
          <GameCard
            key={card.id}
            card={card}
            interactive={!isOpponent}
            battlefield
          />
        ))}
      </div>

      {/* Creatures row */}
      <div style={styles.battlefieldRow}>
        {creatures.map((card) => (
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
}: {
  card: ClientCard
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
  battlefield?: boolean
}) {
  const selectCard = useGameStore((state) => state.selectCard)
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const isSelected = selectedCardId === card.id

  const cardImageUrl = faceDown
    ? 'https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389'
    : `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(card.name)}&format=image&version=normal`

  const width = small ? 60 : battlefield ? 100 : 120
  const height = small ? 84 : battlefield ? 140 : 168

  return (
    <div
      onClick={interactive ? () => selectCard(isSelected ? null : card.id) : undefined}
      style={{
        ...styles.card,
        width,
        height,
        cursor: interactive ? 'pointer' : 'default',
        border: isSelected ? '3px solid #ffff00' : '2px solid #333',
        transform: `${card.isTapped ? 'rotate(90deg)' : ''} ${isSelected ? 'translateY(-8px)' : ''}`,
        boxShadow: isSelected ? '0 8px 20px rgba(255, 255, 0, 0.4)' : '0 2px 8px rgba(0,0,0,0.5)',
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
        <span style={styles.cardName}>{faceDown ? '' : card.name}</span>
        {!faceDown && card.power !== null && card.toughness !== null && (
          <span style={styles.cardPT}>{card.power}/{card.toughness}</span>
        )}
      </div>

      {/* Tapped indicator */}
      {card.isTapped && (
        <div style={styles.tappedOverlay} />
      )}
    </div>
  )
}

/**
 * Action menu that appears when a card with legal actions is selected.
 */
function ActionMenu() {
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const legalActions = useGameStore((state) => state.legalActions)
  const submitAction = useGameStore((state) => state.submitAction)
  const selectCard = useGameStore((state) => state.selectCard)

  if (!selectedCardId) return null

  // Filter legal actions for the selected card
  const cardActions = legalActions.filter((info) => {
    const action = info.action
    switch (action.type) {
      case 'PlayLand':
        return action.cardId === selectedCardId
      case 'CastSpell':
        return action.cardId === selectedCardId
      case 'ActivateManaAbility':
        return action.sourceEntityId === selectedCardId
      case 'DeclareAttacker':
        return action.creatureId === selectedCardId
      case 'DeclareBlocker':
        return action.blockerId === selectedCardId
      default:
        return false
    }
  })

  if (cardActions.length === 0) return null

  return (
    <div style={styles.actionMenuOverlay} onClick={() => selectCard(null)}>
      <div style={styles.actionMenu} onClick={(e) => e.stopPropagation()}>
        <div style={styles.actionMenuTitle}>Actions</div>
        {cardActions.map((info, index) => (
          <button
            key={index}
            onClick={() => {
              submitAction(info.action)
              selectCard(null)
            }}
            style={styles.actionButton}
          >
            {info.description}
          </button>
        ))}
        <button
          onClick={() => selectCard(null)}
          style={styles.cancelButton}
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
    padding: 16,
    gap: 8,
    overflow: 'hidden',
  },
  opponentArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 8,
  },
  centerArea: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    padding: '8px 0',
  },
  playerArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 8,
    marginTop: 'auto',
  },
  playerInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
  },
  playerName: {
    color: '#888',
    fontSize: 14,
  },
  lifeDisplay: {
    width: 48,
    height: 48,
    borderRadius: '50%',
    border: '3px solid',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 20,
    fontWeight: 700,
  },
  playerControls: {
    display: 'flex',
    alignItems: 'center',
    gap: 24,
    padding: 8,
  },
  passButton: {
    padding: '12px 32px',
    fontSize: 16,
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
    gap: 8,
    padding: 8,
    flexWrap: 'wrap',
  },
  emptyZone: {
    color: '#444',
    fontSize: 12,
    padding: 8,
  },
  battlefieldArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 8,
    minHeight: 160,
  },
  battlefieldRow: {
    display: 'flex',
    justifyContent: 'center',
    gap: 8,
    flexWrap: 'wrap',
  },
  card: {
    position: 'relative',
    borderRadius: 8,
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
    padding: 8,
    backgroundColor: '#2a2a4e',
  },
  cardName: {
    color: 'white',
    fontSize: 10,
    textAlign: 'center',
    fontWeight: 500,
  },
  cardPT: {
    color: 'white',
    fontSize: 14,
    fontWeight: 700,
    marginTop: 8,
  },
  tappedOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
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
