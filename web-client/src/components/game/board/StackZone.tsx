import { useGameStore } from '../../../store/gameStore'
import { useStackCards } from '../../../store/selectors'
import type { EntityId } from '../../../types'
import { getCardImageUrl } from '../../../utils/cardImages'
import { ActiveEffectBadges } from '../card/CardOverlays'
import { useResponsiveContext, handleImageError } from './shared'
import { styles } from './styles'

/**
 * Stack display - shows spells/abilities waiting to resolve.
 * Cards stack on top of each other like a physical pile.
 * Also shows a combat trigger indicator when a YesNo decision is pending.
 */
export function StackDisplay() {
  const stackCards = useStackCards()
  const responsive = useResponsiveContext()
  const hoverCard = useGameStore((state) => state.hoverCard)
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const gameState = useGameStore((state) => state.gameState)

  // Combat trigger YesNo: show source card in stack area
  const isCombatTriggerYesNo = pendingDecision?.type === 'YesNoDecision'
    && !!pendingDecision.context.triggeringEntityId
    && !!gameState?.combat

  const showStack = stackCards.length > 0 || isCombatTriggerYesNo
  if (!showStack) return null

  const handleStackItemClick = (cardId: EntityId) => {
    if (!targetingState) return

    const isValidTarget = targetingState.validTargets.includes(cardId)
    const isSelectedTarget = targetingState.selectedTargets.includes(cardId)

    if (isSelectedTarget) {
      removeTarget(cardId)
    } else if (isValidTarget) {
      addTarget(cardId)
    }
  }

  // Offset between cards - shows a sliver of each card below
  const cardOffset = 20
  // Top of stack (most recently cast, resolves first) is last in the array
  const topCard = stackCards[stackCards.length - 1]

  // Get source card info for combat trigger
  const sourceCard = isCombatTriggerYesNo && pendingDecision?.type === 'YesNoDecision'
    ? (() => {
        const sourceId = pendingDecision.context.sourceId
        return sourceId ? gameState?.cards[sourceId] : null
      })()
    : null
  const stackImageWidth = responsive.isMobile ? 44 : 60
  const stackImageHeight = responsive.isMobile ? 62 : 84

  return (
    <div style={{
      ...styles.stackContainer,
      left: responsive.isMobile ? 4 : 16,
      padding: responsive.isMobile ? '4px 6px' : '8px 12px',
    }}>
      {/* Regular stack items */}
      {stackCards.length > 0 && (
        <>
          <div style={{
            ...styles.stackHeader,
            fontSize: responsive.fontSize.small,
          }}>
            Stack ({stackCards.length})
          </div>
          <div style={styles.stackItems}>
            {stackCards.map((card, index) => {
              const isValidTarget = targetingState?.validTargets.includes(card.id) ?? false
              const isSelectedTarget = targetingState?.selectedTargets.includes(card.id) ?? false

              return (
                <div
                  key={card.id}
                  data-card-id={card.id}
                  style={{
                    ...styles.stackItem,
                    marginTop: index === 0 ? 0 : -stackImageHeight + cardOffset, // Overlap cards, showing cardOffset pixels of each
                    zIndex: index + 1, // Later cards (higher index = cast later) on top
                    ...(isValidTarget && !isSelectedTarget ? {
                      boxShadow: '0 0 12px 4px rgba(255, 200, 0, 0.8)',
                      borderRadius: 6,
                    } : {}),
                    ...(isSelectedTarget ? {
                      boxShadow: '0 0 12px 4px rgba(0, 255, 100, 0.8)',
                      borderRadius: 6,
                    } : {}),
                  }}
                  onClick={() => handleStackItemClick(card.id)}
                  onMouseEnter={() => hoverCard(card.id)}
                  onMouseLeave={() => hoverCard(null)}
                >
                  <img
                    src={getCardImageUrl(card.name, card.imageUri, 'small')}
                    alt={card.name}
                    style={{
                      ...styles.stackItemImage,
                      width: stackImageWidth,
                      height: stackImageHeight,
                      cursor: isValidTarget ? 'pointer' : 'default',
                      ...(card.sourceZone === 'GRAVEYARD' ? {
                        opacity: 0.7,
                        filter: 'saturate(0.6)',
                      } : {}),
                    }}
                    title={`${card.name}\n${card.oracleText || ''}`}
                    onError={(e) => handleImageError(e, card.name, 'small')}
                  />
                  {/* Show chosen X value for X spells */}
                  {card.chosenX != null && (
                    <div style={styles.stackXBadge}>
                      X={card.chosenX}
                    </div>
                  )}
                  {/* Show chosen creature type for spells like Aphetto Dredging */}
                  {card.chosenCreatureType && (
                    <div style={{
                      position: 'absolute',
                      bottom: 4,
                      left: 4,
                      backgroundColor: 'rgba(80, 60, 30, 0.9)',
                      color: '#f0d890',
                      fontSize: 9,
                      padding: '1px 4px',
                      borderRadius: 3,
                      border: '1px solid rgba(200, 170, 80, 0.6)',
                      whiteSpace: 'nowrap',
                      pointerEvents: 'none',
                      zIndex: 5,
                    }}>
                      {card.chosenCreatureType}
                    </div>
                  )}
                  {/* Show sacrificed creature types for spells like Endemic Plague */}
                  {card.sacrificedCreatureTypes && card.sacrificedCreatureTypes.length > 0 && (
                    <div style={{
                      position: 'absolute',
                      bottom: card.chosenCreatureType ? 20 : 4,
                      left: 4,
                      backgroundColor: 'rgba(80, 30, 30, 0.9)',
                      color: '#f0a0a0',
                      fontSize: 9,
                      padding: '1px 4px',
                      borderRadius: 3,
                      border: '1px solid rgba(200, 80, 80, 0.6)',
                      whiteSpace: 'nowrap',
                      pointerEvents: 'none',
                      zIndex: 5,
                    }}>
                      {card.sacrificedCreatureTypes.join(', ')}
                    </div>
                  )}
                  {/* Show text modification badges (e.g., Artificial Evolution) */}
                  {card.activeEffects && card.activeEffects.length > 0 && (
                    <div style={styles.stackActiveEffects}>
                      <ActiveEffectBadges effects={card.activeEffects} />
                    </div>
                  )}
                </div>
              )
            })}
            {/* Show name of top card (most recently cast) */}
            {topCard && (
              <div style={{
                ...styles.stackItemName,
                fontSize: responsive.fontSize.small,
                marginTop: 4,
              }}>
                {topCard.name}
              </div>
            )}
          </div>
        </>
      )}

      {/* Combat trigger indicator - shows source card and prompt when YesNo is pending */}
      {isCombatTriggerYesNo && pendingDecision?.type === 'YesNoDecision' && (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 6,
          marginTop: stackCards.length > 0 ? 12 : 0,
        }}>
          {/* "Resolving" header */}
          <div style={{
            ...styles.stackHeader,
            fontSize: responsive.fontSize.small,
            color: '#ff8c42',
            marginBottom: 0,
          }}>
            Resolving
          </div>

          {/* Source card image */}
          {sourceCard && (
            <div
              onMouseEnter={() => sourceCard && hoverCard(pendingDecision.context.sourceId!)}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(sourceCard.name, sourceCard.imageUri, 'small')}
                alt={sourceCard.name}
                style={{
                  ...styles.stackItemImage,
                  width: stackImageWidth,
                  height: stackImageHeight,
                  boxShadow: '0 0 12px 4px rgba(255, 107, 53, 0.6)',
                  borderRadius: 6,
                  cursor: 'default',
                }}
                onError={(e) => handleImageError(e, sourceCard.name, 'small')}
              />
            </div>
          )}

          {/* Source name */}
          <div style={{
            ...styles.stackItemName,
            fontSize: responsive.fontSize.small,
            color: '#ff8c42',
            fontWeight: 600,
          }}>
            {pendingDecision.context.sourceName ?? 'Trigger'}
          </div>

          {/* Prompt text describing what the trigger does */}
          <div style={{
            color: '#ccc',
            fontSize: responsive.isMobile ? 9 : 10,
            textAlign: 'center',
            maxWidth: 100,
            lineHeight: 1.3,
          }}>
            {pendingDecision.prompt}
          </div>
        </div>
      )}
    </div>
  )
}
