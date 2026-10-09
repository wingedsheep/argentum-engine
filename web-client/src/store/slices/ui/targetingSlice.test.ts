import { describe, it, expect, vi } from 'vitest'
import { create } from 'zustand'
import { createTargetingSlice, type TargetingSlice } from './targetingSlice'
import type { GameStore, TargetingState } from '../types'
import type { LegalActionInfo, GameAction, EntityId } from '@/types'

const id = (s: string): EntityId => s as unknown as EntityId

/**
 * Minimal store harness: the real targeting slice plus stubs for the pipeline
 * fields confirmTargeting reads. Everything else in GameStore is untouched.
 */
function makeStore() {
  const advancePipeline = vi.fn()
  const cancelPipeline = vi.fn()
  const action: GameAction = { type: 'CastSpell', playerId: 'p1', cardId: 'spell1' } as GameAction
  const store = create<GameStore>()((set, get, api) => ({
    ...createTargetingSlice(set, get, api),
    interactionEpoch: 'test-epoch',
    pipelineState: {
      interactionEpoch: 'test-epoch',
      actionInfo: { actionType: 'CastSpell', description: 'Cast Test', action } as LegalActionInfo,
      accumulatedAction: action,
      remainingPhases: [{ type: 'targeting' }],
    },
    gameState: {},
    advancePipeline,
    cancelPipeline,
  }) as unknown as GameStore)
  return { store, advancePipeline, cancelPipeline }
}

/** Two-requirement targeting state as pipelinePhases.enterPhase('targeting') builds it. */
function twoRequirementState(action: GameAction): TargetingState {
  return {
    action,
    validTargets: [id('a'), id('b'), id('c')],
    selectedTargets: [],
    minTargets: 1,
    maxTargets: 1,
    currentRequirementIndex: 0,
    allSelectedTargets: [],
    targetRequirements: [
      { index: 0, description: 'any target (takes 2 damage)', minTargets: 1, maxTargets: 1, validTargets: [id('a'), id('b'), id('c')] },
      { index: 1, description: 'any other target (takes 1 damage)', minTargets: 1, maxTargets: 1, validTargets: [id('a'), id('b'), id('c')], mustDifferFromEarlier: true },
    ],
    targetDescription: 'any target (takes 2 damage)',
    totalRequirements: 2,
  }
}

function targeting(store: ReturnType<typeof makeStore>['store']): TargetingState {
  const state = (store.getState() as unknown as TargetingSlice).targetingState
  if (!state) throw new Error('expected active targetingState')
  return state
}

