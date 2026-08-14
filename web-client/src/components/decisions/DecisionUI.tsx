import { useState, useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, ChooseTargetsDecision } from '@/types'
import { ZoneType } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { LibrarySearchUI } from './LibrarySearchUI'
import { ReorderCardsUI } from './ReorderCardsUI'
import { OrderBlockersUI } from './OrderBlockersUI'
import { CombatDamageAssignmentModal } from './CombatDamageAssignmentModal'
import { CombatResolutionBoard } from './CombatResolutionBoard'
import { YesNoDecisionUI } from './YesNoDecisionUI'
import { BatchYesNoDecisionUI } from './BatchYesNoDecisionUI'
import { ChooseNumberDecisionUI } from './ChooseNumberDecisionUI'
import { ChooseOptionDecisionUI } from './ChooseOptionDecisionUI'
import { ChooseReplacementDecisionUI } from './ChooseReplacementDecisionUI'
import { BudgetModalDecisionUI } from './BudgetModalDecisionUI'
import { ChooseColorDecisionUI } from './ChooseColorDecisionUI'
import { CardSelectionDecision } from './CardSelectionDecisionUI'
import { BattlefieldSelectionUI } from './BattlefieldSelectionUI'
import { MultiZoneSelectionUI } from './MultiZoneSelectionUI'
import { ChooseTargetsUI } from './ChooseTargetsUI'
import { PlayerTargetingUI } from './PlayerTargetingUI'
import { SplitPilesUI } from './SplitPilesUI'
import { ManaSourceSelectionUI } from './ManaSourceSelectionUI'
import { isLoneTargetRequirement, partitionTargetsByZone } from '@/utils/targeting.ts'
import styles from './DecisionUI.module.css'

/**
 * Check if a ChooseTargetsDecision is a single player-only requirement asking for one target.
 *
 * Only then is the simple auto-submit [PlayerTargetingUI] banner appropriate. A decision with more
 * than one target requirement (e.g. Iroh, Tea Master: "target opponent" + "target permanent you
 * control") — or a single requirement wanting more than one target (e.g. Parker Luck: "two target
 * players", maxTargets = 2) — must route to [ChooseTargetsUI], which walks each requirement in turn
 * and accumulates player-orb selection through decisionSelectionState with a Confirm step.
 * [PlayerTargetingUI] can only collect a lone player slot and would strand the rest.
 */
function isPlayerOnlyTargeting(decision: ChooseTargetsDecision, playerIds: EntityId[]): boolean {
  if (!isLoneTargetRequirement(decision)) return false
  const legalTargets = decision.legalTargets[0] ?? []
  if (legalTargets.length === 0) return false
  return legalTargets.every((targetId) => playerIds.includes(targetId))
}

/**
 * Decision UI overlay for pending decisions (e.g., discard to hand size, library search).
 */
