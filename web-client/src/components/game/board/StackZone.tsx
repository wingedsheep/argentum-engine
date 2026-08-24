import type React from 'react'
import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useStackCards, selectGameState } from '@/store/selectors.ts'
import { seatColor } from '@/styles/seatColors'
import type { EntityId } from '@/types'
import type { ClientAbilityIdentity, ClientCard } from '@/types/gameState'
import { getCardImageUrl, faceDownImageUrl } from '@/utils/cardImages.ts'
import { ActiveEffectBadges } from '../card/CardOverlays'
import { AbilityText, ManaCost } from '../../ui/ManaSymbols'
import { useOpenCardMenuOnTap } from '@/hooks/useOpenCardMenuOnTap.ts'
import { useResponsiveContext, handleImageError } from './shared'
import { styles } from './styles'
import { chosenModeGroupsForStack, groupStackCards, type StackGroup } from './stackGrouping'
import { YieldContextMenu } from './YieldContextMenu'

/**
 * Stack display - shows spells/abilities waiting to resolve.
 * Cards stack on top of each other like a physical pile.
 * Also shows a combat trigger indicator when a YesNo decision is pending.
 *
 * Contiguous runs of identical items (storm/copy effects, swarms of the same trigger) collapse
 * into a single pile with a `×N` count, expandable on click. This is display-only — see
 * {@link groupStackCards}. The full stack order, targeting, and resolution are unaffected.
 */
