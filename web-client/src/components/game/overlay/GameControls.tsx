import React, { useState, useEffect } from 'react'
import { useGameStore } from '../../../store/gameStore'
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
