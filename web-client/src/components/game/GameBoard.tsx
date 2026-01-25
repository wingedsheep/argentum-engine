import { useGameStore } from '../../store/gameStore'
import { useViewingPlayer, useOpponent, useZoneCards, useZone, useBattlefieldCards, useHasLegalActions, useStackCards } from '../../store/selectors'
import { hand, graveyard } from '../../types'
import type { ClientCard, ZoneId, ClientPlayer, LegalActionInfo, EntityId, Keyword, ClientPlayerEffect } from '../../types'
import { keywordIcons, genericKeywordIcon, displayableKeywords } from '../../assets/icons/keywords'
import { PhaseIndicator } from '../ui/PhaseIndicator'
import { CombatArrows } from '../combat/CombatArrows'
import { TargetingArrows } from '../targeting/TargetingArrows'
import { DraggedCardOverlay } from './DraggedCardOverlay'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import React, { createContext, useContext, useCallback, useEffect } from 'react'

// Context to pass responsive sizes down the component tree
const ResponsiveContext = createContext<ResponsiveSizes | null>(null)

function useResponsiveContext(): ResponsiveSizes {
  const ctx = useContext(ResponsiveContext)
  if (!ctx) throw new Error('ResponsiveContext not provided')
  return ctx
}

/**
 * Get color for P/T display based on modifications.
 * Green = buffed, Red = debuffed, White = normal
 */
function getPTColor(
  power: number | null,
  toughness: number | null,
  basePower: number | null,
  baseToughness: number | null
): string {
  if (power === null || toughness === null || basePower === null || baseToughness === null) {
    return 'white'
  }

  const powerDiff = power - basePower
  const toughnessDiff = toughness - baseToughness

  // If both are increased or both are unchanged, and at least one is increased
  if (powerDiff >= 0 && toughnessDiff >= 0 && (powerDiff > 0 || toughnessDiff > 0)) {
    return '#00ff00' // Green for buffed
  }
  // If both are decreased or both are unchanged, and at least one is decreased
  if (powerDiff <= 0 && toughnessDiff <= 0 && (powerDiff < 0 || toughnessDiff < 0)) {
    return '#ff4444' // Red for debuffed
  }
  // Mixed buff/debuff - show yellow
  if (powerDiff !== 0 || toughnessDiff !== 0) {
    return '#ffff00' // Yellow for mixed
  }

  return 'white'
}

/**
 * Container component for keyword ability icons on a card.
 * Uses SVG icons from assets/icons/keywords.
 */
const KeywordIcons = ({ keywords, size }: { keywords: readonly Keyword[]; size: number }) => {
  const filteredKeywords = keywords.filter(k => displayableKeywords.has(k))

  if (filteredKeywords.length === 0) return null

  return (
    <div style={styles.keywordIconsContainer}>
      {filteredKeywords.map((keyword) => (
        <div key={keyword} style={styles.keywordIconWrapper} title={keyword.replace(/_/g, ' ')}>
          <img
            src={keywordIcons[keyword] ?? genericKeywordIcon}
            alt={keyword}
            style={{
              width: size,
              height: size,
              display: 'block',
              filter: 'brightness(0) invert(1)', // Make SVG white
            }}
          />
        </div>
      ))}
    </div>
  )
}

/**
 * 2D Game board layout - MTG Arena style.
 */
