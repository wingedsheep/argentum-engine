import { useEffect, useMemo, useState } from 'react'
import { useBattlefieldCards, groupCards, selectViewingPlayerId } from '@/store/selectors.ts'
import { useGameStore } from '@/store/gameStore.ts'
import { useInteraction } from '@/hooks/useInteraction.ts'
import { useResponsiveContext, handleImageError } from './shared'
import { styles } from './styles'
import { CardStack } from '../card'
import { GameCard } from '../card'
import { GroupedCard } from '@/store/selectors.ts'
import type { ClientCard, EntityId } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { RenderProfiler } from '@/utils/renderProfiler'
import {
  TARGET_COLOR,
  TARGET_GLOW,
  TARGET_SHADOW,
  SELECTED_COLOR,
  SELECTED_GLOW,
} from '@/styles/targetingColors'

const EMPTY_ATTACHMENTS: readonly ClientCard[] = []

type AttachmentKind = 'attachment' | 'linkedExile'
type TaggedAttachment = { card: ClientCard; kind: AttachmentKind }
const EMPTY_TAGGED: readonly TaggedAttachment[] = []
// At this many attachments, the peek stack gets too tall and starts clipping
// other UI (turn bar, opposing row). Collapse to a badge + browser overlay instead.
const ATTACHMENT_COLLAPSE_THRESHOLD = 3

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

  // Parent card whose attachments the user is currently browsing in the overlay.
  // Only one at a time per battlefield instance (player / opponent each have their own).
  const [browsingAttachmentsOf, setBrowsingAttachmentsOf] = useState<ClientCard | null>(null)

  // Used to highlight the folder tab when something inside the collapsed stack is actionable.
  const legalActions = useGameStore((state) => state.legalActions)
  const targetingState = useGameStore((state) => state.targetingState)
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)

  // How much of each attachment card peeks out from behind its parent
  const attachmentPeek = responsive.isMobile ? 12 : 16
  const cardHeight = Math.round(responsive.battlefieldCardWidth * 1.4)

  const hasActionableAttachment = (attachmentList: readonly TaggedAttachment[]): boolean => {
    const ids = new Set(attachmentList.map((a) => a.card.id))
    if (targetingState && targetingState.validTargets.some((id) => ids.has(id))) return true
    if (decisionSelectionState && decisionSelectionState.validOptions.some((id) => ids.has(id))) return true
    return legalActions.some((info) => {
      const a = info.action
      switch (a.type) {
        case 'ActivateAbility':
        case 'TurnFaceUp':
          return ids.has(a.sourceId)
        case 'CrewVehicle':
          return ids.has(a.vehicleId)
        default:
          return false
      }
    })
  }

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
    const attachments: readonly TaggedAttachment[] =
      attachmentCards.length === 0 && linkedExileCards.length === 0
        ? EMPTY_TAGGED
        : [
            ...attachmentCards.map((card) => ({ card, kind: 'attachment' as const })),
            ...linkedExileCards.map((card) => ({ card, kind: 'linkedExile' as const })),
          ]
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
    // When attachments exceed the threshold, only the first still peeks — the rest collapse
    // into a "+N" badge that opens a browser overlay. Keeps the stack height bounded while
    // still signalling that attachments exist.
    const collapsed = attachments.length >= ATTACHMENT_COLLAPSE_THRESHOLD
    const visibleAttachments = collapsed ? attachments.slice(0, 1) : attachments
    const visiblePeek = visibleAttachments.length * attachmentPeek
    const cardWidth = responsive.battlefieldCardWidth
    // Lay out the whole stack (peeking attachments, main card, tab, click-catcher) in a
    // portrait inner container. When the parent is tapped, rotate that inner container 90°
    // so every element rotates together — no per-element offset math required. The outer
    // container takes the rotated footprint so the flex row reserves landscape space.
    const portraitWidth = cardWidth
    const portraitHeight = cardHeight + visiblePeek
    // A tapped (rotated) card is visually much wider than a portrait one, so the default
    // row gap feels tight next to its upright neighbours. Reserve a bit of extra space
    // inside the outer container so the rotated card doesn't sit flush against the next.
    const tappedGutter = parentTapped ? (responsive.isMobile ? 18 : 28) : 0
    const containerWidth = parentTapped ? portraitHeight + tappedGutter : portraitWidth
    const containerHeight = parentTapped ? portraitWidth : portraitHeight
    const tabHeight = responsive.isMobile ? 14 : 16
    const actionable = collapsed ? hasActionableAttachment(attachments) : false

    return (
      <div
        key={group.cardIds[0]}
        style={{
          position: 'relative',
          width: containerWidth,
          height: containerHeight,
        }}
      >
        <div
          style={{
            position: 'absolute',
            width: portraitWidth,
            height: portraitHeight,
            // Center the portrait inner inside the outer landscape box so rotation around
            // the center keeps the visual centered in the reserved slot.
            left: (containerWidth - portraitWidth) / 2,
            top: (containerHeight - portraitHeight) / 2,
            transform: parentTapped ? 'rotate(90deg)' : undefined,
            transformOrigin: 'center center',
          }}
        >
          {/* Attachments peek above the main card.
           * When collapsed, the visible peek is non-interactive — clicks go through the overlay
           * catcher below so the attachments browser is the single selection path. */}
          {visibleAttachments.map((tagged, index) => {
            const { card: attachment, kind } = tagged
            // Attachments controlled by the player are interactive even on the opponent's battlefield
            // (e.g., aura cast on opponent's creature — caster can still activate abilities)
            const attachmentInteractive = !collapsed && !spectatorMode && attachment.controllerId === viewingPlayerId
            return (
              <div
                key={attachment.id}
                style={{
                  position: 'absolute',
                  left: 0,
                  top: index * attachmentPeek,
                  zIndex: index,
                  pointerEvents: 'none',
                }}
              >
                <GameCard
                  card={attachment}
                  interactive={attachmentInteractive}
                  battlefield
                  isOpponentCard={isOpponent}
                  // Inner wrapper handles the tap rotation for the whole stack; individual
                  // cards must stay unrotated so they don't double-rotate.
                  suppressTapRotation
                  hideKeywordIcons
                  isGhost={kind === 'linkedExile'}
                />
              </div>
            )
          })}
          {collapsed && (
            <div
              onClick={(e) => {
                e.stopPropagation()
                setBrowsingAttachmentsOf(group.card)
              }}
              title={`${attachments.length} attached — click to browse`}
              style={{
                position: 'absolute',
                left: 0,
                top: 0,
                width: portraitWidth,
                height: attachmentPeek + 6,
                zIndex: visibleAttachments.length,
                cursor: 'pointer',
                pointerEvents: 'auto',
              }}
            />
          )}
          {/* Main card, on top of the peeking attachments */}
          <div style={{
            position: 'absolute',
            left: 0,
            top: visiblePeek,
            zIndex: visibleAttachments.length + 1,
            pointerEvents: 'none',
          }}>
            <GameCard
              card={group.card}
              interactive={interactive}
              battlefield
              isOpponentCard={isOpponent}
              // Suppress GameCard's own tap rotation — the outer wrapper rotates instead.
              suppressTapRotation
            />
          </div>
          {collapsed && (
            <button
              onClick={(e) => {
                e.stopPropagation()
                setBrowsingAttachmentsOf(group.card)
              }}
              title={
                actionable
                  ? `${attachments.length} attached — action available`
                  : `${attachments.length} attached — click to browse`
              }
              style={{
                position: 'absolute',
                // Folder tab above the first peeking attachment. Rotates with the inner
                // wrapper when the card is tapped, so it always follows the card.
                top: -tabHeight + 1,
                left: 6,
                height: tabHeight,
                minWidth: tabHeight + 4,
                background: 'rgba(124, 58, 237, 0.95)',
                color: 'white',
                fontWeight: 700,
                fontSize: responsive.isMobile ? 10 : 11,
                padding: '0 8px',
                borderRadius: '6px 6px 0 0',
                border: actionable
                  ? `2px solid ${TARGET_COLOR}`
                  : '1px solid rgba(255, 255, 255, 0.35)',
                borderBottom: 'none',
                cursor: 'pointer',
                pointerEvents: 'auto',
                zIndex: visibleAttachments.length + 2,
                boxShadow: actionable
                  ? `0 -1px 4px ${TARGET_GLOW}, 0 0 10px ${TARGET_SHADOW}`
                  : '0 -1px 3px rgba(0, 0, 0, 0.45)',
                userSelect: 'none',
                lineHeight: 1,
                whiteSpace: 'nowrap',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {attachments.length}
            </button>
          )}
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
      {browsingAttachmentsOf && (() => {
        const resolved = attachmentsByCardId.get(browsingAttachmentsOf.id)
        const attachments = resolved?.attachments ?? []
        const linkedExile = resolved?.linkedExile ?? []
        if (attachments.length === 0 && linkedExile.length === 0) {
          // Attachments cleared out while the overlay was open — close it.
          setBrowsingAttachmentsOf(null)
          return null
        }
        return (
          <AttachmentsBrowser
            parentName={browsingAttachmentsOf.name}
            attachments={attachments}
            linkedExile={linkedExile}
            onClose={() => setBrowsingAttachmentsOf(null)}
          />
        )
      })()}
    </div>
    </RenderProfiler>
  )
}

/**
 * Full-screen overlay for browsing cards attached / linked-exiled to a battlefield permanent.
 * Opened by clicking the "+N" badge that replaces the attachment peek-stack when it gets too tall.
 */
function AttachmentsBrowser({
  parentName,
  attachments,
  linkedExile,
  onClose,
}: {
  parentName: string
  attachments: readonly ClientCard[]
  linkedExile: readonly ClientCard[]
  onClose: () => void
}) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  // Both targeting flows: spell-cast pipeline (targetingState + addTarget/removeTarget)
  // and decision modal like ChooseTargetsDecision (decisionSelectionState + toggleDecisionSelection).
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)
  // Normal battlefield interactions (activate abilities, cast from hand, etc.) route through
  // useInteraction just like GameCard does — so clicking an aura in the overlay behaves
  // exactly as if it were clickable on the battlefield directly.
  const { handleCardClick } = useInteraction()
  const legalActions = useGameStore((state) => state.legalActions)
  const responsive = useResponsiveContext()

  const hasInteractiveAction = (cardId: EntityId): boolean =>
    legalActions.some((info) => {
      const a = info.action
      switch (a.type) {
        case 'ActivateAbility':
        case 'TurnFaceUp':
          return a.sourceId === cardId
        case 'CrewVehicle':
          return a.vehicleId === cardId
        default:
          return false
      }
    })

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  const onCardClick = (cardId: EntityId) => {
    // Targeting / selection modes take precedence — same precedence GameCard uses.
    if (decisionSelectionState?.validOptions.includes(cardId)) {
      toggleDecisionSelection(cardId)
      onClose()
      return
    }
    if (targetingState?.validTargets.includes(cardId)) {
      if (targetingState.selectedTargets.includes(cardId)) {
        removeTarget(cardId)
      } else {
        addTarget(cardId)
      }
      onClose()
      return
    }
    // Otherwise fall through to normal battlefield interaction (ability activation, etc.)
    handleCardClick(cardId)
    onClose()
  }

  const isValidTarget = (cardId: EntityId) =>
    decisionSelectionState?.validOptions.includes(cardId) ||
    targetingState?.validTargets.includes(cardId)

  const isSelectedTarget = (cardId: EntityId) =>
    decisionSelectionState?.selectedOptions.includes(cardId) ||
    targetingState?.selectedTargets.includes(cardId)

  const renderCard = (card: ClientCard, kind: AttachmentKind) => {
    const valid = isValidTarget(card.id)
    const selected = isSelectedTarget(card.id)
    const hasAction = !valid && !selected && hasInteractiveAction(card.id)
    const clickable = valid || selected || hasAction
    // Match the standard on-battlefield card highlight: ice-blue border + soft cyan glow
    // for actionable / valid-target cards, green for confirmed-selected.
    const isExile = kind === 'linkedExile'
    const ringColor = selected
      ? SELECTED_COLOR
      : valid || hasAction
      ? TARGET_COLOR
      : undefined
    const ringGlow = selected
      ? `0 0 12px ${SELECTED_GLOW}, 0 0 24px ${SELECTED_GLOW}`
      : valid || hasAction
      ? `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
      : undefined
    const ghostBorder = isExile && !ringColor ? '2px solid #6644aa' : undefined
    const ghostShadow = isExile && !ringColor ? '0 0 8px rgba(102, 68, 170, 0.4), 0 0 16px rgba(102, 68, 170, 0.2)' : undefined
    return (
      <div
        key={card.id}
        style={{
          position: 'relative',
          width: cardWidth,
          height: cardHeight,
          borderRadius: 6,
          flexShrink: 0,
          cursor: clickable ? 'pointer' : 'default',
          border: ringColor ? `2px solid ${ringColor}` : ghostBorder,
          boxShadow: ringGlow ?? ghostShadow,
          transition: 'border-color 120ms ease, box-shadow 120ms ease',
        }}
        onClick={() => { if (clickable) onCardClick(card.id) }}
        onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
        onMouseLeave={() => hoverCard(null)}
      >
        <img
          src={getCardImageUrl(card.name, card.imageUri, 'normal')}
          alt={card.name}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            borderRadius: 4,
            display: 'block',
            opacity: isExile ? 0.55 : 1,
          }}
          onError={(e) => handleImageError(e, card.name, 'normal')}
        />
        {selected && (
          <div
            aria-hidden
            style={{
              position: 'absolute',
              top: 6,
              right: 6,
              width: 22,
              height: 22,
              borderRadius: '50%',
              background: SELECTED_COLOR,
              color: '#0a2a1a',
              fontSize: 14,
              fontWeight: 800,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.5)',
            }}
          >
            ✓
          </div>
        )}
      </div>
    )
  }

  const renderSection = (kind: AttachmentKind, cards: readonly ClientCard[]) => {
    if (cards.length === 0) return null
    const isExile = kind === 'linkedExile'
    const accent = isExile ? '#a855f7' : '#22d3ee'
    const sectionBg = isExile ? 'rgba(88, 28, 135, 0.25)' : 'rgba(14, 116, 144, 0.20)'
    const label = isExile ? 'Linked Exile' : 'Attachments'
    return (
      <div
        key={kind}
        style={{
          borderRadius: 10,
          border: `1px solid ${accent}55`,
          background: sectionBg,
          padding: '12px 14px 14px',
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            color: accent,
            fontSize: 13,
            fontWeight: 700,
            letterSpacing: 0.5,
            textTransform: 'uppercase',
          }}
        >
          <span
            aria-hidden
            style={{
              display: 'inline-block',
              width: 10,
              height: 10,
              borderRadius: '50%',
              background: accent,
              boxShadow: `0 0 6px ${accent}`,
            }}
          />
          {label}
          <span style={{ color: '#e5e7eb', fontWeight: 600, opacity: 0.8 }}>({cards.length})</span>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, justifyContent: 'flex-start' }}>
          {cards.map((card) => renderCard(card, kind))}
        </div>
      </div>
    )
  }

  return (
    <div style={styles.exileOverlay} onClick={onClose}>
      <div style={styles.exileBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.exileBrowserHeader}>
          <h2 style={styles.exileBrowserTitle}>Attached to {parentName}</h2>
          <button style={styles.exileCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, overflowY: 'auto' }}>
          {renderSection('attachment', attachments)}
          {renderSection('linkedExile', linkedExile)}
        </div>
      </div>
    </div>
  )
}
