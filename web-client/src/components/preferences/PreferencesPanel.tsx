/**
 * The preferences editor — every setting in `store/preferencesStore.ts`, grouped into Gameplay,
 * Battlefield, Display and (signed in) Messages. Shared by the /preferences page and the in-game
 * settings dialog, so the two can never drift. Each control writes straight to the store (which
 * saves to localStorage and, when signed in, to the account); there is no Save button.
 */
import type React from 'react'
import {
  DEFAULT_PREFERENCES,
  STACK_LIMITS,
  STOPPABLE_STEPS,
  usePreferences,
  type MotionPreference,
  type StackingRule,
} from '@/store/preferencesStore'
import { useAuthStore } from '@/store/authStore'
import { Step, StepDisplayNames } from '@/types/enums'
import type { PriorityModeValue } from '@/types/messages'
import s from './Preferences.module.css'

const MODE_OPTIONS: { value: PriorityModeValue; label: string; hint: string }[] = [
  { value: 'auto', label: 'Auto', hint: 'Passes whenever you have nothing meaningful to do, and stops when you can respond.' },
  { value: 'stops', label: 'Stops', hint: "Also stops on every opponent spell and ability, and on combat damage — even when you can't respond." },
  { value: 'fullControl', label: 'Full control', hint: 'Never passes for you: you get every priority window.' },
]

const MOTION_OPTIONS: { value: MotionPreference; label: string }[] = [
  { value: 'system', label: 'Follow system' },
  { value: 'reduce', label: 'Always reduce' },
]

/** On your own turn the engine always stops in your main phases — shown checked and locked. */
const ALWAYS_STOP_MY_TURN = new Set<Step>([Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN])

const ROWS: { key: 'lands' | 'creatures' | 'other'; label: string; hint: string }[] = [
  { key: 'lands', label: 'Lands', hint: 'Basic lands, duals, …' },
  { key: 'creatures', label: 'Creatures', hint: 'Including creature tokens' },
  { key: 'other', label: 'Other permanents', hint: 'Artifacts, enchantments, planeswalkers, battles' },
]

