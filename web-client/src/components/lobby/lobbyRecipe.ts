/**
 * "A game I want to play", written down.
 *
 * Until this existed there was no such object. The wizard produced a three-field {@link Selection},
 * `resolveLaunch` widened it to a five-field `LaunchSpec`, and the home screen's `launch` threw even
 * that away — `createTournamentLobby(['ECL'], format, 6, maxPlayers, 45, false, …)`, with the sets,
 * the pack count, the timer, the visibility and the deck all hardcoded or dropped. The other twenty
 * answers only ever existed as server-owned `LobbySettings` that die with the lobby.
 *
 * Three symptoms, one cause. Repeat play was slow because there was nothing to repeat; the lobby was
 * a wall because it was the only place those answers could live; and there was no rematch because the
 * thing to replay had never been recorded. A recipe is what a saved setup, a `Play again` chip and a
 * rematch all turn out to be.
 *
 * This module is pure — no store reads, no side effects, the same posture as `lobbyViewModel.ts`.
 * {@link useApplyRecipe} is the half that writes.
 *
 * ## What a recipe deliberately does *not* carry
 *
 * - **`maxPlayers`.** {@link seatCap} derives it, and `TournamentLobbySettings` documents why the
 *   Seats row is a cap rather than a prediction: `startBlockReason` counts the players actually
 *   present, so a lobby opens as wide as its shape allows and the host starts when everyone has
 *   arrived. Storing it would reintroduce the number to predict and then correct.
 * - **`teamAssignments`.** Keyed by `playerId`, which means nothing in a new lobby with different
 *   people. `randomTeams` is the half that survives.
 * - **A decklist.** {@link RecipeDeck} stores a *name*. `useUnifiedDecks` merges cloud and local
 *   decks on `name.toLowerCase()`, so a name is the portable key where `cloud:7` or a local uuid is
 *   not — and storing the cards by value would fossilise the deck: edit it, and the setup keeps
 *   playing last month's list. A name that no longer resolves degrades to "the picker opens on Saved
 *   and says so", which is the same *keep the last answer that still holds* rule `pathToDraft` and
 *   the wizard's stored selection already follow.
 * - **An exact AI decklist.** `AiDeckSpecView` is a summary — the list behind a `deck` choice never
 *   rides the lobby broadcast — so a captured recipe can only carry `auto` or `sets`. A host who
 *   handed the AI a specific list gets `auto` back, and {@link recipeFromLobby} says so in its notes
 *   rather than pretending otherwise.
 */
import type {
  AttackMode,
  AvailableSet,
  CommanderPreset,
  DeckFormat,
  GameRules,
  QuickGameLobbyStateMessage,
} from '@/types'
import type { LobbyState } from '@/store/slices/types'
import { CARDS_KINDS, type CardsAxis, type CardsKind } from './axes'
import {
  ROSTERS,
  defaultSoloAiSeats,
  SHAPE_IDS,
  cardsChoices,
  seatCap,
  selectionSummary,
  shapeChoices,
  shapeFromAxes,
  type Roster,
  type Selection,
} from './modeMatrix'
import type { UnifiedLobbyView } from './lobbyViewModel'

/**
 * Bumped when the stored shape changes incompatibly. Unknown versions are **dropped on read, never
 * migrated** — the same posture `pathToDraft` takes towards a URL it cannot parse. A setup is a
 * convenience; silently replaying a misread one would be worse than losing it.
 */
export const RECIPE_VERSION = 1

/** The host-settable lobby fields worth reproducing. Sparse: absent means "take the default". */
export interface RecipeSettings {
  readonly setCodes?: readonly string[]
  readonly boosterCount?: number
  readonly boosterDistribution?: Readonly<Record<string, number>>
  readonly chaosBoosters?: boolean
  readonly includedSetProducts?: Readonly<Record<string, readonly string[]>>
  readonly bannedCardNames?: readonly string[]
  readonly pickTimeSeconds?: number
  readonly picksPerRound?: number
  readonly gamesPerMatch?: number
  readonly deckFormat?: DeckFormat | null
  readonly rules?: GameRules
  readonly deckSizeMin?: number
  readonly allowDuplicates?: boolean
  readonly commanderPreset?: CommanderPreset
  readonly attackMode?: AttackMode
  readonly randomTeams?: boolean
  readonly aiAssistEnabled?: boolean
  readonly isPublic?: boolean
  readonly ranked?: boolean
  readonly cube?: RecipeCube
}

