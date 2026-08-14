/**
 * AI seats under Commander rules.
 *
 * Every surface used to refuse the combination: no generated deck named a commander, so the wizard,
 * the lobby's Rules and Cards rows, the Add-AI button and the quick lobby's ready gate all said
 * "bring a deck for it". The server now builds the AI a legal commander deck from a format's card
 * base *or* from the pool it is dealt, so the only thing left to ask for is the one case the host
 * genuinely has to answer: they picked an exact list and didn't say which card leads it.
 *
 * These tests are the four surfaces agreeing on that. A surface that still refuses an AI Commander
 * seat is a lobby the server would happily have started.
 */
import { describe, expect, it } from 'vitest'
import type { CardsAxis } from './axes'
import { rulesChoices } from './axisChoices'
import { fromQuickGameLobby, fromTournamentLobby, type UnifiedLobbyView } from './lobbyViewModel'
import { shapeChoices } from './modeMatrix'
import type { LobbySettings, QuickGameLobbyStateMessage, TournamentFormat } from '@/types'
import type { LobbyState } from '@/store/slices/types'

const COMMANDER_CARDS: readonly CardsAxis[] = [
  { kind: 'DRAFT', shape: 'COMMANDER' },
  { kind: 'SEALED', shape: 'COMMANDER' },
  { kind: 'BRING_A_DECK', legality: 'COMMANDER' },
]

function settings(overrides: Partial<LobbySettings>): LobbySettings {
  return {
    setCodes: ['CMR'],
    setNames: ['Commander Legends'],
    availableSets: [],
    format: 'COMMANDER_DRAFT',
    boosterCount: 3,
    boosterDistribution: {},
    maxPlayers: 8,
    pickTimeSeconds: 45,
    picksPerRound: 2,
    gamesPerMatch: 1,
    isPublic: false,
    rules: 'COMMANDER',
    deckSizeMin: 60,
    allowDuplicates: true,
    commanderPreset: 'BRAWL',
    chaosBoosters: false,
    bannedCardNames: [],
    aiAssistEnabled: false,
    gameMode: 'TOURNAMENT',
    attackMode: 'MULTIPLE',
    randomTeams: true,
    teamAssignments: {},
    ...overrides,
  } as LobbySettings
}

/** A Commander lobby holding one human host and one AI seat. */
function lobbyWithAi(format: TournamentFormat): LobbyState {
  return {
    lobbyId: 'lobby-1',
    state: 'WAITING_FOR_PLAYERS',
    players: [
      { playerId: 'p0', playerName: 'Host', isHost: true, isAi: false, isConnected: true, deckSubmitted: false },
      { playerId: 'ai-1', playerName: 'Robot', isHost: false, isAi: true, isConnected: true, deckSubmitted: false },
    ],
    settings: settings({ format }),
    isHost: true,
    draftState: null,
    winstonDraftState: null,
    gridDraftState: null,
  } as unknown as LobbyState
}

function view(format: TournamentFormat): UnifiedLobbyView {
  return fromTournamentLobby(lobbyWithAi(format), { aiEnabled: true, playerId: 'p0' })
}

function quickLobby(aiDeck: QuickGameLobbyStateMessage['aiDeck']): QuickGameLobbyStateMessage {
  return {
    lobbyId: 'quick-1',
    vsAi: true,
    setCode: null,
    players: [
      {
        playerId: 'p0', playerName: 'Host', isAi: false, ready: false,
        deckSelected: true, deckCardCount: 100, deckLabel: 'Custom (100)', setCodes: [],
      },
      {
        playerId: 'ai-1', playerName: 'Robot', isAi: true, ready: false,
        deckSelected: true, deckCardCount: 100, deckLabel: 'Auto (Commander)', setCodes: [],
      },
    ],
    youPlayerId: 'p0',
    canStart: false,
    isPublic: false,
    format: 'COMMANDER',
    rules: 'COMMANDER',
    aiDeck,
  } as unknown as QuickGameLobbyStateMessage
}

describe('an AI seat in a Commander lobby', () => {
  it('the wizard offers every shape to a solo Commander player', () => {
    // A solo lobby is an AI lobby. Blocking the AI blocked the whole Commander branch of the wizard.
    for (const cards of COMMANDER_CARDS) {
      for (const choice of shapeChoices('SOLO', cards)) {
        if (choice.value === 'TWO_HEADED_GIANT') continue
        // A limited pool still declines the two-seat single game, which is its own rule.
        if (choice.value === 'ONE_GAME' && cards.kind !== 'BRING_A_DECK') continue
        expect(choice.disabledReason, `${choice.value} / ${JSON.stringify(cards)}`).toBeUndefined()
      }
    }
  })

  it('the Rules axis lets a lobby with an AI seat switch to Commander', () => {
    const standard = fromTournamentLobby(
      { ...lobbyWithAi('PREMADE_DECKS'), settings: settings({ format: 'PREMADE_DECKS', rules: 'STANDARD' }) } as LobbyState,
      { aiEnabled: true, playerId: 'p0' },
    )

    const commander = rulesChoices(standard).find((c) => c.value === 'COMMANDER')

    expect(commander?.availability.kind).not.toBe('BLOCKED')
  })

  it('the host can still add an AI to a Commander limited lobby', () => {
    for (const format of ['COMMANDER_DRAFT', 'COMMANDER_SEALED', 'PREMADE_DECKS'] as const) {
      // Seats are free (maxPlayers 8), so the only thing that could refuse is the Commander rule.
      expect(view(format).canAddAi, format).toBe(true)
    }
  })

  it('a quick lobby readies up against a generated AI Commander deck', () => {
    const generated = quickLobby({ kind: 'auto', setCodes: [], label: null, cardCount: 0, commander: null })

    const action = fromQuickGameLobby(generated, { deckValid: true, deckTab: undefined, aiEnabled: true }).primaryAction

    expect(action?.disabled).toBe(false)
  })

  it('but still asks for a commander when the host picked an exact list without one', () => {
    // The one case the host has to answer: a `deck` spec names its cards but not its leader. The
    // server's ready-up gate refuses the same thing, so the button has to say why first.
    const picked = quickLobby({ kind: 'deck', setCodes: [], label: 'Chosen deck', cardCount: 100, commander: null })

    const action = fromQuickGameLobby(picked, { deckValid: true, deckTab: undefined, aiEnabled: true }).primaryAction

    expect(action?.disabled).toBe(true)
    expect(action?.reason).toBe('Pick a Commander deck for the AI')
  })

  it('and is satisfied once that list designates one', () => {
    const led = quickLobby({ kind: 'deck', setCodes: [], label: 'Chosen deck', cardCount: 100, commander: 'Zetalpa, Primal Dawn' })

    const action = fromQuickGameLobby(led, { deckValid: true, deckTab: undefined, aiEnabled: true }).primaryAction

    expect(action?.disabled).toBe(false)
  })
})
