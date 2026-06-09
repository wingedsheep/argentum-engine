/**
 * Draft slice - handles sealed deck building and drafting logic.
 */
import type { SliceCreator, DeckBuildingState, PickScore, AutoBuildSummary } from './types'
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
import {
  suggestPick as apiSuggestPick,
  autoBuildDeck as apiAutoBuildDeck,
} from '@/api/aiAssist'
import { getWebSocket, saveDeckState } from './shared'

export type { PickScore }

export interface DraftSliceState {
  deckBuildingState: DeckBuildingState | null
  /**
   * AI "Suggest Pick" results for the current pack: card name → score/reason. Null until the player
   * asks for a suggestion; cleared by the draft handlers when the pack changes.
   */
  pickScores: Readonly<Record<string, PickScore>> | null
  /** Card names the AI recommends taking this pick (highlighted in the pack grid). */
  recommendedPick: readonly string[]
  /** True while a suggest-pick / auto-build request is in flight. */
  aiAssistBusy: boolean
  /** Last AI-assist error message (e.g. assistance disabled), or null. */
  aiAssistError: string | null
  /** Score/archetype from the most recent Auto-build, or null until one runs. */
  autoBuildResult: AutoBuildSummary | null
  /** Engine the player selected in the draft "Suggest Pick" dropdown; persists across edits. */
  draftAdvisorId: string | null
  /** Engine the player selected in the deckbuild "Auto-build" dropdown; persists across edits. */
  deckbuildAdvisorId: string | null
  /**
   * Per-card AI scores for the deck builder pool: card name → score/reason. Null until the player
   * asks for them; persists across edits (re-score to refresh) so the badges stay visible.
   */
  deckCardScores: Readonly<Record<string, PickScore>> | null
}

export interface DraftSliceActions {
  createSealedGame: (setCode: string) => void
  joinSealedGame: (sessionId: string) => void
  addCardToDeck: (cardName: string) => void
  removeCardFromDeck: (cardName: string) => void
  clearDeck: () => void
  setLandCount: (landType: string, count: number) => void
  setDeck: (deck: readonly string[], landCounts: Record<string, number>) => void
  /** Set or clear the commander for Commander Draft / Sealed lobbies. */
  setCommander: (cardName: string | null) => void
  setLlmHighlights: (cardNames: readonly string[] | null) => void
  submitSealedDeck: () => void
  unsubmitDeck: () => void
  makePick: (cardNames: string[]) => void
  winstonTakePile: () => void
  winstonSkipPile: () => void
  gridDraftPick: (selection: string) => void
  /** Ask the chosen AI engine to score the current pack and recommend the best pick(s). */
  suggestPick: (advisorId?: string) => Promise<void>
  /** Clear the current pick suggestion overlay. */
  clearPickSuggestion: () => void
  /** Build (empty deck) or complete (partial deck) the deck from the pool with the chosen engine. */
  autoBuildDeck: (advisorId?: string) => Promise<void>
  /** Remember the selected draft "Suggest Pick" engine. */
  setDraftAdvisorId: (advisorId: string | null) => void
  /** Remember the selected deckbuild "Auto-build" engine. */
  setDeckbuildAdvisorId: (advisorId: string | null) => void
  /** Score every card in the deck-builder pool with the chosen engine (deck = colour context). */
  scoreDeckCards: (advisorId?: string) => Promise<void>
  /** Hide the deck-builder per-card score badges. */
  clearDeckCardScores: () => void
}

export type DraftSlice = DraftSliceState & DraftSliceActions

