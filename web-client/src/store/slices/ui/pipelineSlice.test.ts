import { describe, it, expect, vi, beforeEach } from 'vitest'
import { create } from 'zustand'
import { createPipelineSlice } from './pipelineSlice'
import type { GameStore } from '../types'
import type { CostPreviewMessage, EntityId, GameAction } from '@/types'

const id = (s: string): EntityId => s as unknown as EntityId

const sent: unknown[] = []
vi.mock('../shared', () => ({
  getWebSocket: () => ({ send: (m: unknown) => sent.push(m) }),
}))

const draft: GameAction = { type: 'CastSpell', playerId: 'p1', cardId: 'spell' } as GameAction

function makeStore(extra: Record<string, unknown> = {}) {
  return create<GameStore>()((set, get, api) => ({
    ...createPipelineSlice(set, get, api),
    gameState: {},
    ...extra,
  }) as unknown as GameStore)
}

function reply(requestId: string, over: Partial<CostPreviewMessage> = {}): CostPreviewMessage {
  return {
    type: 'costPreview',
    requestId,
    manaCostString: '{2}{G}',
    genericRemaining: 2,
    xValue: 0,
    affordable: true,
    ...over,
  }
}

beforeEach(() => { sent.length = 0 })

/**
 * The cost preview is the one place the client learns what a partial selection costs. These pin
 * down the plumbing the HUDs rely on: one request per draft, only the newest reply counts, and the
 * reply is what tightens the delve / tap caps and prices the manual mana picker.
 */
describe('requestCostPreview / receiveCostPreview', () => {
  it('sends a previewCost message and marks the preview pending', () => {
    const store = makeStore()
    store.getState().requestCostPreview(draft)
    const cp = store.getState().costPreview!
    expect(cp.pending).toBe(true)
    expect(cp.draft).toBe(draft)
    expect(sent).toEqual([{ type: 'previewCost', action: draft, requestId: cp.requestId }])
  })

  it('adopts the reply to the latest request and drops a stale one', () => {
    const store = makeStore()
    store.getState().requestCostPreview(draft)
    const first = store.getState().costPreview!.requestId
    store.getState().requestCostPreview(draft)
    const second = store.getState().costPreview!.requestId

    store.getState().receiveCostPreview(reply(first, { manaCostString: '{9}' }))
    expect(store.getState().costPreview!.pending).toBe(true)
    expect(store.getState().costPreview!.preview).toBeNull()

    store.getState().receiveCostPreview(reply(second))
    expect(store.getState().costPreview!.pending).toBe(false)
    expect(store.getState().costPreview!.preview?.manaCostString).toBe('{2}{G}')
  })

  it('keeps the last answer up while the next request is in flight', () => {
    const store = makeStore()
    store.getState().requestCostPreview(draft)
    store.getState().receiveCostPreview(reply(store.getState().costPreview!.requestId))
    store.getState().requestCostPreview(draft)
    expect(store.getState().costPreview!.pending).toBe(true)
    expect(store.getState().costPreview!.preview?.manaCostString).toBe('{2}{G}')
  })

  it("tightens the delve cap to the server's remaining generic plus what's already exiled", () => {
    const store = makeStore({
      delveSelectionState: {
        actionInfo: {}, cardName: 'x', manaCost: '{6}{B}',
        selectedCards: [id('g1')],
        validCards: [{ entityId: id('g1') }, { entityId: id('g2') }, { entityId: id('g3') }, { entityId: id('g4') }],
        maxDelve: 4, minDelveNeeded: 0,
      },
    })
    store.getState().requestCostPreview(draft)
    // One card exiled, two generic left → at most one more exile buys anything → cap 3 of 4.
    store.getState().receiveCostPreview(reply(store.getState().costPreview!.requestId, { genericRemaining: 2 }))
    expect(store.getState().delveSelectionState?.maxDelve).toBe(3)
  })

  it('tightens the improvise cap only when it follows the generic (not a fixed waterbend {N})', () => {
    const permanents = [{ entityId: id('a1') }, { entityId: id('a2') }, { entityId: id('a3') }]
    const following = makeStore({
      tapForGenericSelectionState: {
        actionInfo: {}, cardName: 'x', manaCost: '{4}{U}', selectedPermanents: [],
        validPermanents: permanents, maxTaps: 3, capFollowsGeneric: true, label: 'improvise',
      },
    })
    following.getState().requestCostPreview(draft)
    following.getState().receiveCostPreview(reply(following.getState().costPreview!.requestId, { genericRemaining: 1 }))
    expect(following.getState().tapForGenericSelectionState?.maxTaps).toBe(1)

    const fixed = makeStore({
      tapForGenericSelectionState: {
        actionInfo: {}, cardName: 'x', manaCost: '{4}{U}', selectedPermanents: [],
        validPermanents: permanents, maxTaps: 2, capFollowsGeneric: false, label: 'waterbend',
      },
    })
    fixed.getState().requestCostPreview(draft)
    fixed.getState().receiveCostPreview(reply(fixed.getState().costPreview!.requestId, { genericRemaining: 1 }))
    expect(fixed.getState().tapForGenericSelectionState?.maxTaps).toBe(2)
  })

  it("prices the manual mana picker and pre-selects the engine's auto-tap, unless the player already picked", () => {
    const base = {
      action: draft, actionInfo: {}, validSources: [id('l1'), id('l2'), id('l3')],
      manaCost: '{3}{G}', sourceColors: {}, sourceManaAmounts: {}, phyrexianLifePipIndices: [],
    }
    const fresh = makeStore({
      manaSelectionState: { ...base, selectedSources: [], previewPending: true, userEdited: false },
    })
    fresh.getState().requestCostPreview(draft)
    fresh.getState().receiveCostPreview(
      reply(fresh.getState().costPreview!.requestId, { autoTapPreview: [id('l1'), id('l3'), id('gone')] }),
    )
    expect(fresh.getState().manaSelectionState).toMatchObject({
      manaCost: '{2}{G}', previewPending: false, selectedSources: [id('l1'), id('l3')],
    })

    const edited = makeStore({
      manaSelectionState: { ...base, selectedSources: [id('l2')], previewPending: true, userEdited: true },
    })
    edited.getState().requestCostPreview(draft)
    edited.getState().receiveCostPreview(
      reply(edited.getState().costPreview!.requestId, { autoTapPreview: [id('l1')] }),
    )
    expect(edited.getState().manaSelectionState).toMatchObject({
      manaCost: '{2}{G}', previewPending: false, selectedSources: [id('l2')],
    })
  })

  it('cancelling the pipeline clears the preview', () => {
    const store = makeStore()
    store.getState().requestCostPreview(draft)
    store.getState().cancelPipeline()
    expect(store.getState().costPreview).toBeNull()
  })
})
