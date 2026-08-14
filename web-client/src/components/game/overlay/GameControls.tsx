import React, { useState, useEffect } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useResponsiveContext } from '../board/shared'

/**
 * Concede button with confirmation, positioned top-right.
 */
export function ConcedeButton() {
  const concede = useGameStore((state) => state.concede)
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
            backgroundColor: '#cc0000',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          Confirm
        </button>
        <button
          onClick={() => setConfirming(false)}
          style={{
            padding: responsive.isMobile ? '6px 10px' : '8px 14px',
            fontSize: responsive.fontSize.small,
            backgroundColor: '#222',
            color: '#aaa',
            border: '1px solid #333',
            borderRadius: 6,
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
          backgroundColor: 'transparent',
          color: '#cc0000',
          border: '1px solid #cc0000',
          borderRadius: 6,
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
            backgroundColor: '#cc0000',
            color: 'white',
            border: 'none',
            borderRadius: 6,
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
            backgroundColor: '#222',
            color: '#aaa',
            border: '1px solid #333',
            borderRadius: 6,
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
          backgroundColor: 'transparent',
          color: '#cc0000',
          border: '1px solid #cc0000',
          borderRadius: 6,
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

  if (spectatorCount <= 0) return null

  const label = spectatorCount === 1 ? '1 watching' : `${spectatorCount} watching`

  return (
    <div
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 8 : 12,
        // Sits to the right of FullscreenButton (which is at left: 8/12).
        // The button is roughly 90–110px wide depending on label, so 110/130
        // gives a safe visual gap without measuring.
        left: responsive.isMobile ? 110 : 130,
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
          borderRadius: 6,
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
            borderRadius: 6,
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

  return (
    <button
      onClick={toggleFullscreen}
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 8 : 12,
        left: responsive.isMobile ? 8 : 12,
        zIndex: 100,
        padding: responsive.isMobile ? '6px 10px' : '8px 14px',
        fontSize: responsive.fontSize.small,
        backgroundColor: 'transparent',
        color: '#888',
        border: '1px solid #444',
        borderRadius: 6,
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        gap: 4,
      }}
      title={isFullscreen ? 'Exit fullscreen (Esc)' : 'Enter fullscreen'}
    >
      {isFullscreen ? '⛶' : '⛶'} {isFullscreen ? 'Exit' : 'Fullscreen'}
    </button>
  )
}