export const createDraftSlice: SliceCreator<DraftSlice> = (set, get) => ({
  // Initial state
  deckBuildingState: null,
  pickScores: null,
  recommendedPick: [],
  aiAssistBusy: false,
  aiAssistError: null,
  autoBuildResult: null,
  draftAdvisorId: null,
  deckbuildAdvisorId: null,
  deckCardScores: null,

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
        autoBuildResult: null,
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
        autoBuildResult: null,
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
        autoBuildResult: null,
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
        autoBuildResult: null,
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

  setCommander: (cardName) => {
    set((state) => {
      if (!state.deckBuildingState) return state
      return {
        deckBuildingState: {
          ...state.deckBuildingState,
          commander: cardName,
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

    // Commander is sent both as the dedicated field and merged into deckList — the server's
    // stripCommanderFromCards subtracts the merged copy when building Deck.cards. The
    // DeckBuilderOverlay invariant ("commander must be in state.deck", enforced by the
    // clearing effect) means the commander is already counted from `deckBuildingState.deck`
    // above, so only add the +1 when it isn't — otherwise the server strips one too few and
    // the pool gets allocated twice for the commander, surfacing as "not enough copies of
    // your commander" on a singleton sealed pool.
    const commander = deckBuildingState.commander
    if (commander && !(commander in deckList)) {
      deckList[commander] = 1
    }

    const landCount = Object.values(deckBuildingState.landCounts).reduce((a, b) => a + b, 0)
    const commanderAlreadyInDeck = commander != null && deckBuildingState.deck.includes(commander)
    trackEvent('sealed_deck_submitted', {
      deck_size: deckBuildingState.deck.length + landCount + (commander && !commanderAlreadyInDeck ? 1 : 0),
      land_count: landCount,
      nonland_count: deckBuildingState.deck.length,
    })
    getWebSocket()?.send(createSubmitSealedDeckMessage(deckList, commander))
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

  suggestPick: async (advisorId) => {
    const { lobbyState } = get()
    const draft = lobbyState?.draftState
    if (!draft || draft.currentPack.length === 0) return

    trackEvent('draft_suggest_pick', { advisor: advisorId ?? 'default' })
    // Remember which pack this request is for; if the pack advances (pick timer fires, or the
    // player picks) while the request is in flight, a late response must not write the previous
    // pack's scores onto the new pack.
    const requestedPack = draft.packNumber
    const requestedPick = draft.pickNumber
    set({ aiAssistBusy: true, aiAssistError: null })
    try {
      const advice = await apiSuggestPick({
        lobbyId: lobbyState?.lobbyId ?? null,
        advisorId: advisorId ?? null,
        pack: draft.currentPack.map((c) => c.name),
        pickedSoFar: draft.pickedCards.map((c) => c.name),
        packNumber: draft.packNumber,
        pickNumber: draft.pickNumber,
        picksRequired: draft.picksPerRound,
        setCodes: lobbyState?.settings.setCodes ?? [],
      })
      const current = get().lobbyState?.draftState
      if (!current || current.packNumber !== requestedPack || current.pickNumber !== requestedPick) {
        // The pack moved on while we were waiting — drop this stale result, but clear the spinner.
        set({ aiAssistBusy: false })
        return
      }
      const scores: Record<string, PickScore> = {}
      for (const s of advice.scores) scores[s.cardName] = { score: s.score, reason: s.reason }
      set({ pickScores: scores, recommendedPick: advice.recommended, aiAssistBusy: false })
    } catch (e) {
      set({
        aiAssistBusy: false,
        aiAssistError: e instanceof Error ? e.message : 'Suggestion failed',
        pickScores: null,
        recommendedPick: [],
      })
    }
  },

  clearPickSuggestion: () => set({ pickScores: null, recommendedPick: [], aiAssistError: null }),

  autoBuildDeck: async (advisorId) => {
    const { deckBuildingState, lobbyState } = get()
    if (!deckBuildingState) return

    trackEvent('deckbuild_auto_build', { advisor: advisorId ?? 'default' })
    set({ aiAssistBusy: true, aiAssistError: null })
    try {
      // Treat the player's current deck (non-land picks + basic lands) as locked, so an empty deck
      // builds fresh and a partial deck is completed without dropping their existing cards.
      const lockedDeck: Record<string, number> = {}
      for (const name of deckBuildingState.deck) lockedDeck[name] = (lockedDeck[name] ?? 0) + 1
      for (const [land, count] of Object.entries(deckBuildingState.landCounts)) {
        if (count > 0) lockedDeck[land] = (lockedDeck[land] ?? 0) + count
      }

      const format = lobbyState?.settings.format
      const isCommander = format === 'COMMANDER_DRAFT' || format === 'COMMANDER_SEALED'
      const targetSize = isCommander ? lobbyState?.settings.deckSizeMin ?? 60 : 40

      const result = await apiAutoBuildDeck({
        lobbyId: lobbyState?.lobbyId ?? null,
        advisorId: advisorId ?? null,
        pool: deckBuildingState.cardPool.map((c) => c.name),
        basics: deckBuildingState.basicLands.map((c) => c.name),
        lockedDeck,
        targetSize,
        setCodes: lobbyState?.settings.setCodes ?? [],
      })

      // Split the built decklist into non-land cards (deck) and basic-land counts (landCounts),
      // then apply via setDeck (which re-caps to pool availability and the 4-of rule).
      const basicNames = new Set(deckBuildingState.basicLands.map((c) => c.name))
      const newDeck: string[] = []
      const newLandCounts: Record<string, number> = {}
      for (const [name, count] of Object.entries(result.deckList)) {
        if (basicNames.has(name)) {
          newLandCounts[name] = count
        } else {
          for (let i = 0; i < count; i++) newDeck.push(name)
        }
      }
      get().setDeck(newDeck, newLandCounts)
      set({
        aiAssistBusy: false,
        autoBuildResult: {
          advisorId: result.advisorId,
          score: result.score,
          archetype: result.archetype,
        },
      })
    } catch (e) {
      set({
        aiAssistBusy: false,
        aiAssistError: e instanceof Error ? e.message : 'Auto-build failed',
        autoBuildResult: null,
      })
    }
  },

  setDraftAdvisorId: (advisorId) => set({ draftAdvisorId: advisorId }),
  setDeckbuildAdvisorId: (advisorId) => set({ deckbuildAdvisorId: advisorId }),

  scoreDeckCards: async (advisorId) => {
    const { deckBuildingState, lobbyState } = get()
    if (!deckBuildingState) return

    trackEvent('deckbuild_score_cards', { advisor: advisorId ?? 'default' })
    set({ aiAssistBusy: true, aiAssistError: null })
    try {
      // Score every distinct pool card via the per-card scorer, using the current deck as colour
      // context (the suggest-pick endpoint returns a score for every card in `pack`).
      const poolNames = Array.from(new Set(deckBuildingState.cardPool.map((c) => c.name)))
      const advice = await apiSuggestPick({
        lobbyId: lobbyState?.lobbyId ?? null,
        advisorId: advisorId ?? null,
        pack: poolNames,
        pickedSoFar: deckBuildingState.deck,
        packNumber: 1,
        pickNumber: 1,
        picksRequired: 1,
        setCodes: lobbyState?.settings.setCodes ?? [],
      })
      const scores: Record<string, PickScore> = {}
      for (const s of advice.scores) scores[s.cardName] = { score: s.score, reason: s.reason }
      set({ deckCardScores: scores, aiAssistBusy: false })
    } catch (e) {
      set({
        aiAssistBusy: false,
        aiAssistError: e instanceof Error ? e.message : 'Scoring failed',
        deckCardScores: null,
      })
    }
  },

  clearDeckCardScores: () => set({ deckCardScores: null }),
})
