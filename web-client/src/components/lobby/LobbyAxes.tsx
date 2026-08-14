/**
 * The four axes, as the lobby's primary controls.
 *
 * Every lobby shows every value of each, whichever server implementation is backing it. What differs
 * is what a value *costs* — `axisChoices.ts` decides that, and this file only renders the answer:
 *
 * - selectable → a normal button
 * - selectable but only on the other lobby kind → a button marked `⇄`, which asks for confirmation
 *   before tearing this lobby down (plan § 4b v1)
 * - not implemented anywhere yet → **disabled with the reason attached**, not hidden
 *
 * Sub-options hang off their controlling axis: sealed and draft shape belong to **Cards**, while
 * deck legality belongs to **Rules** because that choice determines which formats are coherent.
 * This keeps "Format" from meaning two different things and remains the seam the settings groups
 * are cut along (`settingsGroups.ts`).
 *
 * Each axis therefore exports two pieces rather than one row: a **strip** of buttons in the group
 * header and an always-visible **body** of sub-options and captions. Reading order is unchanged: what deck → under what rules → at
 * what table → over how many games.
 */
import { useEffect } from 'react'
import {
  cardsLabel,
  cardsSeatCap,
  isCommanderLimited,
  legalityOptionsForRules,
  rulesTableBlock,
  type CardsAxis,
  type CardsKind,
  type EventAxis,
  type RulesAxis,
  type TableAxis,
} from './axes'
import {
  RECREATE_NOTE,
  cardsChoices,
  eventChoices,
  recreateTargetLabel,
  rulesChoices,
  tableChoices,
  type AxisChoice,
  type RecreateSpec,
} from './axisChoices'
import type { UnifiedLobbyView } from './lobbyViewModel'
import type { LobbyCommands } from './useLobbyCommands'
import styles from '../ui/GameUI.module.css'

const CARDS_CAPTIONS: Record<CardsKind, string> = {
  BRING_A_DECK: 'Everyone plays a deck they already built — saved, pasted or imported.',
  RANDOM:
    'The server rolls you a pool, so there is nothing to prepare. This one is per player: your opponent can still bring a deck of their own.',
  MOMIR:
    'No deckbuilding — everyone runs 60 basics. Discard a card and pay {X} to flip a random creature with mana value X.',
  SEALED: 'Open boosters and build a deck from what you get.',
  DRAFT: 'Pass packs around and pick one card at a time, then build from your picks.',
}

const RULES_CAPTIONS: Record<RulesAxis, string> = {
  STANDARD: 'The ordinary rules for the table you picked — 20 life at 1v1, no command zone.',
  COMMANDER:
    'Everyone designates a commander, which starts in the command zone and can be recast from it (CR 903). 40 life at a pod, and 21 damage from a single commander knocks you out. Independent of where the cards come from — a brought deck, a sealed pool or any draft can be played this way.',
}

const TABLE_CAPTIONS: Record<TableAxis, string> = {
  ONE_V_ONE: 'Two players per game. In a bracket, everyone plays everyone; most match wins takes it.',
  FREE_FOR_ALL: 'One game, everyone at the same table (2-6 players). Last player standing wins.',
  TWO_HEADED_GIANT:
    'Four players in two teams of two. Each team shares one 30-life total, takes turns together, and attacks and blocks as one. Last team standing wins.',
  TEAM_VS_TEAM:
    'An even pod (4/6/8) split into two teams — 2v2, 3v3, or 4v4. Each player keeps their own 20 life and their own turn; players are knocked out one at a time. The last team with anyone standing wins.',
}

interface AxisProps {
  view: UnifiedLobbyView
  commands: LobbyCommands
  /** A value that lives on the other lobby kind — confirmed by the screen before it happens. */
  onRecreate: (spec: RecreateSpec) => void
}

/* ── Strips: always visible, in the group headers ─────────────────────────── */

export function CardsAxisStrip({ view, commands, onRecreate }: AxisProps) {
  return <AxisButtons choices={cardsChoices(view)} onPick={commands.setCards} onRecreate={onRecreate} />
}

export function RulesAxisStrip({ view, commands, onRecreate }: AxisProps) {
  return <AxisButtons choices={rulesChoices(view)} onPick={commands.setRules} onRecreate={onRecreate} />
}

export function TableAxisStrip({ view, commands, onRecreate }: AxisProps) {
  return <AxisButtons choices={tableChoices(view)} onPick={commands.setTable} onRecreate={onRecreate} />
}

export function EventAxisStrip({ view, onRecreate }: AxisProps) {
  return (
    <AxisButtons
      choices={eventChoices(view)}
      // Event has no directly-settable second value on either kind: server-side
      // `gameMode = TOURNAMENT` *is* the bracket. Every cross-value pick recreates.
      onPick={() => {}}
      onRecreate={onRecreate}
    />
  )
}

/* ── Bodies: always-visible sub-options and captions ──────────────────── */

