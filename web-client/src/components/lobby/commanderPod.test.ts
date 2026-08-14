/**
 * Commander at each of the four tables.
 *
 * Commander pods are supported end to end — the engine tallies commander damage per *(commander,
 * defending player)* pair, gives every player their own command zone, and loops the turn order for
 * the CR 903.9a zone choice — so the only table Commander cannot sit at is Two-Headed Giant, whose
 * shared team life total (CR 810.4) has nowhere to put Commander's per-player 40.
 *
 * Four surfaces have to agree on that one exception: the wizard (`modeMatrix`), the lobby's Table
 * and Cards axes (`axisChoices` / `LobbyAxes.shapeBlock`), and the Start button
 * (`lobbyViewModel.startBlockReason`). They are separate modules because the wording differs per
 * situation, but a table one offers and another refuses is a bug — that is what this pins, together
 * with the life total a pod actually starts at.
 *
 * This file stays scoped to the *drafted / sealed* Commander pod, which is what PR #1552 opened up.
 * The Rules axis those surfaces now ask — Commander over any Cards value, and the single predicate
 * they all read — is `rulesAxis.test.ts`.
 */
import { describe, expect, it } from 'vitest'
import {
  COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL,
  COMMANDER_PRESETS,
  effectiveCommanderPreset,
  type CardsAxis,
  type TableAxis,
} from './axes'
import { tableChoices } from './axisChoices'
import { fromTournamentLobby, type UnifiedLobbyView } from './lobbyViewModel'
import { shapeChoices } from './modeMatrix'
import type { LobbyGameMode, LobbySettings, TournamentFormat } from '@/types'
import type { LobbyState } from '@/store/slices/types'

const COMMANDER_CARDS: readonly CardsAxis[] = [
  { kind: 'DRAFT', shape: 'COMMANDER' },
  { kind: 'SEALED', shape: 'COMMANDER' },
]

/** The two multiplayer tables a Commander pod is meant to reach, plus the bracket it always could. */
const COMMANDER_TABLES: readonly TableAxis[] = ['ONE_V_ONE', 'FREE_FOR_ALL', 'TEAM_VS_TEAM']

const GAME_MODE_FOR_TABLE: Record<TableAxis, LobbyGameMode> = {
  ONE_V_ONE: 'TOURNAMENT',
  FREE_FOR_ALL: 'FREE_FOR_ALL',
  TWO_HEADED_GIANT: 'TWO_HEADED_GIANT',
  TEAM_VS_TEAM: 'TEAM_VS_TEAM',
}

const FORMAT_FOR_CARDS: Record<'DRAFT' | 'SEALED', TournamentFormat> = {
  DRAFT: 'COMMANDER_DRAFT',
  SEALED: 'COMMANDER_SEALED',
}

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

/** A four-seat Commander lobby at the given table, ready for the host to press Start. */
function lobby(overrides: Partial<LobbySettings>, seats = 4): LobbyState {
  return {
    lobbyId: 'lobby-1',
    state: 'WAITING_FOR_PLAYERS',
    players: Array.from({ length: seats }, (_, i) => ({
      playerId: `p${i}`,
      playerName: `Player ${i}`,
      isHost: i === 0,
      isAi: false,
      isConnected: true,
      deckSubmitted: false,
    })),
    settings: settings(overrides),
    isHost: true,
    draftState: null,
    winstonDraftState: null,
    gridDraftState: null,
  } as unknown as LobbyState
}

function view(table: TableAxis, cards: CardsAxis): UnifiedLobbyView {
  return fromTournamentLobby(
    lobby({
      format: FORMAT_FOR_CARDS[cards.kind as 'DRAFT' | 'SEALED'],
      gameMode: GAME_MODE_FOR_TABLE[table],
    }),
    { aiEnabled: false, playerId: 'p0' },
  )
}

describe('Commander at a multiplayer table', () => {
  it('the wizard offers Free-for-All and Team vs. Team, and refuses only Two-Headed Giant', () => {
    for (const cards of COMMANDER_CARDS) {
      const byShape = new Map(shapeChoices('GROUP', cards).map((c) => [c.value, c.disabledReason]))

      expect(byShape.get('FREE_FOR_ALL'), JSON.stringify(cards)).toBeUndefined()
      expect(byShape.get('TEAM_VS_TEAM'), JSON.stringify(cards)).toBeUndefined()
      expect(byShape.get('BRACKET'), JSON.stringify(cards)).toBeUndefined()
      expect(byShape.get('TWO_HEADED_GIANT')).toBe(COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL)
    }
  })

  it('the lobby’s Table axis agrees with the wizard, table for table', () => {
    for (const cards of COMMANDER_CARDS) {
      // Asked from a bracket lobby, so every other table is a switch rather than the current value.
      const choices = tableChoices(view('ONE_V_ONE', cards))
      const availability = new Map(choices.map((c) => [c.value, c.availability]))

      for (const table of ['FREE_FOR_ALL', 'TEAM_VS_TEAM'] as const) {
        expect(availability.get(table)?.kind, `${table} / ${JSON.stringify(cards)}`).not.toBe('BLOCKED')
      }
      const twoHeaded = availability.get('TWO_HEADED_GIANT')
      expect(twoHeaded).toEqual({ kind: 'BLOCKED', reason: COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL })
    }
  })

  it('the Start button is enabled at a Commander pod and blocked only at a 2HG table', () => {
    for (const cards of COMMANDER_CARDS) {
      for (const table of COMMANDER_TABLES) {
        const action = view(table, cards).primaryAction
        expect(action?.disabled, `${table} / ${JSON.stringify(cards)}`).toBe(false)
      }
      const twoHeaded = view('TWO_HEADED_GIANT', cards).primaryAction
      expect(twoHeaded?.disabled).toBe(true)
      expect(twoHeaded?.reason).toBe(COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL)
    }
  })
})

describe('the pod life total', () => {
  it('overrides the host’s 1v1 tuning at every multiplayer table, and only there', () => {
    for (const preset of ['BRAWL', 'COMMANDER'] as const) {
      expect(effectiveCommanderPreset(preset, 'TOURNAMENT')).toBe(preset)
      for (const mode of ['FREE_FOR_ALL', 'TWO_HEADED_GIANT', 'TEAM_VS_TEAM'] as const) {
        expect(effectiveCommanderPreset(preset, mode)).toBe('POD')
      }
    }
  })

  it('is paper multiplayer Commander’s 40, mirroring CommanderPreset in the SDK', () => {
    expect(COMMANDER_PRESETS.POD).toMatchObject({ life: 40, damage: 21 })
    expect(COMMANDER_PRESETS.BRAWL).toMatchObject({ life: 25, damage: 16 })
    expect(COMMANDER_PRESETS.COMMANDER).toMatchObject({ life: 30, damage: 21 })
  })

  it('is what the lobby subtitle promises, not the 25 the host left selected', () => {
    const podSubtitle = fromTournamentLobby(
      lobby({ gameMode: 'FREE_FOR_ALL', commanderPreset: 'BRAWL' }),
      { aiEnabled: false, playerId: 'p0' },
    ).subtitle
    expect(podSubtitle).toContain('Pod 40 life')

    const bracketSubtitle = fromTournamentLobby(
      lobby({ gameMode: 'TOURNAMENT', commanderPreset: 'BRAWL' }),
      { aiEnabled: false, playerId: 'p0' },
    ).subtitle
    expect(bracketSubtitle).toContain('Brawl 25 life')
  })
})