/**
 * A cube travels by value, unlike a deck.
 *
 * The asymmetry is the server's: a deck reaches a lobby as a submission the player makes, but a cube
 * reaches one as a full card-name list over `UpdateLobbySettings.cubeCards` — `V12__cubes.sql` says
 * so explicitly, which is what keeps guests and unsaved cubes working. There is no id to reference.
 */
export interface RecipeCube {
  readonly name: string
  readonly cards: readonly string[]
  readonly basicLandSetCode: string
  readonly packSize: number
  readonly poolPlay: boolean
}

/** What this seat brings. A reference, never a card list — see the module comment. */
export type RecipeDeck =
  /** Momir, or a pool built inside the event. Nothing to pick. */
  | { readonly kind: 'NONE' }
  /** The deck picker's Random tab, whose empty list is the server's "roll me one" signal. */
  | { readonly kind: 'RANDOM' }
  | { readonly kind: 'SAVED'; readonly name: string }

/** What the AI opponent plays. Quick vs-AI lobbies only; mirrors the server's `AiDeckSpec`. */
export type RecipeAiDeck =
  | { readonly kind: 'AUTO' }
  | { readonly kind: 'SETS'; readonly setCodes: readonly string[] }

export interface LobbyRecipe {
  readonly v: typeof RECIPE_VERSION
  /** The three wizard answers — the recipe's identity. Everything else refines them. */
  readonly selection: Selection
  readonly settings: RecipeSettings
  readonly deck: RecipeDeck
  /** AI seats to fill after the lobby exists. Added last; see {@link useApplyRecipe}. */
  readonly aiSeats: number
  readonly aiDeck?: RecipeAiDeck
  /**
   * Ready up / start as soon as the lobby is configured.
   *
   * Only ever honoured when the lobby has nobody to invite — a lobby with an invite code exists so
   * that people can join it, and auto-starting one would slam the door on them. `LobbyScreen`
   * enforces that; this flag only expresses the intent.
   */
  readonly autoStart: boolean
}

/* ── From the wizard ────────────────────────────────────────────────────── */

/**
 * The recipe a fresh wizard selection describes: the three answers and nothing else.
 *
 * Every settings field is absent, which means the lobby opens on the server's defaults for that
 * shape — no sets, six packs, 45 seconds. That is the honest starting point, and notably *not* what
 * the old launch path did: it sent `['ECL']`, so every draft lobby created from the wizard opened
 * on a set nobody had chosen.
 */
export function recipeFromSelection(selection: Selection): LobbyRecipe {
  const { roster, cards, shape } = selection
  return {
    v: RECIPE_VERSION,
    selection,
    settings: {},
    deck: deckForCards(cards),
    aiSeats: roster === 'SOLO' && lobbyNeedsAiSeats(selection)
      ? defaultSoloAiSeats(cards, shape)
      : 0,
    // A solo game has nobody to wait for, but a brand-new selection has no deck yet either, so the
    // lobby is where you pick one. Auto-start is earned by a *captured* recipe, which knows the deck.
    autoStart: false,
  }
}

/** Momir and a rolled pool need no deck; sealed and draft build one inside the event. */
function deckForCards(cards: CardsAxis): RecipeDeck {
  switch (cards.kind) {
    case 'MOMIR': return { kind: 'NONE' }
    case 'RANDOM': return { kind: 'RANDOM' }
    case 'SEALED':
    case 'DRAFT': return { kind: 'NONE' }
    case 'BRING_A_DECK': return { kind: 'NONE' }
  }
}

/** Only a tournament-backed solo lobby fills seats with AI; a quick vs-AI lobby gets its one seat
 *  from the create message's `vsAi` flag instead. */
function lobbyNeedsAiSeats(selection: Selection): boolean {
  return !(selection.shape === 'ONE_GAME' || selection.cards.kind === 'RANDOM' ||
    selection.cards.kind === 'MOMIR')
}

/* ── From a live lobby ──────────────────────────────────────────────────── */

/**
 * Capture the lobby as it stands. This is the moment the settings are final and the deck is chosen,
 * which is why the caller is the primary action rather than the wizard (which knows three fields) or
 * the server (which doesn't know your deck's client-side identity).
 *
 * `notes` is not decoration: two things genuinely cannot be captured — an exact AI decklist, whose
 * list never rides the lobby broadcast, and a cube whose card names the client doesn't hold. Saying
 * so at capture time is better than a setup that quietly plays something else later.
 */
