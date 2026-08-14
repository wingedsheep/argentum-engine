/**
 * One view model over the two unrelated server lobby implementations.
 *
 * The server has `QuickGameLobby` (125 lines, in-memory, hard-capped at two seats, flat DTO) and
 * `TournamentLobby` (1884 lines, Redis-backed, 2–8 seats, a state machine) with **no shared
 * interface and no `kind` discriminator**. A single unified lobby is therefore not a client-only
 * change — see `backlog/menu-lobby-restructure-and-help.md` § *The honest constraint*.
 *
 * So the client unifies the *presentation* first: both slices project onto `UnifiedLobbyView`, and
 * `LobbyScreen` renders that and nothing else. Everything the two kinds genuinely disagree about
 * is named here as a field rather than branched on at every call site — which is what lets the
 * server gaps behind it (Phase 5) be closed one at a time without touching the screen.
 *
 * This module is pure: no store reads, no side effects. The commands that write back live in
 * `useLobbyCommands.ts`.
 */
import type { LobbyState } from '@/store/slices/types'
import type { AiDeckSpecView, QuickGameLobbyStateMessage } from '@/types'
import type { DeckPickerTab } from '../ui/DeckPicker'
import {
  axesFromLobbySettings,
  axesFromQuickGameLobby,
  effectiveCommanderPreset,
  rulesFromLobbySettings,
  rulesTableBlock,
  tableFromGameMode,
  type AxisSelection,
  type CardsKind,
} from './axes'
import type { GroupId } from './settingsGroups'

/** Which server implementation is backing this lobby. */
export type LobbyKind = 'QUICK' | 'TOURNAMENT'

export interface LobbyViewPlayer {
  playerId: string
  name: string
  isYou: boolean
  isHost: boolean
  isAi: boolean
  isConnected: boolean
  /** Right-hand status text — "Deck Ready", "Choosing deck…", "✓ Ready · Custom (60)". */
  status: string
  tone: 'ready' | 'joined' | 'disconnected'
  /**
   * For an AI seat: what the host chose for it to play, or null where the choice doesn't exist —
   * on a human seat, and in a lobby whose format deals the AI a pool to build from.
   */
  aiDeck?: AiDeckSpecView | null
}

/**
 * How the lobby is told to go.
 *
 * `PER_PLAYER_READY` — everyone toggles ready and the server starts when all are (quick lobbies).
 * `HOST_START` — the host presses one button (tournament lobbies). Gap #4 in the plan's Phase 5
 * list is giving tournament lobbies the per-player flavour too, which is what makes a two-player
 * game *feel* quick; until then the difference is real and named rather than papered over.
 */
export type StartModel = 'PER_PLAYER_READY' | 'HOST_START'

/**
 * Team setup for the two team tables (2HG — CR 810; Team vs. Team — CR 808).
 *
 * `MANUAL` resolves every seat, defaults included, so the player list doesn't have to re-derive
 * "team by join order" the way three separate call sites used to.
 */
export type LobbyTeams =
  | { mode: 'NONE' }
  | { mode: 'RANDOM' }
  | {
      mode: 'MANUAL'
      byPlayerId: Readonly<Record<string, number>>
      /** Seats per team — both teams must hold exactly this many or the server re-balances. */
      size: number
      balanced: boolean
    }

export interface LobbyPrimaryAction {
  kind: 'READY' | 'UNREADY' | 'START'
  label: string
  disabled: boolean
  /** Why it is disabled — becomes the button's tooltip. */
  reason: string | undefined
}

/** The role-aware sentence at the top of the lobby: what this viewer should do next. */
export interface LobbyGuidance {
  title: string
  detail: string
  tone: 'action' | 'waiting' | 'ready'
}

