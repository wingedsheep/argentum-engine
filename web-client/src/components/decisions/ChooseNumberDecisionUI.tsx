import { useState, useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ChooseNumberDecision } from '@/types'
import { ManaSymbol } from '../ui/ManaSymbols'
import { DecisionContextCards, hasDecisionContextCards, resolveDecisionCards } from './DecisionContextCards'
import styles from './DecisionUI.module.css'

/**
 * Parse a mana distribution prompt to extract color symbols and total.
 * Returns null if the prompt isn't a mana distribution prompt.
 * Expected format: "Choose how much {G} mana to add (rest will be {R}). Total: 3"
 */
function parseManaDistributionPrompt(prompt: string): { firstColor: string; secondColor: string; total: number } | null {
  const match = prompt.match(/Choose how much \{(\w+)\} mana to add \(rest will be \{(\w+)\}\)\. Total: (\d+)/)
  if (!match?.[1] || !match?.[2] || !match?.[3]) return null
  return { firstColor: match[1], secondColor: match[2], total: parseInt(match[3], 10) }
}

/**
 * Mana distribution decision - specialized UI for choosing how to split mana
 * between two colors. Shows mana icons, a slider, and a live preview of the split.
 */
function ManaDistributionUI({
  decision,
  firstColor,
  secondColor,
  total,
  onMinimize,
}: {
  decision: ChooseNumberDecision
  firstColor: string
  secondColor: string
  total: number
  onMinimize?: (() => void) | undefined
}) {
  const [firstAmount, setFirstAmount] = useState(0)
  const submitNumberDecision = useGameStore((s) => s.submitNumberDecision)
  const gameState = useGameStore((s) => s.gameState)
  const cards = resolveDecisionCards(decision.context, gameState)

  const secondAmount = total - firstAmount

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFirstAmount(parseInt(e.target.value, 10))
  }

  const handleConfirm = () => {
    submitNumberDecision(decision.id, firstAmount)
  }

  // Render a row of mana symbols for a given color and count
  const renderManaRow = (color: string, count: number) => {
    if (count === 0) return <span style={{ color: '#666', fontSize: 14, fontStyle: 'italic' }}>None</span>
    const symbols = []
    for (let i = 0; i < count; i++) {
      symbols.push(<ManaSymbol key={i} symbol={color} size={28} />)
    }
    return <>{symbols}</>
  }

  return (
    <>
      <DecisionContextCards cards={cards} />
      {decision.context.sourceName && (
        <h2 className={styles.title}>
          {decision.context.sourceName}
        </h2>
      )}
      <p style={manaDistStyles.subtitle}>
        Add {total} mana in any combination
      </p>

      {/* Mana preview */}
      <div style={manaDistStyles.previewContainer}>
        {/* First color */}
        <div style={manaDistStyles.colorSection}>
          <div style={manaDistStyles.colorHeader}>
            <ManaSymbol symbol={firstColor} size={22} />
            <span style={manaDistStyles.colorCount}>{firstAmount}</span>
          </div>
          <div style={manaDistStyles.manaRow}>
            {renderManaRow(firstColor, firstAmount)}
          </div>
        </div>

        {/* Divider */}
        <div style={manaDistStyles.divider} />

        {/* Second color */}
        <div style={manaDistStyles.colorSection}>
          <div style={manaDistStyles.colorHeader}>
            <ManaSymbol symbol={secondColor} size={22} />
            <span style={manaDistStyles.colorCount}>{secondAmount}</span>
          </div>
          <div style={manaDistStyles.manaRow}>
            {renderManaRow(secondColor, secondAmount)}
          </div>
        </div>
      </div>

      {/* Slider */}
      <div style={manaDistStyles.sliderContainer}>
        <ManaSymbol symbol={secondColor} size={20} />
        <input
          type="range"
          min={0}
          max={total}
          value={firstAmount}
          onChange={handleSliderChange}
          style={manaDistStyles.slider}
        />
        <ManaSymbol symbol={firstColor} size={20} />
      </div>

      {/* Quick select buttons */}
      <div style={manaDistStyles.quickButtons}>
        <button
          onClick={() => setFirstAmount(0)}
          style={{
            ...manaDistStyles.quickButton,
            ...(firstAmount === 0 ? manaDistStyles.quickButtonActive : {}),
          }}
        >
          All <ManaSymbol symbol={secondColor} size={14} />
        </button>
        {total > 1 && (
          <button
            onClick={() => setFirstAmount(Math.floor(total / 2))}
            style={{
              ...manaDistStyles.quickButton,
              ...(firstAmount === Math.floor(total / 2) && total > 1 ? manaDistStyles.quickButtonActive : {}),
            }}
          >
            Split
          </button>
        )}
        <button
          onClick={() => setFirstAmount(total)}
          style={{
            ...manaDistStyles.quickButton,
            ...(firstAmount === total ? manaDistStyles.quickButtonActive : {}),
          }}
        >
          All <ManaSymbol symbol={firstColor} size={14} />
        </button>
      </div>

      {/* Confirm + optional View Battlefield */}
      <div className={styles.buttonContainer}>
        {onMinimize && (
          <button onClick={onMinimize} className={styles.viewBattlefieldButton}>
            View Battlefield
          </button>
        )}
        <button onClick={handleConfirm} className={styles.confirmButton}>
          Add Mana
        </button>
      </div>
    </>
  )
}

