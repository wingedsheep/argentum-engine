import React from 'react'
import type { Keyword, ClientCardEffect, Color } from '../../../types'
import { keywordIcons, genericKeywordIcon, displayableKeywords, protectionIcon } from '../../../assets/icons/keywords'
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
          <div
            style={{
              width: size,
              height: size,
              backgroundColor: PROTECTION_COLORS[color] ?? '#aaa',
              WebkitMaskImage: `url(${protectionIcon})`,
              WebkitMaskSize: 'contain',
              WebkitMaskRepeat: 'no-repeat',
              WebkitMaskPosition: 'center',
              maskImage: `url(${protectionIcon})`,
              maskSize: 'contain',
              maskRepeat: 'no-repeat',
              maskPosition: 'center',
            }}
          />
        </div>
      ))}
    </div>
  )
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
            style={styles.activeEffectBadge}
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
        }}>
          {hoveredEffectData.description}
        </div>
      )}
    </>
  )
}