export interface UnifiedLobbyView {
  kind: LobbyKind
  lobbyId: string
  title: string
  subtitle: string
  isHost: boolean
  /** Pre-game staging: settings are editable and the axes can still be changed. */
  isWaiting: boolean
  /** A vs-AI lobby has nobody to invite, so it shows no code and no QR. */
  invitable: boolean
  /** Where this lobby sits in the Cards / Rules / Table / Event space. */
  axes: AxisSelection
  players: readonly LobbyViewPlayer[]
  you: LobbyViewPlayer | undefined
  maxPlayers: number
  startModel: StartModel
  /** The one action button beside Leave, or null when there is nothing for this viewer to press. */
  primaryAction: LobbyPrimaryAction | null
  /** Viewer-specific next step, derived only from the server's lobby snapshot. */
  guidance: LobbyGuidance
  isPublic: boolean
  /** Whether the host may currently add an AI seat. */
  canAddAi: boolean
  ranked: { available: boolean; on: boolean }
  teams: LobbyTeams
  /**
   * The settings group holding the reason Start is disabled, when one does.
   *
   * Lets the collapsed settings panel open the group that needs attention without maintaining a
   * second "which row fixes this" mapping beside `startBlockReason`. Null when the blocker isn't a
   * setting at all (too few players, decks not submitted) or when nothing is blocking.
   */
  blockGroup: GroupId | null
}

/* ── Quick game ─────────────────────────────────────────────────────────── */

/**
 * @param opts.deckValid the deck picker's live validity — component-local state, so it has to be
 *   passed in rather than read. A quick lobby's ready button gates on it.
 * @param opts.deckTab the deck picker's live tab, which *is* the Cards axis on a quick lobby:
 *   Random pool is the picker's Random tab, so hoisting it is what keeps the axis row, the header
 *   chip and the picker from disagreeing.
 */
export function fromQuickGameLobby(
  lobby: QuickGameLobbyStateMessage,
  opts: { deckValid: boolean; deckTab: DeckPickerTab | undefined; aiEnabled: boolean },
): UnifiedLobbyView {
  const you = lobby.players.find((p) => p.playerId === lobby.youPlayerId)
  // Host is the first non-AI seat — the same convention the server's leave handler uses.
  const isHost = lobby.players.find((p) => !p.isAi)?.playerId === lobby.youPlayerId
  const isMomir = lobby.momirBasic ?? false
  const youReady = you?.ready ?? false
  const needsDeck = !isMomir && (!opts.deckValid || !you?.deckSelected)
  const axes = axesFromQuickGameLobby(lobby, you, opts.deckTab)
  // Only a host-*picked* deck can be missing a commander; the generated sources choose their own.
  // Mirrors the server's ready-up gate in `QuickGameLobbyHandler.handleSetReady`.
  const needsAiCommander =
    lobby.vsAi && axes.rules === 'COMMANDER'
    && lobby.aiDeck?.kind === 'deck' && !lobby.aiDeck.commander

  const players = lobby.players.map((p): LobbyViewPlayer => ({
    playerId: p.playerId,
    name: p.playerName,
    isYou: p.playerId === lobby.youPlayerId,
    isHost: p.playerId === lobby.players.find((q) => !q.isAi)?.playerId,
    isAi: p.isAi,
    // Quick lobbies drop disconnected players outright, so anyone listed is connected.
    isConnected: true,
    status: !p.deckSelected
      ? 'Choosing deck…'
      : p.ready
        ? `✓ Ready · ${p.deckLabel}`
        : `Deck: ${p.deckLabel}`,
    tone: p.ready ? 'ready' : 'joined',
  }))
  const guidance = quickGuidance({
    players,
    isHost,
    youReady,
    needsDeck,
    needsAiCommander,
    invitable: !lobby.vsAi,
  })

  return {
    kind: 'QUICK',
    lobbyId: lobby.lobbyId,
    title: '1v1 Lobby',
    subtitle: quickSubtitle(axes.cards.kind, lobby.vsAi),
    isHost,
    // A quick lobby has no state machine: it is staging right up until the game starts.
    isWaiting: true,
    // An AI fills the only opponent seat. Removing it reopens the same lobby and invite flow.
    invitable: !lobby.vsAi,
    axes,
    players,
    you: players.find((p) => p.isYou),
    maxPlayers: 2,
    startModel: 'PER_PLAYER_READY',
    primaryAction: youReady
      ? { kind: 'UNREADY', label: 'Cancel ready', disabled: false, reason: undefined }
      : {
          kind: 'READY',
          label: "I'm ready",
          disabled: needsDeck || needsAiCommander,
          reason: needsDeck
            ? 'Pick a deck first'
            : needsAiCommander
              ? 'Pick a Commander deck for the AI'
              : undefined,
        },
    guidance,
    isPublic: lobby.isPublic,
    canAddAi: isHost && opts.aiEnabled && !lobby.vsAi && lobby.players.length < 2,
    ranked: { available: lobby.rankedEligible ?? false, on: lobby.ranked ?? false },
    // The server's `QuickGameLobby.twoHeadedGiant` exists but no client has ever reached it (gap
    // #6): it isn't in `QuickGameLobbyStateMessage` at all, so there is nothing here to read.
    teams: { mode: 'NONE' },
    // A quick lobby's only start condition is "pick a deck", which the deck picker answers directly.
    blockGroup: null,
  }
}

