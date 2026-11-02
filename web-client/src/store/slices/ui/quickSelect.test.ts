import { describe, it, expect } from 'vitest'
import { create } from 'zustand'
import { createSelectionSlice } from './selectionSlice'
import type { GameStore, EntityId } from '../types'
const id = (s: string) => s as EntityId

function selectionStore() {
  const store = create<GameStore>()((set, get, api) => ({
    ...createSelectionSlice(set, get, api),
  }) as unknown as GameStore)
  store.getState().startDecisionSelection({ decisionId: 'proliferate', prompt: 'Proliferate',
    validOptions: [id('a'), id('b'), id('c')], selectedOptions: [], minSelections: 0, maxSelections: 3 })
  return store
}

describe('board selection quick picks', () => {
  it('replaces the whole pick, and a later click still toggles one option', () => {
    const store = selectionStore()
    store.getState().toggleDecisionSelection(id('c'))
    store.getState().setDecisionSelection([id('a'), id('b')])
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a'), id('b')])
    store.getState().toggleDecisionSelection(id('b'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a')])
    store.getState().setDecisionSelection([])
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([])
  })

  it('drops ids outside the options and duplicates', () => {
    const store = selectionStore()
    store.getState().setDecisionSelection([id('a'), id('a'), id('zz'), id('c')])
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('a'), id('c')])
  })
})
