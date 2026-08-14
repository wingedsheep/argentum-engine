/**
 * One command surface over both server lobby implementations.
 *
 * `lobbyViewModel.ts` projects the two slices onto one shape to *read*; this is the same job for
 * *writing*. `LobbyScreen` calls `commands.setTable(...)` without knowing whether that becomes a
 * tournament `updateLobbySettings` or is impossible on the lobby it is looking at — the difference
 * is decided once, here and in `axisChoices.ts`, instead of at every control.
 *
 * It also owns the cross-kind switch (plan § 4b v1). When the host picks a value the current
 * backing kind can't express, the lobby is torn down and recreated on the other one. That costs
 * the invite code and drops anyone who has joined, so `LobbyScreen` always confirms first; the
 * `convertLobby` message that would preserve both is Phase 6.
 */
import { useMemo } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { DeckFormat, TournamentFormat } from '@/types'
import { gameModeForTable, type CardsKind, type RulesAxis, type TableAxis } from './axes'
import type { RecreateSpec } from './axisChoices'
import type { UnifiedLobbyView } from './lobbyViewModel'
import { BOOTSTRAP_SET_CODE } from './useApplyRecipe'
import { setPendingLobbyIntent } from '@/store/slices/pendingLobbyIntent'
import type { DeckPickerTab } from '../ui/DeckPicker'

export interface LobbyCommands {
  /** Cards values the current lobby can express directly. Recreate goes through {@link recreate}. */
  setCards: (kind: CardsKind) => void
  /** Cards → Bring a deck: which constructed format submitted decks must be legal in. */
  setLegality: (format: DeckFormat | null) => void
  /** Cards → Sealed / Draft sub-shape, as a concrete tournament format. */
  setCardsShape: (format: TournamentFormat) => void
  /**
   * Which rules the game runs under. Tournament-backed only: a quick lobby derives its rules from
   * deck legality server-side and offers no control, so this is a no-op there (and `rulesChoices`
   * says so rather than letting the button lie).
   */
  setRules: (rules: RulesAxis) => void
  setTable: (table: TableAxis) => void
  /** Move one seat to the other team, sending the full explicit assignment for every seat. */
  togglePlayerTeam: (playerId: string) => void
  setPublic: (isPublic: boolean) => void
  setRanked: (ranked: boolean) => void
  /** Tear this lobby down and stand it back up on the other server implementation. */
  recreate: (spec: RecreateSpec) => void
  /** The one button beside Leave — ready / cancel ready / start, per {@link UnifiedLobbyView}. */
  runPrimary: () => void
  leave: () => void
  addAi: () => void
  removeAi: (playerId: string) => void
}

const noop = () => {}

/** Every command, wired to nothing — used for the render where no lobby slice is populated yet. */
const NO_LOBBY: LobbyCommands = {
  setCards: noop,
  setLegality: noop,
  setCardsShape: noop,
  setRules: noop,
  setTable: noop,
  togglePlayerTeam: noop,
  setPublic: noop,
  setRanked: noop,
  recreate: noop,
  runPrimary: noop,
  leave: noop,
  addAi: noop,
  removeAi: noop,
}