function quickGuidance({
  players,
  isHost,
  youReady,
  needsDeck,
  needsAiCommander,
  invitable,
}: {
  players: readonly LobbyViewPlayer[]
  isHost: boolean
  youReady: boolean
  needsDeck: boolean
  needsAiCommander: boolean
  invitable: boolean
}): LobbyGuidance {
  const other = players.find((p) => !p.isYou)

  if (needsDeck) {
    return {
      title: 'Choose your deck',
      detail: 'Open your player row to choose one. When it is valid, mark yourself ready below.',
      tone: 'action',
    }
  }
  if (needsAiCommander) {
    return {
      title: 'Choose a deck for the AI',
      detail: 'Commander games need an AI deck with a designated commander.',
      tone: 'action',
    }
  }
  if (youReady) {
    return {
      title: 'You’re ready',
      detail: other?.tone === 'ready'
        ? 'Everyone is ready. The game is starting.'
        : `Waiting for ${other?.name ?? 'the other player'} to get ready. You can cancel ready below.`,
      tone: 'ready',
    }
  }
  if (invitable && !other) {
    return {
      title: isHost ? 'Invite your opponent' : 'Waiting for the host',
      detail: isHost
        ? 'Copy the invite code or share the QR code. You can choose your deck while you wait.'
        : 'The host will invite the other player.',
      tone: 'waiting',
    }
  }
  if (other?.tone === 'ready') {
    return {
      title: `${other.name} is ready`,
      detail: 'Mark yourself ready below to start the game.',
      tone: 'action',
    }
  }
  return {
    title: 'Ready when you are',
    detail: 'Mark yourself ready below. The game starts automatically when both players are ready.',
    tone: 'action',
  }
}

/**
 * The line under a quick lobby's title: what it still needs from you, and how it starts.
 *
 * It follows the **Cards** axis and not just `vsAi`, because the three values ask for different
 * things — and two of them ask for nothing. The previous copy branched on `vsAi` alone, so a lobby
 * created from the wizard's "Random pool" opened telling the player to "pick a deck", which is the
 * one instruction that answer had already made obsolete.
 */
function quickSubtitle(cards: CardsKind, vsAi: boolean): string {
  const start = vsAi
    ? 'Ready up and the AI starts.'
    : 'Share the invite code with a friend, then both players ready up.'
  switch (cards) {
    case 'RANDOM':
      // 8 boosters, auto-built into a 40-card deck — `SealedDeckGenerator.generate`.
      return `Nothing to prepare — the server opens boosters and builds your deck when the game starts. ${start}`
    case 'MOMIR':
      return `No deckbuilding — everyone runs 60 basics and flips creatures with the Momir Vig avatar. ${start}`
    default:
      return `Pick a deck. ${start}`
  }
}

/* ── Tournament ─────────────────────────────────────────────────────────── */

