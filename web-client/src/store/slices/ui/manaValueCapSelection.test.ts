import { describe, it, expect } from 'vitest'
import { create } from 'zustand'
import { createSelectionSlice } from './selectionSlice'
import type { GameStore, EntityId } from '../types'
const id = (s: string) => s as EntityId

describe('total mana value capped decisions', () => {
  it('rejects a pick that would push the total over the cap, and accepts it after a deselection', () => {
    const store = create<GameStore>()((set, get, api) => ({ ...createSelectionSlice(set, get, api) }) as unknown as GameStore)
    store.getState().startDecisionSelection({ decisionId: 'kairi', prompt: 'Total mana value 6 or less', minSelections: 0,
      maxSelections: 3, validOptions: [id('bears'), id('giant'), id('angel')], selectedOptions: [],
      manaValueCap: { cap: 6, values: { [id('bears')]: 2, [id('giant')]: 4, [id('angel')]: 3 } } })
    store.getState().toggleDecisionSelection(id('bears'))
    store.getState().toggleDecisionSelection(id('giant'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('bears'), id('giant')])
    store.getState().toggleDecisionSelection(id('angel'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('bears'), id('giant')])
    expect(store.getState().decisionSelectionState!.warning).toContain('total mana value')
    store.getState().toggleDecisionSelection(id('giant'))
    store.getState().toggleDecisionSelection(id('angel'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('bears'), id('angel')])
    expect(store.getState().decisionSelectionState!.warning).toBeNull()
  })
})
