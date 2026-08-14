import { useState, useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { DecisionSelectionState } from '@/store/slices'
import type { EntityId, ChooseTargetsDecision } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { DecisionCardPreview } from './DecisionComponents'
import { DraggableBanner } from './DraggableBanner'
import styles from './DecisionUI.module.css'

/**
 * Board targeting UI for **one** requirement of a ChooseTargetsDecision (battlefield permanents,
 * players, stack objects — anything clickable on the board). Shows a side banner with
 * Confirm/Decline buttons and uses decisionSelectionState for toggle-to-select.
 *
 * Requirement walking (which slot we're on, accumulating picks, submitting) lives in the parent
 * [ChooseTargetsUI]: a decision can mix a board slot with a graveyard slot (The Spot, Living
 * Portal), and only the parent can hand each slot to the UI that can collect it.
 */
export function BattlefieldTargetingUI({
  decision,
  requirementIndex,
  totalRequirements,
  legalTargets,
  initialSelection,
  onComplete,
  onBack,
  pileButton,
}: {
  decision: ChooseTargetsDecision
  requirementIndex: number
  totalRequirements: number
  /** Legal targets for this requirement, already stripped of picks made for earlier requirements. */
  legalTargets: readonly EntityId[]
  /**
   * Picks to pre-select — non-empty when the player stepped Back into this requirement, or came
   * here from the pile picker on a mixed requirement carrying the cards they picked there.
   */
  initialSelection: readonly EntityId[]
  onComplete: (targets: readonly EntityId[]) => void
  /** Present when an earlier requirement can be revised. */
  onBack?: () => void
  /**
   * Present when this requirement's legal targets *also* include graveyard/exile cards, which are
   * not clickable on the board (Taskmaster, Mercenary Mimic: "target creature on the battlefield or
   * creature card in a graveyard"). Opening hands the picks made here to the pile picker so both
   * halves accumulate into the same slot; the banner otherwise stays exactly as it is, because the
   * board half still has to be clickable.
   */
  pileButton?: {
    /** "Graveyard" / "Exile" / "Graveyard / Exile" — the piles holding the other half. */
    zoneLabel: string
    /** How many valid targets are in those piles. */
    count: number
    onOpen: (carried: readonly EntityId[]) => void
  }
}) {
  const startDecisionSelection = useGameStore((s) => s.startDecisionSelection)
  const decisionSelectionState = useGameStore((s) => s.decisionSelectionState)
  const cancelDecisionSelection = useGameStore((s) => s.cancelDecisionSelection)
  const submitCancelDecision = useGameStore((s) => s.submitCancelDecision)
  const gameState = useGameStore((s) => s.gameState)
  const [isHoveringSource, setIsHoveringSource] = useState(false)
  const responsive = useResponsive()

  const targetReq = decision.targetRequirements[requirementIndex]
  const minTargets = targetReq?.minTargets ?? 1
  const maxTargets = targetReq?.maxTargets ?? 1

  // Look up source card image from game state
  const sourceId = decision.context.sourceId
  const sourceCard = sourceId ? gameState?.cards[sourceId] : undefined
  const sourceImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  // Start decision selection state when this component mounts or requirement changes
  useEffect(() => {
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions: [...legalTargets],
      selectedOptions: [...initialSelection],
      minSelections: minTargets,
      maxSelections: maxTargets,
      prompt: targetReq?.description ?? decision.prompt,
    }
    startDecisionSelection(selectionState)

    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id, requirementIndex])

  const selectedCount = decisionSelectionState?.selectedOptions.length ?? 0
  const canConfirm = selectedCount >= minTargets && selectedCount <= maxTargets
  const canDecline = minTargets === 0

  const handleConfirm = () => {
    if (canConfirm && decisionSelectionState) {
      const selected = decisionSelectionState.selectedOptions
      cancelDecisionSelection()
      onComplete(selected)
    }
  }

  const handleDecline = () => {
    cancelDecisionSelection()
    onComplete([])
  }

  const handleCancel = () => {
    cancelDecisionSelection()
    submitCancelDecision()
  }

  const handleBack = () => {
    cancelDecisionSelection()
    onBack?.()
  }

  const handleOpenPile = () => {
    // Carry the board picks over so the pile picker counts them toward this requirement's
    // minimum/maximum and submits them together with whatever is chosen there.
    const carried = [...(decisionSelectionState?.selectedOptions ?? [])]
    cancelDecisionSelection()
    pileButton?.onOpen(carried)
  }

  const requirementLabel = totalRequirements > 1
    ? `Choose Target (${requirementIndex + 1}/${totalRequirements})`
    : 'Choose Target'

  const promptText = targetReq?.description ?? decision.prompt

  return (
    <DraggableBanner className={styles.sideBannerSelection}>
      {sourceImageUrl && (
        <img
          src={sourceImageUrl}
          alt={`Source: ${decision.context.sourceName ?? 'card'}`}
          className={styles.bannerCardImage}
          onMouseEnter={() => setIsHoveringSource(true)}
          onMouseLeave={() => setIsHoveringSource(false)}
        />
      )}
      {isHoveringSource && sourceCard && !responsive.isMobile && (
        <DecisionCardPreview cardName={sourceCard.name} imageUri={sourceCard.imageUri} />
      )}
      <div className={styles.bannerTitleSelection}>
        {requirementLabel}
      </div>
      {decision.context.effectHint && (
        <div className={styles.effectHint}>
          {decision.context.effectHint}
        </div>
      )}
      <div className={styles.hint}>
        {promptText}
      </div>
      <div className={styles.hint}>
        {`${selectedCount} / ${maxTargets} selected`}
      </div>
      {pileButton && (
        <div className={styles.hint}>
          {`Click a highlighted permanent, or open the ${pileButton.zoneLabel.toLowerCase()}`}
        </div>
      )}

      <div className={styles.buttonContainerSmall}>
        {pileButton && (
          <button onClick={handleOpenPile} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            {`${pileButton.zoneLabel} (${pileButton.count})`}
          </button>
        )}
        {onBack && (
          <button onClick={handleBack} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            ← Back
          </button>
        )}
        {canDecline && selectedCount === 0 && (
          <button onClick={handleDecline} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            Decline
          </button>
        )}
        {selectedCount > 0 && (
          <button
            onClick={handleConfirm}
            disabled={!canConfirm}
            className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
          >
            Confirm ({selectedCount})
          </button>
        )}
        {decision.canCancel && (
          <button onClick={handleCancel} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            Cancel
          </button>
        )}
      </div>
    </DraggableBanner>
  )
}
