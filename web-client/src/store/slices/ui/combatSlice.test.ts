import { describe, it, expect } from 'vitest'
import { create } from 'zustand'
import { createCombatSlice, canBlockerBlock } from './combatSlice'
import type { GameStore, CombatState } from '../types'
import type { EntityId } from '@/types'

const id = (s: string): EntityId => s as unknown as EntityId

function blockersState(over: Partial<CombatState> = {}): CombatState {
  return {
    mode: 'declareBlockers',
    actingSeat: id('p2'),
    stickyDefenderId: null,
    selectedAttackers: [],
    attackerTargets: {},
    validAttackTargets: [],
    blockerAssignments: {},
    validCreatures: [id('bears'), id('drake')],
    mandatoryAttackers: [],
    attackingCreatures: [id('flier'), id('ground')],
    mustBeBlockedAttackers: [],
    blockerMaxBlockCounts: {},
    validBlockersByAttacker: {},
    attackerMinBlockers: {},
    bands: [],
    ...over,
  }
}

function makeStore(combatState: CombatState) {
  return create<GameStore>()((set, get, api) => ({
    ...createCombatSlice(set, get, api),
    combatState,
  }) as unknown as GameStore)
}

/**
 * Blocker↔attacker pairing is the server's call (`validBlockersByAttacker` on the DeclareBlockers
 * legal action). The client only reads that list — it has no evasion rules of its own.
 */
describe('canBlockerBlock', () => {
  it('reads the server list: only the listed blockers may block an attacker', () => {
    const state = blockersState({
      validBlockersByAttacker: { [id('flier')]: [id('drake')], [id('ground')]: [id('bears'), id('drake')] },
    })
    expect(canBlockerBlock(state, id('drake'), id('flier'))).toBe(true)
    expect(canBlockerBlock(state, id('bears'), id('flier'))).toBe(false)
    expect(canBlockerBlock(state, id('bears'), id('ground'))).toBe(true)
  })

  it('an attacker absent from the list has no legal blocker', () => {
    const state = blockersState({ validBlockersByAttacker: { [id('ground')]: [id('bears')] } })
    expect(canBlockerBlock(state, id('drake'), id('flier'))).toBe(false)
  })

  it('with no list at all (older server) every pairing is allowed — the server rules on Confirm', () => {
    expect(canBlockerBlock(blockersState(), id('bears'), id('flier'))).toBe(true)
  })
})

describe('assignBlocker', () => {
  it('refuses a pairing the server did not list', () => {
    const store = makeStore(blockersState({
      validBlockersByAttacker: { [id('flier')]: [id('drake')], [id('ground')]: [id('bears'), id('drake')] },
    }))
    store.getState().assignBlocker(id('bears'), id('flier'))
    expect(store.getState().combatState?.blockerAssignments).toEqual({})
    store.getState().assignBlocker(id('bears'), id('ground'))
    expect(store.getState().combatState?.blockerAssignments).toEqual({ bears: ['ground'] })
  })

  it('still honours the per-blocker max-block cap', () => {
    const store = makeStore(blockersState({
      validBlockersByAttacker: { [id('flier')]: [id('drake')], [id('ground')]: [id('drake')] },
    }))
    store.getState().assignBlocker(id('drake'), id('flier'))
    store.getState().assignBlocker(id('drake'), id('ground'))
    expect(store.getState().combatState?.blockerAssignments).toEqual({ drake: ['flier'] })
  })
})