export function PreferencesPanel({ compact = false }: { compact?: boolean }) {
  const prefs = usePreferences((st) => st.prefs)
  const update = usePreferences((st) => st.update)
  const reset = usePreferences((st) => st.reset)
  const { gameplay, battlefield, display } = prefs
  // Messages need an account; a guest has nothing to be alerted to.
  const signedIn = useAuthStore((st) => st.status === 'authenticated')

  const toggleStop = (step: Step, mine: boolean) => {
    const key = mine ? 'myTurnStops' : 'opponentTurnStops'
    const current = gameplay[key]
    update('gameplay', { [key]: current.includes(step) ? current.filter((x) => x !== step) : [...current, step] })
  }
  const setRule = (key: 'lands' | 'creatures' | 'other', patch: Partial<StackingRule>) =>
    update('battlefield', { [key]: { ...battlefield[key], ...patch } })

  return (
    <div className={s.root} data-compact={compact}>
      <section className={s.section}>
        <header className={s.sectionHead}>
          <h2 className={s.sectionTitle}>Gameplay</h2>
          <p className={s.sectionLede}>When the engine passes priority for you, and when it stops to let you act.</p>
        </header>

        <Field label="Starting priority mode" hint={MODE_OPTIONS.find((m) => m.value === gameplay.priorityMode)?.hint}>
          <Segmented
            value={gameplay.priorityMode}
            options={MODE_OPTIONS}
            onChange={(priorityMode) => update('gameplay', { priorityMode })}
            label="Starting priority mode"
          />
        </Field>
        <p className={s.note}>Every game starts in this mode. The mode button next to Pass still switches it for the game you're in.</p>

        <div className={s.field}>
          <div className={s.fieldText}>
            <span className={s.label}>Always stop at</span>
            <span className={s.hint}>
              Extra stops on top of the automatic ones — the engine holds priority here even when you have nothing to do.
              Clicking a step in the in-game step strip changes the same list.
            </span>
          </div>
        </div>
        <table className={s.stopGrid}>
          <thead>
            <tr>
              <th scope="col">Step</th>
              <th scope="col">My turn</th>
              <th scope="col">Opponent's turn</th>
            </tr>
          </thead>
          <tbody>
            {STOPPABLE_STEPS.map((step) => {
              const locked = ALWAYS_STOP_MY_TURN.has(step)
              return (
                <tr key={step}>
                  <th scope="row">{StepDisplayNames[step]}</th>
                  <td>
                    <input
                      type="checkbox"
                      className={s.check}
                      checked={locked || gameplay.myTurnStops.includes(step)}
                      disabled={locked}
                      title={locked ? 'You always get priority in your own main phases' : undefined}
                      aria-label={`Stop at ${StepDisplayNames[step]} on my turn`}
                      onChange={() => toggleStop(step, true)}
                    />
                  </td>
                  <td>
                    <input
                      type="checkbox"
                      className={s.check}
                      checked={gameplay.opponentTurnStops.includes(step)}
                      aria-label={`Stop at ${StepDisplayNames[step]} on an opponent's turn`}
                      onChange={() => toggleStop(step, false)}
                    />
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {(gameplay.myTurnStops.length > 0 || gameplay.opponentTurnStops.length > 0) && (
          <button
            type="button"
            className={s.linkButton}
            onClick={() => update('gameplay', { myTurnStops: [], opponentTurnStops: [] })}
          >
            Clear all extra stops
          </button>
        )}

        <Toggle
          label="Auto-tap mana"
          hint="Pay costs automatically when you cast a spell or activate an ability. Off: pick the mana sources yourself."
          checked={gameplay.autoTap}
          onChange={(autoTap) => update('gameplay', { autoTap })}
        />
        <Toggle
          label="Follow the action"
          hint="In games with three or more players, move the camera to the board where something is happening."
          checked={gameplay.followAction}
          onChange={(followAction) => update('gameplay', { followAction })}
        />
      </section>

      <section className={s.section}>
        <header className={s.sectionHead}>
          <h2 className={s.sectionTitle}>Battlefield</h2>
          <p className={s.sectionLede}>
            Identical permanents — same name, same tapped state, counters, damage and so on — can collapse into a stack.
          </p>
        </header>

        <div className={s.stackRows}>
          {ROWS.map(({ key, label, hint }) => (
            <div key={key} className={s.stackRow}>
              <div className={s.fieldText}>
                <span className={s.label}>{label}</span>
                <span className={s.hint}>{hint}</span>
              </div>
              <label className={s.selectLabel}>
                <span>Stack from</span>
                <select
                  className={s.select}
                  value={battlefield[key].groupFrom}
                  onChange={(e) => setRule(key, { groupFrom: Number(e.target.value) })}
                >
                  <option value={0}>Never stack</option>
                  {range(2, STACK_LIMITS.groupFromMax).map((n) => (
                    <option key={n} value={n}>{n} identical</option>
                  ))}
                </select>
              </label>
              <label className={s.selectLabel}>
                <span>Stacks of</span>
                <select
                  className={s.select}
                  value={battlefield[key].stackSize}
                  disabled={battlefield[key].groupFrom === 0}
                  onChange={(e) => setRule(key, { stackSize: Number(e.target.value) })}
                >
                  <option value={0}>Any size</option>
                  {range(2, STACK_LIMITS.stackSizeMax).map((n) => (
                    <option key={n} value={n}>at most {n}</option>
                  ))}
                </select>
              </label>
              <StackPreview rule={battlefield[key]} layers={battlefield.maxVisibleLayers} />
            </div>
          ))}
        </div>

        <Field
          label="Visible stack layers"
          hint="How many cards a stack fans out before the rest hide behind its count badge. Fewer layers save room on crowded boards."
        >
          <select
            className={s.select}
            value={battlefield.maxVisibleLayers}
            onChange={(e) => update('battlefield', { maxVisibleLayers: Number(e.target.value) })}
            aria-label="Visible stack layers"
          >
            {range(STACK_LIMITS.layersMin, STACK_LIMITS.layersMax).map((n) => (
              <option key={n} value={n}>{n}</option>
            ))}
          </select>
        </Field>
      </section>

      <section className={s.section}>
        <header className={s.sectionHead}>
          <h2 className={s.sectionTitle}>Display</h2>
        </header>
        <Field label="Reduce motion" hint="Cut animations and transitions short. “Follow system” uses your device's accessibility setting.">
          <Segmented
            value={display.motion}
            options={MOTION_OPTIONS}
            onChange={(motion) => update('display', { motion })}
            label="Reduce motion"
          />
        </Field>
        <Toggle
          label="Card preview on hover"
          hint="Show the large card image when the mouse rests on a card. Touch screens keep their long-press preview."
          checked={display.hoverPreview}
          onChange={(hoverPreview) => update('display', { hoverPreview })}
        />
      </section>

      {signedIn && (
        <section className={s.section}>
          <header className={s.sectionHead}>
            <h2 className={s.sectionTitle}>Messages</h2>
          </header>
          <Toggle
            label="Message alerts away from home"
            hint="In a game, a draft or the deckbuilder, show an unread count on the chat button and preview new messages. When off, they wait quietly until you're back on the home screen."
            checked={prefs.messages.alertsAway}
            onChange={(alertsAway) => update('messages', { alertsAway })}
          />
        </section>
      )}

      <footer className={s.footer}>
        <span className={s.hint}>Changes save as you make them.</span>
        <button
          type="button"
          className={s.resetButton}
          disabled={sameAsDefaults(prefs)}
          onClick={reset}
        >
          Reset to defaults
        </button>
      </footer>
    </div>
  )
}

function sameAsDefaults(prefs: ReturnType<typeof usePreferences.getState>['prefs']): boolean {
  return JSON.stringify({ ...prefs, updatedAt: 0 }) === JSON.stringify(DEFAULT_PREFERENCES)
}

function range(from: number, to: number): number[] {
  return Array.from({ length: to - from + 1 }, (_, i) => from + i)
}

function Field({ label, hint, children }: { label: string; hint?: string | undefined; children: React.ReactNode }) {
  return (
    <div className={s.field}>
      <div className={s.fieldText}>
        <span className={s.label}>{label}</span>
        {hint && <span className={s.hint}>{hint}</span>}
      </div>
      <div className={s.control}>{children}</div>
    </div>
  )
}

function Toggle({ label, hint, checked, onChange }: { label: string; hint: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className={s.field}>
      <div className={s.fieldText}>
        <span className={s.label}>{label}</span>
        <span className={s.hint}>{hint}</span>
      </div>
      <div className={s.control}>
        <button
          type="button"
          role="switch"
          aria-checked={checked}
          aria-label={label}
          className={s.switch}
          data-on={checked}
          onClick={() => onChange(!checked)}
        >
          <span className={s.switchKnob} />
        </button>
      </div>
    </div>
  )
}

function Segmented<T extends string>({
  value,
  options,
  onChange,
  label,
}: {
  value: T
  options: readonly { value: T; label: string }[]
  onChange: (v: T) => void
  label: string
}) {
  return (
    <div className={s.segmented} role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={o.value === value}
          className={s.segment}
          data-active={o.value === value}
          onClick={() => onChange(o.value)}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}

/** Seven identical permanents laid out under the rule — the quickest way to read what it does. */
function StackPreview({ rule, layers }: { rule: StackingRule; layers: number }) {
  const N = 7
  const stacks: number[] = []
  if (rule.groupFrom === 0 || N < rule.groupFrom) {
    for (let i = 0; i < N; i++) stacks.push(1)
  } else if (rule.stackSize > 1 && N > rule.stackSize) {
    for (let left = N; left > 0; left -= rule.stackSize) stacks.push(Math.min(rule.stackSize, left))
  } else {
    stacks.push(N)
  }
  return (
    <div className={s.preview} aria-label={`Seven identical cards show as ${stacks.length} ${stacks.length === 1 ? 'stack' : 'stacks'}`}>
      {stacks.map((n, i) => {
        const drawn = Math.min(n, Math.max(1, layers))
        return (
          <span key={i} className={s.previewStack} style={{ width: 14 + (drawn - 1) * 4 }}>
            {Array.from({ length: drawn }, (_, j) => (
              <span key={j} className={s.previewCard} style={{ left: j * 4 }} />
            ))}
            {n > 1 && <span className={s.previewCount}>{n}</span>}
          </span>
        )
      })}
    </div>
  )
}
