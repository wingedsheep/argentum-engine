import { useMemo, useState, type CSSProperties } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { ModalModeSelectionState } from '@/store/slices/types'
import { useResponsive } from '@/hooks/useResponsive'
import { getCardImageUrl } from '@/utils/cardImages'
import { ManaCost, AbilityText } from './ManaSymbols'
import { DecisionCardPreview } from '../decisions/DecisionComponents'
import decisionStyles from '../decisions/DecisionUI.module.css'

/**
 * Single-panel mode selector for choose-N modal spells (Spree / "choose one or more").
 *
 * Replaces the sequential one-mode-at-a-time prompt: the player sees every mode at once with
 * its `+ {cost}`, toggles the subset they want (a count stepper when the spell allows repeats),
 * sees the combined additional cost and grand total update live, and confirms once. Targets are
 * then chosen on the battlefield (server-driven), so this panel only picks the modes.
 *
 * Driven by `modalModeSelectionState` (set by the cast pipeline's `modalModes` phase). Confirm
 * advances the pipeline with the chosen mode indices; cancel rolls the whole cast back.
 */

/** Strip a leading "+ {cost} — " / "+ {cost} - " prefix so the cost can render in its own column. */
function stripCostPrefix(description: string): string {
  return description.replace(/^\+\s*(?:\{[^}]+\}\s*)+[—-]\s*/, '').trim()
}

/** Merge several mana-cost strings into one canonical string (generic summed, colored kept in order). */
function combineManaCosts(costs: readonly string[]): string {
  let generic = 0
  const symbols: string[] = []
  for (const cost of costs) {
    const matches = cost.match(/\{([^}]+)\}/g) ?? []
    for (const m of matches) {
      const inner = m.slice(1, -1)
      if (/^\d+$/.test(inner)) generic += parseInt(inner, 10)
      else symbols.push(inner)
    }
  }
  const genericPart = generic > 0 ? `{${generic}}` : ''
  return genericPart + symbols.map((s) => `{${s}}`).join('')
}

export function ModalModeSelector() {
  const state = useGameStore((s) => s.modalModeSelectionState)
  if (!state) return null
  // Key by source card so the draft selection resets cleanly per spell (the outer component
  // stays mounted in App; the inner panel is remounted whenever a new modal cast begins).
  const key = state.actionInfo.action.type === 'CastSpell' ? state.actionInfo.action.cardId : 'modal'
  return <ModalModePanel key={key} state={state} />
}

