/**
 * The landing screen's PLAY tier: three questions, asked one at a time.
 *
 * It replaces six preset cards that were drawn from four different questions — `vs AI` and
 * `vs Friend` answered *who fills the seats*, `Draft & Sealed` and `Variants` answered **Cards**,
 * `Multiplayer` answered **Table** and `Tournament` answered **Event** — which is why no two of them
 * read as alternatives to each other. The diagnosis is written up in
 * `backlog/menu-lobby-restructure-and-help.md` § 3a.
 *
 * What it is *not* is a second mode picker. Since the lobby unified (§ 4a) it shows all three axes on
 * both server kinds and can change any of them, so a grid in front of it was a competing taxonomy.
 * This is the *creation* path: it collects the whole selection while nothing exists yet, which is
 * what lets it create the right lobby kind first time instead of making the first change pay a `⇄`
 * recreate.
 *
 * Two rules keep it from feeling like a form:
 *
 * - **A step whose options collapse to one answer is skipped**, and the resolved value shows in the
 *   stepper marked `auto` — decided and visible, rather than silently assumed. `Just me → Bring a
 *   deck` therefore still reaches a lobby in two clicks, which is what the old `vs AI` card cost.
 * - **The stepper is the back button.** All three questions are always on screen, numbered, with the
 *   answer under each; clicking an answer reopens that step and re-validates the ones after it.
 *
 * **The draft lives in the URL, not in this component** (`wizardUrl.ts`). Phase 7 shipped the stepper
 * with the answers in `useState`, which left the landing screen with two back affordances that
 * disagreed: the stepper stepped back a question, the browser's Back left Argentum. Now each answer
 * is a path segment, so `history.back()` *is* "drop the last answer" and every step is linkable.
 * One user action = one `navigate`, which is the invariant that keeps Back honest.
 *
 * Everything selectable comes from `lobby/modeMatrix.ts`; this file only renders it.
 */
