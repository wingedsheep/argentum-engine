import { useState, useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, ChooseTargetsDecision, ClientGameState } from '@/types'
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
import { BattlefieldTargetingUI } from './BattlefieldTargetingUI'
import { PlayerTargetingUI } from './PlayerTargetingUI'
import { GraveyardTargetingUI } from './GraveyardTargetingUI'
import { SplitPilesUI } from './SplitPilesUI'
import { ManaSourceSelectionUI } from './ManaSourceSelectionUI'
import styles from './DecisionUI.module.css'

/**
 * Check if all legal targets in a ChooseTargetsDecision are players.
 */
function isPlayerOnlyTargeting(decision: ChooseTargetsDecision, playerIds: EntityId[]): boolean {
  const legalTargets = decision.legalTargets[0] ?? []
  if (legalTargets.length === 0) return false
  return legalTargets.every((targetId) => playerIds.includes(targetId))
}

/**
 * Check if all legal targets in a ChooseTargetsDecision live in a hidden-from-battlefield zone
 * (graveyard or exile). Returns the cards if true, null otherwise — the caller routes the
 * resulting list into [GraveyardTargetingUI], which handles both zones (rendering owner-keyed
 * tabs and selection) since their pile semantics are identical for targeting.
 *
 * Mixed-zone target sets fall through to `null` here so they reach the battlefield targeting
 * path instead — that's a degenerate case today (Blade of the Swarm only targets exile, no
 * known card mixes graveyard + exile in one target slot).
 */
function getGraveyardOrExileTargets(decision: ChooseTargetsDecision, gameState: ClientGameState | null) {
  if (!gameState) return null
  const legalTargets = decision.legalTargets[0] ?? []
  if (legalTargets.length === 0) return null

  const cards = []
  for (const targetId of legalTargets) {
    const card = gameState.cards[targetId]
    const zoneType = card?.zone?.zoneType
    if (!card || (zoneType !== ZoneType.GRAVEYARD && zoneType !== ZoneType.EXILE)) {
      return null
    }
    cards.push(card)
  }
  return cards
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
          Return to decision
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
          Return to decision
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
          Return to decision
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

    // Single-zone, click-on-board targeting style (e.g., Lich's Mastery)
    if (pendingDecision.useTargetingUI) {
      return (
        <BattlefieldSelectionUI
          decision={pendingDecision}
        />
      )
    }

    // Default: full-screen modal
    return (
      <CardSelectionDecision key={pendingDecision.id} decision={pendingDecision} responsive={responsive} />
    )
  }

  // Handle ChooseTargetsDecision
  if (pendingDecision.type === 'ChooseTargetsDecision') {
    const playerIds = gameState?.players.map((p) => p.playerId) ?? []
    const isPlayerOnly = isPlayerOnlyTargeting(pendingDecision, playerIds)
    const zoneTargets = getGraveyardOrExileTargets(pendingDecision, gameState)

    // If all targets are in graveyards or exile, show a pile-selection overlay
    if (zoneTargets && zoneTargets.length > 0) {
      return (
        <GraveyardTargetingUI
          decision={pendingDecision}
          graveyardCards={zoneTargets}
          responsive={responsive}
        />
      )
    }

    // Player-only targeting: simple banner (auto-submit via LifeDisplay click)
    if (isPlayerOnly) {
      return (
        <PlayerTargetingUI decision={pendingDecision} />
      )
    }

    // Battlefield targeting: use selection state with Confirm/Decline buttons
    return (
      <BattlefieldTargetingUI decision={pendingDecision} />
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