function ModalModePanel({ state }: { state: ModalModeSelectionState }) {
  const confirmModalModeSelection = useGameStore((s) => s.confirmModalModeSelection)
  const cancelModalModeSelection = useGameStore((s) => s.cancelModalModeSelection)
  const gameState = useGameStore((s) => s.gameState)
  const responsive = useResponsive()

  const { enumeration, cardName, baseManaCost } = state
  const { modes, minChooseCount, chooseCount, allowRepeat } = enumeration

  const [counts, setCounts] = useState<number[]>(() => new Array(modes.length).fill(0) as number[])
  const [minimized, setMinimized] = useState(false)
  const [isHoveringSource, setIsHoveringSource] = useState(false)

  const totalChosen = useMemo(() => counts.reduce((a, b) => a + b, 0), [counts])

  const sourceId = state.actionInfo.action.type === 'CastSpell' ? state.actionInfo.action.cardId : undefined
  const sourceCard = sourceId ? gameState?.cards[sourceId] : undefined
  const sourceCardImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  const isSelectable = (i: number) => modes[i]?.available === true
  const canAdd = (i: number) => isSelectable(i) && (allowRepeat || (counts[i] ?? 0) === 0) && totalChosen < chooseCount
  const canRemove = (i: number) => (counts[i] ?? 0) > 0

  const add = (i: number) => { if (canAdd(i)) setCounts((p) => p.map((c, j) => (j === i ? c + 1 : c))) }
  const remove = (i: number) => { if (canRemove(i)) setCounts((p) => p.map((c, j) => (j === i ? c - 1 : c))) }
  const toggle = (i: number) => { if ((counts[i] ?? 0) > 0) remove(i); else add(i) }

  // Live cost preview from the currently-selected modes.
  const additionalCost = combineManaCosts(
    counts.flatMap((c, i) => Array.from({ length: c }, () => modes[i]?.additionalManaCost ?? ''))
  )
  const totalCost = combineManaCosts([baseManaCost, additionalCost])

  const withinRange = totalChosen >= minChooseCount && totalChosen <= chooseCount
  const rangeLabel = minChooseCount === chooseCount
    ? `Choose ${chooseCount}`
    : `Choose ${minChooseCount}–${chooseCount}`

  const handleConfirm = () => {
    if (!withinRange) return
    const chosenModes: number[] = []
    counts.forEach((c, i) => { for (let k = 0; k < c; k++) chosenModes.push(modes[i]!.index) })
    confirmModalModeSelection(chosenModes)
  }

  if (minimized) {
    return (
      <button className={decisionStyles.floatingReturnButton} onClick={() => setMinimized(false)}>
        Return to {cardName}
      </button>
    )
  }

  return (
    <div className={decisionStyles.overlay}>
      {sourceCardImageUrl && (
        <img
          src={sourceCardImageUrl}
          alt={`Source: ${cardName}`}
          className={decisionStyles.bannerCardImage}
          onMouseEnter={() => setIsHoveringSource(true)}
          onMouseLeave={() => setIsHoveringSource(false)}
        />
      )}

      <h2 className={decisionStyles.title}>{cardName}</h2>
      <p className={decisionStyles.sourceLabel}>{rangeLabel} — pay for each chosen mode</p>

      {/* Mode list */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%', maxWidth: 540 }}>
        {modes.map((mode, i) => {
          const count = counts[i] ?? 0
          const selected = count > 0
          const selectable = isSelectable(i)
          return (
            <button
              key={mode.index}
              onClick={() => { if (!allowRepeat && selectable) toggle(i) }}
              disabled={!selectable}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                textAlign: 'left',
                cursor: selectable && !allowRepeat ? 'pointer' : 'default',
                background: selected ? 'rgba(232, 197, 109, 0.12)' : 'rgba(255, 255, 255, 0.04)',
                border: selected ? '1px solid rgba(232, 197, 109, 0.4)' : '1px solid rgba(255, 255, 255, 0.08)',
                opacity: selectable ? 1 : 0.4,
                transition: 'all 0.12s ease',
              }}
            >
              {/* Checkbox (single-pick) — repeat spells use the +/- stepper on the right instead */}
              {!allowRepeat && (
                <span
                  aria-hidden
                  style={{
                    width: 18, height: 18, flexShrink: 0, borderRadius: 4,
                    border: selected ? 'none' : '1.5px solid rgba(255,255,255,0.35)',
                    background: selected ? '#e8c56d' : 'transparent',
                    color: '#1a1a1a', fontSize: 13, fontWeight: 800, lineHeight: '18px', textAlign: 'center',
                  }}
                >
                  {selected ? '✓' : ''}
                </span>
              )}

              {/* Cost chip */}
              <span style={{ flexShrink: 0, minWidth: 34, display: 'inline-flex', alignItems: 'center', gap: 2 }}>
                {mode.additionalManaCost
                  ? <ManaCost cost={mode.additionalManaCost} size={16} />
                  : <span style={{ color: 'var(--text-tertiary)', fontSize: 'var(--font-sm)' }}>—</span>}
              </span>

              {/* Description (cost prefix stripped; symbols rendered inline) */}
              <span style={{ flex: 1, fontSize: 'var(--font-md)', lineHeight: 1.35, color: 'var(--text-primary)' }}>
                <AbilityText text={stripCostPrefix(mode.description)} size={14} />
                {!selectable && (
                  <span style={{ color: 'var(--text-tertiary)', fontStyle: 'italic' }}> (no legal target)</span>
                )}
              </span>

              {/* Count stepper for repeatable modes */}
              {allowRepeat && (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                  <span
                    role="button"
                    onClick={(e) => { e.stopPropagation(); remove(i) }}
                    style={stepBtn(canRemove(i), '#ff8888')}
                  >−</span>
                  <span style={{ minWidth: 18, textAlign: 'center', fontWeight: 700, color: selected ? '#e8c56d' : 'var(--text-disabled)' }}>{count}</span>
                  <span
                    role="button"
                    onClick={(e) => { e.stopPropagation(); add(i) }}
                    style={stepBtn(canAdd(i), '#88cc88')}
                  >+</span>
                </span>
              )}
            </button>
          )
        })}
      </div>

      {/* Cost summary */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 18, marginTop: 4, fontSize: 'var(--font-sm)' }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color: 'var(--text-secondary)' }}>
          Additional: {additionalCost ? <ManaCost cost={additionalCost} size={15} /> : <span>—</span>}
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color: 'var(--text-primary)', fontWeight: 600 }}>
          Total: <ManaCost cost={totalCost} size={15} />
        </span>
      </div>

      {isHoveringSource && !responsive.isMobile && (
        <DecisionCardPreview cardName={cardName} imageUri={sourceCard?.imageUri} />
      )}

      <div className={decisionStyles.optionButtonRow}>
        <button onClick={() => setMinimized(true)} className={decisionStyles.viewBattlefieldButton}>
          View Battlefield
        </button>
        <button onClick={cancelModalModeSelection} className={decisionStyles.confirmButton}>
          Cancel
        </button>
        <button onClick={handleConfirm} disabled={!withinRange} className={decisionStyles.confirmButton}>
          Confirm ({totalChosen}/{chooseCount})
        </button>
      </div>
    </div>
  )
}

function stepBtn(enabled: boolean, color: string): CSSProperties {
  return {
    width: 26, height: 26, borderRadius: '50%', display: 'inline-flex', alignItems: 'center',
    justifyContent: 'center', fontSize: 17, fontWeight: 700, userSelect: 'none',
    cursor: enabled ? 'pointer' : 'default',
    background: enabled ? 'rgba(255,255,255,0.12)' : 'rgba(255,255,255,0.04)',
    color: enabled ? color : 'var(--text-disabled)',
  }
}
