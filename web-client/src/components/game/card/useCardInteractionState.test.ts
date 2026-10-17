import { describe, expect, it, vi } from 'vitest'
import { shallow } from 'zustand/shallow'
import type { EntityId } from '@/types'

// The module pulls in gameStore.ts, whose gameplay slice reads localStorage at init — a browser
// global the Node vitest environment doesn't provide (see store/selectors.test.ts).
vi.stubGlobal('localStorage', {
  getItem: () => null,
  setItem: () => {},
  removeItem: () => {},
})

const { selectCardInteraction } = await import('./useCardInteractionState')
type GameStore = Parameters<typeof selectCardInteraction>[0]

const id = (s: string) => s as EntityId
/** Enough ids to push a list past the short-list scan and into the Set cache. */
const many = (prefix: string, n = 40) => Array.from({ length: n }, (_, i) => id(`${prefix}${i}`))

/** A store with no interaction in progress; tests spread in the slice they exercise. */
function store(overrides: Record<string, unknown> = {}): GameStore {
  return {
    gameState: { hotseat: false, combat: null },
    spectatingState: null,
    targetingState: null,
    pendingDecision: null,
    decisionSelectionState: null,
    distributeState: null,
    counterDistributionState: null,
    manaSelectionState: null,
    tapForPowerSelectionState: null,
    convokeSelectionState: null,
    tapForGenericSelectionState: null,
    harmonizeSelectionState: null,
    combatState: null,
    draggingAttackerId: null,
    draggingBlockerId: null,
    ...overrides,
  } as unknown as GameStore
}

const input = (cardId: string, extra: Partial<{ isCreature: boolean; isOpponentCard: boolean }> = {}) => ({
  cardId: id(cardId),
  isCreature: true,
  isOpponentCard: false,
  ...extra,
})

function combat(overrides: Record<string, unknown>) {
  return {
    mode: 'declareAttackers',
    selectedAttackers: [],
    attackerTargets: {},
    validAttackTargets: [],
    attackTargetsByAttacker: {},
    blockerAssignments: {},
    validCreatures: [],
    attackingCreatures: [],
    mustBeBlockedAttackers: [],
    bands: [],
    ...overrides,
  }
}

