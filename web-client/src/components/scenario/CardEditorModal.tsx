/**
 * Per-permanent editor for a battlefield card: tapped / summoning sickness, counters of any
 * type, aura or equipment attachment, and the durable "as this enters, choose …" values the
 * scenario DTO can pre-set (creature type, colour, card type).
 *
 * Everything here maps 1:1 onto `BattlefieldCardConfig` on the server, so what the tester sets
 * is exactly what the scenario boots with — no ETB choice prompts to click through.
 */
import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { ManaCost } from '@/components/ui/ManaSymbols'
import { getCardImageUrl } from '@/utils/cardImages'
import type { ScenarioBattlefieldCard } from './types'
import styles from './ScenarioBuilder.module.css'

/** The counter types worth one click. Anything else can be typed in by name. */
const COMMON_COUNTERS = [
  'PLUS_ONE_PLUS_ONE',
  'MINUS_ONE_MINUS_ONE',
  'LOYALTY',
  'DEFENSE',
  'CHARGE',
  'STUN',
  'SHIELD',
  'LORE',
  'POISON',
  'TIME',
  'OMEN',
  'HONE',
]

const COLORS = ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN']

const CARD_TYPES = [
  'Artifact',
  'Battle',
  'Creature',
  'Enchantment',
  'Instant',
  'Land',
  'Planeswalker',
  'Sorcery',
]

function counterLabel(type: string): string {
  if (type === 'PLUS_ONE_PLUS_ONE') return '+1/+1'
  if (type === 'MINUS_ONE_MINUS_ONE') return '−1/−1'
  return type.toLowerCase().replace(/_/g, ' ')
}