export function recipeFromLobby(
  view: UnifiedLobbyView,
  lobbyState: LobbyState | null,
  quick: QuickGameLobbyStateMessage | null,
  deck: RecipeDeck,
  /** The cube by value, when the host has one loaded locally. The lobby broadcast carries only its
   *  name and card count, so this has to be handed in. */
  cube?: RecipeCube,
): { recipe: LobbyRecipe; notes: string[] } {
  const notes: string[] = []
  const others = view.players.filter((p) => !p.isYou)
  const allOthersAreAi = others.length > 0 && others.every((p) => p.isAi)
  const roster: Roster = allOthersAreAi || (quick?.vsAi ?? false)
    ? 'SOLO'
    : view.maxPlayers <= 2 ? 'FRIEND' : 'GROUP'
  const selection: Selection = {
    roster,
    cards: view.axes.cards,
    shape: shapeFromAxes(view.axes.table, view.axes.event),
  }

  const s = lobbyState?.settings
  const settings: RecipeSettings = s
    ? {
        setCodes: [...s.setCodes],
        boosterCount: s.boosterCount,
        boosterDistribution: { ...s.boosterDistribution },
        chaosBoosters: s.chaosBoosters,
        includedSetProducts: { ...s.includedSetProducts },
        bannedCardNames: [...s.bannedCardNames],
        pickTimeSeconds: s.pickTimeSeconds,
        picksPerRound: s.picksPerRound,
        gamesPerMatch: s.gamesPerMatch,
        deckFormat: s.deckFormat ?? null,
        rules: view.axes.rules,
        deckSizeMin: s.deckSizeMin,
        allowDuplicates: s.allowDuplicates,
        commanderPreset: s.commanderPreset,
        attackMode: s.attackMode,
        randomTeams: s.randomTeams,
        aiAssistEnabled: s.aiAssistEnabled,
        isPublic: s.isPublic,
        ranked: s.ranked ?? false,
        ...(cube ? { cube } : {}),
      }
    : {
        deckFormat: quick?.format ?? null,
        rules: view.axes.rules,
        isPublic: view.isPublic,
        ranked: view.ranked.on,
      }

  if (s?.cubeName && !cube) {
    notes.push(`The cube “${s.cubeName}” isn’t saved on this device, so the setup can’t rebuild it.`)
  }

  const aiDeck = captureAiDeck(quick)
  if (quick?.aiDeck?.kind === 'deck') {
    notes.push('The AI’s exact decklist can’t be saved — it will build its own deck instead.')
  }

  return {
    recipe: {
      v: RECIPE_VERSION,
      selection,
      settings,
      deck,
      aiSeats: view.kind === 'TOURNAMENT' ? view.players.filter((p) => p.isAi).length : 0,
      ...(aiDeck ? { aiDeck } : {}),
      // Earned, not assumed: a lobby nobody can join has nothing left to wait for once the deck is
      // chosen, so replaying it should go straight to the game.
      autoStart: !view.invitable,
    },
    notes,
  }
}

function captureAiDeck(quick: QuickGameLobbyStateMessage | null): RecipeAiDeck | null {
  const spec = quick?.aiDeck
  if (!spec) return null
  if (spec.kind === 'sets' && spec.setCodes && spec.setCodes.length > 0) {
    return { kind: 'SETS', setCodes: [...spec.setCodes] }
  }
  return { kind: 'AUTO' }
}

/* ── Reading one back ───────────────────────────────────────────────────── */

/**
 * Re-validate a stored recipe against the server this browser is talking to *now*.
 *
 * Not politeness. Two of these checks are load-bearing:
 *
 * - **Reachability.** A stored selection may predate a server whose AI is switched off, or a build
 *   where the combination changed shape. Same re-check the wizard's own stored selection did.
 * - **Set codes.** `LobbyHandler.handleUpdateLobbySettings` **`return`s** on an unknown set code —
 *   which silently discards every other field in the same message. A recipe naming a set this server
 *   doesn't have would therefore lose its packs, its timer and its ban list too, with no error. So
 *   unknown codes are dropped here and reported, rather than sent and swallowed.
 *
 * Returns null only when the recipe is unusable; anything salvageable comes back trimmed, with a
 * note per thing that was dropped.
 */
export function validateRecipe(
  raw: unknown,
  ctx: { aiEnabled: boolean; availableSets: readonly AvailableSet[] },
): { recipe: LobbyRecipe; notes: string[] } | null {
  if (typeof raw !== 'object' || raw === null) return null
  const candidate = raw as Partial<LobbyRecipe>
  if (candidate.v !== RECIPE_VERSION) return null

  const selection = validSelection(candidate.selection, ctx.aiEnabled)
  if (selection === null) return null

  const notes: string[] = []
  const settings = trimSettings(candidate.settings, ctx.availableSets, notes)
  const deck = validDeck(candidate.deck)
  const aiDeck = validAiDeck(candidate.aiDeck)

  return {
    recipe: {
      v: RECIPE_VERSION,
      selection,
      settings,
      deck,
      aiSeats: clampInt(candidate.aiSeats, 0, 7, 0),
      ...(aiDeck ? { aiDeck } : {}),
      autoStart: candidate.autoStart === true,
    },
    notes,
  }
}

