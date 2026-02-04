import { useGameStore } from '../../../store/gameStore'
import { useStackCards } from '../../../store/selectors'
import type { EntityId } from '../../../types'
import { getCardImageUrl } from '../../../utils/cardImages'
import { useResponsiveContext, handleImageError } from './shared'
import { styles } from './styles'

/**
 * Stack display - shows spells/abilities waiting to resolve.
 * Cards stack on top of each other like a physical pile.
 */
export function StackDisplay() {
  const stackCards = useStackCards()
  const responsive = useResponsiveContext()
  const hoverCard = useGameStore((state) => state.hoverCard)
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)

  if (stackCards.length === 0) return null

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

  return (
    <div style={styles.stackContainer}>
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
                marginTop: index === 0 ? 0 : -84 + cardOffset, // Overlap cards, showing cardOffset pixels of each
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
                  cursor: isValidTarget ? 'pointer' : 'default',
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
    </div>
  )
}
