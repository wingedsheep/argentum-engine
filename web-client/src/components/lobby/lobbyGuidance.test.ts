import { describe, expect, it } from 'vitest'
import type { LobbyState } from '@/store/slices/types'
import type { LobbySettings, QuickGameLobbyStateMessage } from '@/types'
import { fromQuickGameLobby, fromTournamentLobby } from './lobbyViewModel'

function quick(players: QuickGameLobbyStateMessage['players']): QuickGameLobbyStateMessage {
  return {
    type: 'quickGameLobbyState',
    lobbyId: 'ABC123',
    youPlayerId: 'p1',
    players,
    vsAi: false,
    isPublic: false,
  } as QuickGameLobbyStateMessage
}

function tournament({
  isHost,
  deckSubmitted = false,
  players = 2,
}: {
  isHost: boolean
  deckSubmitted?: boolean
  players?: number
}): LobbyState {
  return {
    lobbyId: 'ABC123',
    state: 'WAITING_FOR_PLAYERS',
    isHost,
    players: Array.from({ length: players }, (_, index) => ({
      playerId: `p${index + 1}`,
      playerName: index === 0 ? 'Alice' : 'Bob',
      isHost: index === 0,
      isAi: false,
      isConnected: true,
      deckSubmitted: index === 0 ? deckSubmitted : false,
    })),
    settings: {
      setCodes: [],
      setNames: [],
      availableSets: [],
      format: 'PREMADE_DECKS',
      boosterCount: 3,
      boosterDistribution: {},
      maxPlayers: 8,
      pickTimeSeconds: 45,
      picksPerRound: 1,
      gamesPerMatch: 1,
      isPublic: false,
      rules: 'STANDARD',
      deckSizeMin: 40,
      allowDuplicates: false,
      commanderPreset: 'COMMANDER',
    chaosBoosters: false,
    includedSetProducts: {},
      bannedCardNames: [],
      aiAssistEnabled: false,
      gameMode: 'TOURNAMENT',
      attackMode: 'MULTIPLE',
      randomTeams: true,
      teamAssignments: {},
    } as LobbySettings,
    draftState: null,
    winstonDraftState: null,
    gridDraftState: null,
  } as unknown as LobbyState
}

describe('role-aware lobby guidance', () => {
  it('tells a lone quick-lobby host to invite an opponent after choosing a deck', () => {
    const view = fromQuickGameLobby(
      quick([{
        playerId: 'p1',
        playerName: 'Alice',
        isAi: false,
        ready: false,
        deckSelected: true,
        deckLabel: 'Custom (60)',
        deckCardCount: 60,
        setCode: null,
      }]),
      { deckValid: true, deckTab: 'saved', aiEnabled: false },
    )

    expect(view.guidance).toMatchObject({ title: 'Invite your opponent', tone: 'waiting' })
  })

  it('tells a ready player exactly who they are waiting for', () => {
    const view = fromQuickGameLobby(
      quick([
        {
          playerId: 'p1', playerName: 'Alice', isAi: false, ready: true,
          deckSelected: true, deckLabel: 'Custom (60)', deckCardCount: 60, setCode: null,
        },
        {
          playerId: 'p2', playerName: 'Bob', isAi: false, ready: false,
          deckSelected: true, deckLabel: 'Custom (60)', deckCardCount: 60, setCode: null,
        },
      ]),
      { deckValid: true, deckTab: 'saved', aiEnabled: false },
    )

    expect(view.guidance.title).toBe('You’re ready')
    expect(view.guidance.detail).toContain('Bob')
  })

  it('gives a tournament guest a deck-submission instruction instead of a generic wait message', () => {
    const view = fromTournamentLobby(
      tournament({ isHost: false }),
      { aiEnabled: false, playerId: 'p1' },
    )

    expect(view.guidance).toMatchObject({
      title: 'Choose and submit your deck',
      tone: 'action',
    })
  })

  it('confirms when a tournament guest has finished their part', () => {
    const view = fromTournamentLobby(
      tournament({ isHost: false, deckSubmitted: true }),
      { aiEnabled: false, playerId: 'p1' },
    )

    expect(view.guidance).toMatchObject({ title: 'Your deck is submitted', tone: 'ready' })
  })

  it('separates the host’s invite blocker from setup blockers', () => {
    const view = fromTournamentLobby(
      tournament({ isHost: true, players: 1 }),
      { aiEnabled: false, playerId: 'p1' },
    )

    expect(view.guidance).toMatchObject({ title: 'Invite players', tone: 'waiting' })
  })
})
