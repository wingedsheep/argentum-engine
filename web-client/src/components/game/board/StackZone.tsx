import { useGameStore } from '@/store/gameStore.ts'
import { useStackCards } from '@/store/selectors.ts'
import type { EntityId } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { ActiveEffectBadges } from '../card/CardOverlays'
import { AbilityText } from '../../ui/ManaSymbols'
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
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const gameState = useGameStore((state) => state.gameState)

  // Trigger YesNo: show source card in stack area when a triggered ability has a triggering entity
  const isTriggerYesNo = pendingDecision?.type === 'YesNoDecision'
    && !!pendingDecision.context.triggeringEntityId

  const showStack = stackCards.length > 0 || isTriggerYesNo
  if (!showStack) return null

  const handleStackItemClick = (cardId: EntityId) => {
    // Decision-time targeting (e.g., cycling Complicate → ChooseTargetsDecision for stack spells)
    if (decisionSelectionState) {
      const isValidOption = decisionSelectionState.validOptions.includes(cardId)
      if (isValidOption) {
        toggleDecisionSelection(cardId)
      }
      return
    }

    // Cast-time targeting (e.g., casting a counterspell targeting a stack spell)
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
  const cardOffset = 25
  // Top of stack (most recently cast, resolves first) is last in the array
  const topCard = stackCards[stackCards.length - 1]

  // Get source card info for combat trigger
  const sourceCard = isTriggerYesNo && pendingDecision?.type === 'YesNoDecision'
    ? (() => {
        const sourceId = pendingDecision.context.sourceId
        return sourceId ? gameState?.cards[sourceId] : null
      })()
    : null
  const stackImageWidth = responsive.isMobile ? 55 : 140
  const stackImageHeight = responsive.isMobile ? 77 : 196

  return (
    <div style={{
      position: 'fixed',
      left: responsive.isMobile ? 12 : 120,
      top: '50%',
      transform: 'translateY(-50%)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 6,
      zIndex: 50,
      maxHeight: '80vh',
    }}>
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: responsive.isMobile ? '4px 6px' : '8px 12px',
      backgroundColor: 'rgba(100, 50, 150, 0.3)',
      borderRadius: 8,
      border: '1px solid rgba(150, 100, 200, 0.4)',
      maxHeight: '60vh',
      overflowY: 'auto',
      maxWidth: 'calc(100vw - 32px)',
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
              const isValidTarget = (targetingState?.validTargets.includes(card.id) ?? false)
                || (decisionSelectionState?.validOptions.includes(card.id) ?? false)
              const isSelectedTarget = (targetingState?.selectedTargets.includes(card.id) ?? false)
                || (decisionSelectionState?.selectedOptions.includes(card.id) ?? false)

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
                    } : isSelectedTarget ? {
                      boxShadow: '0 0 12px 4px rgba(0, 255, 100, 0.8)',
                      borderRadius: 6,
                    } : card.copyIndex != null ? {
                      boxShadow: '0 0 8px 2px rgba(60, 140, 255, 0.5)',
                      borderRadius: 6,
                    } : {}),
                  }}
                  onClick={() => handleStackItemClick(card.id)}
                  onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
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
                    title={card.name}
                    onError={(e) => handleImageError(e, card.name, 'small')}
                  />
                  {/* Show chosen X value for X spells */}
                  {card.chosenX != null && (
                    <div style={styles.stackXBadge}>
                      X={card.chosenX}
                    </div>
                  )}
                  {/* Show kicked badge */}
                  {card.wasKicked && (
                    <div style={styles.stackKickedBadge}>
                      Kicked
                    </div>
                  )}
                  {/* Show gift badge when the caster promised a gift (Bloomburrow) */}
                  {card.giftPromised && (
                    <div
                      style={{
                        ...styles.stackGiftBadge,
                        top: card.wasKicked ? 26 : 4,
                      }}
                      title="Gift promised"
                    >
                      <i className="ms ms-ability-gift" style={{ fontSize: 12 }} />
                      <span>Gift</span>
                    </div>
                  )}
                  {/* Show copy badge for storm/copy effects */}
                  {card.copyIndex != null && card.copyTotal != null && (
                    <div style={styles.stackCopyBadge}>
                      Copy {card.copyIndex}/{card.copyTotal}
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
            {/* Card name below top card */}
            {topCard && (
              <div style={{
                color: '#e0d4f0',
                fontSize: responsive.isMobile ? 10 : 11,
                fontWeight: 600,
                marginTop: 4,
                textAlign: 'center',
                maxWidth: responsive.isMobile ? 80 : 100,
                lineHeight: 1.2,
              }}>
                {topCard.name}
              </div>
            )}
          </div>
        </>
      )}

      {/* Trigger indicator - shows source card and prompt when YesNo is pending */}
      {isTriggerYesNo && pendingDecision?.type === 'YesNoDecision' && (
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
              onMouseEnter={(e) => sourceCard && hoverCard(pendingDecision.context.sourceId!, { x: e.clientX, y: e.clientY })}
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

    {/* Ability text in a separate box below the stack */}
    {/* stackText = server-provided contextual text for spells (null means "don't show") */}
    {/* For abilities (activated/triggered), use oracleText which already contains specific ability text */}
    {(() => {
      if (!topCard) return null
      const isAbility = topCard.typeLine === 'Ability' || topCard.typeLine === 'Triggered Ability'
      const displayText = isAbility ? topCard.oracleText : topCard.stackText
      if (!displayText) return null
      return (
        <div style={{
          padding: responsive.isMobile ? '4px 6px' : '6px 10px',
          backgroundColor: 'rgba(30, 18, 50, 0.85)',
          borderRadius: 6,
          border: '1px solid rgba(150, 100, 200, 0.3)',
          maxWidth: responsive.isMobile ? 120 : 160,
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)',
        }}>
          <div style={{
            color: '#b8a8cc',
            fontSize: responsive.isMobile ? 8 : 9,
            lineHeight: 1.35,
            textAlign: 'center',
            whiteSpace: 'pre-line',
            overflow: 'hidden',
            display: '-webkit-box',
            WebkitLineClamp: 5,
            WebkitBoxOrient: 'vertical',
          }}>
            <AbilityText text={displayText} size={responsive.isMobile ? 9 : 10} />
          </div>
        </div>
      )
    })()}
    </div>
  )
}