function validSelection(raw: unknown, aiEnabled: boolean): Selection | null {
  if (typeof raw !== 'object' || raw === null) return null
  const { roster, cards, shape } = raw as Partial<Selection>
  if (!roster || !ROSTERS.includes(roster)) return null
  if (!shape || !SHAPE_IDS.includes(shape)) return null
  if (!isCardsAxis(cards)) return null
  if (roster === 'SOLO' && !aiEnabled) return null

  // The same two reachability questions the wizard asks, asked of a stored answer.
  const cardsOk = cardsChoices(roster).some((c) => c.value === cards.kind && !c.disabledReason)
  const shapeOk = shapeChoices(roster, cards).some((c) => c.value === shape && !c.disabledReason)
  if (!cardsOk || !shapeOk) return null

  return { roster, cards, shape }
}

/** Structural check on the Cards union, including its sub-shape — a hand-edited blob can name a
 *  draft shape that doesn't exist, and `cardsSeatCap` would fall off the end of its switch. */
function isCardsAxis(raw: unknown): raw is CardsAxis {
  if (typeof raw !== 'object' || raw === null) return false
  const { kind } = raw as { kind?: CardsKind }
  if (!kind || !CARDS_KINDS.includes(kind)) return false
  const shape = (raw as { shape?: string }).shape
  switch (kind) {
    case 'SEALED': return shape === 'STANDARD' || shape === 'COMMANDER'
    case 'DRAFT':
      return shape === 'BOOSTER' || shape === 'WINSTON' || shape === 'GRID' || shape === 'COMMANDER'
    default: return true
  }
}

function validDeck(raw: unknown): RecipeDeck {
  if (typeof raw !== 'object' || raw === null) return { kind: 'NONE' }
  const { kind, name } = raw as { kind?: string; name?: unknown }
  if (kind === 'RANDOM') return { kind: 'RANDOM' }
  if (kind === 'SAVED' && typeof name === 'string' && name.trim() !== '') {
    return { kind: 'SAVED', name }
  }
  return { kind: 'NONE' }
}

function validAiDeck(raw: unknown): RecipeAiDeck | null {
  if (typeof raw !== 'object' || raw === null) return null
  const { kind, setCodes } = raw as { kind?: string; setCodes?: unknown }
  if (kind === 'SETS' && Array.isArray(setCodes)) {
    const codes = setCodes.filter((c): c is string => typeof c === 'string')
    return codes.length > 0 ? { kind: 'SETS', setCodes: codes } : { kind: 'AUTO' }
  }
  return kind === 'AUTO' ? { kind: 'AUTO' } : null
}

/**
 * Drop what this server can't honour, and say what was dropped.
 *
 * A deferred `RANDOM` slot is kept: it isn't a set code, it is an instruction to roll one at game
 * start (`TournamentLobby.RANDOM_SET_CODE`), and the server resolves it itself.
 */