export function fromTournamentLobby(
  lobbyState: LobbyState,
  opts: { aiEnabled: boolean; playerId: string | null },
): UnifiedLobbyView {
  const s = lobbyState.settings
  const isWaiting = lobbyState.state === 'WAITING_FOR_PLAYERS'
  const axes = axesFromLobbySettings(s)
  const playerCount = lobbyState.players.length
  const isWinston = s.format === 'WINSTON_DRAFT'
  const isGridDraft = s.format === 'GRID_DRAFT'
  const maxPlayers = isWinston ? 2 : isGridDraft ? 4 : (s.maxPlayers || 8)

  // The AI's deck is only the host's to pick where the format doesn't deal it a pool; everywhere
  // else it builds from the cards it drafted or opened, which is the format working as intended.
  const aiDeckIsChosen = s.format === 'PREMADE_DECKS'

  // Only a bring-a-deck lobby expects a deck *here*; a draft or sealed lobby deals the pool later,
  // so "no deck yet" would read there as a problem rather than as nothing-to-do-yet.
  const bringsDeck = axes.cards.kind === 'BRING_A_DECK'

  const players = lobbyState.players.map((p): LobbyViewPlayer => ({
    playerId: p.playerId,
    name: p.playerName,
    isYou: p.playerId === opts.playerId,
    isHost: p.isHost,
    isAi: p.isAi,
    isConnected: p.isConnected,
    status: !p.isConnected
      ? 'Disconnected'
      : p.deckSubmitted
        ? 'Deck ready'
        : bringsDeck ? 'No deck yet' : 'Joined',
    tone: !p.isConnected ? 'disconnected' : p.deckSubmitted ? 'ready' : 'joined',
    aiDeck: p.isAi && aiDeckIsChosen ? (p.aiDeck ?? { kind: 'auto' }) : null,
  }))

  const blockReason = startBlockReason(lobbyState)
  const guidance = tournamentGuidance({
    players,
    isHost: lobbyState.isHost,
    isWaiting,
    bringsDeck,
    blockReason: blockReason?.reason ?? null,
  })

  return {
    kind: 'TOURNAMENT',
    lobbyId: lobbyState.lobbyId,
    title: tournamentTitle(lobbyState),
    subtitle: tournamentSubtitle(lobbyState),
    isHost: lobbyState.isHost,
    isWaiting,
    invitable: true,
    axes,
    players,
    you: players.find((p) => p.isYou),
    maxPlayers,
    startModel: 'HOST_START',
    primaryAction: isWaiting && lobbyState.isHost
      ? {
          kind: 'START',
          label: startLabel(lobbyState),
          disabled: blockReason !== null,
          reason: blockReason?.reason,
        }
      : null,
    guidance,
    blockGroup: blockReason?.group ?? null,
    isPublic: s.isPublic,
    // No Commander carve-out: an AI seat builds its own legal commander deck, whether the lobby
    // brings decks or deals a pool.
    canAddAi: isWaiting && lobbyState.isHost && opts.aiEnabled && playerCount < maxPlayers,
    // Ranked is a 1v1-bracket-only concept server-side (`TournamentLobby.rankedEligible`).
    ranked: { available: axes.table === 'ONE_V_ONE', on: s.ranked ?? false },
    teams: tournamentTeams(lobbyState),
  }
}

function tournamentGuidance({
  players,
  isHost,
  isWaiting,
  bringsDeck,
  blockReason,
}: {
  players: readonly LobbyViewPlayer[]
  isHost: boolean
  isWaiting: boolean
  bringsDeck: boolean
  blockReason: string | null
}): LobbyGuidance {
  if (!isWaiting) {
    return {
      title: 'Event in progress',
      detail: 'The lobby will move everyone on when the current stage is complete.',
      tone: 'waiting',
    }
  }

  const you = players.find((p) => p.isYou)
  if (!isHost) {
    if (bringsDeck && you?.tone !== 'ready') {
      return {
        title: 'Choose and submit your deck',
        detail: 'The host can start after every connected player has submitted a deck.',
        tone: 'action',
      }
    }
    return {
      title: bringsDeck ? 'Your deck is submitted' : 'You’re in the lobby',
      detail: 'The host controls the settings and will start when everyone is ready.',
      tone: bringsDeck ? 'ready' : 'waiting',
    }
  }

  if (players.length < 2) {
    return {
      title: 'Invite players',
      detail: 'Share the invite code above. You can finish configuring the lobby while they join.',
      tone: 'waiting',
    }
  }
  if (blockReason) {
    return {
      title: blockReason.startsWith('All connected players')
        ? 'Waiting for deck submissions'
        : 'Finish the lobby setup',
      detail: blockReason,
      tone: 'action',
    }
  }
  return {
    title: 'Ready to start',
    detail: 'The player count and settings are valid. Start the event when your group is ready.',
    tone: 'ready',
  }
}

function tournamentTeams(lobbyState: LobbyState): LobbyTeams {
  const s = lobbyState.settings
  if (s.gameMode !== 'TWO_HEADED_GIANT' && s.gameMode !== 'TEAM_VS_TEAM') return { mode: 'NONE' }
  if (s.randomTeams ?? true) return { mode: 'RANDOM' }

  // Both team modes split the pod into exactly two even teams, so team size follows the seat count
  // and unassigned seats fall back to join order.
  const size = Math.max(1, Math.floor(lobbyState.players.length / 2))
  const assigned = s.teamAssignments ?? {}
  const byPlayerId: Record<string, number> = {}
  lobbyState.players.forEach((p, i) => {
    byPlayerId[p.playerId] = assigned[p.playerId] ?? Math.floor(i / size)
  })
  const n = lobbyState.players.length
  const balanced = n >= 4 && n % 2 === 0 &&
    [0, 1].every((t) => Object.values(byPlayerId).filter((v) => v === t).length === size)
  return { mode: 'MANUAL', byPlayerId, size, balanced }
}