export function GameBoard() {
  const gameState = useGameStore((state) => state.gameState)
  const playerId = useGameStore((state) => state.playerId)
  const submitAction = useGameStore((state) => state.submitAction)
  const combatState = useGameStore((state) => state.combatState)
  const responsive = useResponsive()

  const viewingPlayer = useViewingPlayer()
  const opponent = useOpponent()

  if (!gameState || !playerId || !viewingPlayer) {
    return null
  }

  const hasPriority = gameState.priorityPlayerId === viewingPlayer.playerId
  const isInCombatMode = combatState !== null

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
              {opponent && <LifeDisplay life={opponent.life} playerId={opponent.playerId} />}
              {opponent && <ActiveEffectsBadges effects={opponent.activeEffects} />}
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
        <StackDisplay />
      </div>

      {/* Player area (bottom) */}
      <div style={styles.playerArea}>
        <div style={styles.playerRowWithZones}>
          <div style={styles.playerMainArea}>
            {/* Player battlefield - creatures first (closer to center), then lands */}
            <BattlefieldArea isOpponent={false} />

            {/* Player hand */}
            <div data-zone="hand">
              <CardRow
                zoneId={hand(playerId)}
                faceDown={false}
                interactive
              />
            </div>

            {/* Player info and actions */}
            <div style={styles.playerControls}>
              <div style={styles.playerInfo}>
                <span style={{ ...styles.playerName, fontSize: responsive.fontSize.normal }}>{viewingPlayer.name}</span>
                <LifeDisplay life={viewingPlayer.life} isPlayer playerId={viewingPlayer.playerId} />
                <ActiveEffectsBadges effects={viewingPlayer.activeEffects} />
              </div>

              {/* Hide Pass button during combat - combat overlay handles actions */}
              {hasPriority && !isInCombatMode && (
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

      {/* Combat arrows for blocker assignments */}
      <CombatArrows />

      {/* Targeting arrows for spells on the stack */}
      <TargetingArrows />

      {/* Dragged card overlay */}
      <DraggedCardOverlay />
      <CardPreview />
    </div>
    </ResponsiveContext.Provider>
  )
}

/**
 * Active effects badges - shows status effects on a player.
 */
function ActiveEffectsBadges({ effects }: { effects: readonly ClientPlayerEffect[] | undefined }) {
  const responsive = useResponsiveContext()

  if (!effects || effects.length === 0) return null

  return (
    <div style={styles.effectBadgesContainer}>
      {effects.map((effect) => (
        <div
          key={effect.effectId}
          style={{
            ...styles.effectBadge,
            padding: responsive.isMobile ? '2px 6px' : '4px 8px',
            fontSize: responsive.fontSize.small,
          }}
          title={effect.description ?? effect.name}
        >
          {effect.icon && <span style={styles.effectBadgeIcon}>{getEffectIcon(effect.icon)}</span>}
          <span style={styles.effectBadgeName}>{effect.name}</span>
        </div>
      ))}
    </div>
  )
}

/**
 * Get an emoji or icon for an effect based on its icon identifier.
 */
function getEffectIcon(icon: string): string {
  switch (icon) {
    case 'shield-off':
      return '🛡️'
    case 'skip':
      return '⏭️'
    case 'lock':
      return '🔒'
    default:
      return '⚡'
  }
}

/**
 * Life total display - interactive when in targeting mode.
 */
function LifeDisplay({
  life,
  isPlayer = false,
  playerId
}: {
  life: number
  isPlayer?: boolean
  playerId: EntityId
}) {
  const responsive = useResponsiveContext()
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const confirmTargeting = useGameStore((state) => state.confirmTargeting)

  // Check if this player is a valid target in current targeting mode
  const isValidTarget = targetingState?.validTargets.includes(playerId) ?? false
  const isSelected = targetingState?.selectedTargets.includes(playerId) ?? false

  const handleClick = () => {
    if (targetingState && isValidTarget && !isSelected) {
      addTarget(playerId)

      // Auto-confirm if we have selected enough targets
      const newTargetCount = (targetingState.selectedTargets.length) + 1
      const requiredCount = targetingState.requiredCount
      if (newTargetCount >= requiredCount) {
        setTimeout(() => confirmTargeting(), 0)
      }
    }
  }

  const size = responsive.isMobile ? 36 : responsive.isTablet ? 42 : 48

  // Dynamic styling based on targeting state
  const bgColor = isPlayer ? '#1a3a5a' : '#3a1a4a'
  const borderColor = isSelected
    ? '#ffff00' // Yellow if selected as target
    : isValidTarget
      ? '#ff4444' // Red glow if valid target
      : isPlayer ? '#3a7aba' : '#7a3a9a'

  const cursor = isValidTarget ? 'pointer' : 'default'
  const boxShadow = isSelected
    ? '0 0 20px rgba(255, 255, 0, 0.8)'
    : isValidTarget
      ? '0 0 15px rgba(255, 68, 68, 0.6)'
      : 'none'

  return (
    <div
      data-player-id={playerId}
      onClick={handleClick}
      style={{
        ...styles.lifeDisplay,
        width: size,
        height: size,
        fontSize: responsive.fontSize.large,
        backgroundColor: bgColor,
        borderColor: borderColor,
        cursor,
        boxShadow,
        transition: 'all 0.2s ease-in-out',
      }}
    >
      <span style={{ color: life <= 5 ? '#ff4444' : '#ffffff' }}>{life}</span>
    </div>
  )
}

/**
 * Stack display - shows spells/abilities waiting to resolve.
 */
function StackDisplay() {
  const stackCards = useStackCards()
  const responsive = useResponsiveContext()
  const hoverCard = useGameStore((state) => state.hoverCard)

  if (stackCards.length === 0) return null

  return (
    <div style={styles.stackContainer}>
      <div style={{
        ...styles.stackHeader,
        fontSize: responsive.fontSize.small,
      }}>
        Stack ({stackCards.length})
      </div>
      <div style={styles.stackItems}>
        {stackCards.map((card, index) => (
          <div
            key={card.id}
            style={{
              ...styles.stackItem,
              zIndex: stackCards.length - index,
              transform: `translateY(${index * 4}px)`,
            }}
            onMouseEnter={() => hoverCard(card.id)}
            onMouseLeave={() => hoverCard(null)}
          >
            <img
              src={`https://api.scryfall.com/cards/named?exact=${encodeURIComponent(card.name)}&format=image&version=small`}
              alt={card.name}
              style={styles.stackItemImage}
              title={`${card.name}\n${card.oracleText || ''}`}
            />
            <div style={{
              ...styles.stackItemName,
              fontSize: responsive.fontSize.small,
            }}>
              {card.name}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
function CardPreview() {
  const hoveredCardId = useGameStore((state) => state.hoveredCardId)
  const gameState = useGameStore((state) => state.gameState)
  const responsive = useResponsiveContext()

  if (!hoveredCardId || !gameState) return null

  const card = gameState.cards[hoveredCardId]
  if (!card) return null

  const cardImageUrl = `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(card.name)}&format=image&version=large`

  // Calculate preview size - larger than normal cards
  const previewWidth = responsive.isMobile ? 200 : 280
  const previewHeight = Math.round(previewWidth * 1.4)

  // Determine if stats are modified
  const isPowerBuffed = card.power !== null && card.basePower !== null && card.power > card.basePower
  const isPowerDebuffed = card.power !== null && card.basePower !== null && card.power < card.basePower
  const isToughnessBuffed = card.toughness !== null && card.baseToughness !== null && card.toughness > card.baseToughness
  const isToughnessDebuffed = card.toughness !== null && card.baseToughness !== null && card.toughness < card.baseToughness
  const hasStatModifications = isPowerBuffed || isPowerDebuffed || isToughnessBuffed || isToughnessDebuffed

  return (
    <div style={styles.cardPreviewOverlay}>
      <div style={{
        ...styles.cardPreviewContainer,
        width: previewWidth,
      }}>
        {/* Card image */}
        <div style={{
          ...styles.cardPreviewCard,
          width: previewWidth,
          height: previewHeight,
        }}>
          <img
            src={cardImageUrl}
            alt={card.name}
            style={styles.cardPreviewImage}
          />
        </div>

        {/* Stats box (for creatures with modifications) */}
        {card.power !== null && card.toughness !== null && (
          <div style={{
            ...styles.cardPreviewStats,
            backgroundColor: hasStatModifications ? 'rgba(0, 0, 0, 0.9)' : 'transparent',
          }}>
            {hasStatModifications && (
              <>
                <span style={{
                  color: isPowerBuffed ? '#00ff00' : isPowerDebuffed ? '#ff4444' : '#ffffff',
                  fontWeight: 700,
                  fontSize: responsive.isMobile ? 18 : 24,
                }}>
                  {card.power}
                </span>
                <span style={{ color: '#ffffff', fontSize: responsive.isMobile ? 18 : 24 }}>/</span>
                <span style={{
                  color: isToughnessBuffed ? '#00ff00' : isToughnessDebuffed ? '#ff4444' : '#ffffff',
                  fontWeight: 700,
                  fontSize: responsive.isMobile ? 18 : 24,
                }}>
                  {card.toughness}
                </span>
                {(card.basePower !== null && card.baseToughness !== null) && (
                  <span style={{
                    color: '#888888',
                    fontSize: responsive.isMobile ? 12 : 14,
                    marginLeft: 8,
                  }}>
                    (base: {card.basePower}/{card.baseToughness})
                  </span>
                )}
              </>
            )}
          </div>
        )}

        {/* Keywords/abilities info panel */}
        {card.keywords.length > 0 && (
          <div style={styles.cardPreviewKeywords}>
            {card.keywords.map((keyword) => (
              <div key={keyword} style={styles.cardPreviewKeyword}>
                <span style={styles.cardPreviewKeywordName}>{keyword}</span>
              </div>
            ))}
          </div>
        )}
      </div>
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
  const zone = useZone(zoneId)
  const responsive = useResponsiveContext()

  // For hidden zones (like opponent's hand), use zone size to show face-down placeholders
  const zoneSize = zone?.size ?? 0
  const showPlaceholders = faceDown && cards.length === 0 && zoneSize > 0

  if (cards.length === 0 && !showPlaceholders) {
    return <div style={{ ...styles.emptyZone, fontSize: responsive.fontSize.small }}>No cards</div>
  }

  // Calculate available width for the hand (viewport - padding - zone piles on sides)
  const sideZoneWidth = responsive.pileWidth + 20 // pile + margin
  const availableWidth = responsive.viewportWidth - (responsive.containerPadding * 2) - (sideZoneWidth * 2)

  // Calculate card width that fits all cards
  const cardCount = showPlaceholders ? zoneSize : cards.length
  const baseWidth = small ? responsive.smallCardWidth : responsive.cardWidth
  const minWidth = small ? 30 : 45
  const fittingWidth = calculateFittingCardWidth(
    cardCount,
    availableWidth,
    responsive.cardGap,
    baseWidth,
    minWidth
  )

  // Render face-down placeholders for hidden zones
  if (showPlaceholders) {
    const cardRatio = 1.4
    const height = Math.round(fittingWidth * cardRatio)
    return (
      <div style={{ ...styles.cardRow, gap: responsive.cardGap, padding: responsive.cardGap }}>
        {Array.from({ length: zoneSize }).map((_, index) => (
          <div
            key={`placeholder-${index}`}
            style={{
              ...styles.card,
              width: fittingWidth,
              height,
              borderRadius: responsive.isMobile ? 4 : 8,
              border: '2px solid #333',
              boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
            }}
          >
            <img
              src="https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389"
              alt="Card back"
              style={styles.cardImage}
            />
          </div>
        ))}
      </div>
    )
  }

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
          inHand={interactive && !faceDown}
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
    <div
      style={{
        ...styles.battlefieldArea,
        gap: responsive.cardGap,
        minHeight,
      }}
    >
      {/* First row */}
      <div style={{ ...styles.battlefieldRow, gap: responsive.cardGap }}>
        {firstRow.map((card) => (
          <GameCard
            key={card.id}
            card={card}
            interactive={!isOpponent}
            battlefield
            isOpponentCard={isOpponent}
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
            isOpponentCard={isOpponent}
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
  isOpponentCard = false,
  inHand = false,
}: {
  card: ClientCard
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
  battlefield?: boolean
  overrideWidth?: number
  isOpponentCard?: boolean
  inHand?: boolean
}) {
  const selectCard = useGameStore((state) => state.selectCard)
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const hoverCard = useGameStore((state) => state.hoverCard)
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const confirmTargeting = useGameStore((state) => state.confirmTargeting)
  const combatState = useGameStore((state) => state.combatState)
  const legalActions = useGameStore((state) => state.legalActions)
  const submitAction = useGameStore((state) => state.submitAction)
  const toggleAttacker = useGameStore((state) => state.toggleAttacker)
  const assignBlocker = useGameStore((state) => state.assignBlocker)
  const removeBlockerAssignment = useGameStore((state) => state.removeBlockerAssignment)
  const startDraggingBlocker = useGameStore((state) => state.startDraggingBlocker)
  const stopDraggingBlocker = useGameStore((state) => state.stopDraggingBlocker)
  const draggingBlockerId = useGameStore((state) => state.draggingBlockerId)
  const startDraggingCard = useGameStore((state) => state.startDraggingCard)
  const stopDraggingCard = useGameStore((state) => state.stopDraggingCard)
  const draggingCardId = useGameStore((state) => state.draggingCardId)
  const startTargeting = useGameStore((state) => state.startTargeting)
  const responsive = useResponsiveContext()

  // Hover handlers for card preview
  const handleMouseEnter = useCallback(() => {
    if (!faceDown) {
      hoverCard(card.id)
    }
  }, [card.id, faceDown, hoverCard])

  const handleMouseLeave = useCallback(() => {
    hoverCard(null)
  }, [hoverCard])

  // Check if card has legal actions (is playable)
  const hasLegalActions = useHasLegalActions(card.id)

  const isSelected = selectedCardId === card.id
  const isInTargetingMode = targetingState !== null
  const isValidTarget = targetingState?.validTargets.includes(card.id) ?? false

  // Combat mode checks
  const isInAttackerMode = combatState?.mode === 'declareAttackers'
  const isInBlockerMode = combatState?.mode === 'declareBlockers'
  const isInCombatMode = isInAttackerMode || isInBlockerMode

  // For attacker mode: check if this is a valid attacker (own creature, untapped, no summoning sickness)
  const isOwnCreature = !isOpponentCard && card.cardTypes.includes('CREATURE')
  const isValidAttacker = isInAttackerMode && isOwnCreature && !card.isTapped && combatState.validCreatures.includes(card.id)
  const isSelectedAsAttacker = isInAttackerMode && combatState.selectedAttackers.includes(card.id)

  // For blocker mode: check if this is a valid blocker or an attacking creature to block
  const isValidBlocker = isInBlockerMode && isOwnCreature && !card.isTapped && combatState.validCreatures.includes(card.id)
  const isSelectedAsBlocker = isInBlockerMode && !!combatState?.blockerAssignments[card.id]
  const isAttackingInBlockerMode = isInBlockerMode && isOpponentCard && combatState.attackingCreatures.includes(card.id)

  // Only show playable highlight outside of combat mode (and when not targeting)
  const isPlayable = interactive && hasLegalActions && !faceDown && !isInCombatMode

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

  // Check if this card can be played/cast (for drag-to-play)
  const playableAction = legalActions.find((a) => {
    const action = a.action
    return (action.type === 'PlayLand' && action.cardId === card.id) ||
           (action.type === 'CastSpell' && action.cardId === card.id)
  })
  const canDragToPlay = inHand && playableAction && !isInCombatMode

  // Handle mouse down - start dragging for blockers or hand cards
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (isInBlockerMode && isValidBlocker && !isSelectedAsBlocker) {
      e.preventDefault()
      startDraggingBlocker(card.id)
      return
    }
    // Start dragging card from hand
    if (canDragToPlay) {
      e.preventDefault()
      startDraggingCard(card.id)
    }
  }, [isInBlockerMode, isValidBlocker, isSelectedAsBlocker, startDraggingBlocker, canDragToPlay, startDraggingCard, card.id])

  // Handle mouse up - drop blocker on attacker or cancel drag
  const handleMouseUp = useCallback(() => {
    if (isInBlockerMode && draggingBlockerId && isAttackingInBlockerMode) {
      // Dropping on an attacker - assign the blocker
      assignBlocker(draggingBlockerId, card.id)
      stopDraggingBlocker()
    }
  }, [isInBlockerMode, draggingBlockerId, isAttackingInBlockerMode, assignBlocker, stopDraggingBlocker, card.id])

  // Global mouse up handler for card dragging (to detect drop outside hand)
  useEffect(() => {
    if (draggingCardId !== card.id) return

    const handleGlobalMouseUp = (e: MouseEvent) => {
      // Check if dropped outside the hand area - if so, play the card
      const handEl = document.querySelector('[data-zone="hand"]')
      let isOverHand = false

      if (handEl) {
        const rect = handEl.getBoundingClientRect()
        isOverHand = e.clientX >= rect.left && e.clientX <= rect.right &&
                     e.clientY >= rect.top && e.clientY <= rect.bottom
      }

      if (!isOverHand && playableAction) {
        // Check if action requires targeting
        if (playableAction.requiresTargets && playableAction.validTargets && playableAction.validTargets.length > 0) {
          // Enter targeting mode
          startTargeting({
            action: playableAction.action,
            validTargets: playableAction.validTargets,
            selectedTargets: [],
            requiredCount: playableAction.targetCount ?? 1,
          })
        } else {
          // Play the card directly
          submitAction(playableAction.action)
        }
      }
      stopDraggingCard()
    }

    window.addEventListener('mouseup', handleGlobalMouseUp)
    return () => window.removeEventListener('mouseup', handleGlobalMouseUp)
  }, [draggingCardId, card.id, playableAction, submitAction, stopDraggingCard, startTargeting])

  // Global mouse up handler to cancel drag
  useEffect(() => {
    if (!draggingBlockerId) return

    const handleGlobalMouseUp = () => {
      stopDraggingBlocker()
    }

    window.addEventListener('mouseup', handleGlobalMouseUp)
    return () => window.removeEventListener('mouseup', handleGlobalMouseUp)
  }, [draggingBlockerId, stopDraggingBlocker])

  const handleClick = () => {
    // Handle targeting mode clicks
    if (isInTargetingMode && isValidTarget) {
      addTarget(card.id)
      // Auto-confirm if we have selected enough targets
      const newTargetCount = (targetingState?.selectedTargets.length ?? 0) + 1
      const requiredCount = targetingState?.requiredCount ?? 1
      if (newTargetCount >= requiredCount) {
        // Need to wait a tick for state to update before confirming
        setTimeout(() => confirmTargeting(), 0)
      }
      return
    }

    // Handle attacker mode clicks
    if (isInAttackerMode) {
      if (isValidAttacker) {
        toggleAttacker(card.id)
      }
      return
    }

    // Handle blocker mode clicks - clicking an assigned blocker removes it
    if (isInBlockerMode) {
      if (isValidBlocker && isSelectedAsBlocker) {
        removeBlockerAssignment(card.id)
        return
      }
      // Clicking is also handled by mouseup for drag & drop
      return
    }

    // Normal card selection (outside combat mode)
    if (interactive && !isInTargetingMode) {
      selectCard(isSelected ? null : card.id)
    }
  }

  // Determine border color based on state
  // Priority: attacking > blocking > selected > validTarget > validAttacker/Blocker > playable > default
  let borderStyle = '2px solid #333'
  let boxShadow = '0 2px 8px rgba(0,0,0,0.5)'

  if (isSelectedAsAttacker) {
    // Red for attacking creatures
    borderStyle = '3px solid #ff4444'
    boxShadow = '0 0 16px rgba(255, 68, 68, 0.7), 0 0 32px rgba(255, 68, 68, 0.4)'
  } else if (isSelectedAsBlocker) {
    // Blue for blocking creatures
    borderStyle = '3px solid #4488ff'
    boxShadow = '0 0 16px rgba(68, 136, 255, 0.7), 0 0 32px rgba(68, 136, 255, 0.4)'
  } else if (isAttackingInBlockerMode) {
    // Orange glow for attackers that can be blocked
    borderStyle = '3px solid #ff8800'
    boxShadow = '0 0 12px rgba(255, 136, 0, 0.6), 0 0 24px rgba(255, 136, 0, 0.3)'
  } else if (isSelected && !isInCombatMode) {
    borderStyle = '3px solid #ffff00'
    boxShadow = '0 8px 20px rgba(255, 255, 0, 0.4)'
  } else if (isValidTarget) {
    borderStyle = '3px solid #ff4444'
    boxShadow = '0 4px 15px rgba(255, 68, 68, 0.6)'
  } else if (isValidAttacker || isValidBlocker) {
    // Green highlight for valid attackers/blockers
    borderStyle = '2px solid #00ff00'
    boxShadow = '0 0 12px rgba(0, 255, 0, 0.5), 0 0 24px rgba(0, 255, 0, 0.3)'
  } else if (isPlayable) {
    // Green highlight for playable cards
    borderStyle = '2px solid #00ff00'
    boxShadow = '0 0 12px rgba(0, 255, 0, 0.5), 0 0 24px rgba(0, 255, 0, 0.3)'
  }

  // Determine cursor
  const canInteract = interactive || isValidTarget || isValidAttacker || isValidBlocker || isAttackingInBlockerMode || canDragToPlay
  const baseCursor = canInteract ? 'pointer' : 'default'
  const cursor = (isValidBlocker && !isSelectedAsBlocker) || canDragToPlay ? 'grab' : baseCursor

  // Check if currently being dragged (blocker or hand card)
  const isBeingDragged = draggingBlockerId === card.id || draggingCardId === card.id

  return (
    <div
      data-card-id={card.id}
      onClick={handleClick}
      onMouseDown={handleMouseDown}
      onMouseUp={handleMouseUp}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      style={{
        ...styles.card,
        width,
        height,
        borderRadius: responsive.isMobile ? 4 : 8,
        cursor,
        border: borderStyle,
        transform: `${card.isTapped ? 'rotate(90deg)' : ''} ${isSelected && !isInCombatMode ? 'translateY(-8px)' : ''}`,
        boxShadow,
        opacity: isBeingDragged ? 0.6 : 1,
        userSelect: 'none',
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
          <span style={{
            ...styles.cardPT,
            fontSize: responsive.fontSize.normal,
            color: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness)
          }}>
            {card.power}/{card.toughness}
          </span>
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

      {/* P/T overlay for creatures on battlefield */}
      {battlefield && !faceDown && card.power !== null && card.toughness !== null && (
        <div style={{
          ...styles.ptOverlay,
          backgroundColor: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness) !== 'white'
            ? 'rgba(0, 0, 0, 0.85)'
            : 'rgba(0, 0, 0, 0.7)',
        }}>
          <span style={{
            color: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness),
            fontWeight: 700,
            fontSize: responsive.isMobile ? 10 : 12,
          }}>
            {card.power}/{card.toughness}
          </span>
        </div>
      )}

      {/* Keyword ability icons */}
      {battlefield && !faceDown && card.keywords.length > 0 && (
        <KeywordIcons keywords={card.keywords} size={responsive.isMobile ? 14 : 18} />
      )}

      {/* Playable indicator glow effect (only outside combat mode) */}
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
  const startTargeting = useGameStore((state) => state.startTargeting)
  const responsive = useResponsiveContext()

  // If in targeting mode, show targeting UI instead
  if (targetingState) {
    const selectedCount = targetingState.selectedTargets.length
    const requiredCount = targetingState.requiredCount
    return (
      <div style={{
        ...styles.targetingOverlay,
        padding: responsive.isMobile ? '12px 16px' : '16px 24px',
      }}>
        <div style={{ ...styles.targetingPrompt, fontSize: responsive.fontSize.normal }}>
          Select a target ({selectedCount}/{requiredCount})
        </div>
        <div style={{ color: '#aaa', fontSize: responsive.fontSize.small, marginTop: 4 }}>
          Click a highlighted target
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

  const handleActionClick = (info: LegalActionInfo) => {
    // Check if action requires targeting
    if (info.requiresTargets && info.validTargets && info.validTargets.length > 0) {
      // Enter targeting mode
      startTargeting({
        action: info.action,
        validTargets: info.validTargets,
        selectedTargets: [],
        requiredCount: info.targetCount ?? 1,
      })
      selectCard(null)
    } else {
      // Submit action directly
      submitAction(info.action)
      selectCard(null)
    }
  }

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
            onClick={() => handleActionClick(info)}
            style={{
              ...styles.actionButton,
              padding: responsive.isMobile ? '10px 12px' : '12px 16px',
              fontSize: responsive.fontSize.normal,
            }}
          >
            {info.description}
            {info.requiresTargets && <span style={{ color: '#888', marginLeft: 8 }}>(select target)</span>}
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
    width: '100%',
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
  ptOverlay: {
    position: 'absolute',
    bottom: 4,
    right: 4,
    padding: '2px 6px',
    borderRadius: 4,
    border: '1px solid rgba(255, 255, 255, 0.3)',
  } as React.CSSProperties,
  keywordIconsContainer: {
    position: 'absolute',
    top: 4,
    left: 4,
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
    pointerEvents: 'none',
  } as React.CSSProperties,
  keywordIconWrapper: {
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    borderRadius: 4,
    padding: 2,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: '1px solid rgba(255, 255, 255, 0.2)',
  } as React.CSSProperties,
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
  stackContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginLeft: 24,
    padding: '8px 12px',
    backgroundColor: 'rgba(100, 50, 150, 0.2)',
    borderRadius: 8,
    border: '1px solid rgba(150, 100, 200, 0.4)',
  },
  stackHeader: {
    color: '#b088d0',
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 8,
  },
  stackItems: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    position: 'relative',
  },
  stackItem: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    position: 'relative',
    cursor: 'pointer',
    transition: 'transform 0.15s',
  },
  stackItemImage: {
    width: 60,
    height: 84,
    objectFit: 'cover',
    borderRadius: 4,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
  },
  stackItemName: {
    color: '#ccc',
    marginTop: 4,
    maxWidth: 80,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    textAlign: 'center',
  },
  // Card preview styles
  cardPreviewOverlay: {
    position: 'fixed',
    top: 20,
    left: 20,
    zIndex: 1000,
    pointerEvents: 'none',
  } as React.CSSProperties,
  cardPreviewContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
  } as React.CSSProperties,
  cardPreviewCard: {
    borderRadius: 12,
    overflow: 'hidden',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.8), 0 0 0 2px rgba(255, 255, 255, 0.1)',
  } as React.CSSProperties,
  cardPreviewImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  } as React.CSSProperties,
  cardPreviewStats: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '8px 16px',
    borderRadius: 8,
    gap: 2,
  } as React.CSSProperties,
  cardPreviewKeywords: {
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.85)',
    padding: 12,
    borderRadius: 8,
    border: '1px solid rgba(255, 255, 255, 0.1)',
  } as React.CSSProperties,
  cardPreviewKeyword: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  } as React.CSSProperties,
  cardPreviewKeywordName: {
    color: '#ffcc00',
    fontWeight: 600,
    fontSize: 14,
  } as React.CSSProperties,
  // Active effect badge styles
  effectBadgesContainer: {
    display: 'flex',
    flexDirection: 'row',
    gap: 4,
    flexWrap: 'wrap',
  } as React.CSSProperties,
  effectBadge: {
    display: 'flex',
    alignItems: 'center',
    gap: 4,
    backgroundColor: 'rgba(255, 100, 100, 0.2)',
    border: '1px solid rgba(255, 100, 100, 0.5)',
    borderRadius: 4,
    color: '#ff8888',
  } as React.CSSProperties,
  effectBadgeIcon: {
    fontSize: 12,
  } as React.CSSProperties,
  effectBadgeName: {
    fontWeight: 500,
    whiteSpace: 'nowrap',
  } as React.CSSProperties,
}
