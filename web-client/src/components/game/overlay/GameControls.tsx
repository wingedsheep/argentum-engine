import React, { useState, useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useAuthStore } from '@/store/authStore'
import { useIsSharedLifeTeamGame } from '@/store/selectors'
import { useResponsiveContext } from '../board/shared'
import { GearIcon, PreferencesDialog } from '@/components/preferences/PreferencesDialog'

/**
 * Concede button with confirmation, positioned top-right.
 */
export function ConcedeButton() {
  const concede = useGameStore((state) => state.concede)
  // In a pod, conceding eliminates *you* while the game goes on (CR 800.4a) — say so, since
  // "Concede" reads as "end the game" everywhere else. Two-Headed Giant is the exception: a team
  // wins and loses together, so one concession takes the whole team out (CR 810.8b).
  const isPod = useGameStore((state) => (state.gameState?.players.length ?? 0) > 2)
  const teamConcedes = useIsSharedLifeTeamGame()
  const confirmLabel = teamConcedes ? 'Concede for the team' : isPod ? 'Concede & leave' : 'Confirm'
  // Rides inside the button so the confirm row keeps its width — beside the buttons it ran into
  // the top-right board's name plate, below them into that cell's collapse button.
  const podNote = teamConcedes ? 'your team is out' : isPod ? 'the others play on' : null
  const [confirming, setConfirming] = useState(false)
  const responsive = useResponsiveContext()

  const base: React.CSSProperties = {
    position: 'absolute',
    top: responsive.isMobile ? 8 : 12,
    right: responsive.isMobile ? 8 : 12,
    zIndex: 100,
    display: 'flex',
    gap: 4,
  }

  if (confirming) {
    return (
      <div style={base}>
        <button
          onClick={() => { concede(); setConfirming(false) }}
          style={{
            padding: responsive.isMobile ? '6px 10px' : '8px 14px',
            fontSize: responsive.fontSize.small,
            background: 'var(--gradient-danger)',
            color: 'white',
            border: '1px solid rgba(255, 255, 255, 0.15)',
            borderRadius: 'var(--chrome-radius)',
            cursor: 'pointer',
            fontWeight: 600,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            lineHeight: 1.15,
          }}
        >
          <span>{confirmLabel}</span>
          {podNote && !responsive.isMobile && (
            <span style={{ fontSize: 9, fontWeight: 500, opacity: 0.85 }}>{podNote}</span>
          )}
        </button>
        <button
          onClick={() => setConfirming(false)}
          style={{
            padding: responsive.isMobile ? '6px 10px' : '8px 14px',
            fontSize: responsive.fontSize.small,
            backgroundColor: 'var(--chrome-bg)',
            backdropFilter: 'var(--chrome-blur)',
            WebkitBackdropFilter: 'var(--chrome-blur)',
            color: 'var(--chrome-text)',
            border: '1px solid var(--chrome-border)',
            borderRadius: 'var(--chrome-radius)',
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>
    )
  }

  return (
    <div style={base}>
      <button
        onClick={() => setConfirming(true)}
        style={{
          padding: responsive.isMobile ? '6px 10px' : '8px 14px',
          fontSize: responsive.fontSize.small,
          backgroundColor: 'var(--chrome-bg)',
          backdropFilter: 'var(--chrome-blur)',
          WebkitBackdropFilter: 'var(--chrome-blur)',
          color: 'var(--chrome-danger-text)',
          border: '1px solid var(--chrome-danger-border)',
          borderRadius: 'var(--chrome-radius)',
          cursor: 'pointer',
        }}
      >
        Concede
      </button>
    </div>
  )
}

/**
 * Concede button for use outside GameBoard (e.g. mulligan phase).
 * Does not depend on ResponsiveProvider.
 */
export function StandaloneConcedeButton() {
  const concede = useGameStore((state) => state.concede)
  const [confirming, setConfirming] = useState(false)

  const base: React.CSSProperties = {
    position: 'absolute',
    top: 12,
    right: 12,
    zIndex: 1100,
    display: 'flex',
    gap: 4,
  }

  if (confirming) {
    return (
      <div style={base}>
        <button
          onClick={() => { concede(); setConfirming(false) }}
          style={{
            padding: '8px 14px',
            fontSize: 13,
            background: 'var(--gradient-danger)',
            color: 'white',
            border: '1px solid rgba(255, 255, 255, 0.15)',
            borderRadius: 'var(--chrome-radius)',
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          Confirm
        </button>
        <button
          onClick={() => setConfirming(false)}
          style={{
            padding: '8px 14px',
            fontSize: 13,
            backgroundColor: 'var(--chrome-bg)',
            backdropFilter: 'var(--chrome-blur)',
            WebkitBackdropFilter: 'var(--chrome-blur)',
            color: 'var(--chrome-text)',
            border: '1px solid var(--chrome-border)',
            borderRadius: 'var(--chrome-radius)',
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>
    )
  }

  return (
    <div style={base}>
      <button
        onClick={() => setConfirming(true)}
        style={{
          padding: '8px 14px',
          fontSize: 13,
          backgroundColor: 'var(--chrome-bg)',
          backdropFilter: 'var(--chrome-blur)',
          WebkitBackdropFilter: 'var(--chrome-blur)',
          color: 'var(--chrome-danger-text)',
          border: '1px solid var(--chrome-danger-border)',
          borderRadius: 'var(--chrome-radius)',
          cursor: 'pointer',
        }}
      >
        Concede
      </button>
    </div>
  )
}

/**
 * Subtle eye-icon badge indicating the number of spectators currently watching
 * this player's game. Hidden when there are zero spectators. Hovering reveals
 * a small popover listing the spectator names.
 *
 * Positioned in the top-left, just to the right of the fullscreen button so it
 * sits in the same low-attention strip without crowding the game UI.
 */
export function SpectatorCountBadge() {
  const spectatorCount = useGameStore((state) => state.spectatorCount)
  const spectatorNames = useGameStore((state) => state.spectatorNames)
  const responsive = useResponsiveContext()
  const [hovered, setHovered] = useState(false)
  // A signed-in player has the chat button after the gear; sit after that too.
  const chatButton = useAuthStore((state) => state.accountsEnabled && state.status === 'authenticated')

  if (spectatorCount <= 0) return null

  const label = spectatorCount === 1 ? '1 watching' : `${spectatorCount} watching`

  return (
    <div
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 8 : 12,
        // Sits to the right of the icon-only FullscreenButton (30/34px at left 8/12), the
        // GamePreferencesButton beside it, and the ChatButton after that when signed in.
        left: (responsive.isMobile ? 84 : 96) + (chatButton ? (responsive.isMobile ? 38 : 42) : 0),
        // Above the multiplayer opponent rail (z 120): the name popover drops down
        // into the rail column, and the seat chips would otherwise paint over it.
        // The badge itself sits above the rail's first chip, so nothing is hidden.
        zIndex: 140,
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <div
        aria-label={label}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          padding: responsive.isMobile ? '6px 8px' : '6px 10px',
          fontSize: responsive.fontSize.small,
          backgroundColor: 'rgba(0, 0, 0, 0.35)',
          color: '#9aa6b2',
          border: '1px solid #2c333d',
          borderRadius: 'var(--chrome-radius)',
          userSelect: 'none',
          opacity: 0.85,
          cursor: 'default',
        }}
      >
        <span aria-hidden style={{ fontSize: responsive.fontSize.small }}>👁</span>
        <span>{spectatorCount}</span>
      </div>
      {hovered && (
        <div
          role="tooltip"
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            marginTop: 6,
            padding: '8px 10px',
            minWidth: 140,
            maxWidth: 220,
            fontSize: responsive.fontSize.small,
            // Near-opaque: it now floats over the rail chips, and a see-through
            // panel makes the names underneath it hard to read.
            backgroundColor: 'rgba(0, 0, 0, 0.95)',
            color: '#d4dae1',
            border: '1px solid #2c333d',
            borderRadius: 'var(--chrome-radius)',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.4)',
            pointerEvents: 'none',
            whiteSpace: 'nowrap',
          }}
        >
          <div style={{ color: '#9aa6b2', fontSize: responsive.fontSize.small, marginBottom: 4 }}>
            {label}
          </div>
          {spectatorNames.length > 0 ? (
            spectatorNames.map((name) => (
              <div key={name} style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {name}
              </div>
            ))
          ) : (
            <div style={{ color: '#6b7480', fontStyle: 'italic' }}>(names unavailable)</div>
          )}
        </div>
      )}
    </div>
  )
}

/**
 * Fullscreen toggle button, positioned top-left.
 */
export function FullscreenButton() {
  const [isFullscreen, setIsFullscreen] = useState(false)
  const responsive = useResponsiveContext()

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  const toggleFullscreen = async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen()
      } else {
        await document.exitFullscreen()
      }
    } catch (err) {
      console.error('Fullscreen error:', err)
    }
  }

  const size = responsive.isMobile ? 30 : 34
  const title = isFullscreen ? 'Exit fullscreen (Esc)' : 'Enter fullscreen'
  // Icon-only, like the landing and lobby top bars — the label cost a card's width in the corner.
  return (
    <button
      onClick={toggleFullscreen}
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 8 : 12,
        left: responsive.isMobile ? 8 : 12,
        zIndex: 100,
        width: size,
        height: size,
        padding: 0,
        backgroundColor: 'var(--chrome-bg)',
        backdropFilter: 'var(--chrome-blur)',
        WebkitBackdropFilter: 'var(--chrome-blur)',
        color: 'var(--chrome-text)',
        border: '1px solid var(--chrome-border)',
        borderRadius: 'var(--chrome-radius)',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
      title={title}
      aria-label={title}
    >
      <svg viewBox="0 0 24 24" width={responsive.isMobile ? 15 : 17} height={responsive.isMobile ? 15 : 17} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
        {isFullscreen
          ? <path d="M9 4v5H4 M15 4v5h5 M9 20v-5H4 M15 20v-5h5" />
          : <path d="M4 9V4h5 M20 9V4h-5 M4 15v5h5 M20 15v5h-5" />}
      </svg>
    </button>
  )
}

/**
 * In-game preferences: a gear beside the fullscreen button that opens the preferences dialog over
 * the board — stacking, stops, auto-tap and motion, without leaving the game.
 */
export function GamePreferencesButton() {
  const [open, setOpen] = useState(false)
  const responsive = useResponsiveContext()
  const size = responsive.isMobile ? 30 : 34
  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        style={{
          position: 'absolute',
          top: responsive.isMobile ? 8 : 12,
          // To the right of the FullscreenButton (30/34px at left 8/12).
          left: responsive.isMobile ? 46 : 54,
          zIndex: 100,
          width: size,
          height: size,
          padding: 0,
          backgroundColor: 'var(--chrome-bg)',
          backdropFilter: 'var(--chrome-blur)',
          WebkitBackdropFilter: 'var(--chrome-blur)',
          color: 'var(--chrome-text)',
          border: '1px solid var(--chrome-border)',
          borderRadius: 'var(--chrome-radius)',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
        title="Preferences"
        aria-label="Preferences"
        data-testid="game-preferences-button"
      >
        <GearIcon size={responsive.isMobile ? 15 : 17} />
      </button>
      {open && <PreferencesDialog onClose={() => setOpen(false)} />}
    </>
  )
}
