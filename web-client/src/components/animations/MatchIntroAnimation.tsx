import { useEffect, useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import styles from './MatchIntroAnimation.module.css'

type Phase = 'fadeIn' | 'slideIn' | 'hold' | 'fadeOut' | 'done'

/**
 * Full-screen "Player VS Opponent" intro animation shown when a match starts.
 * Plays before the mulligan screen appears.
 */
export function MatchIntroAnimation() {
  const matchIntro = useGameStore((state) => state.matchIntro)
  const clearMatchIntro = useGameStore((state) => state.clearMatchIntro)
  const [phase, setPhase] = useState<Phase>('fadeIn')

  useEffect(() => {
    if (!matchIntro) return

    setPhase('fadeIn')

    const timers: ReturnType<typeof setTimeout>[] = []

    // Phase 1: Fade in backdrop (0-200ms)
    timers.push(setTimeout(() => setPhase('slideIn'), 200))
    // Phase 2: Slide in names + scale VS (200-700ms)
    timers.push(setTimeout(() => setPhase('hold'), 700))
    // Phase 3: Hold (700-2200ms)
    timers.push(setTimeout(() => setPhase('fadeOut'), 2200))
    // Phase 4: Fade out (2200-2800ms)
    timers.push(setTimeout(() => {
      setPhase('done')
      clearMatchIntro()
    }, 2800))

    return () => { timers.forEach(clearTimeout) }
  }, [matchIntro, clearMatchIntro])

  if (!matchIntro || phase === 'done') return null

  const isFadeOut = phase === 'fadeOut'
  const isVisible = phase === 'slideIn' || phase === 'hold' || phase === 'fadeOut'

  const backdropClass = [
    styles.backdrop,
    isVisible && !isFadeOut ? styles.backdropVisible : '',
    isFadeOut ? styles.backdropFadeOut : '',
  ].filter(Boolean).join(' ')

  const playerClass = [
    styles.playerPanel,
    isVisible && !isFadeOut ? styles.playerPanelVisible : '',
    isFadeOut ? styles.playerPanelFadeOut : '',
  ].filter(Boolean).join(' ')

  const opponentClass = [
    styles.playerPanel,
    styles.opponentPanel,
    isVisible && !isFadeOut ? styles.playerPanelVisible : '',
    isFadeOut ? styles.playerPanelFadeOut : '',
  ].filter(Boolean).join(' ')

  const vsClass = [
    styles.vsContainer,
    isVisible && !isFadeOut ? styles.vsContainerVisible : '',
    isFadeOut ? styles.vsContainerFadeOut : '',
  ].filter(Boolean).join(' ')

  return (
    <div className={backdropClass}>
      <div className={styles.content}>
        {/* Player (left) */}
        <div className={playerClass}>
          <p className={styles.playerName}>{matchIntro.playerName}</p>
          {matchIntro.playerRecord && (
            <p className={styles.playerRecord}>{matchIntro.playerRecord}</p>
          )}
        </div>

        {/* VS (center) */}
        <div className={vsClass}>
          {matchIntro.round != null && (
            <p className={styles.roundLabel}>Round {matchIntro.round}</p>
          )}
          <p className={styles.vsText}>VS</p>
        </div>

        {/* Opponent (right) */}
        <div className={opponentClass}>
          <p className={styles.playerName}>{matchIntro.opponentName}</p>
          {matchIntro.opponentRecord && (
            <p className={styles.playerRecord}>{matchIntro.opponentRecord}</p>
          )}
        </div>
      </div>
    </div>
  )
}
