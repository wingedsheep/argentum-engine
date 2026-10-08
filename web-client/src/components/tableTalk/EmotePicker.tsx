import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useGameStore } from '@/store/gameStore'
import { useIdentityColor } from '@/store/selectors'
import type { EntityId, Emote } from '@/types'
import { EMOTE_COOLDOWN_MS, useTableTalkStore } from '@/store/tableTalkStore'
import { EMOTE_GROUPS } from './emotes'
import { placePicker, type Placement } from './pickerPlacement'
import styles from './TableTalk.module.css'

/**
 * The emote button beside your life orb and the picker it opens: fourteen presets in three groups, and
 * a mute switch per opponent underneath. Sending closes the picker
 * and starts a short cooldown, drawn as a ring draining around the button.
 *
 * The picker is portalled to the body and pinned to the button's rect: the HUD row the button lives
 * in stacks below the battlefields, so a popover rendered in place would open underneath them. It is
 * measured before it shows, so {@link placePicker} can keep it on screen however short the window.
 */
export function EmotePicker() {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const pickerRef = useRef<HTMLDivElement>(null)
  const [placement, setPlacement] = useState<Placement | null>(null)
  useLayoutEffect(() => {
    if (!open) { setPlacement(null); return }
    const place = () => {
      const anchor = rootRef.current?.getBoundingClientRect()
      const picker = pickerRef.current
      if (!anchor || !picker) return
      // Natural size: measured with any earlier max-height lifted.
      const prev = picker.style.maxHeight
      picker.style.maxHeight = 'none'
      const size = { width: picker.offsetWidth, height: picker.offsetHeight }
      picker.style.maxHeight = prev
      setPlacement(placePicker(anchor, size, { width: window.innerWidth, height: window.innerHeight }))
    }
    place()
    window.addEventListener('resize', place)
    return () => window.removeEventListener('resize', place)
  }, [open])
  const sendEmote = useTableTalkStore((s) => s.sendEmote)
  const lastSentAt = useTableTalkStore((s) => s.lastSentAt)
  const muted = useTableTalkStore((s) => s.muted)
  const toggleMute = useTableTalkStore((s) => s.toggleMute)
  const players = useGameStore((s) => s.gameState?.players)
  const viewerId = useGameStore((s) => s.playerId)
  const opponents = useMemo(() => players?.filter((p) => p.playerId !== viewerId) ?? [], [players, viewerId])
  const cooling = useCooldown(lastSentAt)

  const send = (emote: Emote) => {
    if (cooling) return
    sendEmote(emote)
    setOpen(false)
  }

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    const onPointer = (e: PointerEvent) => {
      const target = e.target as Node
      if (!rootRef.current?.contains(target) && !pickerRef.current?.contains(target)) setOpen(false)
    }
    window.addEventListener('keydown', onKey)
    window.addEventListener('pointerdown', onPointer, true)
    return () => {
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('pointerdown', onPointer, true)
    }
  })

  return (
    <div ref={rootRef} className={styles.pickerRoot} onClick={(e) => e.stopPropagation()}>
      <button
        type="button"
        className={styles.emoteButton}
        data-open={open || undefined}
        data-cooling={cooling || undefined}
        style={cooling ? { ['--cooldown-ms' as string]: `${EMOTE_COOLDOWN_MS}ms` } : undefined}
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Emotes"
        title="Emotes"
        data-testid="emote-button"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 9 9 0 0 1-3.8-.8L3 20.5l1.5-4.3A8 8 0 0 1 3 11.5 8.4 8.4 0 0 1 12 3a8.4 8.4 0 0 1 9 8.5z" />
          <path d="M8.5 13.5s1.2 1.6 3.5 1.6 3.5-1.6 3.5-1.6" />
          <path d="M9 9.5h.01M15 9.5h.01" strokeWidth="2.6" />
        </svg>
      </button>

      {open && createPortal(
        <div
          ref={pickerRef}
          className={styles.picker}
          data-direction={placement?.direction ?? 'up'}
          role="menu"
          aria-label="Send an emote"
          onClick={(e) => e.stopPropagation()}
          // Rendered hidden for one layout pass so it can be measured, then placed.
          style={placement
            ? { left: placement.left, top: placement.top, ...(placement.maxHeight ? { maxHeight: placement.maxHeight } : {}) }
            : { left: 0, top: 0, visibility: 'hidden' }}
        >
          {EMOTE_GROUPS.map((group) => (
            <div key={group.title} className={styles.group} data-mood={group.mood}>
              <p className={styles.groupTitle}>{group.title}</p>
              <div className={styles.grid}>
                {group.emotes.map((info) => (
                  <button
                    key={info.emote}
                    type="button"
                    role="menuitem"
                    className={styles.emoteOption}
                    onClick={() => send(info.emote)}
                    disabled={cooling}
                    data-testid={`emote-${info.emote}`}
                  >
                    <span className={styles.optionIcon} aria-hidden>{info.icon}</span>
                    <span className={styles.optionLabel}>{info.label}</span>
                  </button>
                ))}
              </div>
            </div>
          ))}
          {opponents.length > 0 && (
            <div className={styles.muteList}>
              {opponents.map((p) => (
                <label key={p.playerId} className={styles.muteRow}>
                  {opponents.length > 1 && <SeatDot playerId={p.playerId} />}
                  <span className={styles.muteName}>Show {opponents.length > 1 ? `${p.name}’s` : 'their'} emotes</span>
                  <input
                    type="checkbox"
                    role="switch"
                    className={styles.switch}
                    checked={!muted[p.playerId]}
                    onChange={() => toggleMute(p.playerId)}
                    data-testid={`emote-mute-${p.playerId}`}
                  />
                </label>
              ))}
            </div>
          )}
        </div>,
        document.body,
      )}
    </div>
  )
}

/**
 * The seat's colour, matching its rail chip and name plate — at a multiplayer table two AIs can share
 * a name, and the dot is what tells their mute switches apart.
 */
function SeatDot({ playerId }: { playerId: EntityId }) {
  const color = useIdentityColor(playerId)
  return <span className={styles.seatDot} style={{ background: color.base, boxShadow: `0 0 5px ${color.base}` }} aria-hidden />
}

/** True while the last send is inside the cooldown; re-renders once when it lapses. */
function useCooldown(lastSentAt: number): boolean {
  const [, tick] = useState(0)
  const remaining = lastSentAt + EMOTE_COOLDOWN_MS - Date.now()
  useEffect(() => {
    if (remaining <= 0) return
    const id = window.setTimeout(() => tick((n) => n + 1), remaining)
    return () => window.clearTimeout(id)
  }, [remaining])
  return remaining > 0
}
