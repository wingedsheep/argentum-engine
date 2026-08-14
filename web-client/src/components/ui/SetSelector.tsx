/**
 * SetSelector — the one control for "which sets does this thing draw cards from?".
 *
 * {@link SetPickerModal} was already shared, but the row that *fronts* it wasn't: the tournament
 * card-source row, the quick lobby's AI seat, the pod's AI seat and the deck picker's Random tab each
 * grew their own chip strip, and they drifted. One showed raw set codes instead of names, one had no
 * way to remove a chip, one offered no "Random Set" row at all, and the button underneath was
 * variously "Choose sets", "+ Add sets" and "+ Choose sets". This component is that strip, once:
 *
 *   [🜲 Innistrad ×] [🜲 Ixalan ×]  ( + Add set )
 *
 * What stays a per-caller decision is what "random" *means*, because the two meanings are real:
 *   - **Deferred** (tournament): {@link onSelectRandom} appends a {@link RANDOM_SET_CODE} sentinel
 *     that stays hidden as "Random Set" until the server rolls it at game start.
 *   - **Rolled now** (AI seats, deck picker): the caller picks a concrete set immediately, and the
 *     chip names it like any other.
 * Either way the chip renders itself correctly — sentinel codes get the die and the "revealed when
 * the game starts" tooltip — so callers only supply the behaviour, never the presentation.
 */
import { useState } from 'react'
import type { AvailableSet } from '@/types/messages'
import { SetIcon } from './SetIcon'
import { SetPickerModal, setProductLabel } from './SetPickerModal'
import styles from './GameUI.module.css'

/**
 * Sentinel set code for a deferred "Random Set" pick — the concrete set stays hidden until the
 * server rolls it at game start. Mirrors `TournamentLobby.RANDOM_SET_CODE`; multiple random slots
 * use suffixed codes (RANDOM, RANDOM-2, …).
 */
export const RANDOM_SET_CODE = 'RANDOM'

export const isRandomSetCode = (code: string): boolean =>
  code === RANDOM_SET_CODE || code.startsWith(`${RANDOM_SET_CODE}-`)

/** Next free deferred-random code for a selection that may already hold some. */
export function nextRandomSetCode(selectedCodes: readonly string[]): string {
  const existing = selectedCodes.filter(isRandomSetCode).length
  return existing === 0 ? RANDOM_SET_CODE : `${RANDOM_SET_CODE}-${existing + 1}`
}

/**
 * Roll a concrete set for callers whose "random" is resolved client-side. Never returns an extension
 * set (a bonus sheet can't be played on its own) nor one already selected. Null when nothing is left.
 */
export function rollRandomSet(
  sets: readonly AvailableSet[],
  selectedCodes: readonly string[],
): AvailableSet | null {
  const candidates = sets.filter((s) => !s.extensionSet && !selectedCodes.includes(s.code))
  return candidates[Math.floor(Math.random() * candidates.length)] ?? null
}

export interface SetSelectorProps {
  /** Every set the picker can browse (complete + partial). */
  sets: readonly AvailableSet[]
  /** Codes currently selected, in display order. */
  selectedCodes: readonly string[]
  /** Toggle one set — called both by a chip's × and by a row in the modal. */
  onToggleSet: (code: string) => void
  /** 'single' picks exactly one set and closes the modal on click. Defaults to 'multi'. */
  mode?: 'single' | 'multi'
  disabled?: boolean
  /** Modal heading. Defaults to a sensible one for the mode. */
  title?: string
  /** Chip accent — 'draft' is the blue variant that matches the draft formats' accent colour. */
  accent?: 'sealed' | 'draft'
  /** Chip row alignment. 'end' matches the right-aligned tournament settings rows. */
  align?: 'start' | 'end'
  /** When set, the modal offers a "Random Set" row and calls this if the user picks it. */
  onSelectRandom?: () => void
  /**
   * True when an *empty* selection already means "the server rolls a set at game start". The
   * selector then shows a 🎲 "Random Set" chip saying so, rather than an empty-state that reads
   * like something is missing.
   */
  emptyMeansRandom?: boolean
  /** Empty-state text when {@link emptyMeansRandom} is false. */
  emptyLabel?: string
  /** Non-booster product ids selected per set code. Only meaningful in multi mode. */
  selectedProducts?: Readonly<Record<string, readonly string[]>>
  onToggleProduct?: (setCode: string, productId: string) => void
}

