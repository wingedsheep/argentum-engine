import { describe, it, expect } from 'vitest'
import { create } from 'zustand'
import { createSelectionSlice } from './selectionSlice'
import type { GameStore, EntityId } from '../types'
const id = (s: string) => s as EntityId

describe('total-capped decisions', () => {
  it('rejects a pick that would push the total over the cap, and accepts it after a deselection', () => {
    const store = create<GameStore>()((set, get, api) => ({ ...createSelectionSlice(set, get, api) }) as unknown as GameStore)
    store.getState().startDecisionSelection({ decisionId: 'kairi', prompt: 'Total mana value 6 or less', minSelections: 0,
      maxSelections: 3, validOptions: [id('bears'), id('giant'), id('angel')], selectedOptions: [],
      totalCaps: [{ label: 'mana value', cap: 6, values: { [id('bears')]: 2, [id('giant')]: 4, [id('angel')]: 3 } }] })
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
  it('holds a total power cap the same way, naming the measure in the warning', () => {
    const store = create<GameStore>()((set, get, api) => ({ ...createSelectionSlice(set, get, api) }) as unknown as GameStore)
    store.getState().startDecisionSelection({ decisionId: 'nethroi', prompt: 'Total power 10 or less', minSelections: 0,
      maxSelections: 3, validOptions: [id('wurm'), id('giant'), id('elf')], selectedOptions: [],
      totalCaps: [{ label: 'power', cap: 10, values: { [id('wurm')]: 6, [id('giant')]: 5, [id('elf')]: 1 } }] })
    store.getState().toggleDecisionSelection(id('wurm'))
    store.getState().toggleDecisionSelection(id('giant'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('wurm')])
    expect(store.getState().decisionSelectionState!.warning).toContain('total power')
    store.getState().toggleDecisionSelection(id('elf'))
    expect(store.getState().decisionSelectionState!.selectedOptions).toEqual([id('wurm'), id('elf')])
  })
})
