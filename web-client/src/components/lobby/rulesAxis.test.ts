/**
 * Rules is an axis, and there is one statement of the rule that limits it.
 *
 * "Does this game run Commander?" used to be answered from three unrelated fields, and the copies
 * could not see each other: `isCommanderLimited` looked only at the pack shape, so premade Commander
 * in a pod was blocked by a rule that could not observe it, while `legalityOptionsForTable` expressed
 * the same rule by editing a dropdown. What this file pins is the shape that replaced them — Rules is
 * independent of Cards, only Two-Headed Giant refuses it, every surface refuses it for the same
 * reason, and a lobby from before the axis still reads correctly.
 */
import { describe, expect, it } from 'vitest'
import {
  COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL,
  RULES_VALUES,
  TABLE_VALUES,
  axesFromLobbySettings,
  axesFromQuickGameLobby,
  rulesForCards,
  rulesFromLobbySettings,
  rulesTableBlock,
  type CardsAxis,
  type TableAxis,
} from './axes'
import { rulesChoices, tableChoices } from './axisChoices'
import { fromTournamentLobby, type UnifiedLobbyView } from './lobbyViewModel'
import { resolveLaunch, shapeChoices, type Selection } from './modeMatrix'
import type { GameRules, LobbyGameMode, LobbySettings, TournamentFormat } from '@/types'
import type { LobbyState } from '@/store/slices/types'

const GAME_MODE_FOR_TABLE: Record<TableAxis, LobbyGameMode> = {
  ONE_V_ONE: 'TOURNAMENT',
  FREE_FOR_ALL: 'FREE_FOR_ALL',
  TWO_HEADED_GIANT: 'TWO_HEADED_GIANT',
  TEAM_VS_TEAM: 'TEAM_VS_TEAM',
}