function tournamentTitle(lobbyState: LobbyState): string {
  const s = lobbyState.settings
  if (s.cubeName) return s.cubeName
  if (s.format !== 'PREMADE_DECKS') return s.setNames.join(' + ') || 'Lobby'
  switch (s.gameMode) {
    case 'TWO_HEADED_GIANT': return 'Premade Decks Two-Headed Giant'
    case 'TEAM_VS_TEAM': return 'Premade Decks Team vs. Team'
    case 'FREE_FOR_ALL': return 'Premade Decks Free-for-All'
    case 'TOURNAMENT': return 'Premade Decks Tournament'
  }
}

function tournamentSubtitle(lobbyState: LobbyState): string {
  const s = lobbyState.settings
  // With more than one set selected the header names the split rather than a bare total.
  const distText = s.setCodes.length > 1 && Object.keys(s.boosterDistribution).length > 0
    ? Object.entries(s.boosterDistribution)
        .map(([code, count]) => {
          const idx = s.setCodes.indexOf(code)
          return `${count} ${idx >= 0 ? (s.setNames[idx] ?? code) : code}`
        })
        .join(' + ')
    : null
  // A Commander game names the life total it will actually start at — which for a pod is the server's
  // override rather than the host's 1v1 choice. Keyed on the Rules axis, so it shows up on any
  // Commander lobby rather than only on the two Commander pack shapes; a brought deck is paper
  // Commander's 40 whatever the table (the preset only tunes a 60-card limited deck).
  const preset = effectiveCommanderPreset(s.commanderPreset, s.gameMode)
  const presetLabel = preset === 'POD' ? 'Pod 40 life'
    : preset === 'COMMANDER' ? 'Commander 30 life'
    : 'Brawl 25 life'
  const commanderNote = rulesFromLobbySettings(s) !== 'COMMANDER' ? null
    : s.format === 'PREMADE_DECKS' ? 'Commander 40 life'
    : presetLabel
  const pick2 = s.picksPerRound === 2 ? ' · Pick 2' : ''

  const base = (() => {
    const pool = (() => {
      switch (s.format) {
        case 'GRID_DRAFT':
          return `Grid Draft · ${s.boosterCount} boosters · ${s.pickTimeSeconds}s per pick`
        case 'WINSTON_DRAFT':
          return `Winston Draft · ${distText ?? `${s.boosterCount} boosters`} · ${s.pickTimeSeconds}s per turn`
        case 'COMMANDER_DRAFT':
        case 'DRAFT':
          return `${distText ?? `${s.boosterCount} packs`} · ${s.pickTimeSeconds}s per pick${pick2}`
        case 'COMMANDER_SEALED':
          return `${distText ?? `${s.boosterCount} packs`}`
        case 'PREMADE_DECKS':
          return 'Premade Decks · bring your own ≥40-card deck'
        case 'SEALED':
          return distText ?? `${s.boosterCount} boosters per player`
      }
    })()
    return commanderNote ? `${pool} · ${commanderNote}` : pool
  })()
  const poolPlay = Boolean(s.cubeName && s.cubePoolPlay && s.format === 'SEALED')
  const source = s.cubeName
    ? poolPlay
      // Pool Play deals nothing, so pack size and booster count are meaningless here.
      ? `Cube Pool Play · ${s.cubeCardCount ?? 0} cards · everyone builds from the whole cube`
      : `Cube · ${s.cubeCardCount ?? 0} cards · ${s.packSize ?? 15}-card packs · ${base}`
    : base

  const isMultiplayer = s.gameMode !== 'TOURNAMENT'
  const games = s.gamesPerMatch ?? 1
  return !isMultiplayer && games > 1 ? `${source} · ${games} games per matchup` : source
}

function startLabel(lobbyState: LobbyState): string {
  const s = lobbyState.settings
  const isAnyDraft = s.format === 'DRAFT' || s.format === 'WINSTON_DRAFT' ||
    s.format === 'GRID_DRAFT' || s.format === 'COMMANDER_DRAFT'
  if (isAnyDraft) return 'Start Draft'
  if (s.format === 'PREMADE_DECKS' && s.gameMode === 'TOURNAMENT') return 'Start Tournament'
  return 'Start Game'
}

