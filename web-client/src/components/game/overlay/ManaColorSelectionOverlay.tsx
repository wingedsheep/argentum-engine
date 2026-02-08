import { useGameStore } from '../../../store/gameStore'

/**
 * Color styles for the five Magic colors.
 */
const COLOR_STYLES: Record<string, { bg: string; hover: string; label: string }> = {
  WHITE: { bg: '#f5f0e0', hover: '#faf6eb', label: 'White' },
  BLUE: { bg: '#1a4a7a', hover: '#1e5a94', label: 'Blue' },
  BLACK: { bg: '#2a2a2a', hover: '#3a3a3a', label: 'Black' },
  RED: { bg: '#8b2020', hover: '#a02828', label: 'Red' },
  GREEN: { bg: '#1a5a2a', hover: '#1e6e34', label: 'Green' },
}

const COLORS = ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN']

/**
 * Overlay for selecting a mana color when activating "add one mana of any color" abilities.
 */
export function ManaColorSelectionOverlay() {
  const manaColorSelectionState = useGameStore((s) => s.manaColorSelectionState)
  const confirmManaColorSelection = useGameStore((s) => s.confirmManaColorSelection)
  const cancelManaColorSelection = useGameStore((s) => s.cancelManaColorSelection)

  if (!manaColorSelectionState) return null

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 24,
        zIndex: 1000,
        pointerEvents: 'auto',
      }}
    >
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: 24,
          fontWeight: 600,
        }}
      >
        Choose Mana Color
      </h2>

      <div
        style={{
          display: 'flex',
          gap: 12,
          flexWrap: 'wrap',
          justifyContent: 'center',
        }}
      >
        {COLORS.map((color) => {
          const style = COLOR_STYLES[color]!
          const isLight = color === 'WHITE'
          return (
            <button
              key={color}
              onClick={() => confirmManaColorSelection(color)}
              style={{
                backgroundColor: style.bg,
                color: isLight ? '#1a1a1a' : '#f0f0f0',
                border: `2px solid ${isLight ? '#c0b080' : 'transparent'}`,
                padding: '14px 28px',
                fontSize: 18,
                fontWeight: 600,
                borderRadius: 8,
                cursor: 'pointer',
                minWidth: 100,
                transition: 'all 0.15s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = style.hover
                e.currentTarget.style.transform = 'translateY(-2px)'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = style.bg
                e.currentTarget.style.transform = 'translateY(0)'
              }}
            >
              {style.label}
            </button>
          )
        })}
      </div>

      <button
        onClick={cancelManaColorSelection}
        style={{
          backgroundColor: '#444',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          padding: '10px 24px',
          fontSize: 16,
          fontWeight: 600,
          cursor: 'pointer',
          marginTop: 8,
          transition: 'all 0.15s',
        }}
      >
        Cancel
      </button>
    </div>
  )
}
