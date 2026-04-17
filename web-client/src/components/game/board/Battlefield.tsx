import { useMemo } from 'react'
import { useBattlefieldCards, groupCards, selectViewingPlayerId } from '@/store/selectors.ts'
import { useGameStore } from '@/store/gameStore.ts'
import { useResponsiveContext } from './shared'
import { styles } from './styles'
import { CardStack } from '../card'
import { GameCard } from '../card'
import { GroupedCard } from '@/store/selectors.ts'
import type { ClientCard } from '@/types'
import { RenderProfiler } from '@/utils/renderProfiler'

const EMPTY_ATTACHMENTS: readonly ClientCard[] = []

function toSinglesStable(cards: readonly ClientCard[]): GroupedCard[] {
  return cards.map((card) => ({
    card,
    count: 1,
    cardIds: [card.id],
    cards: [card],
  }))
}

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
    attachmentsByCardId,
  } = useBattlefieldCards()
  const responsive = useResponsiveContext()

  const lands = isOpponent ? opponentLands : playerLands
  const creatures = isOpponent ? opponentCreatures : playerCreatures
  const planeswalkers = isOpponent ? opponentPlaneswalkers : playerPlaneswalkers
  const other = isOpponent ? opponentOther : playerOther

  // Group identical lands, display creatures/planeswalkers/other individually.
  // Memoized so these arrays keep stable identity across unrelated store updates —
  // otherwise every battlefield re-render allocates fresh arrays that cascade
  // into child re-renders and invalidate downstream useMemos.
  const groupedLands = useMemo(() => groupCards(lands), [lands])
  const groupedCreatures = useMemo(() => toSinglesStable(creatures), [creatures])
  const groupedPlaneswalkers = useMemo(() => toSinglesStable(planeswalkers), [planeswalkers])
  const groupedOther = useMemo(() => toSinglesStable(other), [other])

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
    const resolved = attachmentsByCardId.get(group.card.id)
    const attachmentCards = resolved?.attachments ?? EMPTY_ATTACHMENTS
    const linkedExileCards = resolved?.linkedExile ?? EMPTY_ATTACHMENTS
    const attachments = attachmentCards.length === 0 && linkedExileCards.length === 0
      ? EMPTY_ATTACHMENTS
      : [...attachmentCards, ...linkedExileCards]
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

    // Tapped: attachments peek horizontally (left offset) to avoid overlap with the rotation gap
    // Untapped: attachments peek vertically (top offset) from above the parent card
    const containerWidth = parentTapped ? cardVisualWidth + totalPeek : cardVisualWidth
    const containerHeight = parentTapped ? cardHeight : cardHeight + totalPeek

    return (
      <div
        key={group.cardIds[0]}
        style={{
          position: 'relative',
          width: containerWidth,
          height: containerHeight,
        }}
      >
        {/* Attachments peek from the parent card */}
        {attachments.map((attachment, index) => {
          // Attachments controlled by the player are interactive even on the opponent's battlefield
          // (e.g., aura cast on opponent's creature — caster can still activate abilities)
          const attachmentInteractive = !spectatorMode && attachment.controllerId === viewingPlayerId
          return (
            <div
              key={attachment.id}
              style={{
                position: 'absolute',
                left: parentTapped ? index * attachmentPeek : 0,
                top: parentTapped ? 0 : index * attachmentPeek,
                zIndex: index,
                pointerEvents: 'none',
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
        {/* Main card, on top */}
        <div style={{
          position: 'absolute',
          left: parentTapped ? totalPeek : 0,
          top: parentTapped ? 0 : totalPeek,
          zIndex: attachments.length + 1,
          pointerEvents: 'none',
        }}>
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
   * Renders a row as two centered flex sub-groups side-by-side:
   *   [center items] [divider] [side items]
   * Each sub-group wraps internally, so when the center column (lands /
   * creatures) overflows, those items stack within their own column
   * instead of pushing the side items (enchantments / planeswalkers)
   * onto a new line below. The two sub-groups are centered as a whole,
   * keeping the row's visual weight at the viewport center.
   */
  const renderGridRow = (
    centerItems: readonly GroupedCard[],
    sideItems: readonly GroupedCard[],
    extra?: React.CSSProperties,
  ) => {
    const hasSide = sideItems.length > 0 && centerItems.length > 0
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'flex-end',
        flexWrap: 'nowrap',
        gap: responsive.cardGap,
        width: '100%',
        ...extra,
      }}>
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'flex-end',
          justifyContent: 'center',
          gap: responsive.cardGap,
          minWidth: 0,
        }}>
          {centerItems.map((group) => renderWithAttachments(group))}
        </div>
        {hasSide && (
          <div style={{
            width: 24,
            alignSelf: 'stretch',
            background: 'radial-gradient(ellipse at center, rgba(120, 140, 180, 0.12) 0%, rgba(120, 140, 180, 0.04) 45%, transparent 75%)',
            pointerEvents: 'none',
            flexShrink: 0,
          }} />
        )}
        {hasSide && (
          <div style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'flex-end',
            gap: responsive.cardGap,
            minWidth: 0,
          }}>
            {sideItems.map((group) => renderWithAttachments(group))}
          </div>
        )}
      </div>
    )
  }

  const dividerMargin = Math.max(10, Math.round(responsive.battlefieldCardHeight * 0.1))
  const renderDivider = () => showDivider ? (
    <div
      style={{
        width: '70%',
        height: 24,
        margin: `${dividerMargin}px 0`,
        background: 'radial-gradient(ellipse at center, rgba(120, 140, 180, 0.12) 0%, rgba(120, 140, 180, 0.04) 45%, transparent 75%)',
        pointerEvents: 'none',
      }}
    />
  ) : null

  const frontRow = renderGridRow(
    groupedCreatures,
    groupedPlaneswalkers,
    { minHeight: responsive.battlefieldCardHeight + responsive.battlefieldRowPadding },
  )

  return (
    <RenderProfiler id={isOpponent ? 'Battlefield(opponent)' : 'Battlefield(player)'}>
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
          {renderGridRow(groupedLands, groupedOther)}
        </>
      )}

      {/* For opponent: back row (top, near hand), then front row (bottom, toward center) */}
      {isOpponent && (
        <>
          {renderGridRow(groupedLands, groupedOther)}
          {renderDivider()}
          {frontRow}
        </>
      )}
    </div>
    </RenderProfiler>
  )
}
