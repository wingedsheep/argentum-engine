import { useBattlefieldCards, groupCards, selectGameState, selectViewingPlayerId } from '../../../store/selectors'
import { useGameStore } from '../../../store/gameStore'
import { useResponsiveContext } from './shared'
import { styles } from './styles'
import { CardStack } from '../card'
import { GameCard } from '../card'
import { GroupedCard } from '../../../store/selectors'
import type { ClientCard } from '../../../types'

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
  const gameState = useGameStore(selectGameState)
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

  // Resolve attachment cards for a permanent
  const getAttachments = (card: ClientCard): ClientCard[] => {
    if (!gameState || card.attachments.length === 0) return []
    return card.attachments
      .map((id) => gameState.cards[id])
      .filter((c): c is ClientCard => c != null)
  }

  const hasCreatures = groupedCreatures.length > 0
  const hasPlaneswalkers = groupedPlaneswalkers.length > 0
  const hasLands = groupedLands.length > 0
  const hasOther = groupedOther.length > 0
  const hasFrontRow = hasCreatures || hasPlaneswalkers
  const hasBackRow = hasLands || hasOther
  const showDivider = hasFrontRow && hasBackRow

  const viewingPlayerId = useGameStore(selectViewingPlayerId)
  const interactive = !spectatorMode && !isOpponent

  // How much of each attachment card peeks out from behind its parent
  const attachmentPeek = responsive.isMobile ? 12 : 16
  const cardHeight = Math.round(responsive.battlefieldCardWidth * 1.4)

  /**
   * Renders a permanent with any attached cards (auras, equipment) stacked underneath.
   * Works for any permanent type - creatures, lands, planeswalkers, etc.
   *
   * Untapped: attachments peek vertically from above the parent card.
   * Tapped: attachments peek horizontally to the right of the parent card.
   */
  const renderWithAttachments = (group: GroupedCard) => {
    const attachments = getAttachments(group.card)
    if (attachments.length === 0) {
      return (
        <CardStack
          key={group.cardIds[0]}
          group={group}
          interactive={interactive}
          isOpponentCard={isOpponent}
        />
      )
    }

    const parentTapped = group.card.isTapped
    const totalPeek = attachments.length * attachmentPeek
    // When tapped, cards rotate 90deg so their visual width becomes the height
    const cardVisualWidth = parentTapped ? cardHeight + 8 : responsive.battlefieldCardWidth

    return (
      <div
        key={group.cardIds[0]}
        style={{
          position: 'relative',
          width: cardVisualWidth,
          height: cardHeight + totalPeek,
        }}
      >
        {/* Attachments peek vertically from above the parent */}
        {attachments.map((attachment, index) => {
          // Attachments controlled by the player are interactive even on the opponent's battlefield
          // (e.g., aura cast on opponent's creature — caster can still activate abilities)
          const attachmentInteractive = !spectatorMode && attachment.controllerId === viewingPlayerId
          return (
            <div
              key={attachment.id}
              style={{
                position: 'absolute',
                left: 0,
                top: index * attachmentPeek,
                zIndex: index,
              }}
            >
              <GameCard
                card={attachment}
                interactive={attachmentInteractive}
                battlefield
                isOpponentCard={isOpponent}
                forceTapped={parentTapped}
              />
            </div>
          )
        })}
        {/* Main card at the bottom, on top */}
        <div style={{ position: 'absolute', left: 0, top: totalPeek, zIndex: attachments.length + 1, pointerEvents: 'none' }}>
          <CardStack
            group={group}
            interactive={interactive}
            isOpponentCard={isOpponent}
          />
        </div>
      </div>
    )
  }

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
      alignItems: 'end',
      width: '100%',
      ...extra,
    }}>
      {/* Left column: side items, right-aligned to sit near center */}
      <div style={{
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'flex-end',
        flexWrap: 'wrap',
        gap: responsive.cardGap,
        paddingRight: sideItems.length > 0 && centerItems.length > 0 ? responsive.cardGap * 2 : 0,
      }}>
        {sideItems.map((group) => renderWithAttachments(group))}
      </div>

      {/* Center column: main items */}
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'flex-end',
        flexWrap: 'wrap',
        gap: responsive.cardGap,
      }}>
        {centerItems.map((group) => renderWithAttachments(group))}
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
      data-zone={isOpponent ? 'opponent-battlefield' : 'player-battlefield'}
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
