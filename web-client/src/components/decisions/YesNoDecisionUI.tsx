import { useGameStore } from '@/store/gameStore.ts'
import type { YesNoDecision, ClientGameState } from '@/types'
import { AbilityText } from '../ui/ManaSymbols'
import { DecisionContextCards, hasDecisionContextCards, resolveDecisionCards } from './DecisionContextCards'
import styles from './DecisionUI.module.css'

/**
 * Yes/No decision - make a binary choice.
 *
 * Shows the source card (the ability owner), the triggering entity when there is one, and — for a
 * prompt raised once per object (Killing Wave asks "pay X life or sacrifice it" for each of your
 * creatures in turn) — the subject this instance is about.
 *
 * Supports an optional hint (e.g., keyword reminder text) and a View Battlefield button.
 */
export function YesNoDecisionUI({
  decision,
  gameState,
  onMinimize,
}: {
  decision: YesNoDecision
  gameState: ClientGameState | null
  onMinimize?: () => void
}) {
  const submitYesNoDecision = useGameStore((s) => s.submitYesNoDecision)

  const handleYes = () => {
    submitYesNoDecision(true)
  }

  const handleNo = () => {
    submitYesNoDecision(false)
  }

  const cards = resolveDecisionCards(decision.context, gameState)
  const showCardContext = hasDecisionContextCards(cards)

  return (
    <>
      <DecisionContextCards cards={cards} />

      <h2 className={styles.title}>
        <AbilityText text={decision.prompt} size={20} />
      </h2>

      {!showCardContext && decision.context.sourceName && (
        <p className={styles.subtitle}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Hint text (e.g., keyword reminder text) */}
      {decision.hint && (
        <p className={styles.hint}>
          <AbilityText text={decision.hint} size={14} />
        </p>
      )}

      {/* Yes/No buttons + optional View Battlefield */}
      <div className={styles.buttonContainer}>
        {onMinimize && (
          <button onClick={onMinimize} className={styles.viewBattlefieldButton}>
            View Battlefield
          </button>
        )}
        <button onClick={handleYes} className={styles.yesButton}>
          <AbilityText text={decision.yesText} size={16} />
        </button>
        <button onClick={handleNo} className={styles.noButton}>
          <AbilityText text={decision.noText} size={16} />
        </button>
      </div>
    </>
  )
}
