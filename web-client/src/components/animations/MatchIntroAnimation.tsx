import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { randomBackground } from '@/utils/background'
import { AvatarArt, hasAvatarArt } from '@/components/profile/AvatarArt'
import { initialOf } from '@/components/profile/avatars'
import styles from './MatchIntroAnimation.module.css'

type Phase = 'fadeIn' | 'slideIn' | 'hold' | 'fadeOut' | 'done'

/**
 * Full-screen intro shown when a match starts, before the mulligan screen: the landing art
 * full-bleed (the one place it isn't behind the interface) with the matchup in a glass band.
 */
export function MatchIntroAnimation() {
  const matchIntro = useGameStore((state) => state.matchIntro)
  const clearMatchIntro = useGameStore((state) => state.clearMatchIntro)
  const [phase, setPhase] = useState<Phase>('fadeIn')

  useEffect(() => {
    if (!matchIntro) return

    setPhase('fadeIn')

    const timers: ReturnType<typeof setTimeout>[] = []

    // Phase 1: Fade in the art (0-200ms)
    timers.push(setTimeout(() => setPhase('slideIn'), 200))
    // Phase 2: Bring up the matchup band (200-700ms)
    timers.push(setTimeout(() => setPhase('hold'), 700))
    // Phase 3: Hold — a beat longer than the old VS card, since the art is the point (700-2600ms)
    timers.push(setTimeout(() => setPhase('fadeOut'), 2600))
    // Phase 4: Fade out (2600-3200ms)
    timers.push(setTimeout(() => {
      setPhase('done')
      clearMatchIntro()
    }, 3200))

    return () => { timers.forEach(clearTimeout) }
  }, [matchIntro, clearMatchIntro])

  if (!matchIntro || phase === 'done') return null

  const isFadeOut = phase === 'fadeOut'
  const isVisible = phase === 'slideIn' || phase === 'hold' || phase === 'fadeOut'

  const backdropClass = [
    styles.backdrop,
    phase !== 'fadeIn' && !isFadeOut ? styles.backdropVisible : '',
    isFadeOut ? styles.backdropFadeOut : '',
  ].filter(Boolean).join(' ')

  const bandClass = [
    styles.band,
    isVisible && !isFadeOut ? styles.bandVisible : '',
    isFadeOut ? styles.bandFadeOut : '',
  ].filter(Boolean).join(' ')

  return (
    <div className={backdropClass}>
      <div className={styles.art} style={{ backgroundImage: `url(${randomBackground})` }} aria-hidden />
      <div className={styles.shade} aria-hidden />
      <div className={bandClass}>
        {/* Player (left): portrait on the outer edge, name toward the VS. */}
        <div className={`${styles.side} ${styles.sidePlayer}`}>
          <Portrait name={matchIntro.playerName} avatar={matchIntro.playerAvatar} />
          <div className={styles.sideText}>
            <p className={styles.playerName}>{matchIntro.playerName}</p>
            {matchIntro.playerRecord && (
              <p className={styles.playerRecord}>{matchIntro.playerRecord}</p>
            )}
          </div>
        </div>

        {/* VS (center) */}
        <div className={styles.vs}>
          {matchIntro.round != null && (
            <p className={styles.roundLabel}>Round {matchIntro.round}</p>
          )}
          <p className={styles.vsText}>vs</p>
        </div>

        {/* Opponent (right) */}
        <div className={`${styles.side} ${styles.sideOpponent}`}>
          {matchIntro.opponentNames.length > 1 ? (
            // A pod: the opponents side by side, each a medallion with its name beneath — sized down
            // as the table fills so the row always fits its half of the band.
            <div className={styles.pod} data-count={Math.min(matchIntro.opponentNames.length, 5)}>
              {matchIntro.opponentNames.map((name, i) => (
                <div key={i} className={styles.podSeat}>
                  <Portrait name={name} avatar={matchIntro.opponentAvatars?.[i]} seat />
                  <p className={styles.podName} title={name}>{name}</p>
                </div>
              ))}
            </div>
          ) : (
            <>
              <div className={styles.sideText}>
                <p className={styles.playerName}>{matchIntro.opponentName}</p>
                {matchIntro.opponentRecord && (
                  <p className={styles.playerRecord}>{matchIntro.opponentRecord}</p>
                )}
              </div>
              <Portrait name={matchIntro.opponentName} avatar={matchIntro.opponentAvatars?.[0]} />
            </>
          )}
        </div>
      </div>
    </div>
  )
}

/** A seat's account avatar, or its initial for guests and AI, so both sides of the VS stay balanced. */
function Portrait({ name, avatar, seat = false }: { name: string; avatar: string | null | undefined; seat?: boolean }) {
  const art = hasAvatarArt(avatar)
  return (
    <span className={seat ? styles.portraitSeat : styles.portrait} data-portrait={art} aria-hidden>
      {art ? <AvatarArt avatar={avatar} /> : initialOf(name)}
    </span>
  )
}