export function SetSelector({
  sets,
  selectedCodes,
  onToggleSet,
  mode = 'multi',
  disabled = false,
  title,
  accent = 'sealed',
  align = 'end',
  onSelectRandom,
  emptyMeansRandom = false,
  emptyLabel = 'No sets selected yet',
  selectedProducts,
  onToggleProduct,
}: SetSelectorProps) {
  const [pickerOpen, setPickerOpen] = useState(false)

  const isSingle = mode === 'single'
  const hasSelection = selectedCodes.length > 0
  // A bonus sheet can only be drafted alongside a real set, so say so rather than letting the
  // server reject the lobby at start. Single mode never offers extension sets in the first place.
  const needsBaseSet =
    !isSingle &&
    hasSelection &&
    !selectedCodes.some((code) => isRandomSetCode(code) || !sets.find((s) => s.code === code)?.extensionSet)

  const buttonLabel = isSingle
    ? hasSelection ? 'Change set' : 'Choose a set'
    : hasSelection ? '+ Add set' : 'Choose sets'

  return (
    <div className={`${styles.setSelection} ${align === 'start' ? styles.setSelectionStart : ''}`}>
      {hasSelection || emptyMeansRandom ? (
        <div className={styles.setChips}>
          {/* The one chip that stands for an absence: nothing is selected, and that *is* the
              answer — the server rolls a set when the game starts. */}
          {!hasSelection ? (
            <span className={styles.setChip} title="A random set is rolled when the game starts">
              <span className={styles.setChipIcon} aria-hidden>🎲</span>
              <span className={styles.setChipName}>Random Set</span>
            </span>
          ) : (
            selectedCodes.map((code) => (
              <SelectedSetChip
                key={code}
                code={code}
                sets={sets}
                accent={accent}
                removable={!disabled}
                productLabels={(selectedProducts?.[code] ?? []).map(setProductLabel)}
                onRemove={() => onToggleSet(code)}
              />
            ))
          )}
        </div>
      ) : (
        <span className={styles.setSelectionEmpty}>{emptyLabel}</span>
      )}

      {needsBaseSet && (
        <span className={styles.setSelectionEmpty}>
          Extension sets need a regular set alongside them.
        </span>
      )}

      <button
        type="button"
        className={styles.addSetsButton}
        onClick={() => setPickerOpen(true)}
        disabled={disabled}
      >
        {buttonLabel}
      </button>

      {pickerOpen && (
        <SetPickerModal
          sets={sets}
          mode={mode}
          selectedCodes={selectedCodes}
          onToggleSet={onToggleSet}
          onClose={() => setPickerOpen(false)}
          title={title ?? (isSingle ? 'Choose a set' : 'Choose sets')}
          {...(onSelectRandom ? { onSelectRandom } : {})}
          {...(selectedProducts ? { selectedProducts } : {})}
          {...(onToggleProduct ? { onToggleProduct } : {})}
        />
      )}
    </div>
  )
}

/**
 * One selected set, as a chip. Resolves its own name/partial/extension flags off `sets` so callers
 * hand over codes and nothing else, and renders a deferred {@link RANDOM_SET_CODE} slot as the die
 * rather than leaking the sentinel string into the UI.
 */
function SelectedSetChip({
  code,
  sets,
  accent,
  removable,
  productLabels,
  onRemove,
}: {
  code: string
  sets: readonly AvailableSet[]
  accent: 'sealed' | 'draft'
  removable: boolean
  productLabels: readonly string[]
  onRemove: () => void
}) {
  const random = isRandomSetCode(code)
  const set = random ? undefined : sets.find((s) => s.code === code)
  const name = random ? 'Random Set' : set?.name ?? code
  const partial = set?.partial ?? false

  const title = productLabels.length > 0
    ? `${name} — additions: ${productLabels.join(', ')}`
    : random
      ? 'Random Set — revealed when the game starts'
      : partial
        ? `${name} — partial (reduced card pool)`
        : set?.extensionSet
          ? `${name} — extension set (needs a regular set alongside)`
          : name

  return (
    <span
      className={[
        styles.setChip,
        accent === 'draft' ? styles.setChipDraft : '',
        partial ? styles.setChipPartial : '',
      ].filter(Boolean).join(' ')}
      title={title}
    >
      {random
        ? <span className={styles.setChipIcon} aria-hidden>🎲</span>
        : <SetIcon code={code} className={styles.setChipIcon} />}
      <span className={styles.setChipName}>{name}</span>
      {productLabels.length > 0 && (
        <span className={styles.setChipExtras} aria-label={`Additions: ${productLabels.join(', ')}`}>+</span>
      )}
      {removable && (
        <button
          type="button"
          className={styles.setChipRemove}
          onClick={onRemove}
          aria-label={`Remove ${name}`}
        >
          ×
        </button>
      )}
    </span>
  )
}
