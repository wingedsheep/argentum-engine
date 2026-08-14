import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore'
import { selectGameState, selectViewingPlayerId, useViewedOpponent } from '@/store/selectors'
import { hasPendingInputSelection } from '@/store/slices/ui/boardViewSlice'
import type { ClientPlayer, EntityId } from '@/types'

/**
 * Multiplayer camera controls: follow-the-action and keyboard board switching.
 *
 * Follow-the-action moves the viewed opponent board only on *coarse* boundaries —
 * an opponent's turn starting, or the attacking player's board when you're being
 * attacked — and is refused inside `followViewTo` whenever the player has any
 * pending input (the camera never moves under an in-progress selection) or has
 * pinned the view.
 *
 * Keyboard: 1..9 select the Nth living opponent (in rail order); 0 toggles the
 * table overview; Esc unpins.
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
        // A selected card has an open action menu; don't yank the camera out from under a
        // half-finished interaction. (There is deliberately no number-key ability activation —
        // the action menu is click/tap only.)
        if (store.selectedCardId) return
        const living = opponents.filter((o) => !o.hasLost)
        const picked = living[Number(e.key) - 1]
        if (picked) store.viewOpponent(picked.playerId)
      } else if (e.key === '0') {
        if (store.selectedCardId) return
        store.toggleOverviewMode()
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

const NO_EXTRA_BOARDS: readonly EntityId[] = Object.freeze([])

/**
 * Combat between two *other* players: the extra defending seats whose boards should be
 * shown alongside the viewed board so the fight is visible as real arrows between real
 * boards instead of a bundled arrow onto a rail chip.
 *
 * Active only while the server's confirmed combat has attackers, the viewing player is
 * neither the attacker nor among the defenders (those cases keep today's behavior — your
 * own half is always visible), and follow-the-action is on and unpinned. Entering the
 * split view is refused while the player has any pending input (the camera never moves
 * under an in-progress selection), but once active it stays for the whole combat so the
 * boards don't shift mid-fight.
 */
export function useCombatDefenderFocus(enabled: boolean): readonly EntityId[] {
  const gameState = useGameStore(selectGameState)
  const viewingPlayerId = useGameStore(selectViewingPlayerId)
  const viewedOpponent = useViewedOpponent()
  const viewedOpponentId = viewedOpponent?.playerId ?? null
  const [extras, setExtras] = useState<readonly EntityId[]>(NO_EXTRA_BOARDS)

  const combat = gameState?.combat ?? null

  useEffect(() => {
    const reset = () => setExtras((prev) => (prev.length === 0 ? prev : NO_EXTRA_BOARDS))
    if (!enabled || !gameState || !combat || combat.attackers.length === 0 || !viewingPlayerId) {
      reset()
      return
    }
    if (combat.attackingPlayerId === viewingPlayerId) {
      reset()
      return
    }
    const defenders = new Set<EntityId>()
    for (const a of combat.attackers) {
      const d =
        a.attackingTarget.type === 'Player'
          ? a.attackingTarget.playerId
          : gameState.cards[a.attackingTarget.permanentId]?.controllerId
      if (d) defenders.add(d)
    }
    // You're among the defenders → the attacker slides into view (useMultiplayerView)
    // and your own half is already on screen. No split needed.
    if (defenders.has(viewingPlayerId)) {
      reset()
      return
    }
    // Both sides of the fight must be on screen: the attacker and every defending
    // seat, minus whichever of them already occupies the viewed slot.
    const combatants = new Set<EntityId>(defenders)
    combatants.add(combat.attackingPlayerId)
    const next = gameState.players
      .filter((p) => !p.hasLost && combatants.has(p.playerId))
      .map((p) => p.playerId)
      .filter((id) => id !== viewedOpponentId)
    if (next.length === 0) {
      reset()
      return
    }
    setExtras((prev) => {
      // Entering (not updating) the split view respects the camera guards.
      if (prev.length === 0) {
        const store = useGameStore.getState()
        if (!store.followAction || store.viewPinned || hasPendingInputSelection(store)) return prev
      }
      return prev.length === next.length && prev.every((id, i) => id === next[i]) ? prev : next
    })
  }, [enabled, gameState, combat, viewingPlayerId, viewedOpponentId])

  return extras
}
