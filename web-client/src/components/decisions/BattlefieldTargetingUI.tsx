import { useState, useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { DecisionSelectionState } from '@/store/slices'
import type { EntityId, ChooseTargetsDecision } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { DecisionCardPreview } from './DecisionComponents'
import styles from './DecisionUI.module.css'

/**
 * Battlefield targeting UI for ChooseTargetsDecision (non-player, non-graveyard targets).
 * Shows a side banner with Confirm/Decline buttons, uses decisionSelectionState for toggle-to-select.
 */
export function BattlefieldTargetingUI({
  decision,
}: {
  decision: ChooseTargetsDecision
}) {
  const startDecisionSelection = useGameStore((s) => s.startDecisionSelection)
  const decisionSelectionState = useGameStore((s) => s.decisionSelectionState)
  const cancelDecisionSelection = useGameStore((s) => s.cancelDecisionSelection)
  const submitTargetsDecision = useGameStore((s) => s.submitTargetsDecision)
  const submitCancelDecision = useGameStore((s) => s.submitCancelDecision)
  const gameState = useGameStore((s) => s.gameState)
  const [isHoveringSource, setIsHoveringSource] = useState(false)
  const responsive = useResponsive()

  // Multi-requirement state: track which requirement we're on and accumulated targets
  const [currentReqIndex, setCurrentReqIndex] = useState(0)
  const [collectedTargets, setCollectedTargets] = useState<Record<number, readonly EntityId[]>>({})

  const totalRequirements = decision.targetRequirements.length
  const targetReq = decision.targetRequirements[currentReqIndex]
  const minTargets = targetReq?.minTargets ?? 1
  const maxTargets = targetReq?.maxTargets ?? 1
  const legalTargets = decision.legalTargets[currentReqIndex] ?? []

  // For multi-requirement, exclude already-selected targets from valid options
  const alreadySelected = Object.values(collectedTargets).flat()
  const filteredLegalTargets = legalTargets.filter((id) => !alreadySelected.includes(id))

  // Look up source card image from game state
  const sourceId = decision.context.sourceId
  const sourceCard = sourceId ? gameState?.cards[sourceId] : undefined
  const sourceImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  // Start decision selection state when this component mounts or requirement changes
  useEffect(() => {
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions: [...filteredLegalTargets],
      selectedOptions: [],
      minSelections: minTargets,
      maxSelections: maxTargets,
      prompt: targetReq?.description ?? decision.prompt,
    }
    startDecisionSelection(selectionState)

    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id, currentReqIndex])

  const selectedCount = decisionSelectionState?.selectedOptions.length ?? 0
  const canConfirm = selectedCount >= minTargets && selectedCount <= maxTargets
  const canDecline = minTargets === 0

  const handleConfirm = () => {
    if (canConfirm && decisionSelectionState) {
      const updatedTargets = { ...collectedTargets, [currentReqIndex]: decisionSelectionState.selectedOptions }

      if (currentReqIndex + 1 < totalRequirements) {
        // More requirements — advance to the next one
        setCollectedTargets(updatedTargets)
        cancelDecisionSelection()
        setCurrentReqIndex(currentReqIndex + 1)
      } else {
        // All requirements satisfied — submit
        submitTargetsDecision(updatedTargets)
        cancelDecisionSelection()
      }
    }
  }

  const handleDecline = () => {
    const updatedTargets = { ...collectedTargets, [currentReqIndex]: [] as EntityId[] }
    if (currentReqIndex + 1 < totalRequirements) {
      setCollectedTargets(updatedTargets)
      cancelDecisionSelection()
      setCurrentReqIndex(currentReqIndex + 1)
    } else {
      submitTargetsDecision(updatedTargets)
      cancelDecisionSelection()
    }
  }

  const handleCancel = () => {
    cancelDecisionSelection()
    submitCancelDecision()
  }

  const requirementLabel = totalRequirements > 1
    ? `Choose Target (${currentReqIndex + 1}/${totalRequirements})`
    : 'Choose Target'

  const promptText = targetReq?.description ?? decision.prompt

  return (
    <div className={styles.sideBannerSelection}>
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
        {selectedCount > 0
          ? `${selectedCount} / ${maxTargets} selected`
          : 'Click a valid target'}
      </div>
      {decisionSelectionState?.warning && (
        <div
          role="alert"
          style={{
            marginTop: 6,
            padding: '6px 10px',
            borderRadius: 6,
            background: 'rgba(251, 191, 36, 0.15)',
            border: '1px solid rgba(251, 191, 36, 0.7)',
            color: '#fde68a',
            fontSize: 13,
            fontWeight: 600,
            lineHeight: 1.3,
          }}
        >
          {decisionSelectionState.warning}
        </div>
      )}

      <div className={styles.buttonContainerSmall}>
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
    </div>
  )
}