const manaDistStyles: Record<string, React.CSSProperties> = {
  subtitle: {
    margin: '0 0 20px 0',
    color: '#999',
    fontSize: 14,
    textAlign: 'center',
  },
  previewContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'stretch',
    gap: 16,
    marginBottom: 24,
    padding: '16px 12px',
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 10,
    border: '1px solid rgba(255, 255, 255, 0.08)',
  },
  colorSection: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 8,
    minHeight: 60,
  },
  colorHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
  },
  colorCount: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    minWidth: 24,
    textAlign: 'center',
  },
  manaRow: {
    display: 'flex',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 4,
    minHeight: 28,
    alignItems: 'center',
  },
  divider: {
    width: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
    alignSelf: 'stretch',
  },
  sliderContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 16,
    padding: '0 8px',
  },
  slider: {
    flex: 1,
    height: 6,
    appearance: 'none' as const,
    backgroundColor: '#333',
    borderRadius: 3,
    cursor: 'pointer',
    outline: 'none',
  },
  quickButtons: {
    display: 'flex',
    justifyContent: 'center',
    gap: 8,
    marginBottom: 20,
  },
  quickButton: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 4,
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    color: '#bbb',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderRadius: 6,
    cursor: 'pointer',
    transition: 'all 0.15s',
  },
  quickButtonActive: {
    backgroundColor: 'rgba(100, 160, 255, 0.15)',
    borderColor: 'rgba(100, 160, 255, 0.4)',
    color: '#fff',
  },
}

/**
 * Choose number decision - select a number from a range.
 * Detects mana distribution prompts and renders a specialized mana UI.
 */
export function ChooseNumberDecisionUI({
  decision,
  onMinimize,
}: {
  decision: ChooseNumberDecision
  onMinimize?: () => void
}) {
  // Check if this is a mana distribution prompt
  const manaDistribution = useMemo(() => parseManaDistributionPrompt(decision.prompt), [decision.prompt])

  if (manaDistribution) {
    return (
      <ManaDistributionUI
        decision={decision}
        firstColor={manaDistribution.firstColor}
        secondColor={manaDistribution.secondColor}
        total={manaDistribution.total}
        onMinimize={onMinimize}
      />
    )
  }

  // Default number selection UI
  const [selectedNumber, setSelectedNumber] = useState(decision.minValue)
  const submitNumberDecision = useGameStore((s) => s.submitNumberDecision)
  const gameState = useGameStore((s) => s.gameState)
  const cards = resolveDecisionCards(decision.context, gameState)

  const clamp = (n: number) => Math.min(decision.maxValue, Math.max(decision.minValue, n))

  const handleConfirm = () => {
    submitNumberDecision(decision.id, clamp(selectedNumber))
  }

  // A short range is fastest to tap as a button grid; a wide range (e.g.
  // "any number", sent as 0–99) becomes an unusable wall of buttons, so we
  // switch to a stepper with direct numeric entry.
  const rangeSize = decision.maxValue - decision.minValue + 1
  const useStepper = rangeSize > 12

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const parsed = parseInt(e.target.value, 10)
    setSelectedNumber(Number.isNaN(parsed) ? decision.minValue : clamp(parsed))
  }

  return (
    <>
      <DecisionContextCards cards={cards} />

      <h2 className={styles.title}>
        {decision.prompt}
      </h2>

      {!hasDecisionContextCards(cards) && decision.context.sourceName && (
        <p className={styles.subtitle}>
          {decision.context.sourceName}
        </p>
      )}

      {useStepper ? (
        <>
          <div className={styles.stepperContainer}>
            <button
              type="button"
              className={styles.stepperButton}
              onClick={() => setSelectedNumber((n) => clamp(n - 1))}
              disabled={selectedNumber <= decision.minValue}
              aria-label="Decrease"
            >
              −
            </button>
            <input
              type="number"
              className={styles.stepperInput}
              min={decision.minValue}
              max={decision.maxValue}
              value={selectedNumber}
              onChange={handleInputChange}
            />
            <button
              type="button"
              className={styles.stepperButton}
              onClick={() => setSelectedNumber((n) => clamp(n + 1))}
              disabled={selectedNumber >= decision.maxValue}
              aria-label="Increase"
            >
              +
            </button>
          </div>
          <p className={styles.stepperRangeHint}>
            {decision.minValue} – {decision.maxValue}
          </p>
        </>
      ) : (
        /* Number selection buttons */
        <div className={styles.numberContainer}>
          {Array.from({ length: rangeSize }, (_, i) => decision.minValue + i).map((num) => (
            <button
              key={num}
              onClick={() => setSelectedNumber(num)}
              className={`${styles.numberButton} ${selectedNumber === num ? styles.numberButtonSelected : ''}`}
            >
              {num}
            </button>
          ))}
        </div>
      )}

      {/* Confirm + optional View Battlefield */}
      <div className={styles.buttonContainer}>
        {onMinimize && (
          <button onClick={onMinimize} className={styles.viewBattlefieldButton}>
            View Battlefield
          </button>
        )}
        <button onClick={handleConfirm} className={styles.confirmButton}>
          Confirm
        </button>
      </div>
    </>
  )
}
