import { useGameStore } from '@/store/gameStore.ts'
import type { ChooseTargetsDecision } from '@/types'
import { DraggableBanner } from './DraggableBanner'
import styles from './DecisionUI.module.css'

/**
 * Player-only targeting UI for ChooseTargetsDecision.
 * Shows a side banner with Cancel button when the decision supports cancellation.
 */
export function PlayerTargetingUI({
  decision,
}: {
  decision: ChooseTargetsDecision
}) {
  const submitCancelDecision = useGameStore((s) => s.submitCancelDecision)

  const handleCancel = () => {
    submitCancelDecision()
  }

  return (
    <DraggableBanner className={styles.sideBannerTarget}>
      <div className={styles.bannerTitle}>
        Choose Target
      </div>
      <div className={styles.prompt}>
        {decision.prompt}
      </div>
      <div className={styles.hint}>
        Click a player's life total
      </div>
      {decision.canCancel && (
        <div className={styles.buttonContainerSmall}>
          <button onClick={handleCancel} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            Cancel
          </button>
        </div>
      )}
    </DraggableBanner>
  )
}
