import React from 'react'
import type { Keyword, AbilityFlag, ClientCardEffect, Color } from '@/types'
import { keywordManaClass, displayableKeywords } from '@/assets/icons/keywords'
import { styles } from '../board/styles'

/** MTG color to mana-font protection class mapping */
const PROTECTION_CLASSES: Record<string, string> = {
  WHITE: 'ability-protection-white',
  BLUE: 'ability-protection-blue',
  BLACK: 'ability-protection-black',
  RED: 'ability-protection-red',
  GREEN: 'ability-protection-green',
}

/** MTG color to CSS color for protection / hexproof icon tinting */
const COLOR_TINTS: Record<string, string> = {
  WHITE: '#f5f0e0',
  BLUE: '#4a90d9',
  BLACK: '#888888',
  RED: '#d04040',
  GREEN: '#40a050',
}

/**
 * Container component for keyword ability icons on a card.
 * Uses mana-font icon classes for keyword and protection symbols.
 */
export function KeywordIcons({
  keywords,
  abilityFlags,
  protections,
  hexproofFromColors,
  size,
}: {
  keywords: readonly Keyword[]
  abilityFlags?: readonly AbilityFlag[]
  protections: readonly Color[]
  hexproofFromColors?: readonly Color[]
  size: number
}) {
  // Filter out PROTECTION (rendered via protections array) and FIRST_STRIKE when DOUBLE_STRIKE is present.
  // Also drop generic HEXPROOF when the creature only has per-color hexproof — the colored shields below
  // already convey the protection set, and showing an uncolored shield alongside misleads the player.
  const hexproofFromList = hexproofFromColors ?? []
  const hasFullHexproof = keywords.includes('HEXPROOF' as Keyword)
  const hasDoubleStrike = keywords.includes('DOUBLE_STRIKE' as Keyword)
  const filteredKeywords = keywords.filter(k =>
    displayableKeywords.has(k)
    && k !== 'PROTECTION'
    && !(k === 'FIRST_STRIKE' && hasDoubleStrike)
    && !(k === 'HEXPROOF' && !hasFullHexproof && hexproofFromList.length > 0)
  )
  const displayableFlags = (abilityFlags ?? []).filter(f => displayableKeywords.has(f))
  const hasProtections = protections.length > 0
  const hasHexproofFrom = hexproofFromList.length > 0
  const hasKeywords = filteredKeywords.length > 0 || displayableFlags.length > 0

  if (!hasKeywords && !hasProtections && !hasHexproofFrom) return null

  return (
    <div style={styles.keywordIconsContainer}>
      {filteredKeywords.map((keyword) => (
        <div key={keyword} style={styles.keywordIconWrapper} title={keyword.replace(/_/g, ' ')}>
          <i
            className={`ms ms-${keywordManaClass[keyword] ?? 'ability-static'}`}
            style={{
              fontSize: size,
              color: '#ffffff',
              display: 'block',
              lineHeight: 1,
            }}
          />
        </div>
      ))}
      {displayableFlags.map((flag) => (
        <div key={flag} style={styles.keywordIconWrapper} title={flag.replace(/_/g, ' ')}>
          <i
            className={`ms ms-${keywordManaClass[flag] ?? 'ability-static'}`}
            style={{
              fontSize: size,
              color: '#ffffff',
              display: 'block',
              lineHeight: 1,
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
          <i
            className={`ms ms-${PROTECTION_CLASSES[color] ?? 'ability-protection'}`}
            style={{
              fontSize: size,
              color: COLOR_TINTS[color] ?? '#aaa',
              display: 'block',
              lineHeight: 1,
            }}
          />
        </div>
      ))}
      {hexproofFromList.map((color) => (
        <div
          key={`hexproof-${color}`}
          style={{
            ...styles.keywordIconWrapper,
            // Tinted ring + tinted icon make the per-color shield read at a glance.
            border: `1px solid ${COLOR_TINTS[color] ?? '#aaa'}`,
            boxShadow: `0 0 4px ${COLOR_TINTS[color] ?? '#aaa'}`,
          }}
          title={`Hexproof from ${color.toLowerCase()}`}
        >
          <i
            className="ms ms-ability-hexproof"
            style={{
              fontSize: size,
              color: COLOR_TINTS[color] ?? '#aaa',
              display: 'block',
              lineHeight: 1,
            }}
          />
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
    case 'must-attack':
      return {
        backgroundColor: 'rgba(200, 120, 20, 0.9)',
        border: '1px solid rgba(255, 180, 80, 0.5)',
      }
    case 'condition-met':
      return {
        backgroundColor: 'rgba(40, 120, 60, 0.9)',
        border: '1px solid rgba(120, 220, 140, 0.5)',
      }
    case 'condition-unmet':
      return {
        backgroundColor: 'rgba(100, 100, 100, 0.9)',
        border: '1px solid rgba(160, 160, 160, 0.5)',
      }
    case 'cant-attack':
      return {
        backgroundColor: 'rgba(180, 60, 60, 0.9)',
        border: '1px solid rgba(255, 140, 140, 0.5)',
      }
    case 'exile-on-death':
      return {
        backgroundColor: 'rgba(120, 60, 140, 0.9)',
        border: '1px solid rgba(200, 140, 255, 0.5)',
      }
    case 'redirect':
      return {
        backgroundColor: 'rgba(180, 130, 40, 0.9)',
        border: '1px solid rgba(255, 210, 100, 0.5)',
      }
    case 'lost-abilities':
      return {
        backgroundColor: 'rgba(70, 70, 90, 0.9)',
        border: '1px solid rgba(160, 160, 200, 0.5)',
      }
    case 'type-change':
      return {
        backgroundColor: 'rgba(80, 110, 160, 0.9)',
        border: '1px solid rgba(160, 200, 255, 0.5)',
      }
    case 'color-change':
      // Dark badge with a five-color rainbow border — text stays legible while the
      // rainbow ring instantly tells the player "colors changed / all colors".
      return {
        backgroundColor: 'rgba(20, 20, 30, 0.92)',
        border: '2px solid transparent',
        backgroundImage:
          'linear-gradient(rgba(20, 20, 30, 0.92), rgba(20, 20, 30, 0.92)),' +
          'linear-gradient(90deg, #f5f0e0 0%, #4a90d9 25%, #888888 50%, #d04040 75%, #40a050 100%)',
        backgroundOrigin: 'border-box',
        backgroundClip: 'padding-box, border-box',
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
    case 'cant-attack':
      return 'rgba(180, 60, 60, 0.5)'
    case 'must-attack':
      return 'rgba(200, 120, 20, 0.5)'
    case 'condition-met':
      return 'rgba(40, 120, 60, 0.5)'
    case 'condition-unmet':
      return 'rgba(100, 100, 100, 0.5)'
    case 'exile-on-death':
      return 'rgba(120, 60, 140, 0.5)'
    case 'redirect':
      return 'rgba(180, 130, 40, 0.5)'
    case 'lost-abilities':
      return 'rgba(160, 160, 200, 0.5)'
    case 'type-change':
      return 'rgba(160, 200, 255, 0.5)'
    case 'color-change':
      return 'rgba(255, 255, 255, 0.7)'
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
