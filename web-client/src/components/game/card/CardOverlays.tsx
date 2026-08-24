import React from 'react'
import { createPortal } from 'react-dom'
import type { Keyword, AbilityFlag, ClientCardEffect, Color } from '@/types'
import { keywordManaClass, keywordSvgIcon, displayableKeywords } from '@/assets/icons/keywords'
import { SvgGlyph } from '@/assets/icons/SvgGlyph'
import { AbilityText } from '../../ui/ManaSymbols'
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

/** Neutral tint for quality-scoped hexproof shields (e.g. "from monocolored") — no single color fits. */
const HEXPROOF_QUALITY_TINT = '#c8b86a'

/**
 * Renders a single keyword glyph: prefer a local SVG when mapped (for keywords
 * the mana-font Arena set lacks, e.g. PERSIST), otherwise fall back to mana-font.
 */
function KeywordGlyph({ name, size }: { name: string; size: number }) {
  const svgUrl = keywordSvgIcon[name]
  if (svgUrl) {
    return <SvgGlyph url={svgUrl} size={size} color="#ffffff" />
  }
  return (
    <i
      className={`ms ms-${keywordManaClass[name] ?? 'ability-static'}`}
      style={{
        fontSize: size,
        color: '#ffffff',
        display: 'block',
        lineHeight: 1,
      }}
    />
  )
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
  hexproofFromMonocolored,
  hexproofFromMulticolored,
  isSuspected,
  isSolved,
  topOffset,
  size,
}: {
  keywords: readonly Keyword[]
  abilityFlags?: readonly AbilityFlag[]
  protections: readonly Color[]
  hexproofFromColors?: readonly Color[]
  /** Hexproof from monocolored (CR 105.2) — an uncolored hexproof-quality shield. */
  hexproofFromMonocolored?: boolean
  /** Hexproof from multicolored (CR 105.2b) — an uncolored hexproof-quality shield. */
  hexproofFromMulticolored?: boolean
  /** Whether the permanent currently has the suspected status (CR 701.60). */
  isSuspected?: boolean
  /** Whether the permanent is a solved Case (CR 719.3b). */
  isSolved?: boolean
  /** Override the column's top offset (px) so it can clear the ring-bearer badge in the same corner. */
  topOffset?: number
  size: number
}) {
  // Filter out PROTECTION (rendered via protections array) and FIRST_STRIKE when DOUBLE_STRIKE is present.
  // Also drop generic HEXPROOF when the creature only has per-color hexproof — the colored shields below
  // already convey the protection set, and showing an uncolored shield alongside misleads the player.
  const hexproofFromList = hexproofFromColors ?? []
  const hasHexproofFromMonocolored = hexproofFromMonocolored === true
  const hasHexproofFromMulticolored = hexproofFromMulticolored === true
  // Any "hexproof from [quality]" — per-color, monocolored, or multicolored — that conveys the
  // protection set.
  const hasScopedHexproof =
    hexproofFromList.length > 0 || hasHexproofFromMonocolored || hasHexproofFromMulticolored
  const hasFullHexproof = keywords.includes('HEXPROOF' as Keyword)
  const hasDoubleStrike = keywords.includes('DOUBLE_STRIKE' as Keyword)
  const filteredKeywords = keywords.filter(k =>
    displayableKeywords.has(k)
    && k !== 'PROTECTION'
    && !(k === 'FIRST_STRIKE' && hasDoubleStrike)
    && !(k === 'HEXPROOF' && !hasFullHexproof && hasScopedHexproof)
  )
  const displayableFlags = (abilityFlags ?? []).filter(f => displayableKeywords.has(f))
  const hasProtections = protections.length > 0
  const hasHexproofFrom = hasScopedHexproof
  const hasKeywords = filteredKeywords.length > 0 || displayableFlags.length > 0
  const hasSuspected = isSuspected === true
  const hasSolved = isSolved === true

  if (!hasKeywords && !hasProtections && !hasHexproofFrom && !hasSuspected && !hasSolved) return null

  return (
    <div style={topOffset === undefined ? styles.keywordIconsContainer : { ...styles.keywordIconsContainer, top: topOffset }}>
      {hasSuspected && (
        <div key="suspected" style={styles.keywordIconWrapper} title="Suspected (has menace and can't block)">
          <KeywordGlyph name="SUSPECTED" size={size} />
        </div>
      )}
      {hasSolved && (
        <div key="solved" style={styles.keywordIconWrapper} title="Solved (its Solved — ability is active)">
          <KeywordGlyph name="SOLVED" size={size} />
        </div>
      )}
      {filteredKeywords.map((keyword) => (
        <div key={keyword} style={styles.keywordIconWrapper} title={keyword.replace(/_/g, ' ')}>
          <KeywordGlyph name={keyword} size={size} />
        </div>
      ))}
      {displayableFlags.map((flag) => (
        <div key={flag} style={styles.keywordIconWrapper} title={flag.replace(/_/g, ' ')}>
          <KeywordGlyph name={flag} size={size} />
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
      {hasHexproofFromMonocolored && (
        <div
          key="hexproof-monocolored"
          style={{
            ...styles.keywordIconWrapper,
            // Neutral grey ring distinguishes the quality shield from the per-color ones.
            border: `1px solid ${HEXPROOF_QUALITY_TINT}`,
            boxShadow: `0 0 4px ${HEXPROOF_QUALITY_TINT}`,
          }}
          title="Hexproof from monocolored"
        >
          <i
            className="ms ms-ability-hexproof"
            style={{
              fontSize: size,
              color: HEXPROOF_QUALITY_TINT,
              display: 'block',
              lineHeight: 1,
            }}
          />
        </div>
      )}
      {hasHexproofFromMulticolored && (
        <div
          key="hexproof-multicolored"
          style={{
            ...styles.keywordIconWrapper,
            // Same neutral quality ring as the monocolored chip; the tooltip tells them apart.
            border: `1px solid ${HEXPROOF_QUALITY_TINT}`,
            boxShadow: `0 0 4px ${HEXPROOF_QUALITY_TINT}`,
          }}
          title="Hexproof from multicolored"
        >
          <i
            className="ms ms-ability-hexproof"
            style={{
              fontSize: size,
              color: HEXPROOF_QUALITY_TINT,
              display: 'block',
              lineHeight: 1,
            }}
          />
        </div>
      )}
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
    case 'can-attack':
      // Positive/green — a Defender that can attack right now (e.g. after an artifact entered).
      return {
        backgroundColor: 'rgba(40, 120, 60, 0.9)',
        border: '1px solid rgba(120, 220, 140, 0.5)',
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
    // Damage this creature deals is doubled (Mjölnir, Hammer of Thor). Hot red-orange rather than
    // the neutral default: it is a threat readout, and it should read at a glance the way the
    // prevent-damage shield does in the other direction.
    case 'double-damage':
      return {
        backgroundColor: 'rgba(190, 70, 30, 0.9)',
        border: '1px solid rgba(255, 160, 100, 0.5)',
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
    // A creature type this permanent has noted (Long List of the Ents) or secretly chosen
    // (A Killer Among Us). Muted parchment — it is a memo the permanent is carrying, not a
    // change to the board, and the secret variant is only ever shown to the player who wrote it.
    case 'creature-type':
      return {
        backgroundColor: 'rgba(90, 78, 55, 0.92)',
        border: '1px dashed rgba(225, 205, 155, 0.6)',
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
    case 'granted-ability':
      return {
        backgroundColor: 'rgba(150, 50, 200, 0.9)',
        border: '1px solid rgba(220, 160, 255, 0.6)',
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
    case 'can-attack':
      return 'rgba(120, 220, 140, 0.5)'
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
    case 'double-damage':
      return 'rgba(255, 160, 100, 0.5)'
    case 'lost-abilities':
      return 'rgba(160, 160, 200, 0.5)'
    case 'creature-type':
      return 'rgba(225, 205, 155, 0.5)'
    case 'type-change':
      return 'rgba(160, 200, 255, 0.5)'
    case 'color-change':
      return 'rgba(255, 255, 255, 0.7)'
    case 'granted-ability':
      return 'rgba(220, 160, 255, 0.6)'
    default:
      return 'rgba(150, 50, 200, 0.5)'
  }
}

/**
 * Responsive sizing for the active-effect badge column. Derived from `responsive.badges`
 * at the call site so the badges shrink with the actual battlefield card width instead of
 * staying a fixed size that swamps the art on a small (e.g. laptop) board.
 */
export interface ActiveEffectBadgeSizing {
  fontSize: number
  /** CSS padding shorthand, e.g. "1px 3px". */
  padding: string
  /** Distance from the card bottom — scales so the column clears the (also-scaled) P/T overlay. */
  bottom: number
  gap: number
  borderRadius: number
}

/**
 * Container component for active effect badges on a card.
 * Used for temporary effects like "can't be blocked except by black creatures".
 */
export function ActiveEffectBadges({ effects, sizing }: {
  effects: readonly ClientCardEffect[]
  sizing?: ActiveEffectBadgeSizing
}) {
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
      <div style={sizing
        ? { ...styles.activeEffectsContainer, bottom: sizing.bottom, gap: sizing.gap }
        : styles.activeEffectsContainer}>
        {effects.map((effect) => (
          <div
            key={effect.effectId}
            style={{
              ...styles.activeEffectBadge,
              ...getBadgeStyle(effect.icon),
              ...(sizing ? { padding: sizing.padding, borderRadius: sizing.borderRadius } : {}),
            }}
            onMouseEnter={(e) => handleMouseEnter(effect.effectId, e)}
            onMouseLeave={handleMouseLeave}
          >
            <span style={sizing
              ? { ...styles.activeEffectText, fontSize: sizing.fontSize }
              : styles.activeEffectText}>{effect.name}</span>
          </div>
        ))}
      </div>
      {/* Portalled to <body>: the tooltip is position:fixed with viewport coordinates,
          but it renders from inside a card that may sit under a transform (tapped-card
          rotation, the multiplayer board strip's translateX) — a transformed ancestor
          would re-anchor `fixed` to itself and misplace the tooltip. */}
      {hoveredEffect && tooltipPos && hoveredEffectData?.description && createPortal(
        <div style={{
          ...styles.cardEffectTooltip,
          left: tooltipPos.x,
          top: tooltipPos.y,
          borderColor: getTooltipBorderColor(hoveredEffectData.icon),
        }}>
          <AbilityText text={hoveredEffectData.description} size={13} />
        </div>,
        document.body,
      )}
    </>
  )
}
