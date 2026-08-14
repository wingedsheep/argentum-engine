import { useEffect, useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { usePlayer } from '@/store/selectors'
import type { DecisionSelectionState } from '@/store/slices'
import type {
  ManaSourceOption,
  SelectManaSourcesDecision,
} from '@/types'
import { parseManaCost } from '@/utils/manaCost'
import { computeCoverage, isCovered } from './manaCoverage'
import { AbilityText, ManaSymbol } from '../ui/ManaSymbols'
import { DraggableBanner } from './DraggableBanner'
import styles from './DecisionUI.module.css'


/**
 * Payment UI for a [SelectManaSourcesDecision] — the engine asking one player for mana outside of
 * casting a spell (ward, "you may pay {B}", an attack tax, a draw replacement).
 *
 * There are two ways to produce the mana, and the banner has to make both discoverable:
 *  1. **Click a highlighted source.** The engine pre-computed a menu of `{T}`-shaped sources; they
 *     light up on the battlefield and are tapped when the player confirms.
 *  2. **Activate a mana ability from a permanent's menu.** CR 605.3a allows this whenever a rule or
 *     effect asks for a mana payment, and it is the only route for anything the solver can't model
 *     — Ashnod's Altar, a Forage sub-cost, an ability with no `{T}` in its activation cost. That
 *     mana lands in the pool immediately, so the readout below counts it as already paid.
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
  const manaPool = usePlayer(decision.playerId)?.manaPool ?? null

  const waterbendPermanents = decision.waterbendPermanents ?? []
  const waterbendIds = useMemo(
    () => new Set(waterbendPermanents.map((p) => p.entityId)),
    [waterbendPermanents],
  )

  // Start decision selection state when this component mounts. Both mana sources and
  // Waterbend-eligible permanents are clickable on the battlefield; they're partitioned on submit.
  //
  // Re-runs on `autoPaySuggestion` as well as on a new decision id. Activating a mana ability
  // mid-payment re-raises the *same* decision, refreshed: the source just tapped is gone from
  // `availableSources` and the suggestion now covers only what the new floating mana doesn't. That
  // is the signal that the board moved under the player, so the selection is re-seeded from it —
  // otherwise a pre-selected land stays ticked and Pay taps it on top of mana already in the pool.
  const suggestionKey = decision.autoPaySuggestion.join(',')
  useEffect(() => {
    const validOptions = [
      ...decision.availableSources.map((s) => s.entityId),
      ...waterbendPermanents.map((p) => p.entityId),
    ]
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions,
      selectedOptions: decision.autoPaySuggestion.filter((id) => validOptions.includes(id)),
      minSelections: 1,
      maxSelections: validOptions.length,
      prompt: decision.prompt,
    }
    startDecisionSelection(selectionState)

    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id, suggestionKey])

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
  const coverage = useMemo(
    () =>
      computeCoverage(
        costSymbols,
        manaPool,
        selectedManaSources,
        decision.availableSources,
        selectedWaterbend.length,
      ),
    [costSymbols, manaPool, selectedManaSources, selectedWaterbend, decision.availableSources],
  )
  const isCostCovered = coverage.every(isCovered)
  const floatingCoversAll = coverage.every((pip) => pip.floating || pip.symbol === 'X')

  const handleAutoPay = () => {
    submitManaSourcesDecision([], true)
    cancelDecisionSelection()
  }

  const handleConfirm = () => {
    if (!isCostCovered) return
    // A payment made entirely from mana the player floated themselves submits no sources; the
    // server distinguishes it from a refusal by the absence of the `declined` flag.
    submitManaSourcesDecision(selectedManaSources, false, selectedWaterbend)
    cancelDecisionSelection()
  }

  const handleDecline = () => {
    submitManaSourcesDecision([], false, [], true)
    cancelDecisionSelection()
  }

  const payLabel = floatingCoversAll && selectedManaSources.length === 0 ? 'Pay' : `Pay (${selectedManaSources.length})`

  return (
    <DraggableBanner className={styles.sideBannerSelection}>
      <div className={styles.bannerTitleSelection}>
        {decision.canDecline ? 'Pay cost?' : 'Pay cost'}
      </div>
      {decision.context.sourceName && (
        <div className={styles.hint}>
          <AbilityText text={decision.prompt} size={13} />
        </div>
      )}

      {/* Live readout: solid = already floating, outlined = will be tapped on Pay, dim = missing. */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, margin: '6px 0' }}>
        {coverage.map((pip, i) => (
          <span
            key={i}
            title={pip.floating ? 'Paid from your mana pool' : pip.pending ? 'Covered by a selected source' : 'Not yet covered'}
            style={{
              display: 'inline-flex',
              borderRadius: '50%',
              opacity: pip.floating ? 1 : pip.pending ? 0.85 : 0.3,
              filter: isCovered(pip) ? 'none' : 'grayscale(70%)',
              boxShadow: pip.pending ? '0 0 0 2px rgba(120, 220, 140, 0.9)' : 'none',
              transition: 'opacity 0.15s, filter 0.15s, box-shadow 0.15s',
            }}
          >
            <ManaSymbol symbol={pip.symbol} size={20} />
          </span>
        ))}
      </div>

      <div className={styles.hint}>
        {isCostCovered ? 'Cost covered — press Pay.' : 'Click a highlighted source to tap it.'}
      </div>
      {/* Always visible: the escape hatch for costs the highlighted menu can't cover. */}
      <div className={styles.hint} style={{ opacity: 0.7, fontSize: 11 }}>
        You can also click any permanent to use its mana ability.
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
      {sacrificedSources.length > 0 && (
        <div className={styles.effectHint}>
          Will sacrifice: {sacrificedSources.map((s) => s.name).join(', ')}
        </div>
      )}

      <div className={styles.buttonContainerSmall}>
        {!decision.canDecline && (
          <button
            onClick={handleAutoPay}
            className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
          >
            Auto Pay
          </button>
        )}
        <button
          onClick={handleConfirm}
          disabled={!isCostCovered}
          className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
        >
          {payLabel}
        </button>
        {decision.canDecline && (
          <button
            onClick={handleDecline}
            className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
          >
            Decline
          </button>
        )}
      </div>
    </DraggableBanner>
  )
}