export function useLobbyCommands(
  /** Null while no lobby slice is populated — every command is then a no-op. */
  view: UnifiedLobbyView | null,
  /** Hoisted deck-picker tab, so Cards → Random pool can drive it. Quick lobbies only. */
  setDeckTab: (tab: DeckPickerTab) => void,
): LobbyCommands {
  // Read the store lazily rather than subscribing: these are all write paths, and a bare
  // `useGameStore()` would re-render the whole lobby on every unrelated store change.
  const s = useGameStore.getState

  return useMemo<LobbyCommands>(() => {
    if (view === null) return NO_LOBBY
    const isQuick = view.kind === 'QUICK'

    const recreate = (spec: RecreateSpec) => {
      // Leave first: the quick-game create handler rejects outright with "Already in a lobby"
      // (`QuickGameLobbyHandler.handleCreate`), and while the tournament create handler does call
      // `leaveCurrentLobbyIfPresent`, that only knows about *tournament* lobbies. Messages go out
      // over one socket in order, so leave-then-create is enough.
      if (isQuick) s().leaveQuickGameLobby()
      else s().leaveLobby()

      if (spec.to === 'QUICK') {
        // Random pool is the picker's Random tab, not a lobby flag, so a switch that promised one
        // has to move the picker itself — otherwise the new lobby opens on "Bring a deck". Leaving
        // has already unmounted this screen (both slices are null until the new lobby arrives), so
        // the tab is handed to the *next* one rather than set on this one.
        setPendingLobbyIntent({ deckTab: spec.deckTab })
        s().createQuickGameLobby(false, undefined, view.isPublic, spec.format ?? undefined, spec.momirBasic)
      } else {
        // A recreate deliberately does *not* carry the old lobby's settings across: the confirm
        // dialog has just told the host that "set selection and any submitted decks are reset",
        // because the new lobby is a different shape and most of what was configured wouldn't mean
        // the same thing. It is the one path that still opens on the bootstrap set.
        s().createTournamentLobby(
          [BOOTSTRAP_SET_CODE], spec.format, 6, 8, 45, view.isPublic, spec.gameMode,
        )
      }
    }

    return {
      recreate,

      setCards: (kind) => {
        if (isQuick) {
          switch (kind) {
            case 'MOMIR':
              s().setQuickGameLobbyFormat(null, true)
              return
            case 'RANDOM':
              // Random pool *is* the picker's Random tab — its empty deck list is the server's
              // "roll me one" signal — so selecting it here just moves the picker.
              s().setQuickGameLobbyFormat(null, false)
              setDeckTab('random')
              return
            case 'BRING_A_DECK':
              s().setQuickGameLobbyFormat(null, false)
              if (view.axes.cards.kind === 'RANDOM') setDeckTab('saved')
              return
            default:
              return
          }
        }
        switch (kind) {
          case 'BRING_A_DECK': s().updateLobbySettings({ format: 'PREMADE_DECKS' }); return
          case 'SEALED': s().updateLobbySettings({ format: 'SEALED' }); return
          case 'DRAFT': s().updateLobbySettings({ format: 'DRAFT' }); return
          default: return
        }
      },

      setLegality: (format) => {
        if (isQuick) s().setQuickGameLobbyFormat(format, false)
        else s().updateLobbySettings({ deckFormat: format ?? '' })
      },

      setCardsShape: (format) => {
        if (!isQuick) s().updateLobbySettings({ format })
      },

      setRules: (rules) => {
        if (isQuick) {
          const current = view.axes.cards.kind === 'BRING_A_DECK' ? view.axes.cards.legality : null
          if (rules === 'COMMANDER') s().setQuickGameLobbyFormat('COMMANDER', false)
          else s().setQuickGameLobbyFormat(current && !['COMMANDER', 'BRAWL', 'STANDARD_BRAWL'].includes(current) ? current : 'STANDARD', false)
        } else {
          s().updateLobbySettings({ rules })
        }
      },

      setTable: (table) => {
        if (!isQuick) s().updateLobbySettings({ gameMode: gameModeForTable(table) })
      },

      togglePlayerTeam: (playerId) => {
        if (view.teams.mode !== 'MANUAL') return
        const next = { ...view.teams.byPlayerId }
        next[playerId] = next[playerId] === 0 ? 1 : 0
        s().updateLobbySettings({ teamAssignments: next })
      },

      setPublic: (isPublic) => {
        if (isQuick) s().setQuickGameLobbyPublic(isPublic)
        else s().updateLobbySettings({ isPublic })
      },

      setRanked: (ranked) => {
        if (isQuick) s().setQuickGameLobbyRanked(ranked)
        else s().updateLobbySettings({ ranked })
      },

      runPrimary: () => {
        switch (view.primaryAction?.kind) {
          case 'READY': s().setQuickGameLobbyReady(true); return
          case 'UNREADY': s().setQuickGameLobbyReady(false); return
          case 'START': s().startLobby(); return
          default: return
        }
      },

      leave: () => {
        if (isQuick) s().leaveQuickGameLobby()
        else s().leaveLobby()
      },

      addAi: () => isQuick ? s().addQuickGameAi() : s().addAiToLobby(),
      removeAi: (playerId) => isQuick ? s().removeQuickGameAi() : s().removeAiFromLobby(playerId),
    }
  }, [s, view?.kind, view?.isPublic, view?.axes.cards, view?.primaryAction?.kind, view?.teams, setDeckTab])
}