describe('selectCardInteraction', () => {
  it('answers targeting membership for long lists, not just short ones', () => {
    const valid = many('t')
    const s = store({ targetingState: { action: { type: 'CastSpell', cardId: id('bolt') }, validTargets: valid, selectedTargets: [valid[30]] } })
    expect(selectCardInteraction(s, input('t30'))).toMatchObject({ isInTargetingMode: true, isValidTarget: true, isSelectedTarget: true, isBeingCast: false })
    expect(selectCardInteraction(s, input('t5'))).toMatchObject({ isValidTarget: true, isSelectedTarget: false })
    expect(selectCardInteraction(s, input('elsewhere'))).toMatchObject({ isValidTarget: false, isSelectedTarget: false })
    expect(selectCardInteraction(s, input('bolt')).isBeingCast).toBe(true)
  })

  it('leaves a card whose answers did not change shallow-equal, so it is not re-rendered', () => {
    const valid = many('t')
    const before = store({ targetingState: { action: { type: 'PassPriority' }, validTargets: valid, selectedTargets: [valid[1]] } })
    const after = store({ targetingState: { action: { type: 'PassPriority' }, validTargets: valid, selectedTargets: [valid[2]] } })
    expect(shallow(selectCardInteraction(before, input('t7')), selectCardInteraction(after, input('t7')))).toBe(true)
    expect(shallow(selectCardInteraction(before, input('t2')), selectCardInteraction(after, input('t2')))).toBe(false)
  })

  it('keys attacker validity off the row, except in hotseat', () => {
    const c = combat({ validCreatures: [id('bear')] })
    expect(selectCardInteraction(store({ combatState: c }), input('bear')).isValidAttacker).toBe(true)
    expect(selectCardInteraction(store({ combatState: c }), input('bear', { isOpponentCard: true })).isValidAttacker).toBe(false)
    const hotseat = store({ combatState: c, gameState: { hotseat: true, combat: null } })
    expect(selectCardInteraction(hotseat, input('bear', { isOpponentCard: true })).isValidAttacker).toBe(true)
    expect(selectCardInteraction(hotseat, input('bear', { isCreature: false })).isValidAttacker).toBe(false)
  })

  it('reads hotseat and server combat from the spectated game when spectating', () => {
    const c = combat({ validCreatures: [id('bear')] })
    const s = store({
      combatState: c,
      gameState: { hotseat: false, combat: null },
      spectatingState: { gameState: { hotseat: true, combat: null } },
    })
    expect(selectCardInteraction(s, input('bear', { isOpponentCard: true })).isValidAttacker).toBe(true)
  })

  it('numbers bands from the local declaration first, then from the server by first appearance', () => {
    const local = store({ combatState: combat({ bands: [[id('a'), id('b')], [id('c')]] }) })
    expect(selectCardInteraction(local, input('b')).bandIndex).toBe(0)
    expect(selectCardInteraction(local, input('c')).bandIndex).toBe(1)
    expect(selectCardInteraction(local, input('z')).bandIndex).toBe(-1)

    const attackers = [
      { creatureId: id('x'), bandId: 'band-P' },
      { creatureId: id('y'), bandId: null },
      { creatureId: id('w'), bandId: 'band-Q' },
      { creatureId: id('v'), bandId: 'band-P' },
    ]
    const server = store({ gameState: { hotseat: false, combat: { attackers } } })
    expect(selectCardInteraction(server, input('x')).bandIndex).toBe(0)
    expect(selectCardInteraction(server, input('v')).bandIndex).toBe(0)
    expect(selectCardInteraction(server, input('w')).bandIndex).toBe(1)
    expect(selectCardInteraction(server, input('y')).bandIndex).toBe(-1)
  })

  it('dims a multiplayer attacker that attacks someone else while you block', () => {
    const attackers = [{ creatureId: id('mine') }, { creatureId: id('theirs') }]
    const s = store({
      combatState: combat({ mode: 'declareBlockers', attackingCreatures: [id('mine')], validCreatures: [id('wall')] }),
      gameState: { hotseat: false, combat: { attackers } },
    })
    expect(selectCardInteraction(s, input('theirs', { isOpponentCard: true }))).toMatchObject({ isBystanderAttacker: true, isAttackingInBlockerMode: false })
    expect(selectCardInteraction(s, input('mine', { isOpponentCard: true }))).toMatchObject({ isBystanderAttacker: false, isAttackingInBlockerMode: true })
    expect(selectCardInteraction(s, input('wall')).isValidBlocker).toBe(true)
  })

  it('fills distribute totals in only for a distribute target', () => {
    const s = store({ distributeState: { targets: [id('a'), id('b')], distribution: { a: 2, b: 1 }, totalAmount: 5, minPerTarget: 1, maxPerTarget: { a: 2 } } })
    expect(selectCardInteraction(s, input('a'))).toMatchObject({
      isDistributeTarget: true, distributeAllocated: 2, distributeRemaining: 2, distributeMinPerTarget: 1, distributeMaxForCard: 2,
    })
    expect(selectCardInteraction(s, input('c'))).toMatchObject({ isDistributeTarget: false, distributeAllocated: 0, distributeRemaining: 0 })
  })

  it('answers the entity-object lists used by the payment modes', () => {
    const creatures = many('k').map((entityId) => ({ entityId }))
    const s = store({
      convokeSelectionState: { validCreatures: creatures, selectedCreatures: [{ entityId: id('k20'), payingColor: null }] },
      tapForPowerSelectionState: { validCreatures: creatures, selectedCreatures: [id('k3')] },
    })
    expect(selectCardInteraction(s, input('k20'))).toMatchObject({ isValidConvokeCreature: true, isSelectedConvokeCreature: true, isValidTapForPowerCreature: true, isSelectedTapForPowerCreature: false })
    expect(selectCardInteraction(s, input('nope'))).toMatchObject({ isValidConvokeCreature: false, isValidTapForPowerCreature: false })
  })
})

it('equality-group decisions highlight the narrowed selection pool', () => {
  const state = store({
    pendingDecision: { type: 'ChooseTargetsDecision', targetRequirements: [{ index: 0 }], legalTargets: { 0: [id('a'), id('b')] }, context: {} },
    decisionSelectionState: { validOptions: [id('a')], selectedOptions: [id('a')] },
  })
  const result = selectCardInteraction(state, input('b'))
  expect(result.isValidDecisionTarget).toBe(false)
  expect(result.isValidDecisionSelection).toBe(false)
})
