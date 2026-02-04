import { useBattlefieldCards, groupCards } from '../../../store/selectors'
import { useResponsiveContext } from './shared'
import { styles } from './styles'
import { CardStack } from '../card'

/**
 * Battlefield area with lands and creatures.
 * For player: creatures first (closer to center), then lands (closer to player)
 * For opponent: lands first (closer to opponent), then creatures (closer to center)
 */
export function Battlefield({ isOpponent, spectatorMode = false }: { isOpponent: boolean; spectatorMode?: boolean }) {
  const {
    playerLands,
    playerCreatures,
    playerOther,
    opponentLands,
    opponentCreatures,
    opponentOther,
  } = useBattlefieldCards()
  const responsive = useResponsiveContext()

  const lands = isOpponent ? opponentLands : playerLands
  const creatures = isOpponent ? opponentCreatures : playerCreatures
  const other = isOpponent ? opponentOther : playerOther

  // Group identical lands, display creatures and other individually
  const groupedLands = groupCards(lands)
  const groupedCreatures = creatures.map((card) => ({
    card,
    count: 1,
    cardIds: [card.id] as const,
    cards: [card] as const,
  }))
  const groupedOther = other.map((card) => ({
    card,
    count: 1,
    cardIds: [card.id] as const,
    cards: [card] as const,
  }))

  // Layout: Lands anchored near hand, creatures toward center
  // For player: lands at bottom (near hand), creatures above
  // For opponent: lands at top (near hand), creatures below
  const hasCreatures = groupedCreatures.length > 0
  const hasLands = groupedLands.length > 0
  const hasOther = groupedOther.length > 0
  const showDivider = (hasCreatures || hasOther) && hasLands

  return (
    <div
      style={{
        ...styles.battlefieldArea,
        justifyContent: isOpponent ? 'flex-start' : 'flex-end',
        gap: 0,
      }}
    >
      {/* For player: creatures first (top, toward center) */}
      {!isOpponent && (
        <>
          {/* Creatures row */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            minHeight: responsive.battlefieldCardHeight + responsive.battlefieldRowPadding,
          }}>
            {groupedCreatures.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>

          {/* Other permanents row */}
          {hasOther && (
            <div style={{ ...styles.battlefieldRow, gap: responsive.cardGap, marginTop: 4 }}>
              {groupedOther.map((group) => (
                <CardStack
                  key={group.cardIds[0]}
                  group={group}
                  interactive={!spectatorMode && !isOpponent}
                  isOpponentCard={isOpponent}
                />
              ))}
            </div>
          )}

          {/* Divider */}
          {showDivider && (
            <div
              style={{
                width: '40%',
                height: 1,
                backgroundColor: '#444',
                margin: '6px 0',
              }}
            />
          )}

          {/* Lands row (bottom, near hand) */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            marginBottom: -40,
          }}>
            {groupedLands.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>
        </>
      )}

      {/* For opponent: lands first (top, near hand) */}
      {isOpponent && (
        <>
          {/* Lands row (top, near hand) */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            marginTop: -40,
          }}>
            {groupedLands.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>

          {/* Divider */}
          {showDivider && (
            <div
              style={{
                width: '40%',
                height: 1,
                backgroundColor: '#444',
                margin: '6px 0',
              }}
            />
          )}

          {/* Other permanents row */}
          {hasOther && (
            <div style={{ ...styles.battlefieldRow, gap: responsive.cardGap, marginBottom: 4 }}>
              {groupedOther.map((group) => (
                <CardStack
                  key={group.cardIds[0]}
                  group={group}
                  interactive={!spectatorMode && !isOpponent}
                  isOpponentCard={isOpponent}
                />
              ))}
            </div>
          )}

          {/* Creatures row (bottom, toward center) */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            minHeight: responsive.battlefieldCardHeight + responsive.battlefieldRowPadding,
          }}>
            {groupedCreatures.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
