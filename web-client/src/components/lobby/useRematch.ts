/**
 * "Play again" on the game-over screen.
 *
 * There was no rematch of any kind for a 1v1 game: the overlay offered *Return to Menu* and *Watch
 * Replay*, and that was it. The underlying reason is that the quick lobby is destroyed the moment
 * the game starts (`QuickGameLobbyHandler.startGame` removes it from the repository), so there is
 * nothing left to go back to.
 *
 * But the *recipe* survives — the lobby wrote one down when Start was pressed. A rematch is that
 * recipe replayed, which means this needs **no new protocol at all**: create, configure, resubmit
 * the same saved deck, ready. Say the relationship out loud so nobody builds a second mechanism:
 *
 * > A rematch is a recipe replayed with the seats intact. A setup is the same recipe replayed with
 * > the seats open.
 *
 * ## What this deliberately does not offer
 *
 * - **A human 1v1 rematch.** Every input above is decidable on *this* client, which is exactly why
 *   it works against the AI; a human opponent has to be asked, re-seated and have their deck
 *   resubmitted, and that is server work (a `RequestRematch` on the game session, modelled on
 *   `FreeForAllHandler.handleReadyForNextGame`). Offering a button that quietly opened a lobby the
 *   other player knew nothing about would be worse than offering none.
 * - **A tournament rematch.** Pods and brackets already have one — `readyForNextRound` keeps the
 *   same seats and the same decks, which is strictly better than rebuilding the lobby.
 */
import { useMemo } from 'react'
import { useGameStore } from '@/store/gameStore'
import { LAST_SETUP_ID, useSetupLibrary } from '@/store/setupLibrary'
import { lobbyKindFor } from './modeMatrix'
import { validateRecipe } from './lobbyRecipe'
import { useApplyRecipe } from './useApplyRecipe'

export function useRematch(): { play: () => void } | null {
  const aiEnabled = useGameStore((s) => s.aiEnabled)
  const availableSets = useGameStore((s) => s.availableSets)
  const returnToMenu = useGameStore((s) => s.returnToMenu)
  const setups = useSetupLibrary((s) => s.setups)
  const applyRecipe = useApplyRecipe()

  const checked = useMemo(() => {
    const last = setups.find((s) => s.id === LAST_SETUP_ID)
    if (!last) return null
    const validated = validateRecipe(last.recipe, { aiEnabled, availableSets })
    if (!validated) return null
    // Only the quick lobby is rebuildable from this side; see the note above.
    if (lobbyKindFor(validated.recipe.selection) !== 'QUICK') return null
    if (validated.recipe.selection.roster !== 'SOLO') return null
    return validated
  }, [setups, aiEnabled, availableSets])

  if (!checked) return null

  return {
    play: () => {
      // Leave the finished game first — `applyRecipe` clears the *lobby* slices, not the game one,
      // and the game screen would otherwise still be mounted over the new lobby.
      returnToMenu()
      applyRecipe({ ...checked.recipe, autoStart: true }, checked.notes)
    },
  }
}
