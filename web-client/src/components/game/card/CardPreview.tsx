import { useState, useEffect } from 'react'
import { useGameStore } from '../../../store/gameStore'
import { selectGameState } from '../../../store/selectors'
import type { EntityId } from '../../../types'
import { getCardImageUrl } from '../../../utils/cardImages'
import { useResponsiveContext, handleImageError, getCounterStatModifier, hasStatCounters } from '../board/shared'
import { styles } from '../board/styles'

/**
 * Card preview overlay - shows enlarged card when hovering.
 * Shows rulings after hovering for 1 second.
 */
export function CardPreview() {
  const hoveredCardId = useGameStore((state) => state.hoveredCardId)
  const gameState = useGameStore(selectGameState)
  const responsive = useResponsiveContext()
  const [showRulings, setShowRulings] = useState(false)
  const [lastHoveredId, setLastHoveredId] = useState<EntityId | null>(null)

  // Show rulings after hovering for 1 second
  useEffect(() => {
    if (hoveredCardId !== lastHoveredId) {
      setLastHoveredId(hoveredCardId)
      setShowRulings(false)
    }

    if (!hoveredCardId) {
      setShowRulings(false)
      return
    }

    const timer = setTimeout(() => {
      setShowRulings(true)
    }, 1000)

    return () => clearTimeout(timer)
  }, [hoveredCardId, lastHoveredId])

  if (!hoveredCardId || !gameState) return null

  const card = gameState.cards[hoveredCardId]
  if (!card) return null

  const cardImageUrl = getCardImageUrl(card.name, card.imageUri, 'large')

  // Calculate preview size - larger than normal cards
  const previewWidth = responsive.isMobile ? 200 : 280
  const previewHeight = Math.round(previewWidth * 1.4)

  // Determine if stats are modified
  const isPowerBuffed = card.power !== null && card.basePower !== null && card.power > card.basePower
  const isPowerDebuffed = card.power !== null && card.basePower !== null && card.power < card.basePower
  const isToughnessBuffed = card.toughness !== null && card.baseToughness !== null && card.toughness > card.baseToughness
  const isToughnessDebuffed = card.toughness !== null && card.baseToughness !== null && card.toughness < card.baseToughness
  const hasStatModifications = isPowerBuffed || isPowerDebuffed || isToughnessBuffed || isToughnessDebuffed

  // Calculate stat breakdown
  const counterModifier = getCounterStatModifier(card)
  const hasCounters = hasStatCounters(card)
  // Effects = total change - counter contribution
  const effectPowerMod = card.power !== null && card.basePower !== null
    ? (card.power - card.basePower) - counterModifier
    : 0
  const effectToughnessMod = card.toughness !== null && card.baseToughness !== null
    ? (card.toughness - card.baseToughness) - counterModifier
    : 0
  const hasEffects = effectPowerMod !== 0 || effectToughnessMod !== 0

  const hasRulings = card.rulings && card.rulings.length > 0

  return (
    <div style={styles.cardPreviewOverlay}>
      <div style={{
        ...styles.cardPreviewContainer,
        width: previewWidth,
      }}>
        {/* Card image */}
        <div style={{
          ...styles.cardPreviewCard,
          width: previewWidth,
          height: previewHeight,
        }}>
          <img
            src={cardImageUrl}
            alt={card.name}
            style={styles.cardPreviewImage}
            onError={(e) => handleImageError(e, card.name, 'large')}
          />
        </div>

        {/* Stats box (for creatures with modifications) */}
        {card.power !== null && card.toughness !== null && hasStatModifications && (
          <div style={styles.cardPreviewStatsBox}>
            {/* Current P/T (large) */}
            <div style={styles.cardPreviewStatsMain}>
              <span style={{
                color: isPowerBuffed ? '#00ff00' : isPowerDebuffed ? '#ff4444' : '#ffffff',
                fontWeight: 700,
                fontSize: responsive.isMobile ? 20 : 26,
              }}>
                {card.power}
              </span>
              <span style={{ color: '#ffffff', fontSize: responsive.isMobile ? 20 : 26 }}>/</span>
              <span style={{
                color: isToughnessBuffed ? '#00ff00' : isToughnessDebuffed ? '#ff4444' : '#ffffff',
                fontWeight: 700,
                fontSize: responsive.isMobile ? 20 : 26,
              }}>
                {card.toughness}
              </span>
            </div>
            {/* Breakdown rows */}
            <div style={styles.cardPreviewStatsBreakdown}>
              {/* Base stats */}
              {card.basePower !== null && card.baseToughness !== null && (
                <div style={styles.cardPreviewStatsRow}>
                  <span style={styles.cardPreviewStatsLabel}>Base</span>
                  <span style={styles.cardPreviewStatsValue}>
                    {card.basePower}/{card.baseToughness}
                  </span>
                </div>
              )}
              {/* Counter contribution */}
              {hasCounters && (
                <div style={styles.cardPreviewStatsRow}>
                  <span style={{...styles.cardPreviewStatsLabel, color: '#66ccff'}}>
                    <span style={{marginRight: 4}}>⬡</span>Counters
                  </span>
                  <span style={{...styles.cardPreviewStatsValue, color: counterModifier >= 0 ? '#66ccff' : '#ff6666'}}>
                    {counterModifier >= 0 ? '+' : ''}{counterModifier}/{counterModifier >= 0 ? '+' : ''}{counterModifier}
                  </span>
                </div>
              )}
              {/* Effects contribution */}
              {hasEffects && (
                <div style={styles.cardPreviewStatsRow}>
                  <span style={{...styles.cardPreviewStatsLabel, color: '#ffcc66'}}>Effects</span>
                  <span style={{...styles.cardPreviewStatsValue, color: '#ffcc66'}}>
                    {effectPowerMod >= 0 ? '+' : ''}{effectPowerMod}/{effectToughnessMod >= 0 ? '+' : ''}{effectToughnessMod}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Keywords/abilities info panel */}
        {card.keywords.length > 0 && (
          <div style={styles.cardPreviewKeywords}>
            {card.keywords.map((keyword) => (
              <div key={keyword} style={styles.cardPreviewKeyword}>
                <span style={styles.cardPreviewKeywordName}>{keyword}</span>
              </div>
            ))}
          </div>
        )}

        {/* Rulings panel - appears after 1 second of hovering */}
        {showRulings && hasRulings && (
          <div style={styles.cardPreviewRulings}>
            <div style={styles.cardPreviewRulingsHeader}>Rulings</div>
            {card.rulings!.map((ruling, index) => (
              <div key={index} style={styles.cardPreviewRuling}>
                <div style={styles.cardPreviewRulingDate}>{ruling.date}</div>
                <div style={styles.cardPreviewRulingText}>{ruling.text}</div>
              </div>
            ))}
          </div>
        )}

        {/* Rulings indicator - shows immediately if card has rulings */}
        {!showRulings && hasRulings && (
          <div style={styles.cardPreviewRulingsHint}>
            Hold to see rulings...
          </div>
        )}
      </div>
    </div>
  )
}
