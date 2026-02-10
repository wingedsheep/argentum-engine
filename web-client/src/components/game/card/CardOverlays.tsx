import React from 'react'
import type { Keyword, ClientCardEffect, Color } from '../../../types'
import { keywordIcons, genericKeywordIcon, displayableKeywords } from '../../../assets/icons/keywords'
import { styles } from '../board/styles'

/** MTG color to CSS color mapping for protection shield icons */
const PROTECTION_COLORS: Record<string, string> = {
  WHITE: '#f5f0e0',
  BLUE: '#4a90d9',
  BLACK: '#888888',
  RED: '#d04040',
  GREEN: '#40a050',
}

/**
 * Container component for keyword ability icons on a card.
 * Uses SVG icons from assets/icons/keywords.
 * Protection icons are rendered separately with color tinting.
 */
export function KeywordIcons({
  keywords,
  protections,
  size,
}: {
  keywords: readonly Keyword[]
  protections: readonly Color[]
  size: number
}) {
  // Filter out PROTECTION from normal keywords (rendered via protections array instead)
  const filteredKeywords = keywords.filter(k => displayableKeywords.has(k) && k !== 'PROTECTION')
  const hasProtections = protections.length > 0
  const hasKeywords = filteredKeywords.length > 0

  if (!hasKeywords && !hasProtections) return null

  return (
    <div style={styles.keywordIconsContainer}>
      {filteredKeywords.map((keyword) => (
        <div key={keyword} style={styles.keywordIconWrapper} title={keyword.replace(/_/g, ' ')}>
          <img
            src={keywordIcons[keyword] ?? genericKeywordIcon}
            alt={keyword}
            style={{
              width: size,
              height: size,
              display: 'block',
              filter: 'brightness(0) invert(1)', // Make SVG white
            }}
          />
        </div>
      ))}
      {protections.map((color) => (
        <div
          key={`prot-${color}`}
          style={styles.keywordIconWrapper}
          title={`Protection from ${color.toLowerCase()}`}
        >
          <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M12 2L3 6v5c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V6l-9-4zm0 2.18l7 3.12V11c0 4.52-3.13 8.69-7 9.93C8.13 19.69 5 15.52 5 11V7.3l7-3.12zM7 8.73V11c0 3.33 2.18 6.46 5 7.74 2.82-1.28 5-4.41 5-7.74V8.73l-5-2.22-5 2.22z"
              fill={PROTECTION_COLORS[color] ?? '#aaa'}
            />
          </svg>
        </div>
      ))}
    </div>
  )
}

/** Get badge style overrides based on effect icon type */
function getBadgeStyle(icon?: string): React.CSSProperties {
  switch (icon) {
    case 'prevent-damage':
      return {
        backgroundColor: 'rgba(60, 130, 180, 0.9)',
        border: '1px solid rgba(140, 200, 255, 0.5)',
      }
    case 'regeneration':
      return {
        backgroundColor: 'rgba(40, 120, 60, 0.9)',
        border: '1px solid rgba(120, 220, 140, 0.5)',
      }
    case 'cant-block':
      return {
        backgroundColor: 'rgba(180, 60, 60, 0.9)',
        border: '1px solid rgba(255, 140, 140, 0.5)',
      }
    default:
      return {}
  }
}

/** Get tooltip border color based on effect icon type */
function getTooltipBorderColor(icon?: string): string {
  switch (icon) {
    case 'prevent-damage':
      return 'rgba(60, 130, 180, 0.5)'
    case 'regeneration':
      return 'rgba(40, 120, 60, 0.5)'
    case 'cant-block':
      return 'rgba(180, 60, 60, 0.5)'
    default:
      return 'rgba(150, 50, 200, 0.5)'
  }
}

/**
 * Container component for active effect badges on a card.
 * Used for temporary effects like "can't be blocked except by black creatures".
 */
export function ActiveEffectBadges({ effects }: { effects: readonly ClientCardEffect[] }) {
  const [hoveredEffect, setHoveredEffect] = React.useState<string | null>(null)
  const [tooltipPos, setTooltipPos] = React.useState<{ x: number; y: number } | null>(null)

  if (!effects || effects.length === 0) return null

  const handleMouseEnter = (effectId: string, e: React.MouseEvent) => {
    const rect = e.currentTarget.getBoundingClientRect()
    setTooltipPos({ x: rect.left + rect.width / 2, y: rect.top })
    setHoveredEffect(effectId)
  }

  const handleMouseLeave = () => {
    setHoveredEffect(null)
    setTooltipPos(null)
  }

  const hoveredEffectData = effects.find(e => e.effectId === hoveredEffect)

  return (
    <>
      <div style={styles.activeEffectsContainer}>
        {effects.map((effect) => (
          <div
            key={effect.effectId}
            style={{ ...styles.activeEffectBadge, ...getBadgeStyle(effect.icon) }}
            onMouseEnter={(e) => handleMouseEnter(effect.effectId, e)}
            onMouseLeave={handleMouseLeave}
          >
            <span style={styles.activeEffectText}>{effect.name}</span>
          </div>
        ))}
      </div>
      {hoveredEffect && tooltipPos && hoveredEffectData?.description && (
        <div style={{
          ...styles.cardEffectTooltip,
          left: tooltipPos.x,
          top: tooltipPos.y,
          borderColor: getTooltipBorderColor(hoveredEffectData.icon),
        }}>
          {hoveredEffectData.description}
        </div>
      )}
    </>
  )
}
