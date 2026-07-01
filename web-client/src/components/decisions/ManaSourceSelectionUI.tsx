import { useEffect, useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { DecisionSelectionState } from '@/store/slices'
import type {
  EntityId,
  ManaSourceOption,
  SelectManaSourcesDecision,
} from '@/types'
import { parseManaCost } from '@/utils/manaCost'
import { AbilityText } from '../ui/ManaSymbols'
import { DraggableBanner } from './DraggableBanner'
import styles from './DecisionUI.module.css'

// Server serializes Color enums by name ("BLACK"), but cost symbols use pip letters ("B").
const COLOR_NAME_TO_PIP: Record<string, string> = {
  WHITE: 'W', BLUE: 'U', BLACK: 'B', RED: 'R', GREEN: 'G',
}

const toPip = (color: string): string => COLOR_NAME_TO_PIP[color] ?? color

/**
 * Greedy check: do the selected sources produce enough mana to cover `costSymbols`?
 * Mirrors the engine's solver well enough for a UI hint — the server still re-solves
 * on submit. Skips X (variable). `extraGeneric` folds in non-mana payment (each tapped
 * Waterbend permanent pays {1} generic).
 */
function selectionCoversCost(
  selectedIds: readonly EntityId[],
  availableSources: readonly ManaSourceOption[],
  costSymbols: readonly string[],
  extraGeneric = 0,
): boolean {
  const sourceById = new Map(availableSources.map((s) => [s.entityId, s]))
  const coloredReqs: Record<string, number> = {}
  let genericReq = 0
  for (const s of costSymbols) {
    if (s === 'X') continue
    const num = parseInt(s, 10)
    if (!isNaN(num)) genericReq += num
    else coloredReqs[s] = (coloredReqs[s] ?? 0) + 1
  }

  // Waterbend taps pay generic only (CR — each tapped artifact/creature pays {1}).
  genericReq = Math.max(0, genericReq - extraGeneric)

  for (const id of selectedIds) {
    const src = sourceById.get(id)
    if (!src) continue
    const colors = (src.producesColors ?? []).map(toPip)
    let consumed = false
    for (const color of colors) {
      if ((coloredReqs[color] ?? 0) > 0) {
        coloredReqs[color]!--
        consumed = true
        break
      }
    }
    if (!consumed && genericReq > 0) {
      genericReq--
    }
  }

  if (genericReq > 0) return false
  for (const v of Object.values(coloredReqs)) if (v > 0) return false
  return true
}

/**
 * Mana source selection UI for SelectManaSourcesDecision.
 * Shows a side banner and allows clicking lands/sources on the battlefield,
 * with an "Auto Pay" shortcut button.
 */
export function ManaSourceSelectionUI({
  decision,
}: {
  decision: SelectManaSourcesDecision
}) {
  const startDecisionSelection = useGameStore((s) => s.startDecisionSelection)
  const decisionSelectionState = useGameStore((s) => s.decisionSelectionState)
  const cancelDecisionSelection = useGameStore((s) => s.cancelDecisionSelection)
  const submitManaSourcesDecision = useGameStore((s) => s.submitManaSourcesDecision)

  const waterbendPermanents = decision.waterbendPermanents ?? []
  const waterbendIds = useMemo(
    () => new Set(waterbendPermanents.map((p) => p.entityId)),
    [waterbendPermanents],
  )

  // Start decision selection state when this component mounts. Both mana sources and
  // Waterbend-eligible permanents are clickable on the battlefield; they're partitioned on submit.
  useEffect(() => {
    const validOptions = [
      ...decision.availableSources.map((s) => s.entityId),
      ...waterbendPermanents.map((p) => p.entityId),
    ]
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions,
      selectedOptions: [...decision.autoPaySuggestion],
      minSelections: 1,
      maxSelections: validOptions.length,
      prompt: decision.prompt,
    }
    startDecisionSelection(selectionState)

    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id])

  const selectedCount = decisionSelectionState?.selectedOptions.length ?? 0
  const selectedOptions = decisionSelectionState?.selectedOptions

  // Partition the clicked permanents: mana sources vs Waterbend taps (each pays {1} generic).
  const selectedWaterbend = useMemo(
    () => (selectedOptions ?? []).filter((id) => waterbendIds.has(id)),
    [selectedOptions, waterbendIds],
  )
  const selectedManaSources = useMemo(
    () => (selectedOptions ?? []).filter((id) => !waterbendIds.has(id)),
    [selectedOptions, waterbendIds],
  )

  const sacrificedSources = useMemo(() => {
    if (!selectedOptions) return []
    const byId = new Map(decision.availableSources.map((s) => [s.entityId, s]))
    return selectedOptions
      .map((id) => byId.get(id))
      .filter((s): s is ManaSourceOption => !!s && !!s.requiresSacrifice)
  }, [selectedOptions, decision.availableSources])

  const costSymbols = useMemo(
    () => parseManaCost(decision.requiredCost),
    [decision.requiredCost],
  )
  const isSelectionSufficient = useMemo(
    () =>
      selectionCoversCost(
        selectedManaSources,
        decision.availableSources,
        costSymbols,
        selectedWaterbend.length,
      ),
    [selectedManaSources, selectedWaterbend, decision.availableSources, costSymbols],
  )

  const handleAutoPay = () => {
    submitManaSourcesDecision([], true)
    cancelDecisionSelection()
  }

  const handleConfirm = () => {
    if (decisionSelectionState && isSelectionSufficient) {
      submitManaSourcesDecision(selectedManaSources, false, selectedWaterbend)
      cancelDecisionSelection()
    }
  }

  const handleDecline = () => {
    submitManaSourcesDecision([], false)
    cancelDecisionSelection()
  }

  return (
    <DraggableBanner className={styles.sideBannerSelection}>
      <div className={styles.bannerTitleSelection}>
        {decision.canDecline ? 'Activate Ability?' : 'Select Mana Sources'}
      </div>
      {decision.context.sourceName && (
        <div className={styles.hint}>
          <AbilityText text={decision.prompt} size={13} />
        </div>
      )}
      <div className={styles.hint}>
        {selectedCount > 0
          ? `${selectedCount} source${selectedCount !== 1 ? 's' : ''} selected`
          : 'Click lands to select'}
      </div>
      {waterbendPermanents.length > 0 && (
        <div className={styles.hint}>
          <AbilityText
            text="Waterbend: tap artifacts/creatures you control to pay {1} each."
            size={12}
          />
          {selectedWaterbend.length > 0 && (
            <div>{selectedWaterbend.length} tapped for Waterbend</div>
          )}
        </div>
      )}
      {!isSelectionSufficient && (
        <div className={styles.effectHint}>
          Not enough mana selected
        </div>
      )}
      {sacrificedSources.length > 0 && (
        <div className={styles.effectHint}>
          Will sacrifice: {sacrificedSources.map((s) => s.name).join(', ')}
        </div>
      )}

      <div className={styles.buttonContainerSmall}>
        {decision.canDecline ? (
          <>
            <button
              onClick={handleConfirm}
              disabled={!isSelectionSufficient}
              className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
            >
              Confirm
            </button>
            <button
              onClick={handleDecline}
              className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
            >
              Decline
            </button>
          </>
        ) : (
          <>
            <button
              onClick={handleAutoPay}
              className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
            >
              Auto Pay
            </button>
            <button
              onClick={handleConfirm}
              disabled={!isSelectionSufficient}
              className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
            >
              Confirm ({selectedCount})
            </button>
          </>
        )}
      </div>
    </DraggableBanner>
  )
}
