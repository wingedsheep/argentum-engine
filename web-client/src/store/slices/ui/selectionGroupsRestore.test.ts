import { describe, it, expect } from 'vitest'
import { create } from 'zustand'
import { createSelectionSlice } from './selectionSlice'
import type { GameStore, EntityId } from '../types'
const id = (s: string) => s as EntityId

describe('restored server-group decisions', () => {
  it('restores a constrained pool and never accepts an incompatible pick', () => {
    const store = create<GameStore>()((set, get, api) => ({
      ...createSelectionSlice(set, get, api),
    }) as unknown as GameStore)
    store.getState().startDecisionSelection({ decisionId: 'decision', prompt: 'Equal toughness', minSelections: 0, maxSelections: 3,
      validOptions: [id('a'), id('b'), id('c')], selectedOptions: [id('a')], selectionGroups: [[id('a'), id('b')], [id('c')]] })
    expect(store.getState().decisionSelectionState!.validOptions).toEqual([id('a'), id('b')])
    store.getState().toggleDecisionSelection(id('c'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a')])
    store.getState().toggleDecisionSelection(id('a'))
    expect(store.getState().decisionSelectionState!.validOptions).toEqual([id('a'), id('b'), id('c')])
    store.getState().toggleDecisionSelection(id('c'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('c')])
  })
  it('restores owner groups, rejects a second pick in one group, and reopens it after deselection', () => {
    const store = create<GameStore>()((set, get, api) => ({ ...createSelectionSlice(set, get, api) }) as unknown as GameStore)
    store.getState().startDecisionSelection({ decisionId: 'owners', prompt: 'One per owner', minSelections: 2, maxSelections: 2,
      validOptions: [id('a'), id('b'), id('c')], selectedOptions: [id('a')], exclusiveGroups: [[id('a'), id('b')], [id('c')]] })
    expect(store.getState().decisionSelectionState!.validOptions).toEqual([id('a'), id('c')])
    store.getState().toggleDecisionSelection(id('b'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a')])
    store.getState().toggleDecisionSelection(id('c'))
    expect(store.getState().decisionSelectionState!.validOptions).toEqual([id('a'), id('c')])
    store.getState().toggleDecisionSelection(id('a'))
    expect(store.getState().decisionSelectionState!.validOptions).toEqual([id('a'), id('b'), id('c')])
    store.getState().toggleDecisionSelection(id('b'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('c'), id('b')])
  })

})
