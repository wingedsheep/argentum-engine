import { useEffect } from 'react'
import { useGameStore } from '@/store/gameStore'
import { selectGameState, selectViewingPlayerId } from '@/store/selectors'
import type { ClientPlayer } from '@/types'

/**
 * Multiplayer camera controls: follow-the-action and keyboard board switching.
 *
 * Follow-the-action moves the viewed opponent board only on *coarse* boundaries —
 * an opponent's turn starting, or the attacking player's board when you're being
 * attacked — and is refused inside `followViewTo` whenever the player has any
 * pending input (the camera never moves under an in-progress selection) or has
 * pinned the view.
 *
 * Keyboard: 1..9 select the Nth living opponent (in rail order); Esc unpins.
 */
export function useMultiplayerView(enabled: boolean, opponents: readonly ClientPlayer[]) {
  const gameState = useGameStore(selectGameState)
  const viewingPlayerId = useGameStore(selectViewingPlayerId)
  const followViewTo = useGameStore((state) => state.followViewTo)

  const activePlayerId = gameState?.activePlayerId ?? null
  const priorityPlayerId = gameState?.priorityPlayerId ?? null
  const hotseat = gameState?.hotseat ?? false
  const combat = gameState?.combat ?? null

  // Turn boundary: slide to the active opponent when their turn starts.
  useEffect(() => {
    if (!enabled || !activePlayerId || !viewingPlayerId) return
    if (activePlayerId === viewingPlayerId) return
    followViewTo(activePlayerId)
  }, [enabled, activePlayerId, viewingPlayerId, followViewTo])

  // Being attacked: slide to the attacking player's board.
  useEffect(() => {
    if (!enabled || !combat || !viewingPlayerId || !gameState) return
    if (combat.attackingPlayerId === viewingPlayerId) return
    const attacksMe = combat.attackers.some((a) =>
      a.attackingTarget.type === 'Player'
        ? a.attackingTarget.playerId === viewingPlayerId
        : gameState.cards[a.attackingTarget.permanentId]?.controllerId === viewingPlayerId
    )
    if (attacksMe) followViewTo(combat.attackingPlayerId)
  }, [enabled, combat, viewingPlayerId, gameState, followViewTo])

  // Hotseat dev loop: the single connection acts for whichever seat holds
  // priority — keep that seat's board in view so its permanents are clickable.
  useEffect(() => {
    if (!enabled || !hotseat || !priorityPlayerId || !viewingPlayerId) return
    if (priorityPlayerId === viewingPlayerId) return
    followViewTo(priorityPlayerId)
  }, [enabled, hotseat, priorityPlayerId, viewingPlayerId, followViewTo])

  // Keyboard switching.
  useEffect(() => {
    if (!enabled) return
    const onKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null
      if (target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return
      const store = useGameStore.getState()
      if (e.key >= '1' && e.key <= '9') {
        // Number keys also activate abilities while a card's action menu is open —
        // that interaction wins.
        if (store.selectedCardId) return
        const living = opponents.filter((o) => !o.hasLost)
        const picked = living[Number(e.key) - 1]
        if (picked) store.viewOpponent(picked.playerId)
      } else if (e.key === 'Escape') {
        // Esc means "cancel" inside selection modes; only unpin when idle.
        if (
          !store.targetingState &&
          !store.decisionSelectionState &&
          !store.manaSelectionState &&
          !store.combatState
        ) {
          store.unpinView()
        }
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [enabled, opponents])
}
