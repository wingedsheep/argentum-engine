/**
 * Decks & Cubes / Deck builder | Cube builder — the builder's two modes, in the page title, after a
 * link back to the shelf both kinds of thing live on (`/library`).
 *
 * Decks and cubes are built with the same card browser, so they are one tool with two modes rather
 * than two tools; this switch is what says so. Each mode remembers where it was for the rest of the
 * session, so switching away and back returns to the same deck or cube instead of a blank one — and,
 * through {@link stashWorkingCopy} / {@link peekWorkingCopy}, to the same *unsaved* contents: a tab
 * flip must never cost the player the deck or cube they were in the middle of building.
 */
import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import styles from './BuilderModeSwitch.module.css'

export type BuilderMode = 'deck' | 'cube'

const LAST_PATH_KEY: Record<BuilderMode, string> = {
  deck: 'argentum.builder.lastDeckPath',
  cube: 'argentum.builder.lastCubePath',
}

const WORKING_COPY_KEY: Record<BuilderMode, string> = {
  deck: 'argentum.builder.workingDeck',
  cube: 'argentum.builder.workingCube',
}

const DEFAULT_PATH: Record<BuilderMode, string> = {
  deck: '/deckbuilder',
  cube: '/cubes/new',
}

function readLastPath(mode: BuilderMode): string {
  try {
    return window.sessionStorage.getItem(LAST_PATH_KEY[mode]) ?? DEFAULT_PATH[mode]
  } catch {
    return DEFAULT_PATH[mode]
  }
}

/**
 * Keeps a mode's in-progress (possibly unsaved) contents for this session, tied to the page they
 * came from. Only a return to that same page picks them up again.
 */
export function stashWorkingCopy<T>(mode: BuilderMode, data: T) {
  try {
    window.sessionStorage.setItem(
      WORKING_COPY_KEY[mode],
      JSON.stringify({ path: window.location.pathname, data }),
    )
  } catch {
    // Storage blocked or full: the switch still works, the working copy just isn't kept.
  }
}

/**
 * The working copy stashed for this page, if any. Non-destructive so it is safe in render (StrictMode
 * runs initialisers twice); call {@link clearWorkingCopy} once it has been adopted.
 */
export function peekWorkingCopy<T>(mode: BuilderMode, pathname: string): T | null {
  try {
    const raw = window.sessionStorage.getItem(WORKING_COPY_KEY[mode])
    if (!raw) return null
    const stash = JSON.parse(raw) as { path?: string; data?: T }
    return stash.path === pathname && stash.data !== undefined ? stash.data : null
  } catch {
    return null
  }
}

export function clearWorkingCopy(mode: BuilderMode) {
  try {
    window.sessionStorage.removeItem(WORKING_COPY_KEY[mode])
  } catch {
    // Nothing to clear.
  }
}

export function BuilderModeSwitch({
  mode,
  onLeave,
}: {
  mode: BuilderMode
  /**
   * Called just before switching to the other mode — each builder stashes its working copy here
   * ({@link stashWorkingCopy}), so switching back restores it, unsaved changes and all.
   */
  onLeave?: () => void
}) {
  const navigate = useNavigate()
  const location = useLocation()

  // Remember this mode's current place, so coming back lands on it.
  useEffect(() => {
    try {
      window.sessionStorage.setItem(LAST_PATH_KEY[mode], location.pathname + location.search)
    } catch {
      // Storage blocked: switching back simply opens the mode fresh.
    }
  }, [mode, location.pathname, location.search])

  function go(target: BuilderMode) {
    if (target === mode) return
    onLeave?.()
    navigate(readLastPath(target))
  }

  return (
    <span className={styles.crumbs}>
      <Link
        to={`/library?tab=${mode === 'deck' ? 'decks' : 'cubes'}`}
        className={styles.library}
        onClick={() => onLeave?.()}
        title={mode === 'deck' ? 'All your decks' : 'All your cubes'}
      >
        Decks &amp; Cubes
      </Link>
      <span className={styles.sep} aria-hidden>/</span>
    <div className={styles.switch} role="tablist" aria-label="Build">
      {(['deck', 'cube'] as const).map((m) => (
        <button
          key={m}
          type="button"
          role="tab"
          aria-selected={mode === m}
          className={styles.option}
          onClick={() => go(m)}
        >
          {m === 'deck' ? 'Deck builder' : 'Cube builder'}
        </button>
      ))}
    </div>
    </span>
  )
}