export function CardsAxisBody({ view, commands }: Omit<AxisProps, 'onRecreate'>) {
  const cards = view.axes.cards

  return (
    <>
      <div className={styles.settingsRow}>
        <span className={styles.settingsLabel} />
        <div className={styles.variantCaption}>{CARDS_CAPTIONS[cards.kind]}</div>
      </div>

      {/* Cards → Sealed: which sealed shape. */}
      {cards.kind === 'SEALED' && (
        <div className={`${styles.settingsRow} ${styles.settingsRowSub}`}>
          <span className={styles.settingsLabel}>Sealed shape</span>
          <div className={styles.variantGroup}>
            <div className={styles.settingsButtons}>
              <ShapeButton
                label="Standard"
                active={cards.shape === 'STANDARD'}
                onClick={() => commands.setCardsShape('SEALED')}
              />
              <ShapeButton
                label="Commander"
                active={cards.shape === 'COMMANDER'}
                blocked={shapeBlock(view, { kind: 'SEALED', shape: 'COMMANDER' })}
                onClick={() => commands.setCardsShape('COMMANDER_SEALED')}
              />
            </div>
            <div className={styles.variantCaption}>
              {cards.shape === 'COMMANDER'
                ? 'Open Commander-shaped packs and build a 60-card deck around a commander from your pool. Up to 8 players, playing a 1v1 bracket or one pod at 40 life.'
                : 'Open 6 boosters and build a 40-card deck.'}
            </div>
          </div>
        </div>
      )}

      {/* Cards → Draft: which of the four draft shapes. */}
      {cards.kind === 'DRAFT' && (
        <div className={`${styles.settingsRow} ${styles.settingsRowSub}`}>
          <span className={styles.settingsLabel}>Draft shape</span>
          <div className={styles.variantGroup}>
            <div className={styles.settingsButtons}>
              <ShapeButton
                label="Booster"
                draft
                active={cards.shape === 'BOOSTER'}
                blocked={shapeBlock(view, { kind: 'DRAFT', shape: 'BOOSTER' })}
                onClick={() => commands.setCardsShape('DRAFT')}
              />
              <ShapeButton
                label="Winston"
                draft
                active={cards.shape === 'WINSTON'}
                blocked={shapeBlock(view, { kind: 'DRAFT', shape: 'WINSTON' })}
                onClick={() => commands.setCardsShape('WINSTON_DRAFT')}
              />
              <ShapeButton
                label="Grid"
                draft
                active={cards.shape === 'GRID'}
                blocked={shapeBlock(view, { kind: 'DRAFT', shape: 'GRID' })}
                onClick={() => commands.setCardsShape('GRID_DRAFT')}
              />
              <ShapeButton
                label="Commander"
                draft
                active={cards.shape === 'COMMANDER'}
                blocked={shapeBlock(view, { kind: 'DRAFT', shape: 'COMMANDER' })}
                onClick={() => commands.setCardsShape('COMMANDER_DRAFT')}
              />
            </div>
            <div className={styles.variantCaption}>
              {cards.shape === 'COMMANDER'
                ? 'Commander-shaped 20-card packs; pick a commander from your pool. Up to 8 drafters, playing a 1v1 bracket or one pod at 40 life.'
                : cards.shape === 'WINSTON' ? 'Pick from 3 face-down piles. 2 players.'
                : cards.shape === 'GRID' ? 'Pick a row or column from a 3×3 grid. 2-4 players.'
                : 'Pass packs around the table. 3-8 players.'}
            </div>
          </div>
        </div>
      )}
    </>
  )
}

export function RulesAxisBody({ view, commands }: { view: UnifiedLobbyView; commands: LobbyCommands }) {
  // A lobby can be sitting on a Rules × Table contradiction (the server defaults Rules to Commander
  // when the host picks commander deck legality, whatever the table). Say so here rather than only
  // on a disabled Start button.
  const rulesConflict = rulesTableBlock(view.axes.rules, view.axes.table)
  const cards = view.axes.cards
  const legalityOptions = legalityOptionsForRules(view.axes.rules, view.axes.table)

  // Rules own deck construction. If the host changes Rules, immediately replace a now-incoherent
  // legality; Commander rules always carry one of the commander-aware formats.
  useEffect(() => {
    if (cards.kind !== 'BRING_A_DECK') return
    if (legalityOptions.length === 0) return
    if (cards.legality && legalityOptions.some((option) => option.value === cards.legality)) return
    commands.setLegality(view.axes.rules === 'COMMANDER' ? 'COMMANDER' : 'STANDARD')
  }, [cards, commands, legalityOptions, view.axes.rules])

  return (
    <>
      <div className={styles.settingsRow}>
        <span className={styles.settingsLabel} />
        <div className={styles.variantCaption}>
          {rulesConflict ?? RULES_CAPTIONS[view.axes.rules]}
        </div>
      </div>
      {cards.kind === 'BRING_A_DECK' && (
        <div className={`${styles.settingsRow} ${styles.settingsRowSub}`}>
          <span className={styles.settingsLabel}>Deck legality</span>
          <select
            value={cards.legality ?? ''}
            onChange={(e) => commands.setLegality(e.target.value as never)}
            className={styles.settingsSelect}
            title="Submitted decks must be legal under the selected rules and constructed format."
          >
            {legalityOptions.map((format) => (
              <option key={format.value} value={format.value}>{format.label}</option>
            ))}
          </select>
        </div>
      )}
    </>
  )
}

