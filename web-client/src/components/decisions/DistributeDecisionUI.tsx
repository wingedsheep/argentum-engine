import { useState, useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, DistributeDecision } from '../../types'
import type { ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

interface TargetCardInfo {
  id: EntityId
  name: string
  imageUri: string | null | undefined
  power: number | null | undefined
  toughness: number | null | undefined
}

interface DistributeDecisionUIProps {
  decision: DistributeDecision
  responsive: ResponsiveSizes
}

/**
 * UI for distributing damage (or other amounts) among multiple targets.
 * Shows cards with +/- buttons to allocate amounts, respecting min per target.
 */
export function DistributeDecisionUI({ decision, responsive }: DistributeDecisionUIProps) {
  const submitDistributeDecision = useGameStore((s) => s.submitDistributeDecision)
  const gameState = useGameStore((s) => s.gameState)
  const hoverCard = useGameStore((s) => s.hoverCard)

  // Initialize distribution with minPerTarget for each target
  const [distribution, setDistribution] = useState<Record<EntityId, number>>(() => {
    const initial: Record<EntityId, number> = {}
    for (const targetId of decision.targets) {
      initial[targetId] = decision.minPerTarget
    }
    return initial
  })

  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)

  // Calculate remaining amount to distribute
  const totalAllocated = useMemo(() => {
    return Object.values(distribution).reduce((sum, val) => sum + val, 0)
  }, [distribution])

  const remaining = decision.totalAmount - totalAllocated
  const canConfirm = remaining === 0

  // Get card info for each target
  const targetCards: TargetCardInfo[] = useMemo(() => {
    return decision.targets.map((targetId): TargetCardInfo => {
      const card = gameState?.cards[targetId]
      return {
        id: targetId,
        name: card?.name ?? 'Unknown',
        imageUri: card?.imageUri,
        power: card?.power,
        toughness: card?.toughness,
      }
    })
  }, [decision.targets, gameState])

  const handleIncrease = (targetId: EntityId) => {
    if (remaining <= 0) return
    setDistribution((prev) => ({
      ...prev,
      [targetId]: (prev[targetId] ?? 0) + 1,
    }))
  }

  const handleDecrease = (targetId: EntityId) => {
    const current = distribution[targetId] ?? 0
    if (current <= decision.minPerTarget) return
    setDistribution((prev) => ({
      ...prev,
      [targetId]: current - 1,
    }))
  }

  const handleConfirm = () => {
    if (!canConfirm) return
    submitDistributeDecision(distribution)
  }

  const handleMouseEnter = (cardId: EntityId) => {
    setHoveredCardId(cardId)
    hoverCard(cardId)
  }

  const handleMouseLeave = () => {
    setHoveredCardId(null)
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
          Distribute Damage
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {decision.prompt}
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
          const isHovered = hoveredCardId === card.id
          const cardImageUrl = getCardImageUrl(card.name, card.imageUri)

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
                  border: isHovered ? '2px solid #666' : '2px solid #333',
                  boxShadow: isHovered
                    ? '0 8px 20px rgba(255, 255, 255, 0.15)'
                    : '0 4px 12px rgba(0, 0, 0, 0.6)',
                  transition: 'all 0.2s ease-out',
                  transform: isHovered ? 'translateY(-4px) scale(1.02)' : 'none',
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
              </div>

              {/* Card name */}
              <span
                style={{
                  color: 'white',
                  fontSize: responsive.fontSize.normal,
                  fontWeight: 500,
                  textAlign: 'center',
                  maxWidth: cardWidth,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {card.name}
              </span>

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
                  disabled={allocated <= decision.minPerTarget}
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 8,
                    border: 'none',
                    backgroundColor: allocated <= decision.minPerTarget ? '#333' : '#dc2626',
                    color: allocated <= decision.minPerTarget ? '#666' : 'white',
                    fontSize: 20,
                    fontWeight: 'bold',
                    cursor: allocated <= decision.minPerTarget ? 'not-allowed' : 'pointer',
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

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
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
          marginTop: responsive.isMobile ? 8 : 16,
          transition: 'all 0.15s',
        }}
      >
        Confirm Damage
      </button>
    </div>
  )
}
