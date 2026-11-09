import { useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { DecisionSelectionState } from '@/store/slices'
import type { EntityId, SelectCardsDecision } from '@/types'
import { DraggableBanner } from './DraggableBanner'
import { AbilityText } from '../ui/ManaSymbols'
import { CounterAllocationControls } from '../game/card/CounterAllocationControls'
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
  const setDecisionSelection = useGameStore((s) => s.setDecisionSelection)
  const submitDecision = useGameStore((s) => s.submitDecision)
  const cards = useGameStore((s) => s.gameState?.cards)

  // Start decision selection state when this component mounts
  useEffect(() => {
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions: decision.options,
      selectedOptions: [],
      minSelections: decision.minSelections,
      maxSelections: decision.maxSelections,
      prompt: decision.prompt,
      selectionLimits: decision.selectionLimits ?? null,
    }
    startDecisionSelection(selectionState)

    // Cleanup when unmounting
    return () => {
      cancelDecisionSelection(decision.id)
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
  const canConfirm = (decision.selectionLimits == null || selectedCount === maxSelections) &&
    selectedCount >= minSelections &&
    selectedCount <= maxSelections &&
    (selectedCount >= requiredMinimum || satisfiesConditionalMinimum)
  const canSkip = minSelections === 0

  // Quick picks for "any number of" board selections (proliferate's "permanents and/or players"):
  // "All" when every option fits and nothing couples the picks, "Recommended" when the server
  // sent a suggestion. Each is a starting point the player can still adjust on the board.
  const unconstrained = decision.selectionLimits == null && conditionalMinimums.length === 0 &&
    !decision.onePerCardType && !decision.onePerColor && !decision.onePerCardName &&
    !decision.onePerBasicLandType && !decision.onePerPower && !decision.onePerManaValue &&
    decision.maxTotalManaValue == null && decision.minTotalManaValue == null && decision.maxTotalPower == null
  const recommended = decision.recommendedSelection ?? null
  const showSelectAll = unconstrained && minSelections === 0 &&
    maxSelections >= decision.options.length && decision.options.length > 1
  const showRecommended = unconstrained && recommended != null
  const isExactly = (ids: readonly EntityId[]) =>
    ids.length === selectedOptions.length && ids.every((id) => selectedOptions.includes(id))

  const handleConfirm = () => {
    if (canConfirm && decisionSelectionState) {
      submitDecision(decision.id, decisionSelectionState.selectedOptions)
    }
  }

  const handleSkip = () => {
    submitDecision(decision.id, [])
  }

  // Side banner (similar to ChooseTargetsDecision)
  return (
    <DraggableBanner className={styles.sideBannerSelection}>
      <div className={styles.bannerTitleSelection}>
        {/* The prompt is effect-authored text and may name a cost — Thelon's Curse and Magnetic
            Mountain both price their untap per creature ("pay {U} for each"). Render it through
            AbilityText so those braces become symbols instead of literal "{U}". */}
        <AbilityText text={decision.prompt} size={13} />
      </div>
      <div className={styles.hint}>
        {selectedCount > 0
          ? `${selectedCount} / ${maxSelections} selected`
          : decision.selectionLimits != null ? 'Use + and − on permanents to allocate counters' : 'Click cards to select'}
        {conditionalMinimums.length > 0 ? ` (normally ${requiredMinimum})` : minSelections > 0 && ` (min ${minSelections})`}
        {(showSelectAll || showRecommended) && selectedCount > 0 && (
          <>
            {' · '}
            <button type="button" onClick={() => setDecisionSelection([])} className={styles.quickSelectClear}>
              Clear
            </button>
          </>
        )}
      </div>
      {conditionalMinimums.map((minimum) => (
        <div key={`${minimum.requiredSelections}-${minimum.minimumSelections}-${minimum.requiredMatches}`} className={styles.hint}>
          {minimum.description ?? `You may select ${minimum.minimumSelections} if it matches the requirement.`}
        </div>
      ))}

      {(showSelectAll || showRecommended) && (
        <div className={styles.quickSelectRow} role="group" aria-label="Quick select">
          {showRecommended && (
            <button
              type="button"
              onClick={() => setDecisionSelection(recommended)}
              disabled={recommended.length === 0}
              aria-pressed={recommended.length > 0 && isExactly(recommended)}
              className={recommended.length > 0 && isExactly(recommended) ? styles.quickSelectChipActive : styles.quickSelectChip}
              title={recommended.length === 0
                ? 'Nothing here is worth another counter for you'
                : 'Select the picks that usually help you most — adjust on the board before confirming'}
            >
              <span aria-hidden="true" className={styles.quickSelectStar}>★</span>
              Recommended
              <span className={styles.quickSelectCount}>{recommended.length}</span>
            </button>
          )}
          {showSelectAll && (
            <button
              type="button"
              onClick={() => setDecisionSelection(decision.options)}
              aria-pressed={isExactly(decision.options)}
              className={isExactly(decision.options) ? styles.quickSelectChipActive : styles.quickSelectChip}
            >
              All
              <span className={styles.quickSelectCount}>{decision.options.length}</span>
            </button>
          )}
        </div>
      )}

      {decision.selectionLimits != null && (
        <details style={{ marginTop: 8 }}>
          <summary style={{ cursor: 'pointer', fontSize: 12 }}>Adjust allocations</summary>
          <div style={{ maxHeight: '35vh', overflowY: 'auto', display: 'flex',
            flexDirection: 'column', gap: 6, marginTop: 6 }}>
            {decision.options.map((id) => (
              <CounterAllocationControls key={id} cardId={id} presentation="inline"
                cardName={cards?.[id]?.name ?? decision.cardInfo?.[id]?.name ?? 'Permanent'} />
            ))}
          </div>
        </details>
      )}

      {/* Confirm/Skip buttons */}
      <div className={styles.buttonContainerSmall}>
        {canSkip && selectedCount === 0 && (
          <button onClick={handleSkip} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            {decision.declineLabel ?? 'Select None'}
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
