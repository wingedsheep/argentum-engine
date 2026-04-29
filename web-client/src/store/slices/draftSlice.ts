/**
 * Draft slice - handles sealed deck building and drafting logic.
 */
import type { SliceCreator, DeckBuildingState } from './types'
import {
  createCreateSealedGameMessage,
  createJoinSealedGameMessage,
  createSubmitSealedDeckMessage,
  createUnsubmitDeckMessage,
  createMakePickMessage,
  createWinstonTakePileMessage,
  createWinstonSkipPileMessage,
  createGridDraftPickMessage,
} from '@/types'
import { trackEvent } from '@/utils/analytics.ts'
import { getWebSocket, saveDeckState } from './shared'

export interface DraftSliceState {
  deckBuildingState: DeckBuildingState | null
}

export interface DraftSliceActions {
  createSealedGame: (setCode: string) => void
  joinSealedGame: (sessionId: string) => void
  addCardToDeck: (cardName: string) => void
  removeCardFromDeck: (cardName: string) => void
  clearDeck: () => void
  setLandCount: (landType: string, count: number) => void
  setDeck: (deck: readonly string[], landCounts: Record<string, number>) => void
  setLlmHighlights: (cardNames: readonly string[] | null) => void
  submitSealedDeck: () => void
  unsubmitDeck: () => void
  makePick: (cardNames: string[]) => void
  winstonTakePile: () => void
  winstonSkipPile: () => void
  gridDraftPick: (selection: string) => void
}

export type DraftSlice = DraftSliceState & DraftSliceActions

export const createDraftSlice: SliceCreator<DraftSlice> = (set, get) => ({
  // Initial state
  deckBuildingState: null,

  // Actions
  createSealedGame: (setCode) => {
    trackEvent('sealed_game_created', { set_code: setCode })
    getWebSocket()?.send(createCreateSealedGameMessage(setCode))
  },

  joinSealedGame: (sessionId) => {
    getWebSocket()?.send(createJoinSealedGameMessage(sessionId))
  },

  addCardToDeck: (cardName) => {
    set((state) => {
      if (!state.deckBuildingState) return state

      // Enforce 4-copy limit (except basic lands, but those are managed via landCounts)
      const currentCount = state.deckBuildingState.deck.filter((c) => c === cardName).length
      if (currentCount >= 4) return state

      const newDeck = [...state.deckBuildingState.deck, cardName]
      saveDeckState(newDeck, state.deckBuildingState.landCounts)

      return {
        deckBuildingState: {
          ...state.deckBuildingState,
          deck: newDeck,
        },
      }
    })
  },

  removeCardFromDeck: (cardName) => {
    set((state) => {
      if (!state.deckBuildingState) return state

      const index = state.deckBuildingState.deck.indexOf(cardName)
      if (index === -1) return state

      const newDeck = [...state.deckBuildingState.deck]
      newDeck.splice(index, 1)
      saveDeckState(newDeck, state.deckBuildingState.landCounts)

      return {
        deckBuildingState: {
          ...state.deckBuildingState,
          deck: newDeck,
        },
      }
    })
  },

  clearDeck: () => {
    set((state) => {
      if (!state.deckBuildingState) return state

      const emptyLandCounts = Object.fromEntries(
        Object.keys(state.deckBuildingState.landCounts).map((k) => [k, 0])
      )
      saveDeckState([], emptyLandCounts)

      return {
        deckBuildingState: {
          ...state.deckBuildingState,
          deck: [],
          landCounts: emptyLandCounts,
        },
      }
    })
  },

  setLandCount: (landType, count) => {
    set((state) => {
      if (!state.deckBuildingState) return state

      const newLandCounts = {
        ...state.deckBuildingState.landCounts,
        [landType]: Math.max(0, count),
      }
      saveDeckState(state.deckBuildingState.deck, newLandCounts)

      return {
        deckBuildingState: {
          ...state.deckBuildingState,
          landCounts: newLandCounts,
        },
      }
    })
  },

  setDeck: (deck, landCounts) => {
    set((state) => {
      if (!state.deckBuildingState) return state

      // Cap each non-basic card at the available pool count and at 4 copies.
      const poolCounts: Record<string, number> = {}
      for (const card of state.deckBuildingState.cardPool) {
        poolCounts[card.name] = (poolCounts[card.name] ?? 0) + 1
      }

      const requested: Record<string, number> = {}
      for (const name of deck) {
        requested[name] = (requested[name] ?? 0) + 1
      }

      const newDeck: string[] = []
      for (const [name, requestedCount] of Object.entries(requested)) {
        const cap = Math.min(poolCounts[name] ?? 0, 4)
        const final = Math.min(requestedCount, cap)
        for (let i = 0; i < final; i++) newDeck.push(name)
      }

      // Sanitize land counts: only keep known basic land keys, clamp to >= 0.
      const baseLandKeys = Object.keys(state.deckBuildingState.landCounts)
      const newLandCounts: Record<string, number> = {}
      for (const key of baseLandKeys) {
        newLandCounts[key] = Math.max(0, landCounts[key] ?? 0)
      }
      // Allow extra basic land names that the server's basicLands set knows about.
      for (const card of state.deckBuildingState.basicLands) {
        if (!(card.name in newLandCounts)) {
          newLandCounts[card.name] = Math.max(0, landCounts[card.name] ?? 0)
        }
      }

      saveDeckState(newDeck, newLandCounts)

      return {
        deckBuildingState: {
          ...state.deckBuildingState,
          deck: newDeck,
          landCounts: newLandCounts,
        },
      }
    })
  },

  setLlmHighlights: (cardNames) => {
    set((state) => {
      if (!state.deckBuildingState) return state
      const next = cardNames && cardNames.length > 0 ? Array.from(new Set(cardNames)) : null
      return {
        deckBuildingState: {
          ...state.deckBuildingState,
          llmHighlightedCards: next,
        },
      }
    })
  },

  submitSealedDeck: () => {
    const { deckBuildingState } = get()
    if (!deckBuildingState) return

    const deckList: Record<string, number> = {}

    // Count non-land cards
    for (const cardName of deckBuildingState.deck) {
      deckList[cardName] = (deckList[cardName] || 0) + 1
    }

    // Add basic lands
    for (const [landType, count] of Object.entries(deckBuildingState.landCounts)) {
      if (count > 0) {
        deckList[landType] = count
      }
    }

    const landCount = Object.values(deckBuildingState.landCounts).reduce((a, b) => a + b, 0)
    trackEvent('sealed_deck_submitted', {
      deck_size: deckBuildingState.deck.length + landCount,
      land_count: landCount,
      nonland_count: deckBuildingState.deck.length,
    })
    getWebSocket()?.send(createSubmitSealedDeckMessage(deckList))
  },

  unsubmitDeck: () => {
    getWebSocket()?.send(createUnsubmitDeckMessage())
  },

  makePick: (cardNames) => {
    trackEvent('draft_pick_made', { card_names: cardNames })
    getWebSocket()?.send(createMakePickMessage(cardNames))
  },

  winstonTakePile: () => {
    trackEvent('winston_take_pile')
    getWebSocket()?.send(createWinstonTakePileMessage())
  },

  winstonSkipPile: () => {
    trackEvent('winston_skip_pile')
    getWebSocket()?.send(createWinstonSkipPileMessage())
  },

  gridDraftPick: (selection) => {
    trackEvent('grid_draft_pick', { selection })
    getWebSocket()?.send(createGridDraftPickMessage(selection))
  },
})