import { useEffect, useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { HelpTip } from '@/components/help/HelpTip'
import {
  cardsChoices,
  defaultCardsAxis,
  flowStages,
  rosterChoices,
  rosterLabel,
  shapeChoices,
  shapeLabel,
  type Choice,
  type Roster,
  type Selection,
  type ShapeId,
} from '../lobby/modeMatrix'
import { cardsLabel, type CardsAxis } from '../lobby/axes'
import {
  EMPTY_DRAFT as EMPTY,
  WIZARD_PREFIX,
  draftToPath,
  pathToDraft,
  type WizardDraft as Draft,
} from './wizardUrl'
import styles from './GameUI.module.css'

/** How many answers a wizard path spells out — the query string is a refinement, not an answer. */
function answerCount(path: string): number {
  return (path.split('?')[0] ?? '').split('/').filter(Boolean).length
}

/** The three questions. */
type AnsweredStep = 'roster' | 'cards' | 'shape'

/** The three steps, plus the state after the last one is answered. */
type StepId = AnsweredStep | 'done'

export function PlayWizard({
  aiEnabled,
  onLaunch,
}: {
  aiEnabled: boolean
  /**
   * Create the lobby this selection describes. The wizard never touches the store itself, and no
   * longer knows how a selection becomes messages either — `recipeFromSelection` does.
   */
  onLaunch: (selection: Selection) => void
}) {
  const navigate = useNavigate()
  const { pathname, search } = useLocation()
  const draft = useMemo(() => pathToDraft(pathname, aiEnabled), [pathname, aiEnabled])

  /** Answer a step: one history entry, so Back drops exactly this answer. */
  const setDraft = (next: Draft) => navigate(draftToPath(next))

  /**
   * Normalise the address bar onto the canonical path for what is selected — `/play/solo/bring-a-deck`
   * gains the shape that was auto-resolved for it, a saved `?seats=4` loses a query that no longer
   * answers anything. Replace, never push, so tidying a URL never becomes an extra Back to press.
   *
   * **It may only ever complete a path, never shorten one.** Shortening would mean acting on a
   * *reachability* verdict, and reachability depends on `aiEnabled`, which arrives with the connection
   * — so a truncating normalise races the socket and can delete a perfectly good shared link before
   * the server has said whether it works. When a link turns out to be unreachable the wizard shows the
   * step with the option disabled and its reason attached, and the next click rewrites the URL anyway.
   * Keeping the link and explaining beats silently discarding it.
   *
   * Scoped to paths the wizard owns: `/` carries query params belonging to other features
   * (`?spectate=`, `?token=`, `?decks=open`, `?profile=1`) that normalising there would strip.
   */
  const canonical = draftToPath(draft)
  const wizardOwnsUrl = pathname === WIZARD_PREFIX || pathname.startsWith(`${WIZARD_PREFIX}/`)
  useEffect(() => {
    if (!wizardOwnsUrl) return
    const current = `${pathname}${search}`
    if (current === canonical) return
    if (answerCount(canonical) < answerCount(current)) return
    navigate(canonical, { replace: true })
  }, [wizardOwnsUrl, canonical, pathname, search, navigate])

  // Step 3 is only a step when there is more than one shape to pick between. With one, `pickCards`
  // has already resolved it and the grid would be a question with a single answer.
  const shapeIsAQuestion =
    draft.roster !== null && draft.cards !== null &&
    shapeChoices(draft.roster, draft.cards).filter((c) => !c.disabledReason).length > 1

  const step: StepId =
    draft.roster === null ? 'roster'
      : draft.cards === null ? 'cards'
        : shapeIsAQuestion ? 'shape'
          : 'done'

  /**
   * Why the shapes you *didn't* get are unavailable, when step 3 was skipped.
   *
   * Skipping a one-answer step is right, but it hides the disabled tiles — and with them the reasons.
   * For a solo draft that is no loss ("of course a pod plays a bracket"); for a group picking
   * Commander it hides exactly the thing worth saying, which is that Commander has no multiplayer
   * table rather than some limit on how many people can share the pool.
   */
  const skippedShapeReasons: string[] =
    draft.roster !== null && draft.cards !== null && !shapeIsAQuestion
      ? [...new Set(
          shapeChoices(draft.roster, draft.cards)
            .map((c) => c.disabledReason)
            .filter((r): r is string => !!r),
        )]
      : []

  /** Answer step 1. Cards and shape are re-asked, since both depend on the roster. */
  const pickRoster = (roster: Roster) => setDraft({ ...EMPTY, roster })

  /**
   * Answer step 2. Skips step 3 when the roster and Cards value leave only one shape.
   *
   * The answer is a Cards *kind* at its default shape. Which sealed or draft shape it is stays a
   * lobby sub-option, in the same category as deck legality: it hangs off the Cards axis rather than
   * being one of the three questions, the lobby already owns a row for it (`LobbyAxes`), and asking
   * "Booster, Winston, Grid or Commander?" before someone has said who they are playing with is a
   * question only a drafter can have an opinion about. It also keeps one segment per answer in the
   * URL, which is what `wizardUrl` is built on.
   */
  const pickCards = (cards: CardsAxis) => {
    const roster = draft.roster
    if (roster === null) return
    const open = shapeChoices(roster, cards).filter((c) => !c.disabledReason)
    setDraft({ roster, cards, shape: open.length === 1 ? open[0]!.value : null })
  }

  const pickShape = (shape: ShapeId) => {
    const { roster, cards } = draft
    if (roster === null || cards === null) return
    setDraft({ roster, cards, shape })
  }

  /**
   * Reopen an answered step, dropping every answer that depended on it.
   *
   * Reopening the shape is only meaningful when it was a question — with one reachable shape,
   * clearing it would land on a screen with nothing to pick and no way forward, so the stepper
   * renders that answer as `auto` and doesn't offer this.
   */
  const reopen = (target: AnsweredStep) => {
    switch (target) {
      case 'roster': return setDraft(EMPTY)
      case 'cards': return setDraft({ ...EMPTY, roster: draft.roster })
      case 'shape': if (shapeIsAQuestion) setDraft({ ...draft, shape: null })
    }
  }

  const launch = (selection: Selection) => {
    // Hand the URL back before the lobby appears. A lobby is not a wizard step, and leaving
    // `/play/...` in the address bar would have it describe a screen that is no longer showing —
    // giving the in-`/` screens their own URLs is the rest of Phase 6.
    navigate('/', { replace: true })
    onLaunch(selection)
  }

  const complete: Selection | null =
    draft.roster !== null && draft.cards !== null && draft.shape !== null
      ? { roster: draft.roster, cards: draft.cards, shape: draft.shape }
      : null

  return (
    <>
      <WizardStepper
        step={step}
        draft={draft}
        shapeIsAQuestion={shapeIsAQuestion}
        skippedShapeReasons={skippedShapeReasons}
        onReopen={reopen}
      />

      {step === 'roster' && (
        <OptionGrid
          choices={rosterChoices(aiEnabled)}
          selected={null}
          testIdPrefix="wizard-roster"
          testId={(r) => r.toLowerCase()}
          onPick={pickRoster}
        />
      )}

      {step === 'cards' && draft.roster !== null && (
        <OptionGrid
          choices={cardsChoices(draft.roster)}
          selected={null}
          testIdPrefix="wizard-cards"
          testId={(k) => k.toLowerCase().replace(/_/g, '-')}
          // Sealed and Draft commit on their *kind* and open at the default shape. Which sealed or
          // draft shape it is stays a lobby sub-option, alongside deck legality — see the note above
          // `pickCards`.
          onPick={(kind) => pickCards(defaultCardsAxis(kind))}
        />
      )}

      {step === 'shape' && draft.roster !== null && draft.cards !== null && (
        <OptionGrid
          choices={shapeChoices(draft.roster, draft.cards)}
          selected={draft.shape}
          testIdPrefix="wizard-shape"
          testId={(s) => s.toLowerCase().replace(/_/g, '-')}
          onPick={pickShape}
        />
      )}

      {complete !== null && (
        <>
          {/* One block for everything that is about launching, rather than three rows loose under
              the tiles. The stages lead it — "Open boosters → Build a deck → Everyone plays everyone
              → Standings" is the only place that says how many steps this is before you commit. */}
          <div className={styles.wizardFooter}>
            <p className={styles.wizardFlow}>
              {flowStages(complete).map((stage, i) => (
                <span key={stage}>
                  {i > 0 && <span className={styles.wizardFlowArrow} aria-hidden> → </span>}
                  <span className={styles.wizardFlowStage}>{stage}</span>
                </span>
              ))}
            </p>
            <button
              type="button"
              className={styles.primaryButton}
              data-testid="wizard-create"
              onClick={() => launch(complete)}
            >
              {complete.roster === 'SOLO' ? 'Start playing' : 'Create lobby'} →
            </button>
          </div>
        </>
      )}
    </>
  )
}

/**
 * All three questions, numbered, with each answer under its own heading.
 *
 * The first version of this was a row of small chips floating to the right of the current step's
 * title, which read as decoration: nothing said they were previous *answers*, that they could be
 * changed, or what order they came in. This says all three — the numbers give the sequence, the
 * question word says what each one decided, and an answered step is a button with a pencil on it.
 *
 * A step that was skipped is shown as `auto` rather than as a button, because there is nothing to
 * pick: with one reachable shape, reopening it would land on a dead screen.
 */
function WizardStepper({
  step,
  draft,
  shapeIsAQuestion,
  skippedShapeReasons,
  onReopen,
}: {
  step: StepId
  draft: Draft
  shapeIsAQuestion: boolean
  /** Reasons the other shapes were unavailable, when step 3 was skipped. */
  skippedShapeReasons: string[]
  onReopen: (step: AnsweredStep) => void
}) {
  const slots: Array<{
    id: AnsweredStep
    word: string
    value: string | null
    /** Decided for you — there was only one possibility. */
    auto: boolean
  }> = [
    { id: 'roster', word: 'Who with', value: draft.roster && rosterLabel(draft.roster), auto: false },
    { id: 'cards', word: 'What with', value: draft.cards && cardsLabel(draft.cards), auto: false },
    {
      id: 'shape',
      word: 'How',
      value: draft.shape && shapeLabel(draft.shape),
      auto: draft.shape !== null && !shapeIsAQuestion,
    },
  ]

  const titles: Record<StepId, string> = {
    roster: 'Who are you playing with?',
    cards: 'What are you playing with?',
    shape: 'How do you play it?',
    done: 'Ready when you are.',
  }

  return (
    <div className={styles.wizardStepper}>
      <ol className={styles.wizardStepperTrack}>
        {slots.map((slot, i) => {
          const isCurrent = step === slot.id
          // An answer shows as soon as it exists, including on the step you are standing on — that
          // step's tiles are already the way to change it, so it needs no pencil of its own.
          const answered = slot.value !== null && !isCurrent
          const currentValue = slot.value !== null && isCurrent
          return (
            <li
              key={slot.id}
              className={[
                styles.wizardStepSlot,
                isCurrent ? styles.wizardStepSlotCurrent : '',
                answered ? styles.wizardStepSlotDone : '',
              ].filter(Boolean).join(' ')}
            >
              <span className={styles.wizardStepNum} aria-hidden>{i + 1}</span>
              <span className={styles.wizardStepBody}>
                <span className={styles.wizardStepWord}>{slot.word}</span>
                {answered ? (
                  slot.auto ? (
                    <span
                      className={styles.wizardStepAuto}
                      title={
                        skippedShapeReasons.length > 0
                          ? `The only option for what you picked. ${skippedShapeReasons.join(' ')}`
                          : 'The only option for what you picked.'
                      }
                    >
                      {slot.value}
                      <span className={styles.wizardStepAutoMark}> auto</span>
                    </span>
                  ) : (
                    <button
                      type="button"
                      className={styles.wizardStepChange}
                      onClick={() => onReopen(slot.id)}
                      data-testid={`wizard-back-${slot.id}`}
                      title={`Change this — currently ${slot.value}`}
                    >
                      {slot.value}
                      <span className={styles.wizardStepPencil} aria-hidden>✎</span>
                    </button>
                  )
                ) : currentValue ? (
                  <span className={styles.wizardStepCurrentValue}>{slot.value}</span>
                ) : (
                  <span className={styles.wizardStepPending}>
                    {isCurrent ? 'choosing…' : '—'}
                  </span>
                )}
              </span>
            </li>
          )
        })}
      </ol>
      <p className={styles.wizardStepTitle}>{titles[step]}</p>
      {/* Stated, not hidden behind a tooltip on a tile that isn't rendered any more. */}
      {step === 'done' && skippedShapeReasons.map((reason) => (
        <p key={reason} className={styles.wizardAutoNote}>{reason}</p>
      ))}
    </div>
  )
}

/**
 * One step's options.
 *
 * Disabled tiles stay visible with their reason on hover, which is the same rule the lobby's axis
 * rows follow: an option you can see and can't use teaches the shape of the system, while an option
 * that isn't rendered just looks like nobody thought of it. Every one of them is a Phase 5 gap.
 */
function OptionGrid<V extends string>({
  choices,
  selected,
  testIdPrefix,
  testId,
  onPick,
}: {
  choices: Choice<V>[]
  selected: V | null
  testIdPrefix: string
  testId: (value: V) => string
  onPick: (value: V) => void
}) {
  return (
    <div className={`${styles.presetGrid} ${choices.length === 4 ? styles.presetGridPairs : ''}`}>
      {choices.map((choice) => (
        // A wrapper div rather than one big button: the HelpTip is itself a button, and nesting
        // interactive elements is invalid HTML (and unreachable by keyboard).
        <div
          key={choice.value}
          className={`${styles.presetCard} ${choice.value === selected ? styles.presetCardSelected : ''}`}
        >
          <span className={styles.presetCardHelp}>
            <HelpTip topicId={choice.topicId} label={`What is ${choice.label}?`} size="sm" />
          </span>
          <button
            type="button"
            disabled={!!choice.disabledReason}
            onClick={() => onPick(choice.value)}
            data-testid={`${testIdPrefix}-${testId(choice.value)}`}
            className={styles.presetCardButton}
            title={choice.disabledReason ?? ''}
          >
            <span className={styles.presetCardTitle}>{choice.label}</span>
            <span className={styles.presetCardTagline}>
              {choice.disabledReason ?? choice.caption}
            </span>
            {choice.badge && !choice.disabledReason && (
              <span
                className={`${styles.presetCardBadge} ${
                  choice.badge.weight === 'EVENT'
                    ? styles.presetCardBadge_event
                    : styles.presetCardBadge_quick
                }`}
              >
                {choice.badge.text}
              </span>
            )}
          </button>
        </div>
      ))}
    </div>
  )
}

