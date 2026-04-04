import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ChooseColorDecision } from '@/types'
import { ColorDisplayNames } from '@/types'
import { ManaSymbol } from '@/components/ui/ManaSymbols.tsx'

/**
 * Map color enum names to mana symbol keys (W, U, B, R, G).
 */
const COLOR_TO_SYMBOL: Record<string, string> = {
  WHITE: 'W',
  BLUE: 'U',
  BLACK: 'B',
  RED: 'R',
  GREEN: 'G',
}

/**
 * Color styling for buttons — gradient backgrounds inspired by MTG mana colors.
 */
const COLOR_CONFIG: Record<
  string,
  { gradient: string; hoverGradient: string; glow: string; border: string; textColor: string }
> = {
  WHITE: {
    gradient: 'linear-gradient(135deg, #FFF9E6 0%, #F5EDD0 50%, #E8DDB8 100%)',
    hoverGradient: 'linear-gradient(135deg, #FFFDF5 0%, #FAF3E0 50%, #F0E6CC 100%)',
    glow: 'rgba(245, 220, 150, 0.5)',
    border: '#C8B464',
    textColor: '#5A4A1E',
  },
  BLUE: {
    gradient: 'linear-gradient(135deg, #0A3D6B 0%, #1565A8 50%, #1976C8 100%)',
    hoverGradient: 'linear-gradient(135deg, #0E4D82 0%, #1A75C0 50%, #2186D8 100%)',
    glow: 'rgba(30, 120, 200, 0.5)',
    border: '#2A8AE0',
    textColor: '#E8F0FA',
  },
  BLACK: {
    gradient: 'linear-gradient(135deg, #1A1A1A 0%, #2C2C2C 50%, #383838 100%)',
    hoverGradient: 'linear-gradient(135deg, #222222 0%, #363636 50%, #444444 100%)',
    glow: 'rgba(100, 80, 120, 0.5)',
    border: '#6A5A7A',
    textColor: '#D8D0E0',
  },
  RED: {
    gradient: 'linear-gradient(135deg, #8B1A1A 0%, #C02020 50%, #D83030 100%)',
    hoverGradient: 'linear-gradient(135deg, #A02424 0%, #D43030 50%, #E84040 100%)',
    glow: 'rgba(220, 50, 50, 0.5)',
    border: '#E84848',
    textColor: '#FFE8E0',
  },
  GREEN: {
    gradient: 'linear-gradient(135deg, #0D4A20 0%, #1A7A38 50%, #228B44 100%)',
    hoverGradient: 'linear-gradient(135deg, #125A28 0%, #208A42 50%, #2AA050 100%)',
    glow: 'rgba(40, 160, 70, 0.5)',
    border: '#30B050',
    textColor: '#E0F5E8',
  },
}

/**
 * Choose color decision — floating bottom panel with mana symbol buttons.
 * Keeps the battlefield visible so the player can see the board when deciding.
 */
export function ChooseColorDecisionUI({
  decision,
}: {
  decision: ChooseColorDecision
}) {
  const submitColorDecision = useGameStore((s) => s.submitColorDecision)
  const [hoveredColor, setHoveredColor] = useState<string | null>(null)
  const [collapsed, setCollapsed] = useState(false)

  const handleColorClick = (color: string) => {
    submitColorDecision(color)
  }

  if (collapsed) {
    return (
      <button
        onClick={() => setCollapsed(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 'var(--z-modal)' as unknown as number,
          padding: '8px 20px',
          fontSize: 'var(--font-md)',
          fontWeight: 600,
          background: 'rgba(10, 10, 15, 0.9)',
          color: 'var(--text-primary)',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          borderRadius: 8,
          cursor: 'pointer',
          backdropFilter: 'blur(8px)',
          pointerEvents: 'auto',
          whiteSpace: 'nowrap',
        }}
      >
        {decision.prompt}
      </button>
    )
  }

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        zIndex: 'var(--z-modal)' as unknown as number,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 10,
        padding: '12px 16px 20px',
        background: 'linear-gradient(to top, rgba(10, 10, 15, 0.95) 0%, rgba(10, 10, 15, 0.85) 100%)',
        borderTop: '1px solid rgba(255, 255, 255, 0.1)',
        backdropFilter: 'blur(12px)',
        pointerEvents: 'auto',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
        }}
      >
        <div
          style={{
            color: 'var(--text-primary)',
            fontSize: 'var(--font-md)',
            fontWeight: 600,
            textAlign: 'center',
          }}
        >
          {decision.prompt}
          {decision.context.sourceName && (
            <span style={{ color: 'var(--text-tertiary)', fontWeight: 400, marginLeft: 8 }}>
              — {decision.context.sourceName}
            </span>
          )}
        </div>
        <button
          onClick={() => setCollapsed(true)}
          title="Collapse to view hand"
          style={{
            background: 'none',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 6,
            color: 'var(--text-tertiary)',
            cursor: 'pointer',
            padding: '2px 8px',
            fontSize: 'var(--font-sm)',
            lineHeight: 1,
            flexShrink: 0,
          }}
        >
          ▼
        </button>
      </div>

      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'center',
          gap: 12,
        }}
      >
        {decision.availableColors.map((color) => {
          const config = COLOR_CONFIG[color]
          const displayName = ColorDisplayNames[color as keyof typeof ColorDisplayNames] ?? color
          const isHovered = hoveredColor === color
          const symbol = COLOR_TO_SYMBOL[color]

          return (
            <button
              key={color}
              onClick={() => handleColorClick(color)}
              onMouseEnter={() => setHoveredColor(color)}
              onMouseLeave={() => setHoveredColor(null)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 6,
                background: isHovered ? config?.hoverGradient : config?.gradient,
                color: config?.textColor ?? '#f0f0f0',
                border: `2px solid ${isHovered ? config?.border : 'rgba(255,255,255,0.15)'}`,
                borderRadius: 10,
                padding: '14px 20px 10px',
                cursor: 'pointer',
                minWidth: 80,
                transition: 'all 0.2s ease',
                transform: isHovered ? 'translateY(-3px) scale(1.05)' : 'translateY(0) scale(1)',
                boxShadow: isHovered
                  ? `0 6px 20px ${config?.glow ?? 'rgba(0,0,0,0.3)'}, 0 0 0 1px rgba(255,255,255,0.1)`
                  : '0 2px 8px rgba(0,0,0,0.3)',
              }}
            >
              <div
                style={{
                  filter: isHovered ? 'drop-shadow(0 0 6px rgba(255,255,255,0.3))' : 'none',
                  transition: 'filter 0.2s ease',
                }}
              >
                {symbol ? <ManaSymbol symbol={symbol} size={36} /> : null}
              </div>
              <span
                style={{
                  fontSize: 12,
                  fontWeight: 600,
                  letterSpacing: '0.5px',
                  textTransform: 'uppercase',
                }}
              >
                {displayName}
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
