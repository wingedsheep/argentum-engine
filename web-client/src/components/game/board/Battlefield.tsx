import { useBattlefieldCards, groupCards } from '../../../store/selectors'
import { useResponsiveContext } from './shared'
import { styles } from './styles'
import { CardStack } from '../card'
import { GroupedCard } from '../../../store/selectors'

/**
 * Battlefield area with two rows per player, each using a 3-column grid:
 *
 *   Front row: [planeswalkers (right-aligned)] | [creatures (centered)] | [spacer]
 *   Back row:  [enchantments/artifacts (right-aligned)] | [lands (centered)] | [spacer]
 *
 * For player: front row on top (toward center), back row on bottom (near hand).
 * For opponent: back row on top (near hand), front row on bottom (toward center).
 */
export function Battlefield({ isOpponent, spectatorMode = false }: { isOpponent: boolean; spectatorMode?: boolean }) {
  const {
    playerLands,
    playerCreatures,
    playerPlaneswalkers,
    playerOther,
    opponentLands,
    opponentCreatures,
    opponentPlaneswalkers,
    opponentOther,
  } = useBattlefieldCards()
  const responsive = useResponsiveContext()

  const lands = isOpponent ? opponentLands : playerLands
  const creatures = isOpponent ? opponentCreatures : playerCreatures
  const planeswalkers = isOpponent ? opponentPlaneswalkers : playerPlaneswalkers
  const other = isOpponent ? opponentOther : playerOther

  // Group identical lands, display creatures/planeswalkers/other individually
  const groupedLands = groupCards(lands)
  const toSingles = (cards: typeof creatures) => cards.map((card) => ({
    card,
    count: 1,
    cardIds: [card.id] as const,
    cards: [card] as const,
  }))
  const groupedCreatures = toSingles(creatures)
  const groupedPlaneswalkers = toSingles(planeswalkers)
  const groupedOther = toSingles(other)

  const hasCreatures = groupedCreatures.length > 0
  const hasPlaneswalkers = groupedPlaneswalkers.length > 0
  const hasLands = groupedLands.length > 0
  const hasOther = groupedOther.length > 0
  const hasFrontRow = hasCreatures || hasPlaneswalkers
  const hasBackRow = hasLands || hasOther
  const showDivider = hasFrontRow && hasBackRow

  const interactive = !spectatorMode && !isOpponent

  /**
   * Renders a 3-column grid row:
   *   [side items (right-aligned)] | [center items (centered)] | [spacer]
   * Used for both the creature row and the land row.
   */
  const renderGridRow = (
    centerItems: readonly GroupedCard[],
    sideItems: readonly GroupedCard[],
    extra?: React.CSSProperties,
  ) => (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '1fr auto 1fr',
      alignItems: 'center',
      width: '100%',
      ...extra,
    }}>
      {/* Left column: side items, right-aligned to sit near center */}
      <div style={{
        display: 'flex',
        justifyContent: 'flex-end',
        flexWrap: 'wrap',
        gap: responsive.cardGap,
        paddingRight: sideItems.length > 0 && centerItems.length > 0 ? responsive.cardGap * 2 : 0,
      }}>
        {sideItems.map((group) => (
          <CardStack
            key={group.cardIds[0]}
            group={group}
            interactive={interactive}
            isOpponentCard={isOpponent}
          />
        ))}
      </div>

      {/* Center column: main items */}
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        flexWrap: 'wrap',
        gap: responsive.cardGap,
      }}>
        {centerItems.map((group) => (
          <CardStack
            key={group.cardIds[0]}
            group={group}
            interactive={interactive}
            isOpponentCard={isOpponent}
          />
        ))}
      </div>

      {/* Right column: empty spacer to balance the grid */}
      <div />
    </div>
  )

  const renderDivider = () => showDivider ? (
    <div
      style={{
        width: '40%',
        height: 1,
        backgroundColor: '#444',
        margin: '6px 0',
      }}
    />
  ) : null

  const frontRow = renderGridRow(
    groupedCreatures,
    groupedPlaneswalkers,
    { minHeight: responsive.battlefieldCardHeight + responsive.battlefieldRowPadding },
  )

  return (
    <div
      style={{
        ...styles.battlefieldArea,
        justifyContent: isOpponent ? 'flex-start' : 'flex-end',
        gap: 0,
      }}
    >
      {/* For player: front row (top, toward center), then back row (bottom, near hand) */}
      {!isOpponent && (
        <>
          {frontRow}
          {renderDivider()}
          {renderGridRow(groupedLands, groupedOther, { marginBottom: -40 })}
        </>
      )}

      {/* For opponent: back row (top, near hand), then front row (bottom, toward center) */}
      {isOpponent && (
        <>
          {renderGridRow(groupedLands, groupedOther, { marginTop: -40 })}
          {renderDivider()}
          {frontRow}
        </>
      )}
    </div>
  )
}