export function TableAxisBody({ view }: { view: UnifiedLobbyView }) {
  return (
    <div className={styles.settingsRow}>
      <span className={styles.settingsLabel} />
      <div className={styles.variantCaption}>{TABLE_CAPTIONS[view.axes.table]}</div>
    </div>
  )
}

export function EventAxisBody({ view }: { view: UnifiedLobbyView }) {
  const caption = eventCaption(view)
  if (!caption) return null
  return (
    <div className={styles.settingsRow}>
      <span className={styles.settingsLabel} />
      <div className={styles.variantCaption}>{caption}</div>
    </div>
  )
}

/** The caption under Event: what the value you *didn't* pick would take. Empty when there is
 *  nothing to say, which is how `LobbyScreen` knows the group has no body. */
export function eventCaption(view: UnifiedLobbyView): string {
  const other = eventChoices(view).find((c) => !c.selected)
  if (!other) return ''
  switch (other.availability.kind) {
    case 'BLOCKED':
      return other.availability.reason
    case 'RECREATE':
      return `“${other.label}” runs on a different lobby — picking it starts a fresh one.`
    case 'DIRECT':
      return ''
  }
}

function AxisButtons<V extends CardsKind | RulesAxis | TableAxis | EventAxis>({
  choices,
  onPick,
  onRecreate,
}: {
  choices: AxisChoice<V>[]
  onPick: (value: V) => void
  onRecreate: (spec: RecreateSpec) => void
}) {
  return (
    <div className={styles.settingsButtons}>
      {choices.map((choice) => {
        const a = choice.availability
        const recreates = a.kind === 'RECREATE'
        return (
          <button
            key={choice.value}
            type="button"
            disabled={a.kind === 'BLOCKED'}
            aria-pressed={choice.selected}
            onClick={() => {
              if (a.kind === 'RECREATE') onRecreate(a.spec)
              else if (a.kind === 'DIRECT' && !choice.selected) onPick(choice.value)
            }}
            className={[
              styles.settingsButton,
              choice.selected ? styles.settingsButtonActive : '',
              recreates ? styles.settingsButtonRecreate : '',
            ].filter(Boolean).join(' ')}
            title={
              a.kind === 'BLOCKED' ? a.reason
                : a.kind === 'RECREATE'
                  ? `Starts a new lobby: ${recreateTargetLabel(a.spec)}. ${RECREATE_NOTE[a.spec.to]} Your invite code will change.`
                  : ''
            }
            data-testid={`axis-choice-${choice.value.toLowerCase().replace(/_/g, '-')}`}
          >
            {choice.label}
            {recreates && <span className={styles.settingsButtonRecreateMark} aria-hidden> ⇄</span>}
          </button>
        )
      })}
    </div>
  )
}

/**
 * Why a Cards sub-shape can't be picked here, or null.
 *
 * Two reasons, both facts shared with the landing wizard rather than numbers written at the call
 * site: the shape seats fewer players than this lobby is holding ({@link cardsSeatCap}), or it would
 * default the Rules axis to Commander at a table that can't have it ({@link rulesTableBlock} — the
 * one statement of that rule, so this row can never offer what the Rules row refuses).
 */
function shapeBlock(view: UnifiedLobbyView, cards: CardsAxis): string | null {
  const cap = cardsSeatCap(cards)
  if (view.players.length > cap) {
    return `${cardsLabel(cards)} seats at most ${cap} — this lobby has ${view.players.length}`
  }
  // Picking a Commander pack shape defaults Rules to Commander, so it inherits both of the rules'
  // own blocks; a non-commander shape asks them of the rules the lobby already has.
  const wouldRun: RulesAxis = isCommanderLimited(cards) ? 'COMMANDER' : view.axes.rules
  return rulesTableBlock(wouldRun, view.axes.table)
}

function ShapeButton({
  label,
  active,
  blocked = null,
  draft = false,
  onClick,
}: {
  label: string
  active: boolean
  blocked?: string | null
  draft?: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      disabled={blocked !== null}
      aria-pressed={active}
      onClick={() => { if (blocked === null && !active) onClick() }}
      className={[
        styles.settingsButton,
        active ? styles.settingsButtonActive : '',
        active && draft ? styles.settingsButtonDraft : '',
      ].filter(Boolean).join(' ')}
      title={blocked ?? ''}
    >
      {label}
    </button>
  )
}
