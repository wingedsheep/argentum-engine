/**
 * "Play this cube" — a button that opens a small panel asking the two things a cube lobby needs
 * that the cube itself can't say: draft or sealed, and who with. Confirming parks a launch for the
 * home screen (`playCube.ts`) and goes there; the lobby opens with the cube already on the table.
 */
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { SharedCube } from '@/api/account'
import { cubeOpponentRange, cubePlayRecipe, queueCubePlay, type CubePlayFormat, type CubePlayWith } from './playCube'
import styles from './CubePlayButton.module.css'

interface CubePlayButtonProps {
  readonly cube: SharedCube
  /** Why the cube can't be played yet (unimplemented cards, empty) — disables the button. */
  readonly blockedReason?: string | null
  readonly className?: string | undefined
  /** Which edge of the button the panel lines up with; `end` for a button at the right of its row. */
  readonly panelAlign?: 'start' | 'end'
}

export function CubePlayButton({ cube, blockedReason = null, className, panelAlign = 'start' }: CubePlayButtonProps) {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [format, setFormat] = useState<CubePlayFormat>('DRAFT')
  const [playWith, setPlayWith] = useState<CubePlayWith>('FRIENDS')
  const range = cubeOpponentRange(format)
  const [opponents, setOpponents] = useState(range.fallback)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const close = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    const escape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', escape)
    }
  }, [open])

  const clampedOpponents = Math.min(range.max, Math.max(range.min, opponents))

  function launch() {
    queueCubePlay(cubePlayRecipe(cube, { format, playWith, opponents: clampedOpponents }))
    navigate('/')
  }

  return (
    <div ref={rootRef} className={styles.playAnchor}>
      <button
        type="button"
        className={className ?? styles.playButton}
        disabled={blockedReason !== null}
        title={blockedReason ?? undefined}
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        Play
      </button>
      {open && (
        <div className={styles.playPanel} data-align={panelAlign} role="dialog" aria-label={`Play ${cube.name}`}>
          <div className={styles.playRow}>
            <span className={styles.playLabel} id="cube-play-format">Format</span>
            <div className={styles.segmented} role="radiogroup" aria-labelledby="cube-play-format">
              {(['DRAFT', 'SEALED'] as const).map((value) => (
                <button
                  key={value}
                  type="button"
                  role="radio"
                  aria-checked={format === value}
                  className={styles.segment}
                  onClick={() => setFormat(value)}
                >
                  {value === 'DRAFT' ? 'Booster draft' : 'Sealed'}
                </button>
              ))}
            </div>
          </div>
          <div className={styles.playRow}>
            <span className={styles.playLabel} id="cube-play-with">Players</span>
            <div className={styles.segmented} role="radiogroup" aria-labelledby="cube-play-with">
              {(['FRIENDS', 'AI'] as const).map((value) => (
                <button
                  key={value}
                  type="button"
                  role="radio"
                  aria-checked={playWith === value}
                  className={styles.segment}
                  onClick={() => setPlayWith(value)}
                >
                  {value === 'FRIENDS' ? 'With friends' : 'Against AI'}
                </button>
              ))}
            </div>
          </div>
          {playWith === 'AI' && range.max > range.min && (
            <label className={styles.playRow}>
              <span className={styles.playLabel}>AI opponents</span>
              <input
                className={styles.opponentInput}
                type="number"
                min={range.min}
                max={range.max}
                value={clampedOpponents}
                onChange={(e) => setOpponents(Number(e.target.value) || range.min)}
              />
            </label>
          )}
          <p className={styles.playHint}>
            {playWith === 'FRIENDS'
              ? 'Opens a lobby with this cube loaded. Share its invite code; you start when everyone is in.'
              : 'Opens a lobby with this cube loaded and starts as soon as the AI players are seated.'}
          </p>
          <button type="button" className={styles.playGo} onClick={launch}>
            Open lobby
          </button>
        </div>
      )}
    </div>
  )
}