export function StackDisplay() {
  const stackCards = useStackCards()
  const responsive = useResponsiveContext()
  const hoverCard = useGameStore((state) => state.hoverCard)
  const openCardMenuOnTap = useOpenCardMenuOnTap()
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const gameState = useGameStore((state) => state.gameState)
  // Which collapsible piles the player has manually expanded (by groupId = first member's id).
  const [expandedGroups, setExpandedGroups] = useState<ReadonlySet<EntityId>>(() => new Set())
  // Right-click yield menu (MTGO-style persistent yields — backlog §C), anchored to a stack ability.
  const [yieldMenu, setYieldMenu] = useState<{ identity: ClientAbilityIdentity; sourceName: string; x: number; y: number } | null>(null)
  const openYieldMenu = (card: ClientCard, e: React.MouseEvent) => {
    if (!card.abilityIdentity) return
    e.preventDefault()
    setYieldMenu({ identity: card.abilityIdentity, sourceName: card.name, x: e.clientX, y: e.clientY })
  }
  const toggleGroup = (groupId: EntityId) =>
    setExpandedGroups((prev) => {
      const next = new Set(prev)
      if (next.has(groupId)) next.delete(groupId)
      else next.add(groupId)
      return next
    })
  // Multiplayer: stack items are wrapped in their caster's seat color (full border + glow) and
  // tagged with the caster's name, so "whose spell is that" reads at a glance. 2-player games
  // have only one possible caster per side, so this stays off.
  const players = useGameStore((state) => selectGameState(state)?.players)
  const isMulti = (players?.length ?? 0) > 2
  const seatMetaFor = (controllerId: EntityId) => {
    if (!isMulti || !players) return null
    const idx = players.findIndex((p) => p.playerId === controllerId)
    if (idx < 0) return null
    return { name: players[idx]?.name ?? 'Player', seat: seatColor(idx) }
  }
  const seatBorderFor = (controllerId: EntityId): React.CSSProperties => {
    const meta = seatMetaFor(controllerId)
    if (!meta) return {}
    return {
      border: `1.5px solid ${meta.seat.base}`,
      borderLeft: `4px solid ${meta.seat.base}`,
      borderRadius: 6,
      boxShadow: `0 0 7px 1px ${meta.seat.soft}`,
    }
  }

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

  /**
   * A tap/click on a stack item. Targeting and decision selection own it whenever either is
   * running; otherwise the gesture only ever means "let me read this", which without hover has to
   * go through the action menu's "View card" row — the same route as any other card the player
   * can't do anything with.
   */
  const handleStackItemTap = (cardId: EntityId) => {
    if (targetingState || decisionSelectionState) {
      handleStackItemClick(cardId)
      return
    }
    openCardMenuOnTap?.(cardId)
  }

  // A card the player can currently target/select — such a card must never be hidden inside a
  // collapsed pile, so we force-expand any group containing one.
  const isTargetableOrSelectable = (card: ClientCard): boolean =>
    (targetingState?.validTargets.includes(card.id) ?? false)
    || (targetingState?.selectedTargets.includes(card.id) ?? false)
    || (decisionSelectionState?.validOptions.includes(card.id) ?? false)
    || (decisionSelectionState?.selectedOptions.includes(card.id) ?? false)

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
  // Pre-rotation dimensions for a sideways-printed card, chosen so that after `rotate(90deg)` the
  // image occupies the full column width and proportional height: the rotation swaps the axes, so
  // the element's *height* becomes its on-screen width.
  const landscapeStackImageHeight = stackImageWidth
  const landscapeStackImageWidth = Math.round(stackImageWidth * stackImageWidth / stackImageHeight)

  /**
   * Render one card slot in the fanned pile. `renderIndex` is the slot's position across the whole
   * fan (drives overlap + z-order). `opts` adds the collapsed-pile affordances.
   */
  const renderStackCard = (
    card: ClientCard,
    renderIndex: number,
    opts: {
      domKey: string
      /** Show a "×N" pip — this slot represents a collapsed pile of N identical items. */
      countBadge?: number
      /** Layered "deck" shadow implying more cards behind. */
      stacked?: boolean
      /** Override the default click (targeting) — used to expand a collapsed pile. */
      onClick?: () => void
      /** Show a "⊟ N" re-collapse chip (first member of an expanded pile). */
      collapseControl?: { count: number; onCollapse: () => void }
    },
  ) => {
    const isValidTarget = (targetingState?.validTargets.includes(card.id) ?? false)
      || (decisionSelectionState?.validOptions.includes(card.id) ?? false)
    const isSelectedTarget = (targetingState?.selectedTargets.includes(card.id) ?? false)
      || (decisionSelectionState?.selectedOptions.includes(card.id) ?? false)

    const pileShadow = '5px 5px 0 -2px rgba(40, 22, 64, 0.9), 9px 9px 0 -3px rgba(40, 22, 64, 0.6), 0 2px 8px rgba(0, 0, 0, 0.5)'
    const highlight: React.CSSProperties = isValidTarget && !isSelectedTarget
      ? { boxShadow: '0 0 12px 4px rgba(255, 200, 0, 0.8)', borderRadius: 6 }
      : isSelectedTarget
        ? { boxShadow: '0 0 12px 4px rgba(0, 255, 100, 0.8)', borderRadius: 6 }
        : opts.stacked
          ? { boxShadow: pileShadow, borderRadius: 6 }
          : card.copyIndex != null
            ? { boxShadow: '0 0 8px 2px rgba(60, 140, 255, 0.5)', borderRadius: 6 }
            : {}

    // Badges sharing the top-left corner stack downwards in a fixed order, so adding one never
    // silently parks it on top of another. `topOf` returns the row a badge occupies, given which
    // of the ones above it are showing.
    const topLeftBadges = [
      card.castProvenanceLabel ? 'provenance' : null,
      card.costSacrificeLabel ? 'costSacrifice' : null,
      card.optionalCostLabel ? 'optionalCost' : null,
      card.giftPromised ? 'gift' : null,
      card.wasBlightPaid ? 'blight' : null,
    ].filter((b): b is string => b !== null)
    const topOf = (badge: string) => 4 + Math.max(0, topLeftBadges.indexOf(badge)) * 22

    return (
      <div
        key={opts.domKey}
        data-card-id={card.id}
        style={{
          ...styles.stackItem,
          marginTop: renderIndex === 0 ? 0 : -stackImageHeight + cardOffset, // Overlap cards, showing cardOffset pixels of each
          zIndex: renderIndex + 1, // Later cards (higher index = cast later) on top
          ...seatBorderFor(card.controllerId),
          ...highlight,
        }}
        onClick={opts.onClick ?? (() => handleStackItemTap(card.id))}
        onContextMenu={(e) => openYieldMenu(card, e)}
        /* Pointer events with a touch guard, as on GameCard: a tap synthesizes mouseenter and
           never the matching mouseleave, which used to open the preview on top of whatever the
           same tap opened and leave it stranded there. */
        onPointerEnter={(e) => { if (e.pointerType !== 'touch') hoverCard(card.id, { x: e.clientX, y: e.clientY }) }}
        onPointerLeave={(e) => { if (e.pointerType !== 'touch') hoverCard(null) }}
      >
        {(() => {
          // A sideways-printed card (split layout, Room, battle) reads landscape only once rotated
          // 90°. The slot keeps its portrait footprint — the pile's overlap maths assumes a uniform
          // item height — so the rotated card is scaled to the column width and centred inside it,
          // costing a little dead space above and below and nothing else.
          const isLandscape = card.isLandscapeFace === true
          const dimmed = card.sourceZone === 'GRAVEYARD'
            ? { opacity: 0.7, filter: 'saturate(0.6)' }
            : {}
          // A spell cast face down (morph, disguise) is drawn as its mechanic's helper card on the
          // stack, the same as the permanent it becomes — the morph helmet, or "A Mysterious
          // Creature" for disguise. Its controller still sees the real card on hover, exactly as
          // for their own face-down permanents.
          const faceDown = card.isFaceDown === true
          const image = (
            <img
              src={faceDown
                ? faceDownImageUrl(card.faceDownMode)
                : getCardImageUrl(card.name, card.imageUri, 'small')}
              alt={faceDown ? 'Face-down spell' : card.name}
              style={{
                ...styles.stackItemImage,
                ...(isLandscape
                  ? {
                      position: 'absolute' as const,
                      top: '50%',
                      left: '50%',
                      width: landscapeStackImageWidth,
                      height: landscapeStackImageHeight,
                      transform: 'translate(-50%, -50%) rotate(90deg)',
                    }
                  : { width: stackImageWidth, height: stackImageHeight }),
                cursor: isValidTarget || opts.onClick ? 'pointer' : 'default',
                ...dimmed,
              }}
              title={faceDown ? undefined : card.name}
              onError={(e) => { if (!faceDown) handleImageError(e, card.name, 'small') }}
            />
          )
          if (!isLandscape) return image
          return (
            <div style={{ position: 'relative', width: stackImageWidth, height: stackImageHeight }}>
              {image}
            </div>
          )
        })()}
        {/* Collapsed-pile count pip */}
        {opts.countBadge != null && (
          <div style={styles.stackCountBadge} title={`${opts.countBadge} identical items`}>
            ×{opts.countBadge}
          </div>
        )}
        {/* Re-collapse chip on an expanded pile */}
        {opts.collapseControl && (
          <div
            style={styles.stackCollapseChip}
            title="Collapse identical items"
            onClick={(e) => {
              e.stopPropagation()
              opts.collapseControl!.onCollapse()
            }}
          >
            <span aria-hidden>⊟</span>
            <span>{opts.collapseControl.count}</span>
          </div>
        )}
        {/* Caster tag (multiplayer) — names whose spell/ability this is, in their seat color */}
        {(() => {
          const meta = seatMetaFor(card.controllerId)
          if (!meta) return null
          return (
            <div
              title={`Cast by ${meta.name}`}
              style={{
                position: 'absolute',
                top: 3,
                left: 3,
                maxWidth: stackImageWidth - 10,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 3,
                padding: '1px 6px 1px 4px',
                borderRadius: 4,
                background: 'rgba(8, 10, 16, 0.82)',
                border: `1px solid ${meta.seat.base}`,
                zIndex: 3,
                pointerEvents: 'none',
              }}
            >
              <span aria-hidden style={{ width: 7, height: 7, borderRadius: '50%', background: meta.seat.base, boxShadow: `0 0 4px ${meta.seat.base}`, flexShrink: 0 }} />
              <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: '0.02em', color: meta.seat.bright, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {meta.name}
              </span>
            </div>
          )
        })()}
        {/* Show chosen X value for X spells */}
        {card.chosenX != null && (
          <div style={styles.stackXBadge}>
            X={card.chosenX}
          </div>
        )}
        {/* Show how the spell was cast — "Disturb · Graveyard" — for anything but a plain hand cast.
            The tooltip carries the whole story, since a spell with something on top of it is clipped
            to its first badge row. */}
        {card.castProvenanceLabel && (
          <div
            style={{ ...styles.stackCastProvenanceBadge, top: topOf('provenance') }}
            title={[
              `Cast: ${card.castProvenanceLabel}`,
              card.costSacrificeLabel,
              card.manaPaidCost && `Paid ${card.manaPaidCost}`,
            ].filter(Boolean).join(' · ')}
          >
            {card.castProvenanceLabel}
          </div>
        )}
        {/* The mana that actually paid, for any alternative-cost cast — the printed pips on the card
            were never what was spent. Top-*right*, so it shares the one badge row that stays visible
            when a cast trigger (or any later spell) covers the rest of this card. Shifted left when
            an X badge already owns that corner. */}
        {card.manaPaidCost && (
          <div
            style={{
              ...styles.stackManaPaidBadge,
              top: topOf('provenance'),
              right: card.chosenX != null ? 44 : 4,
            }}
            title={`Paid ${card.manaPaidCost}`}
          >
            <span style={{ opacity: 0.75 }}>Paid</span>
            <ManaCost cost={card.manaPaidCost} size={9} gap={1} />
          </div>
        )}
        {/* What the alternative cost ate — "Sacrificed Niblis of the Urn". Emerge (CR 702.119a)
            prices itself off that creature's mana value, so without this the cheap cast has no
            visible cause from the other seat. */}
        {card.costSacrificeLabel && (
          <div
            style={{ ...styles.stackCostSacrificeBadge, top: topOf('costSacrifice') }}
            title={card.costSacrificeLabel}
          >
            {card.costSacrificeLabel}
          </div>
        )}
        {/* Show the declared optional additional cost — Kicked / Bargained / Offspring */}
        {card.optionalCostLabel && (
          <div style={{ ...styles.stackKickedBadge, top: topOf('optionalCost') }}>
            {card.optionalCostLabel}
          </div>
        )}
        {/* Show gift badge when the caster promised a gift (Bloomburrow) */}
        {card.giftPromised && (
          <div
            style={{ ...styles.stackGiftBadge, top: topOf('gift') }}
            title="Gift promised"
          >
            <i className="ms ms-ability-gift" style={{ fontSize: 12 }} />
            <span>Gift</span>
          </div>
        )}
        {/* Show blight-paid badge when the optional Blight additional cost was paid (Lorwyn Eclipsed) */}
        {card.wasBlightPaid && (
          <div
            style={{ ...styles.stackBlightPaidBadge, top: topOf('blight') }}
            title="Blight cost paid"
          >
            <i className="ms ms-counter-minus" style={{ fontSize: 12 }} />
            <span>Blight</span>
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
  }

  // Fold contiguous identical items into piles, then flatten into render slots: a collapsed pile
  // is one slot; an expanded (or single, or targetable) pile contributes one slot per member.
  const groups = groupStackCards(stackCards)
  type RenderSlot =
    | { kind: 'collapsed'; group: StackGroup }
    | { kind: 'card'; card: ClientCard; collapse?: { count: number; groupId: EntityId } }
  const slots: RenderSlot[] = []
  for (const group of groups) {
    const collapsible = group.items.length >= 2
    const forceExpanded = group.items.some(isTargetableOrSelectable)
    const expanded = expandedGroups.has(group.groupId) || forceExpanded
    if (collapsible && !expanded) {
      slots.push({ kind: 'collapsed', group })
    } else {
      group.items.forEach((card, i) => {
        slots.push({
          kind: 'card',
          card,
          // Offer re-collapse on the first member of an expanded pile (not while targeting).
          ...(collapsible && !forceExpanded && i === 0
            ? { collapse: { count: group.items.length, groupId: group.groupId } }
            : {}),
        })
      })
    }
  }

  return (
    <>
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
            {slots.map((slot, index) => {
              if (slot.kind === 'collapsed') {
                // The top-of-run member represents the pile (all members are identical).
                const rep = slot.group.items[slot.group.items.length - 1]!
                return renderStackCard(rep, index, {
                  domKey: `grp-${slot.group.groupId}`,
                  countBadge: slot.group.items.length,
                  stacked: true,
                  onClick: () => toggleGroup(slot.group.groupId),
                })
              }
              return renderStackCard(slot.card, index, {
                domKey: slot.card.id,
                ...(slot.collapse
                  ? { collapseControl: { count: slot.collapse.count, onCollapse: () => toggleGroup(slot.collapse!.groupId) } }
                  : {}),
              })
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
              onPointerEnter={(e) => { if (e.pointerType !== 'touch' && sourceCard) hoverCard(pendingDecision.context.sourceId!, { x: e.clientX, y: e.clientY }) }}
              onPointerLeave={(e) => { if (e.pointerType !== 'touch') hoverCard(null) }}
              onClick={() => handleStackItemTap(pendingDecision.context.sourceId!)}
              style={{ cursor: openCardMenuOnTap ? 'pointer' : 'default' }}
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

      // Modal spells and triggered abilities render their chosen modes (and any targets) below,
      // so opponents can see exactly what's been committed before responding. Triggered modes are
      // chosen as the ability is put on the stack, just like a modal spell's cast-time choice.
      const perModeGroups = chosenModeGroupsForStack(topCard)
      if (perModeGroups.length > 0) {
        return (
          <div style={{
            padding: responsive.isMobile ? '4px 6px' : '6px 10px',
            backgroundColor: 'rgba(30, 18, 50, 0.85)',
            borderRadius: 6,
            border: '1px solid rgba(150, 100, 200, 0.3)',
            maxWidth: responsive.isMobile ? 140 : 200,
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)',
            display: 'flex',
            flexDirection: 'column',
            gap: 4,
          }}>
            {perModeGroups.map((group, i) => (
              <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <div style={{
                  color: '#e0d4f0',
                  fontSize: responsive.isMobile ? 9 : 10,
                  lineHeight: 1.35,
                  fontWeight: 600,
                  display: 'flex',
                  gap: 4,
                }}>
                  <span style={{ color: '#b8a8cc' }}>•</span>
                  <span style={{ flex: 1 }}>
                    <AbilityText text={group.modeDescription} size={responsive.isMobile ? 9 : 10} />
                  </span>
                </div>
                {group.targetNames.length > 0 && (
                  <div style={{
                    color: '#ffcc66',
                    fontSize: responsive.isMobile ? 8 : 9,
                    lineHeight: 1.3,
                    paddingLeft: 10,
                    fontStyle: 'italic',
                  }}>
                    → {group.targetNames.join(', ')}
                  </div>
                )}
              </div>
            ))}
          </div>
        )
      }

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
    {yieldMenu && (
      <YieldContextMenu
        identity={yieldMenu.identity}
        sourceName={yieldMenu.sourceName}
        position={{ x: yieldMenu.x, y: yieldMenu.y }}
        existing={gameState?.activeYields?.find(
          (y) => y.cardDefinitionId === yieldMenu.identity.cardDefinitionId && y.abilityId === yieldMenu.identity.abilityId,
        )}
        onClose={() => setYieldMenu(null)}
      />
    )}
    </>
  )
}