function settings(overrides: Partial<LobbySettings>): LobbySettings {
  return {
    setCodes: ['ECL'],
    setNames: ['Edge of Eternities'],
    availableSets: [],
    format: 'PREMADE_DECKS',
    rules: 'STANDARD',
    boosterCount: 0,
    boosterDistribution: {},
    maxPlayers: 4,
    pickTimeSeconds: 45,
    picksPerRound: 1,
    gamesPerMatch: 1,
    isPublic: false,
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

/** A four-seat lobby with every deck in, so the Start button's only possible complaint is the axes. */
function lobby(overrides: Partial<LobbySettings>): LobbyState {
  return {
    lobbyId: 'lobby-1',
    state: 'WAITING_FOR_PLAYERS',
    players: Array.from({ length: 4 }, (_, i) => ({
      playerId: `p${i}`,
      playerName: `Player ${i}`,
      isHost: i === 0,
      isAi: false,
      isConnected: true,
      deckSubmitted: true,
    })),
    settings: settings(overrides),
    isHost: true,
    draftState: null,
    winstonDraftState: null,
    gridDraftState: null,
  } as unknown as LobbyState
}

function view(overrides: Partial<LobbySettings>): UnifiedLobbyView {
  return fromTournamentLobby(lobby(overrides), { aiEnabled: false, playerId: 'p0' })
}

/** The three ways a lobby can get its cards, so "Rules is independent of Cards" can be walked. */
const CARD_SOURCES: ReadonlyArray<{ label: string; format: TournamentFormat }> = [
  { label: 'a brought deck', format: 'PREMADE_DECKS' },
  { label: 'a sealed pool', format: 'SEALED' },
  { label: 'a draft', format: 'DRAFT' },
]

describe('Rules is independent of Cards', () => {
  it('runs Commander over a brought deck, a sealed pool or a draft', () => {
    for (const source of CARD_SOURCES) {
      const axes = axesFromLobbySettings(settings({ format: source.format, rules: 'COMMANDER' }))

      expect(axes.rules, source.label).toBe('COMMANDER')
      // …and the Cards axis still reports where the cards came from, unchanged by the rules.
      expect(axes.cards.kind, source.label).toBe(
        source.format === 'PREMADE_DECKS' ? 'BRING_A_DECK' : source.format,
      )
    }
  })

  it('leaves a Commander pack shape free to run Standard rules', () => {
    // The pack shape only *defaults* the axis server-side. Nothing downstream may re-derive it, or
    // the host's explicit choice would be silently overruled — which is what the old disjunction did.
    expect(rulesFromLobbySettings(settings({ format: 'COMMANDER_DRAFT', rules: 'STANDARD' })))
      .toBe('STANDARD')
  })

  it('lets the host switch Rules directly on a lobby of any Cards value', () => {
    for (const source of CARD_SOURCES) {
      const byValue = new Map(
        rulesChoices(view({ format: source.format })).map((c) => [c.value, c.availability]),
      )
      for (const rules of RULES_VALUES) {
        expect(byValue.get(rules)?.kind, `${rules} / ${source.label}`).toBe('DIRECT')
      }
    }
  })
})

describe('Commander at each table', () => {
  it('is refused only at Two-Headed Giant', () => {
    for (const table of TABLE_VALUES) {
      const blocked = rulesTableBlock('COMMANDER', table)
      expect(blocked, table).toBe(table === 'TWO_HEADED_GIANT' ? COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL : null)
      // Standard rules sit at every table, so the predicate is about Commander and not about 2HG.
      expect(rulesTableBlock('STANDARD', table), table).toBeNull()
    }
  })

  it('is refused there by the Rules row, the Table row and the Start button alike', () => {
    for (const table of TABLE_VALUES) {
      const commanderPod = view({ rules: 'COMMANDER', gameMode: GAME_MODE_FOR_TABLE[table] })
      const expected = table === 'TWO_HEADED_GIANT' ? COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL : null

      // Start button.
      expect(commanderPod.primaryAction?.reason ?? null, table).toBe(expected)
      expect(commanderPod.primaryAction?.disabled, table).toBe(expected !== null)

      // Rules row, asked of the lobby that is already there: a contradiction shows on the value it
      // is sitting on, so the host can read why and pick the other one.
      const commanderChoice = rulesChoices(commanderPod).find((c) => c.value === 'COMMANDER')
      expect(
        commanderChoice?.availability.kind === 'BLOCKED' ? commanderChoice.availability.reason : null,
        table,
      ).toBe(expected)

      // Table row, asked from a Commander bracket: switching to this table.
      const fromBracket = tableChoices(view({ rules: 'COMMANDER' })).find((c) => c.value === table)
      expect(
        fromBracket?.availability.kind === 'BLOCKED' ? fromBracket.availability.reason : null,
        table,
      ).toBe(table === 'TWO_HEADED_GIANT' ? expected : null)
    }
  })

  it('is refused for a brought deck too, which the old pack-shape check could not see', () => {
    // The bug this design removes: a premade Commander lobby reached Commander through `deckFormat`,
    // which `isCommanderLimited` structurally could not observe — so the client offered 2HG and the
    // server refused it at Start.
    const premadeCommander = view({
      format: 'PREMADE_DECKS',
      deckFormat: 'COMMANDER',
      rules: 'COMMANDER',
      gameMode: 'TWO_HEADED_GIANT',
    })
    expect(premadeCommander.primaryAction?.reason).toBe(COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL)
  })
})

describe('the wizard and the lobby agree', () => {
  const commanderCards: readonly CardsAxis[] = [
    { kind: 'DRAFT', shape: 'COMMANDER' },
    { kind: 'SEALED', shape: 'COMMANDER' },
  ]

  it('through the one predicate, on the rules the chosen Cards value implies', () => {
    for (const cards of commanderCards) {
      expect(rulesForCards(cards), JSON.stringify(cards)).toBe('COMMANDER')

      const byShape = new Map(shapeChoices('GROUP', cards).map((c) => [c.value, c.disabledReason]))
      for (const shape of ['FREE_FOR_ALL', 'TEAM_VS_TEAM', 'BRACKET'] as const) {
        expect(byShape.get(shape), `${shape} / ${JSON.stringify(cards)}`).toBeUndefined()
      }
      expect(byShape.get('TWO_HEADED_GIANT')).toBe(COMMANDER_NEEDS_ITS_OWN_LIFE_TOTAL)
    }
  })

  it('and the wizard hands the inferred rules to the lobby it creates', () => {
    // Rules is not a fourth wizard question — it is derived from Cards and then editable in the
    // lobby. The launch spec has to carry it, or the lobby would come back Standard and disagree
    // with the reasoning the wizard just did.
    const selection = (cards: CardsAxis): Selection =>
      ({ roster: 'GROUP', cards, shape: 'FREE_FOR_ALL' })

    for (const cards of commanderCards) {
      const spec = resolveLaunch(selection(cards))
      expect(spec.kind === 'TOURNAMENT' ? spec.rules : null, JSON.stringify(cards)).toBe('COMMANDER')
    }
    const standard = resolveLaunch(selection({ kind: 'SEALED', shape: 'STANDARD' }))
    expect(standard.kind === 'TOURNAMENT' ? standard.rules : null).toBe('STANDARD')
  })
})

describe('a lobby from before the Rules axis', () => {
  it('infers Commander from a Commander pack shape', () => {
    for (const format of ['COMMANDER_DRAFT', 'COMMANDER_SEALED'] as const) {
      const legacy = settings({ format }) as LobbySettings & { rules?: GameRules }
      delete legacy.rules

      expect(rulesFromLobbySettings(legacy), format).toBe('COMMANDER')
    }
  })

  it('infers Commander from commander-shaped deck legality — the premade path', () => {
    for (const deckFormat of ['COMMANDER', 'BRAWL', 'STANDARD_BRAWL'] as const) {
      const legacy = settings({ format: 'PREMADE_DECKS', deckFormat }) as LobbySettings & { rules?: GameRules }
      delete legacy.rules

      expect(rulesFromLobbySettings(legacy), deckFormat).toBe('COMMANDER')
    }
  })

  it('infers Standard when neither said Commander', () => {
    const legacy = settings({ format: 'SEALED', deckFormat: 'STANDARD' }) as LobbySettings & { rules?: GameRules }
    delete legacy.rules

    expect(rulesFromLobbySettings(legacy)).toBe('STANDARD')
  })

  it('infers the same way for a quick lobby, whose rules follow its deck legality', () => {
    expect(axesFromQuickGameLobby({ format: 'COMMANDER' }).rules).toBe('COMMANDER')
    expect(axesFromQuickGameLobby({ format: 'STANDARD' }).rules).toBe('STANDARD')
    // An explicit field from a current server always wins over the inference.
    expect(axesFromQuickGameLobby({ format: 'COMMANDER', rules: 'STANDARD' }).rules).toBe('STANDARD')
  })
})
