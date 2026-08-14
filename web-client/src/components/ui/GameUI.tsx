import { useGameStore } from '@/store/gameStore.ts'
import { LobbyScreen } from '../lobby/LobbyScreen'
import { TournamentOverlay } from '../tournament/TournamentOverlay'
import { FreeForAllOverlay } from '../tournament/FreeForAllOverlay'
import { HomeScreen } from './HomeScreen'

/**
 * Connection/lobby UI - shown when not in a game.
 * Combat mode and game UI are handled in App.tsx and GameBoard.tsx.
 *
 * This file is only the router between the pre-game screens. The screens themselves live in
 * `HomeScreen.tsx`, `components/lobby/` and `components/tournament/`.
 *
 * Note there is **one** lobby branch, not two. The server still has two unrelated lobby
 * implementations and the store still has a slice for each, but which one is populated no longer
 * picks a screen — `LobbyScreen` reads whichever it is through `lobbyViewModel.ts`.
 */
export function GameUI() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const sessionId = useGameStore((state) => state.sessionId)
  const lastError = useGameStore((state) => state.lastError)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const ffaState = useGameStore((state) => state.ffaState)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const quickGameLobbyState = useGameStore((state) => state.quickGameLobbyState)

  // Don't show connection overlay if actively building deck (but show during 'waiting' phase)
  // Exception: always show if tournamentState/ffaState exists (for the standings overlays)
  if (deckBuildingState && deckBuildingState.phase !== 'waiting' && !tournamentState && !ffaState) return null

  // Standings take precedence over the staging screen: on a reconnect mid-event both the lobby and
  // the tournament/pod state are populated.
  if (tournamentState) return <TournamentOverlay tournamentState={tournamentState} />
  if (ffaState) return <FreeForAllOverlay ffaState={ffaState} />

  if ((quickGameLobbyState && !sessionId) || lobbyState) return <LobbyScreen />

  return (
    <HomeScreen
      status={connectionStatus}
      sessionId={sessionId}
      error={lastError?.message}
    />
  )
}