export function CardEditorModal({
  card,
  summary,
  hostOptions,
  creatureTypes,
  onChange,
  onRemove,
  onClose,
}: {
  card: ScenarioBattlefieldCard
  summary: CardSummary | undefined
  /** Other permanents on the same battlefield — the legal `attachedTo` hosts. */
  hostOptions: string[]
  creatureTypes: string[]
  onChange: (patch: Partial<ScenarioBattlefieldCard>) => void
  onRemove: () => void
  onClose: () => void
}) {
  const [newCounter, setNewCounter] = useState('')

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const counters = card.counters ?? {}
  const setCounter = (type: string, value: number) => {
    const next = { ...counters }
    if (value > 0) next[type] = value
    else delete next[type]
    onChange({ counters: next })
  }

  const typeLine = summary
    ? [summary.supertypes, summary.cardTypes, summary.subtypes]
        .flat()
        .filter(Boolean)
        .map((t) => t[0]! + t.slice(1).toLowerCase())
        .join(' ')
    : ''

  return createPortal(
    <div className={styles.modalBackdrop} onClick={onClose} role="presentation">
      <div
        className={styles.modal}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-label={`Edit ${card.name}`}
      >
        <div className={styles.modalArt}>
          <img src={getCardImageUrl(card.name, summary?.imageUri ?? null, 'normal')} alt={card.name} />
        </div>
        <div className={styles.modalBody}>
          <div className={styles.modalHead}>
            <h2 className={styles.modalTitle}>{card.name}</h2>
            <ManaCost cost={summary?.manaCost || null} size={13} />
          </div>
          {typeLine && <div className={styles.modalType}>{typeLine}</div>}

          <div className={styles.fieldGroup}>
            <span className={styles.fieldLabel}>State</span>
            <div className={styles.toggleRow}>
              <label className={card.tapped ? styles.toggleActive : styles.toggle}>
                <input
                  type="checkbox"
                  checked={!!card.tapped}
                  onChange={(e) => onChange({ tapped: e.target.checked })}
                />
                Tapped
              </label>
              <label className={card.summoningSickness ? styles.toggleActive : styles.toggle}>
                <input
                  type="checkbox"
                  checked={!!card.summoningSickness}
                  onChange={(e) => onChange({ summoningSickness: e.target.checked })}
                />
                Summoning sickness
              </label>
            </div>
          </div>

          <div className={styles.fieldGroup}>
            <span className={styles.fieldLabel}>Counters</span>
            {Object.entries(counters).map(([type, count]) => (
              <div key={type} className={styles.counterRow}>
                <span className={styles.counterName}>{counterLabel(type)}</span>
                <button
                  type="button"
                  className={styles.stepBtn}
                  aria-label={`Remove one ${counterLabel(type)} counter`}
                  onClick={() => setCounter(type, count - 1)}
                >
                  −
                </button>
                <input
                  type="number"
                  className={styles.numberInput}
                  value={count}
                  min={0}
                  aria-label={`${counterLabel(type)} counters`}
                  onChange={(e) => setCounter(type, Number(e.target.value))}
                />
                <button
                  type="button"
                  className={styles.stepBtn}
                  aria-label={`Add one ${counterLabel(type)} counter`}
                  onClick={() => setCounter(type, count + 1)}
                >
                  +
                </button>
              </div>
            ))}
            <div className={styles.counterRow}>
              <input
                className={styles.textInput}
                list="scenario-counter-types"
                placeholder="Add a counter type…"
                value={newCounter}
                onChange={(e) => setNewCounter(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && newCounter.trim()) {
                    e.preventDefault()
                    const type = newCounter.trim().toUpperCase().replace(/[\s-]+/g, '_')
                    setCounter(type, (counters[type] ?? 0) + 1)
                    setNewCounter('')
                  }
                }}
              />
              <button
                type="button"
                className={styles.ghostBtn}
                disabled={!newCounter.trim()}
                onClick={() => {
                  const type = newCounter.trim().toUpperCase().replace(/[\s-]+/g, '_')
                  setCounter(type, (counters[type] ?? 0) + 1)
                  setNewCounter('')
                }}
              >
                Add
              </button>
              <datalist id="scenario-counter-types">
                {COMMON_COUNTERS.map((c) => (
                  <option key={c} value={c}>
                    {counterLabel(c)}
                  </option>
                ))}
              </datalist>
            </div>
          </div>

          {hostOptions.length > 0 && (
            <div className={styles.fieldGroup}>
              <span className={styles.fieldLabel}>Attached to (auras / equipment)</span>
              <select
                className={styles.textInput}
                value={card.attachedTo ?? ''}
                onChange={(e) => onChange({ attachedTo: e.target.value })}
              >
                <option value="">Not attached</option>
                {hostOptions.map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className={styles.fieldGroup}>
            <span className={styles.fieldLabel}>Pre-set “as this enters, choose …”</span>
            <div className={styles.counterRow}>
              <input
                className={styles.textInput}
                list="scenario-creature-types"
                placeholder="Creature type"
                value={card.chosenCreatureType ?? ''}
                onChange={(e) => onChange({ chosenCreatureType: e.target.value })}
              />
              <select
                className={styles.textInput}
                value={card.chosenColor ?? ''}
                onChange={(e) => onChange({ chosenColor: e.target.value })}
              >
                <option value="">Colour…</option>
                {COLORS.map((c) => (
                  <option key={c} value={c}>
                    {c[0]! + c.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
              <select
                className={styles.textInput}
                value={card.chosenCardType ?? ''}
                onChange={(e) => onChange({ chosenCardType: e.target.value })}
              >
                <option value="">Card type…</option>
                {CARD_TYPES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
            <datalist id="scenario-creature-types">
              {creatureTypes.map((t) => (
                <option key={t} value={t} />
              ))}
            </datalist>
          </div>

          <div className={styles.modalActions}>
            <button type="button" className={styles.ghostBtn} onClick={onClose}>
              Done
            </button>
            <button
              type="button"
              className={styles.dangerBtn}
              onClick={() => {
                onRemove()
                onClose()
              }}
            >
              Remove from battlefield
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body,
  )
}
