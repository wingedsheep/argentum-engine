import { useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { DecisionSelectionState } from '@/store/slices'
import type { SelectCardsDecision } from '@/types'
import { DraggableBanner } from './DraggableBanner'
import styles from './DecisionUI.module.css'

/**
 * Battlefield selection UI for SelectCardsDecision with useTargetingUI=true.
 * Shows a side banner and allows clicking cards on the battlefield to select them.
 */
export function BattlefieldSelectionUI({
  decision,
}: {
  decision: SelectCardsDecision
}) {
  const startDecisionSelection = useGameStore((s) => s.startDecisionSelection)
  const decisionSelectionState = useGameStore((s) => s.decisionSelectionState)
  const cancelDecisionSelection = useGameStore((s) => s.cancelDecisionSelection)
  const submitDecision = useGameStore((s) => s.submitDecision)

  // Start decision selection state when this component mounts
  useEffect(() => {
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions: decision.options,
      selectedOptions: [],
      minSelections: decision.minSelections,
      maxSelections: decision.maxSelections,
      prompt: decision.prompt,
    }
    startDecisionSelection(selectionState)

    // Cleanup when unmounting
    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id])

  const selectedCount = decisionSelectionState?.selectedOptions.length ?? 0
  const minSelections = decision.minSelections
  const maxSelections = decision.maxSelections
  const selectedOptions = decisionSelectionState?.selectedOptions ?? []
  const conditionalMinimums = decision.conditionalMinimums ?? []
  const satisfiesConditionalMinimum = conditionalMinimums.some((minimum) => {
    const matching = selectedOptions.filter((id) => minimum.matchingOptions.includes(id)).length
    return selectedCount >= minimum.minimumSelections && matching >= minimum.requiredMatches
  })
  const requiredMinimum = conditionalMinimums.length > 0
    ? Math.max(...conditionalMinimums.map((minimum) => minimum.requiredSelections))
    : minSelections
  const canConfirm = selectedCount >= minSelections &&
    selectedCount <= maxSelections &&
    (selectedCount >= requiredMinimum || satisfiesConditionalMinimum)
  const canSkip = minSelections === 0

  const handleConfirm = () => {
    if (canConfirm && decisionSelectionState) {
      submitDecision(decisionSelectionState.selectedOptions)
      cancelDecisionSelection()
    }
  }

  const handleSkip = () => {
    submitDecision([])
    cancelDecisionSelection()
  }

  // Side banner (similar to ChooseTargetsDecision)
  return (
    <DraggableBanner className={styles.sideBannerSelection}>
      <div className={styles.bannerTitleSelection}>
        {decision.prompt}
      </div>
      <div className={styles.hint}>
        {selectedCount > 0
          ? `${selectedCount} / ${maxSelections} selected`
          : 'Click cards to select'}
        {conditionalMinimums.length > 0 ? ` (normally ${requiredMinimum})` : minSelections > 0 && ` (min ${minSelections})`}
      </div>
      {conditionalMinimums.map((minimum) => (
        <div key={`${minimum.requiredSelections}-${minimum.minimumSelections}-${minimum.requiredMatches}`} className={styles.hint}>
          {minimum.description ?? `You may select ${minimum.minimumSelections} if it matches the requirement.`}
        </div>
      ))}

      {/* Confirm/Skip buttons */}
      <div className={styles.buttonContainerSmall}>
        {canSkip && selectedCount === 0 && (
          <button onClick={handleSkip} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            Select None
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
      </div>
    </DraggableBanner>
  )
}