describe('targetingSlice — multi-target back navigation', () => {
  it('confirming a requirement snapshots it for goBackTargeting', () => {
    const { store } = makeStore()
    const s = store.getState() as unknown as TargetingSlice
    s.startTargeting(twoRequirementState(store.getState().pipelineState!.accumulatedAction))

    s.addTarget(id('a'))
    s.confirmTargeting('test-epoch')

    const state = targeting(store)
    expect(state.currentRequirementIndex).toBe(1)
    // "Another target": the pick for requirement 0 leaves the pool
    expect(state.validTargets).toEqual([id('b'), id('c')])
    expect(state.previousRequirementStates).toHaveLength(1)
    expect(state.previousRequirementStates![0]!.selectedTargets).toEqual([id('a')])
  })

  it('keeps an earlier pick selectable when the next requirement is a separate "target"', () => {
    // Seeds of Strength: three "target creature" requirements may all choose the same creature.
    const { store, advancePipeline } = makeStore()
    const s = store.getState() as unknown as TargetingSlice
    const base = twoRequirementState(store.getState().pipelineState!.accumulatedAction)
    s.startTargeting({
      ...base,
      targetRequirements: base.targetRequirements!.map((r) => ({ ...r, mustDifferFromEarlier: false })),
    })

    s.addTarget(id('a'))
    s.confirmTargeting('test-epoch')
    expect(targeting(store).validTargets).toEqual([id('a'), id('b'), id('c')])

    store.getState().addTarget(id('a'))
    store.getState().confirmTargeting('test-epoch')
    expect(advancePipeline).toHaveBeenCalledWith({
      type: 'targeting',
      selectedTargets: [id('a'), id('a')],
      targetGroupCounts: [1, 1],
    })
  })

  it('goBackTargeting restores the previous requirement with its picks selected', () => {
    const { store } = makeStore()
    const s = store.getState() as unknown as TargetingSlice
    s.startTargeting(twoRequirementState(store.getState().pipelineState!.accumulatedAction))

    s.addTarget(id('a'))
    s.confirmTargeting('test-epoch')
    store.getState().goBackTargeting()

    const state = targeting(store)
    expect(state.currentRequirementIndex).toBe(0)
    expect(state.selectedTargets).toEqual([id('a')])
    expect(state.validTargets).toEqual([id('a'), id('b'), id('c')])
    expect(state.allSelectedTargets).toEqual([])
    expect(state.previousRequirementStates ?? []).toHaveLength(0)
  })

  it('re-confirming after a revised pick re-filters the next requirement', () => {
    const { store } = makeStore()
    const s = store.getState() as unknown as TargetingSlice
    s.startTargeting(twoRequirementState(store.getState().pipelineState!.accumulatedAction))

    s.addTarget(id('a'))
    s.confirmTargeting('test-epoch')
    store.getState().goBackTargeting()
    // maxTargets is 1, so picking 'b' replaces 'a'
    store.getState().addTarget(id('b'))
    store.getState().confirmTargeting('test-epoch')

    const state = targeting(store)
    expect(state.currentRequirementIndex).toBe(1)
    expect(state.validTargets).toEqual([id('a'), id('c')])
    expect(state.previousRequirementStates![0]!.selectedTargets).toEqual([id('b')])
  })

  it('goBackTargeting is a no-op on the first requirement', () => {
    const { store } = makeStore()
    const s = store.getState() as unknown as TargetingSlice
    s.startTargeting(twoRequirementState(store.getState().pipelineState!.accumulatedAction))

    s.addTarget(id('a'))
    store.getState().goBackTargeting()

    const state = targeting(store)
    expect(state.currentRequirementIndex).toBe(0)
    expect(state.selectedTargets).toEqual([id('a')])
  })

  it('confirming the last requirement submits all picks in requirement order', () => {
    const { store, advancePipeline } = makeStore()
    const s = store.getState() as unknown as TargetingSlice
    s.startTargeting(twoRequirementState(store.getState().pipelineState!.accumulatedAction))

    s.addTarget(id('a'))
    s.confirmTargeting('test-epoch')
    store.getState().addTarget(id('c'))
    store.getState().confirmTargeting('test-epoch')

    expect(store.getState().targetingState).toBeNull()
    expect(advancePipeline).toHaveBeenCalledWith({
      type: 'targeting',
      selectedTargets: [id('a'), id('c')],
      targetGroupCounts: [1, 1],
    })
  })
})


describe('dependent spell targets', () => {
  it('uses server branches and recomputes after changing the earlier choice', () => {
    const { store } = makeStore()
    const s = store.getState() as unknown as TargetingSlice
    const base = twoRequirementState(store.getState().pipelineState!.accumulatedAction)
    s.startTargeting({ ...base, targetRequirements: [base.targetRequirements![0]!, {
      ...base.targetRequirements![1]!, mustDifferFromEarlier: false,
      validTargetsByPrefix: { a: [id('b')], c: [id('a')] },
    }] })
    s.addTarget(id('a'))
    s.confirmTargeting('test-epoch')
    expect(targeting(store).validTargets).toEqual([id('b')])
    store.getState().goBackTargeting()
    store.getState().removeTarget(id('a'))
    store.getState().addTarget(id('c'))
    store.getState().confirmTargeting('test-epoch')
    expect(targeting(store).validTargets).toEqual([id('a')])
  })
})