function trimSettings(
  raw: RecipeSettings | undefined,
  availableSets: readonly AvailableSet[],
  notes: string[],
): RecipeSettings {
  if (!raw || typeof raw !== 'object') return {}
  const out: { -readonly [K in keyof RecipeSettings]: RecipeSettings[K] } = {}

  if (Array.isArray(raw.setCodes)) {
    // An empty catalogue means the sets haven't arrived over the socket yet — trimming against it
    // would delete every code. Trust the stored list until we have something to check it against.
    const known = availableSets.length > 0
      ? raw.setCodes.filter((c) => isRandomSetCode(c) || availableSets.some((s) => s.code === c))
      : [...raw.setCodes]
    const dropped = raw.setCodes.filter((c) => !known.includes(c))
    if (dropped.length > 0) {
      notes.push(`${dropped.join(', ')} ${dropped.length === 1 ? 'is' : 'are'} not on this server, so ${dropped.length === 1 ? 'it was' : 'they were'} left out.`)
    }
    out.setCodes = known
  }

  if (raw.boosterCount !== undefined) out.boosterCount = clampInt(raw.boosterCount, 1, 16, 6)
  if (raw.pickTimeSeconds !== undefined) out.pickTimeSeconds = clampInt(raw.pickTimeSeconds, 10, 300, 45)
  if (raw.picksPerRound !== undefined) out.picksPerRound = clampInt(raw.picksPerRound, 1, 2, 1)
  if (raw.gamesPerMatch !== undefined) out.gamesPerMatch = clampInt(raw.gamesPerMatch, 1, 5, 1)
  if (raw.deckSizeMin !== undefined) out.deckSizeMin = clampInt(raw.deckSizeMin, 40, 100, 60)
  if (typeof raw.chaosBoosters === 'boolean') out.chaosBoosters = raw.chaosBoosters
  if (raw.includedSetProducts && typeof raw.includedSetProducts === 'object') {
    out.includedSetProducts = Object.fromEntries(
      Object.entries(raw.includedSetProducts)
        .filter(([, ids]) => Array.isArray(ids))
        .map(([code, ids]) => [code, ids.filter((id): id is string => typeof id === 'string')]),
    )
  }
  if (typeof raw.allowDuplicates === 'boolean') out.allowDuplicates = raw.allowDuplicates
  if (typeof raw.randomTeams === 'boolean') out.randomTeams = raw.randomTeams
  if (typeof raw.aiAssistEnabled === 'boolean') out.aiAssistEnabled = raw.aiAssistEnabled
  if (typeof raw.isPublic === 'boolean') out.isPublic = raw.isPublic
  if (typeof raw.ranked === 'boolean') out.ranked = raw.ranked
  if (raw.rules === 'STANDARD' || raw.rules === 'COMMANDER') out.rules = raw.rules
  if (raw.attackMode === 'MULTIPLE' || raw.attackMode === 'LEFT' || raw.attackMode === 'RIGHT') {
    out.attackMode = raw.attackMode
  }
  if (raw.commanderPreset === 'BRAWL' || raw.commanderPreset === 'COMMANDER' || raw.commanderPreset === 'POD') {
    out.commanderPreset = raw.commanderPreset
  }
  if (raw.deckFormat !== undefined) out.deckFormat = raw.deckFormat ?? null
  if (Array.isArray(raw.bannedCardNames)) {
    out.bannedCardNames = raw.bannedCardNames.filter((n): n is string => typeof n === 'string')
  }
  if (raw.boosterDistribution && typeof raw.boosterDistribution === 'object') {
    out.boosterDistribution = { ...raw.boosterDistribution }
  }
  if (raw.cube && typeof raw.cube === 'object' && Array.isArray(raw.cube.cards) && raw.cube.cards.length > 0) {
    out.cube = raw.cube
  }
  return out
}

const RANDOM_SET_CODE = 'RANDOM'
function isRandomSetCode(code: string): boolean {
  return code === RANDOM_SET_CODE || code.startsWith(`${RANDOM_SET_CODE}-`)
}

function clampInt(raw: unknown, min: number, max: number, fallback: number): number {
  if (typeof raw !== 'number' || !Number.isFinite(raw)) return fallback
  return Math.min(max, Math.max(min, Math.round(raw)))
}

/* ── Labelling ──────────────────────────────────────────────────────────── */

/**
 * "Just me · Booster Draft · Round-robin bracket · ECL + BLB · 6 packs".
 *
 * The selection half is {@link selectionSummary}, unchanged — a setup chip and the wizard's recap are
 * describing the same three answers and should read the same. The tail is what the recipe adds over
 * a bare selection, and it is the whole reason a setup is worth one click.
 */
export function recipeSummary(
  recipe: LobbyRecipe,
  /** Set names, when known — the chip reads better as "Bloomburrow" than "BLB". */
  availableSets: readonly AvailableSet[] = [],
): string {
  const parts = [selectionSummary(recipe.selection)]
  const tail = settingsTail(recipe, availableSets)
  return tail.length > 0 ? [...parts, ...tail].join(' · ') : parts[0]!
}

function settingsTail(recipe: LobbyRecipe, availableSets: readonly AvailableSet[]): string[] {
  const s = recipe.settings
  const tail: string[] = []

  if (s.cube) {
    tail.push(`${s.cube.name} (${s.cube.cards.length})`)
  } else if (s.setCodes && s.setCodes.length > 0) {
    tail.push(s.setCodes
      .map((c) => (isRandomSetCode(c) ? 'Random set' : availableSets.find((a) => a.code === c)?.name ?? c))
      .join(' + '))
  }

  const packs = s.boosterCount
  if (packs !== undefined && needsPacks(recipe.selection.cards)) {
    tail.push(`${packs} ${recipe.selection.cards.kind === 'DRAFT' ? 'packs' : 'boosters'}`)
  }
  if (recipe.deck.kind === 'SAVED') tail.push(recipe.deck.name)
  return tail
}

function needsPacks(cards: CardsAxis): boolean {
  return cards.kind === 'SEALED' || cards.kind === 'DRAFT'
}