/**
 * Why the host can't press start yet, or null when they can — and which settings group holds the
 * answer.
 *
 * Every branch of the seat-count rule gets its own sentence. The old inline version fell through
 * to a bare "Need at least 2 players" for the exact-count shapes, so a Two-Headed Giant lobby
 * holding three players offered a disabled button and no explanation.
 *
 * The `group` half is what lets the settings panel collapse safely: the group holding the blocker
 * opens itself and flags a `!`, derived from *this* function rather than from a second table that
 * would drift from it. Null means nothing here can be opened to fix it — "need at least 2 players"
 * is answered by the player list.
 */
function startBlockReason(lobbyState: LobbyState): { reason: string; group: GroupId | null } | null {
  const s = lobbyState.settings
  const n = lobbyState.players.length
  const at = (group: GroupId | null, reason: string) => ({ reason, group })

  switch (s.gameMode) {
    case 'TWO_HEADED_GIANT':
      if (n !== 4) return at('TABLE', `Two-Headed Giant is exactly 4 players — this lobby has ${n}`)
      break
    case 'TEAM_VS_TEAM':
      if (n < 4 || n % 2 !== 0) return at('TABLE', `Team vs. Team needs an even pod of 4, 6 or 8 — this lobby has ${n}`)
      break
    default:
      break
  }
  switch (s.format) {
    case 'WINSTON_DRAFT':
      if (n !== 2) return at('CARDS', `Winston Draft is exactly 2 players — this lobby has ${n}`)
      break
    case 'GRID_DRAFT':
      if (n < 2 || n > 4) return at('CARDS', `Grid Draft seats 2 to 4 players — this lobby has ${n}`)
      break
    default:
      break
  }
  // The Rules × Table conflict, asked of the Rules axis rather than of the pack format. Sharing a
  // *pool* and sharing a *game* are separate questions — eight people can draft Commander and play a
  // 1v1 bracket or one pod — so this is not a seat limit; it is the one table Commander cannot have,
  // and it now also catches a Commander lobby whose decks were brought rather than drafted. Same
  // rejection the server's start gate sends, said before the host presses Start.
  const rulesConflict = rulesTableBlock(rulesFromLobbySettings(s), tableFromGameMode(s.gameMode))
  if (rulesConflict !== null) return at('RULES', rulesConflict)
  if (n < 2) return at(null, 'Need at least 2 players')

  if (s.format === 'PREMADE_DECKS') {
    const allSubmitted = lobbyState.players.filter((p) => p.isConnected).every((p) => p.deckSubmitted)
    return allSubmitted ? null : at(null, 'All connected players must submit a deck first')
  }
  if (s.cubeName) {
    // Pool Play hands every player the whole cube instead of dealing from it, so it has no capacity
    // constraint at all — mirrors TournamentLobby.cubeCapacityError.
    if (s.cubePoolPlay && s.format === 'SEALED') return null
    const packSize = s.packSize ?? 15
    const sharedPool = s.format === 'WINSTON_DRAFT' || s.format === 'GRID_DRAFT'
    const packsNeeded = sharedPool ? s.boosterCount : n * s.boosterCount
    const cardsNeeded = packsNeeded * packSize
    const cubeCards = s.cubeCardCount ?? 0
    if (cardsNeeded > cubeCards) {
      return at('CARDS', sharedPool
        ? `${s.boosterCount} packs × ${packSize} = ${cardsNeeded} cards needed, cube has ${cubeCards}`
        : `${n} players × ${s.boosterCount} packs × ${packSize} = ${cardsNeeded} cards needed, cube has ${cubeCards}`)
    }
    return null
  }
  if (s.setCodes.length === 0) return at('CARDS', 'Select at least one set')
  // Extension sets (bonus sheets) can't carry a pool alone. Unknown codes count as regular; the
  // server re-validates. A deferred random slot always rolls a regular set, so it satisfies this.
  const hasBaseSet = s.setCodes.some(
    (code) => !s.availableSets.find((a) => a.code === code)?.extensionSet,
  )
  if (!hasBaseSet) return at('CARDS', 'Extension sets need a regular set alongside them — add one')
  return null
}