export function DecisionUI() {
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const gameState = useGameStore((state) => state.gameState)
  const responsive = useResponsive()
  const [decisionMinimized, setDecisionMinimized] = useState(false)

  // Reset minimized state when decision changes
  useEffect(() => {
    setDecisionMinimized(false)
  }, [pendingDecision?.id])

  if (!pendingDecision) return null

  // A prompt raised once per object names its subject on the minimized button too, so a player
  // who stepped out to read the board knows which creature they are coming back to answer for.
  const subjectName = pendingDecision.context.subjectEntityId
    ? gameState?.cards[pendingDecision.context.subjectEntityId]?.name
    : undefined
  const returnLabel = subjectName ? `Return to decision — ${subjectName}` : 'Return to decision'

  // Handle SelectManaSourcesDecision (mana source selection for Lightning Rift etc.)
  if (pendingDecision.type === 'SelectManaSourcesDecision') {
    return <ManaSourceSelectionUI decision={pendingDecision} />
  }

  // Handle SearchLibraryDecision with dedicated UI
  if (pendingDecision.type === 'SearchLibraryDecision') {
    return <LibrarySearchUI decision={pendingDecision} responsive={responsive} />
  }

  // Handle ReorderLibraryDecision with dedicated UI
  if (pendingDecision.type === 'ReorderLibraryDecision') {
    return <ReorderCardsUI key={pendingDecision.id} decision={pendingDecision} responsive={responsive} />
  }

  // Handle OrderObjectsDecision (e.g., damage assignment order for blockers)
  if (pendingDecision.type === 'OrderObjectsDecision') {
    // Combat phase ordering uses dedicated blocker ordering UI
    if (pendingDecision.context.phase === 'COMBAT') {
      return <OrderBlockersUI key={pendingDecision.id} decision={pendingDecision} responsive={responsive} />
    }
    // Other ordering decisions could use a generic ordering UI (not yet implemented)
    return null
  }

  // Handle YesNoDecision (e.g., "You may shuffle your library")
  if (pendingDecision.type === 'YesNoDecision') {
    // Cards like Dragon Shadow set inlineOnTrigger to render yes/no on the triggering creature
    if (pendingDecision.context.inlineOnTrigger && pendingDecision.context.triggeringEntityId) {
      const triggeringCard = gameState?.cards[pendingDecision.context.triggeringEntityId]
      if (triggeringCard?.zone?.zoneType === ZoneType.BATTLEFIELD) {
        return null
      }
    }
    if (decisionMinimized) {
      return (
        <button
          className={styles.floatingReturnButton}
          onClick={() => setDecisionMinimized(false)}
        >
          {returnLabel}
        </button>
      )
    }
    return (
      <div className={styles.overlay}>
        <YesNoDecisionUI
          decision={pendingDecision}
          gameState={gameState}
          onMinimize={() => setDecisionMinimized(true)}
        />
      </div>
    )
  }

  // Handle BatchYesNoDecision (one yes/no covering N identical optional triggers)
  if (pendingDecision.type === 'BatchYesNoDecision') {
    if (decisionMinimized) {
      return (
        <button
          className={styles.floatingReturnButton}
          onClick={() => setDecisionMinimized(false)}
        >
          {returnLabel}
        </button>
      )
    }
    return (
      <div className={styles.overlay}>
        <BatchYesNoDecisionUI
          decision={pendingDecision}
          gameState={gameState}
          onMinimize={() => setDecisionMinimized(true)}
        />
      </div>
    )
  }

  // Handle ChooseNumberDecision (e.g., "Choose how many cards to draw")
  if (pendingDecision.type === 'ChooseNumberDecision') {
    if (decisionMinimized) {
      return (
        <button
          className={styles.floatingReturnButton}
          onClick={() => setDecisionMinimized(false)}
        >
          {returnLabel}
        </button>
      )
    }
    return (
      <div className={styles.overlay}>
        <ChooseNumberDecisionUI
          decision={pendingDecision}
          onMinimize={() => setDecisionMinimized(true)}
        />
      </div>
    )
  }

  // Handle ChooseOptionDecision (e.g., "Choose a creature type")
  if (pendingDecision.type === 'ChooseOptionDecision') {
    return (
      <ChooseOptionDecisionUI key={pendingDecision.id} decision={pendingDecision} />
    )
  }

  // Handle ChooseReplacementDecision (text-change "from → to": Crystal Spray, Artificial Evolution)
  if (pendingDecision.type === 'ChooseReplacementDecision') {
    return (
      <ChooseReplacementDecisionUI key={pendingDecision.id} decision={pendingDecision} />
    )
  }

  // Handle BudgetModalDecision (Bloomburrow Season cycle pawprint modes)
  if (pendingDecision.type === 'BudgetModalDecision') {
    return (
      <BudgetModalDecisionUI key={pendingDecision.id} decision={pendingDecision} />
    )
  }

  // DistributeDecision is handled inline on the board (GameCard + LifeDisplay + GameBoard confirm bar)
  if (pendingDecision.type === 'DistributeDecision') {
    return null
  }

  // Handle AssignDamageDecision (legacy per-attacker combat damage assignment)
  if (pendingDecision.type === 'AssignDamageDecision') {
    return <CombatDamageAssignmentModal key={pendingDecision.id} decision={pendingDecision} />
  }

  // Handle CombatResolutionDecision (the bipartite combat resolution board)
  if (pendingDecision.type === 'CombatResolutionDecision') {
    return <CombatResolutionBoard key={pendingDecision.id} decision={pendingDecision} />
  }

  // Handle ChooseColorDecision (e.g., "Choose a color for protection")
  // Rendered as a floating bottom panel so the battlefield stays visible
  if (pendingDecision.type === 'ChooseColorDecision') {
    return <ChooseColorDecisionUI decision={pendingDecision} />
  }

  // Handle SelectCardsDecision
  if (pendingDecision.type === 'SelectCardsDecision') {
    // Multi-zone selections always need zone grouping so the player can tell
    // which cards are in hand vs on the battlefield (e.g., Celestial Reunion
    // beholding from BATTLEFIELD + HAND). This applies whether or not the card
    // opted into useTargetingUI.
    const zones = new Set<string>()
    for (const cardId of pendingDecision.options) {
      const card = gameState?.cards[cardId]
      if (card?.zone?.zoneType) {
        zones.add(card.zone.zoneType)
      }
    }
    if (zones.size > 1) {
      return (
        <MultiZoneSelectionUI
          key={pendingDecision.id}
          decision={pendingDecision}
          responsive={responsive}
        />
      )
    }

    // Single-zone, click-on-board targeting style (e.g., Lich's Mastery). A pile isn't clickable
    // card-by-card, so options that all sit in a graveyard/exile pile fall through to the modal
    // picker below even when the server asked for the board UI — the same trap that made a mixed
    // target requirement's graveyard half unreachable in the two targeting paths. (Options spanning
    // two zones are already handled above by MultiZoneSelectionUI, which renders every zone.)
    if (pendingDecision.useTargetingUI) {
      const { hasBoardTargets } = partitionTargetsByZone(pendingDecision.options, gameState?.cards)
      if (hasBoardTargets) {
        return (
          <BattlefieldSelectionUI
            decision={pendingDecision}
          />
        )
      }
    }

    // Default: full-screen modal
    return (
      <CardSelectionDecision key={pendingDecision.id} decision={pendingDecision} responsive={responsive} />
    )
  }

  // Handle ChooseTargetsDecision
  if (pendingDecision.type === 'ChooseTargetsDecision') {
    const playerIds = gameState?.players.map((p) => p.playerId) ?? []

    // Player-only targeting: simple banner (auto-submit via LifeDisplay click)
    if (isPlayerOnlyTargeting(pendingDecision, playerIds)) {
      return (
        <PlayerTargetingUI decision={pendingDecision} />
      )
    }

    // Everything else walks the requirements one at a time, routing each to the pile picker
    // or the board-click banner depending on where that requirement's legal targets live.
    return (
      <ChooseTargetsUI key={pendingDecision.id} decision={pendingDecision} />
    )
  }

  // Handle SplitPilesDecision (e.g., Surveil - put cards on top of library or into graveyard)
  if (pendingDecision.type === 'SplitPilesDecision') {
    return (
      <div className={styles.overlay}>
        <SplitPilesUI decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Other decision types not yet implemented
  return null
}
