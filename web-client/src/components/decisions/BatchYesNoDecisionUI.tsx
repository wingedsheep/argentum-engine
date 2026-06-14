import { useGameStore } from '@/store/gameStore.ts'
import type { BatchYesNoDecision, ClientGameState } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { AbilityText } from '../ui/ManaSymbols'
import styles from './DecisionUI.module.css'

/**
 * Batched yes/no for a run of N identical optional ("you may …") triggers (e.g. a board of
 * pingers all triggering off one creature entering). The controller answers the repeated
 * may-question once instead of N times — Magic Online's "auto-stack identical triggers" affordance.
 *
 * Four verbs following the bulk-action grammar:
 *  - Yes / No        → answer just this one; the batch re-raises for the remaining instances.
 *  - Yes to all N / No to all N → resolve the whole run with one answer.
 */
export function BatchYesNoDecisionUI({
  decision,
  gameState,
  onMinimize,
}: {
  decision: BatchYesNoDecision
  gameState: ClientGameState | null
  onMinimize?: () => void
}) {
  const submitBatchYesNoDecision = useGameStore((s) => s.submitBatchYesNoDecision)

  const sourceCard = decision.context.sourceId ? gameState?.cards[decision.context.sourceId] : undefined
  const sourceImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined
  const showCardContext = sourceImageUrl != null

  return (
    <>
      {showCardContext && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 24, marginBottom: 8 }}>
          <div style={{ textAlign: 'center', position: 'relative' }}>
            {/* "deck" shadow hinting at the stack of identical instances */}
            <div
              style={{
                position: 'absolute',
                inset: 0,
                transform: 'translate(8px, 8px)',
                borderRadius: 8,
                background: 'rgba(0,0,0,0.35)',
              }}
            />
            <img
              src={sourceImageUrl}
              alt={sourceCard?.name ?? ''}
              style={{ position: 'relative', width: 160, borderRadius: 8, boxShadow: '0 4px 16px rgba(0,0,0,0.6)' }}
            />
            <div
              style={{
                position: 'absolute',
                top: -10,
                right: -10,
                background: 'var(--accent, #c9a14a)',
                color: '#1a1a1a',
                fontWeight: 700,
                fontSize: 'var(--font-sm)',
                borderRadius: 999,
                padding: '2px 10px',
                boxShadow: '0 2px 6px rgba(0,0,0,0.5)',
              }}
            >
              ×{decision.count}
            </div>
          </div>
        </div>
      )}

      <h2 className={styles.title}>
        <AbilityText text={decision.prompt} size={20} />
      </h2>

      <p className={styles.subtitle}>
        {decision.context.sourceName ? `${decision.context.sourceName} — ` : ''}
        {decision.count} identical triggers
      </p>

      <div className={styles.buttonContainer}>
        {onMinimize && (
          <button onClick={onMinimize} className={styles.viewBattlefieldButton}>
            View Battlefield
          </button>
        )}
        <button onClick={() => submitBatchYesNoDecision(true, false)} className={styles.yesButton}>
          <AbilityText text={decision.yesText} size={16} />
        </button>
        <button onClick={() => submitBatchYesNoDecision(false, false)} className={styles.noButton}>
          <AbilityText text={decision.noText} size={16} />
        </button>
        <button onClick={() => submitBatchYesNoDecision(true, true)} className={styles.yesButton}>
          {decision.yesText} to all {decision.count}
        </button>
        <button onClick={() => submitBatchYesNoDecision(false, true)} className={styles.noButton}>
          {decision.noText} to all {decision.count}
        </button>
      </div>
    </>
  )
}
