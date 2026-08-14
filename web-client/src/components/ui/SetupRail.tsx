/**
 * Your saved setups, above the wizard.
 *
 * The wizard is excellent once and tedious the fifth time. It is deliberately explicit — three
 * questions, one at a time, with every answer named — which is exactly right for someone who has
 * never opened Argentum and exactly wrong for someone who plays the same 8-player cube draft every
 * Thursday. This rail is the answer to the second case without compromising the first.
 *
 * **It does not render at all until you have played something.** A new player sees precisely the
 * screen they saw before; the fast path appears only once there is something to be fast about.
 *
 * One click rebuilds the whole lobby — not just the three answers the wizard's old `Play again` chip
 * could carry, but the sets, the pack counts, the timer, the ban list, the cube and the deck. See
 * `lobbyRecipe.ts` for why that was the thing missing, and `useApplyRecipe.ts` for how it is
 * replayed. For a lobby with an invite code the click ends on a configured lobby with the code
 * already on screen; for one nobody can join, it ends in the game.
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore'
import { useSetupLibrary, usableSetups, LAST_SETUP_ID, type SavedSetup } from '@/store/setupLibrary'
import { recipeSummary, type LobbyRecipe } from '../lobby/lobbyRecipe'
import styles from './GameUI.module.css'

export function SetupRail({
  onLaunch,
}: {
  onLaunch: (recipe: LobbyRecipe, notes: readonly string[]) => void
}) {
  const aiEnabled = useGameStore((s) => s.aiEnabled)
  const availableSets = useGameStore((s) => s.availableSets)
  const setups = useSetupLibrary((s) => s.setups)
  const hydrate = useSetupLibrary((s) => s.hydrate)
  const deleteSetup = useSetupLibrary((s) => s.deleteSetup)
  const renameSetup = useSetupLibrary((s) => s.renameSetup)
  const [menuFor, setMenuFor] = useState<string | null>(null)
  const menuRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => { hydrate() }, [hydrate])

  useEffect(() => {
    if (menuFor === null) return
    const closeWhenOutside = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setMenuFor(null)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuFor(null)
    }
    document.addEventListener('pointerdown', closeWhenOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeWhenOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [menuFor])

  // Re-checked against *this* server, not trusted: a setup outlives the build that made it, and the
  // AI can be switched off between sessions. Anything unreachable simply isn't offered.
  const usable = useMemo(
    () => usableSetups(setups, { aiEnabled, availableSets }),
    [setups, aiEnabled, availableSets],
  )

  if (usable.length === 0) return null

  return (
    <div className={styles.setupRail} data-testid="setup-rail">
      <div className={styles.setupRailHeading}>Your setups</div>
      <div className={styles.setupRailChips}>
        {usable.map((setup) => (
          <div key={setup.id} className={styles.setupChipWrap}>
            <button
              type="button"
              className={styles.setupChip}
              data-testid={`setup-chip-${slug(setup)}`}
              onClick={() => onLaunch(setup.recipe, setup.notes)}
              title={setup.notes.length > 0
                ? `${recipeSummary(setup.recipe, availableSets)}\n\n${setup.notes.join('\n')}`
                : recipeSummary(setup.recipe, availableSets)}
            >
              <span className={styles.setupChipName}>
                {setup.id === LAST_SETUP_ID && <span aria-hidden>↺ </span>}
                {setup.name}
              </span>
              <span className={styles.setupChipBody}>
                {recipeSummary(setup.recipe, availableSets)}
              </span>
              {/* Stated on the chip, not only in the tooltip: a setup that will come back missing
                  its cube should say so before it is clicked, not after. */}
              {setup.notes.length > 0 && (
                <span className={styles.setupChipNote}>
                  {setup.notes.length === 1 ? '1 setting can’t be restored' : `${setup.notes.length} settings can’t be restored`}
                </span>
              )}
            </button>
            <button
              type="button"
              className={styles.setupChipMenuButton}
              aria-label={`Options for ${setup.name}`}
              onClick={() => setMenuFor(menuFor === setup.id ? null : setup.id)}
            >
              ⋯
            </button>
            {menuFor === setup.id && (
              <div ref={menuRef} className={styles.setupChipMenu} role="menu">
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => {
                    // Naming the auto-captured slot is how you keep it: it stops being overwritten
                    // by the next game only once it is a setup of your own.
                    const next = window.prompt('Name this setup', setup.name)
                    if (next?.trim()) renameSetup(setup.id, next.trim())
                    setMenuFor(null)
                  }}
                >
                  {setup.id === LAST_SETUP_ID ? 'Keep and name…' : 'Rename…'}
                </button>
                <button
                  type="button"
                  role="menuitem"
                  className={styles.setupChipMenuDanger}
                  onClick={() => { deleteSetup(setup.id); setMenuFor(null) }}
                >
                  Delete
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

/** A stable test id per chip. The auto-captured slot has a fixed one; named setups use their name. */
function slug(setup: SavedSetup): string {
  if (setup.id === LAST_SETUP_ID) return 'last'
  return setup.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || setup.id
}
