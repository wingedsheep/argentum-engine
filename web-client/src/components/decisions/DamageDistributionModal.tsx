import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

interface TargetCardInfo {
  id: EntityId
  name: string
  imageUri: string | null | undefined
  power: number | null | undefined
  toughness: number | null | undefined
}

/**
 * Modal for distributing damage at cast time for DividedDamageEffect spells (e.g., Forked Lightning).
 * Shows selected target cards with +/- buttons to allocate damage amounts.
 *
 * This is shown AFTER target selection, BEFORE the spell is cast.
 */
export function DamageDistributionModal() {
  const damageDistributionState = useGameStore((s) => s.damageDistributionState)
  const updateDamageDistribution = useGameStore((s) => s.updateDamageDistribution)
  const cancelDamageDistribution = useGameStore((s) => s.cancelDamageDistribution)
  const confirmDamageDistribution = useGameStore((s) => s.confirmDamageDistribution)
  const gameState = useGameStore((s) => s.gameState)
  const hoverCard = useGameStore((s) => s.hoverCard)
  const responsive = useResponsive()
  const [minimized, setMinimized] = useState(false)

  if (!damageDistributionState) return null

  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 1000,
          padding: '8px 20px',
          fontSize: 15,
          fontWeight: 600,
          backgroundColor: '#2563eb',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          whiteSpace: 'nowrap',
          pointerEvents: 'auto',
        }}
      >
        Return to Damage Distribution
      </button>
    )
  }

  const { cardName, targetIds, totalDamage, minPerTarget, distribution } = damageDistributionState

  // Calculate remaining amount to distribute
  const totalAllocated = Object.values(distribution).reduce((sum, val) => sum + val, 0)
  const remaining = totalDamage - totalAllocated
  const canConfirm = remaining === 0

  // Get card info for each target
  const targetCards: TargetCardInfo[] = targetIds.map((targetId): TargetCardInfo => {
    const card = gameState?.cards[targetId]
    return {
      id: targetId,
      name: card?.name ?? 'Unknown',
      imageUri: card?.imageUri,
      power: card?.power,
      toughness: card?.toughness,
    }
  })

  const handleIncrease = (targetId: EntityId) => {
    if (remaining <= 0) return
    const current = distribution[targetId] ?? 0
    updateDamageDistribution(targetId, current + 1)
  }

  const handleDecrease = (targetId: EntityId) => {
    const current = distribution[targetId] ?? 0
    if (current <= minPerTarget) return
    updateDamageDistribution(targetId, current - 1)
  }

  const handleMouseEnter = (cardId: EntityId) => {
    hoverCard(cardId)
  }

  const handleMouseLeave = () => {
    hoverCard(null)
  }

  const cardWidth = responsive.isMobile ? 100 : 140
  const cardHeight = Math.round(cardWidth * 1.4)

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: responsive.isMobile ? 16 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      {/* Header */}
      <div style={{ textAlign: 'center' }}>
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 20 : 28,
            fontWeight: 600,
          }}
        >
          {cardName}
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          Divide {totalDamage} damage among {targetIds.length} targets
        </p>
      </div>

      {/* Remaining indicator */}
      <div
        style={{
          backgroundColor: remaining === 0 ? '#1a3320' : '#3d2020',
          padding: responsive.isMobile ? '8px 16px' : '12px 24px',
          borderRadius: 8,
          border: remaining === 0 ? '2px solid #4ade80' : '2px solid #f87171',
        }}
      >
        <span
          style={{
            color: 'white',
            fontSize: responsive.fontSize.large,
            fontWeight: 600,
          }}
        >
          {remaining === 0 ? (
            'All damage allocated'
          ) : (
            <>
              Remaining: <span style={{ color: '#f87171' }}>{remaining}</span> damage
            </>
          )}
        </span>
      </div>

      {/* Target cards */}
      <div
        style={{
          display: 'flex',
          gap: responsive.isMobile ? 16 : 24,
          flexWrap: 'wrap',
          justifyContent: 'center',
          padding: responsive.isMobile ? 8 : 16,
        }}
      >
        {targetCards.map((card) => {
          const allocated = distribution[card.id] ?? 0
          const cardImageUrl = getCardImageUrl(card.name, card.imageUri)
          const toughness = card.toughness ?? 0
          const isLethal = allocated >= toughness && toughness > 0

          return (
            <div
              key={card.id}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 8,
              }}
            >
              {/* Card image */}
              <div
                onMouseEnter={() => handleMouseEnter(card.id)}
                onMouseLeave={handleMouseLeave}
                style={{
                  width: cardWidth,
                  height: cardHeight,
                  borderRadius: responsive.isMobile ? 6 : 10,
                  overflow: 'hidden',
                  border: isLethal ? '3px solid #dc2626' : '2px solid #333',
                  boxShadow: isLethal
                    ? '0 0 16px rgba(220, 38, 38, 0.6), 0 4px 12px rgba(0, 0, 0, 0.6)'
                    : '0 4px 12px rgba(0, 0, 0, 0.6)',
                  position: 'relative',
                }}
              >
                <img
                  src={cardImageUrl}
                  alt={card.name}
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={(e) => {
                    e.currentTarget.style.display = 'none'
                  }}
                />

                {/* Damage badge */}
                {allocated > 0 && (
                  <div
                    style={{
                      position: 'absolute',
                      top: 8,
                      right: 8,
                      backgroundColor: '#dc2626',
                      color: 'white',
                      width: 36,
                      height: 36,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 'bold',
                      fontSize: responsive.isMobile ? 16 : 18,
                      boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
                    }}
                  >
                    {allocated}
                  </div>
                )}

                {/* Lethal indicator */}
                {isLethal && (
                  <div
                    style={{
                      position: 'absolute',
                      top: 8,
                      left: 8,
                      backgroundColor: '#000',
                      color: '#dc2626',
                      padding: '2px 6px',
                      borderRadius: 4,
                      fontSize: responsive.isMobile ? 10 : 12,
                      fontWeight: 'bold',
                      textTransform: 'uppercase',
                      letterSpacing: '0.5px',
                    }}
                  >
                    Lethal
                  </div>
                )}
              </div>

              {/* Card name and stats */}
              <div style={{ textAlign: 'center' }}>
                <span
                  style={{
                    color: 'white',
                    fontSize: responsive.fontSize.normal,
                    fontWeight: 500,
                    display: 'block',
                    maxWidth: cardWidth,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {card.name}
                </span>
                {/* Damage vs Toughness indicator */}
                {toughness > 0 && (
                  <span
                    style={{
                      color: isLethal ? '#f87171' : '#888',
                      fontSize: responsive.fontSize.small,
                      display: 'block',
                      marginTop: 2,
                    }}
                  >
                    {allocated} damage / {toughness} toughness
                  </span>
                )}
              </div>

              {/* +/- controls */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <button
                  onClick={() => handleDecrease(card.id)}
                  disabled={allocated <= minPerTarget}
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 8,
                    border: 'none',
                    backgroundColor: allocated <= minPerTarget ? '#333' : '#dc2626',
                    color: allocated <= minPerTarget ? '#666' : 'white',
                    fontSize: 20,
                    fontWeight: 'bold',
                    cursor: allocated <= minPerTarget ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  -
                </button>

                <span
                  style={{
                    color: 'white',
                    fontSize: responsive.fontSize.large,
                    fontWeight: 600,
                    minWidth: 40,
                    textAlign: 'center',
                  }}
                >
                  {allocated}
                </span>

                <button
                  onClick={() => handleIncrease(card.id)}
                  disabled={remaining <= 0}
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 8,
                    border: 'none',
                    backgroundColor: remaining <= 0 ? '#333' : '#16a34a',
                    color: remaining <= 0 ? '#666' : 'white',
                    fontSize: 20,
                    fontWeight: 'bold',
                    cursor: remaining <= 0 ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  +
                </button>
              </div>
            </div>
          )
        })}
      </div>

      {/* Action buttons */}
      <div style={{ display: 'flex', gap: 16, marginTop: responsive.isMobile ? 8 : 16 }}>
        <button
          onClick={cancelDamageDistribution}
          style={{
            padding: responsive.isMobile ? '12px 24px' : '16px 32px',
            fontSize: responsive.fontSize.normal,
            backgroundColor: '#4b5563',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 500,
          }}
        >
          Cancel
        </button>
        <button
          onClick={() => setMinimized(true)}
          style={{
            padding: responsive.isMobile ? '12px 24px' : '16px 32px',
            fontSize: responsive.fontSize.normal,
            backgroundColor: '#2563eb',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          View Battlefield
        </button>
        <button
          onClick={confirmDamageDistribution}
          disabled={!canConfirm}
          style={{
            padding: responsive.isMobile ? '12px 32px' : '16px 48px',
            fontSize: responsive.fontSize.large,
            backgroundColor: canConfirm ? '#16a34a' : '#333',
            color: canConfirm ? 'white' : '#666',
            border: 'none',
            borderRadius: 8,
            cursor: canConfirm ? 'pointer' : 'not-allowed',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          Cast Spell
        </button>
      </div>
    </div>
  )
}
