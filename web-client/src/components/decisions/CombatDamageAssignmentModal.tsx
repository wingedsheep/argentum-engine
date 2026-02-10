import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { AssignDamageDecision } from '../../types'
import type { EntityId } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

interface TargetInfo {
  id: EntityId
  name: string
  imageUri: string | null | undefined
  toughness: number | null | undefined
  isPlayer: boolean
  lifeTotal?: number
}

/**
 * Modal for assigning combat damage from an attacker to blockers (and defending player for trample).
 * Pre-filled with the default (optimal) damage distribution. Player can adjust with +/- buttons.
 */
export function CombatDamageAssignmentModal({ decision }: { decision: AssignDamageDecision }) {
  const submitDamageAssignment = useGameStore((s) => s.submitDamageAssignmentDecision)
  const gameState = useGameStore((s) => s.gameState)
  const hoverCard = useGameStore((s) => s.hoverCard)
  const responsive = useResponsive()
  const [minimized, setMinimized] = useState(false)

  // Initialize distribution from defaultAssignments
  const [distribution, setDistribution] = useState<Record<EntityId, number>>(
    () => ({ ...decision.defaultAssignments })
  )

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
          backgroundColor: '#dc2626',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          whiteSpace: 'nowrap',
          pointerEvents: 'auto',
        }}
      >
        Return to Damage Assignment
      </button>
    )
  }

  // Build target info from ordered targets + defending player
  const allTargetIds = [
    ...decision.orderedTargets,
    ...(decision.defenderId ? [decision.defenderId] : []),
  ]

  const targets: TargetInfo[] = allTargetIds.map((targetId) => {
    const card = gameState?.cards[targetId]
    const player = gameState?.players.find((p) => p.playerId === targetId)

    if (player) {
      return {
        id: targetId,
        name: player.name,
        imageUri: null,
        toughness: null,
        isPlayer: true,
        lifeTotal: player.life,
      }
    }

    return {
      id: targetId,
      name: card?.name ?? 'Unknown',
      imageUri: card?.imageUri,
      toughness: card?.toughness,
      isPlayer: false,
    }
  })

  const totalAllocated = Object.values(distribution).reduce((sum, val) => sum + val, 0)
  const remaining = decision.availablePower - totalAllocated
  const canConfirm = remaining === 0

  const handleIncrease = (targetId: EntityId) => {
    if (remaining <= 0) return
    setDistribution((prev) => ({
      ...prev,
      [targetId]: (prev[targetId] ?? 0) + 1,
    }))
  }

  const handleDecrease = (targetId: EntityId) => {
    const current = distribution[targetId] ?? 0
    const minimum = decision.minimumAssignments[targetId] ?? 0
    if (current <= minimum) return
    setDistribution((prev) => ({
      ...prev,
      [targetId]: (prev[targetId] ?? 0) - 1,
    }))
  }

  const handleReset = () => {
    setDistribution({ ...decision.defaultAssignments })
  }

  const handleConfirm = () => {
    submitDamageAssignment(distribution)
  }

  const handleMouseEnter = (cardId: EntityId) => {
    hoverCard(cardId)
  }

  const handleMouseLeave = () => {
    hoverCard(null)
  }

  // Get attacker info for header
  const attackerCard = gameState?.cards[decision.attackerId]
  const attackerName = attackerCard?.name ?? 'Attacker'

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
          Assign Combat Damage
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {attackerName} ({decision.availablePower} power)
          {decision.hasTrample && (
            <span style={{ color: '#f59e0b', marginLeft: 8 }}>Trample</span>
          )}
          {decision.hasDeathtouch && (
            <span style={{ color: '#a855f7', marginLeft: 8 }}>Deathtouch</span>
          )}
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
            'All damage assigned'
          ) : (
            <>
              Remaining: <span style={{ color: '#f87171' }}>{remaining}</span> damage
            </>
          )}
        </span>
      </div>

      {/* Targets */}
      <div
        style={{
          display: 'flex',
          gap: responsive.isMobile ? 16 : 24,
          flexWrap: 'wrap',
          justifyContent: 'center',
          padding: responsive.isMobile ? 8 : 16,
        }}
      >
        {targets.map((target) => {
          const allocated = distribution[target.id] ?? 0
          const toughness = target.toughness ?? 0
          const isLethal = !target.isPlayer && allocated >= toughness && toughness > 0

          return (
            <div
              key={target.id}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 8,
              }}
            >
              {/* Card image or player avatar */}
              {target.isPlayer ? (
                <div
                  style={{
                    width: cardWidth,
                    height: cardHeight,
                    borderRadius: responsive.isMobile ? 6 : 10,
                    backgroundColor: '#1a1a2e',
                    border: '2px solid #333',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 8,
                    position: 'relative',
                  }}
                >
                  <span style={{ color: '#888', fontSize: 32 }}>&#9823;</span>
                  <span style={{ color: 'white', fontSize: responsive.fontSize.normal, fontWeight: 500 }}>
                    {target.name}
                  </span>
                  <span style={{ color: '#888', fontSize: responsive.fontSize.small }}>
                    Life: {target.lifeTotal}
                  </span>

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
              ) : (
                <div
                  onMouseEnter={() => handleMouseEnter(target.id)}
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
                    src={getCardImageUrl(target.name, target.imageUri)}
                    alt={target.name}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    onError={(e) => {
                      e.currentTarget.style.display = 'none'
                    }}
                  />

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
              )}

              {/* Name and stats */}
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
                  {target.name}
                </span>
                {!target.isPlayer && toughness > 0 && (
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
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <button
                  onClick={() => handleDecrease(target.id)}
                  disabled={allocated <= (decision.minimumAssignments[target.id] ?? 0)}
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 8,
                    border: 'none',
                    backgroundColor:
                      allocated <= (decision.minimumAssignments[target.id] ?? 0) ? '#333' : '#dc2626',
                    color:
                      allocated <= (decision.minimumAssignments[target.id] ?? 0) ? '#666' : 'white',
                    fontSize: 20,
                    fontWeight: 'bold',
                    cursor:
                      allocated <= (decision.minimumAssignments[target.id] ?? 0)
                        ? 'not-allowed'
                        : 'pointer',
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
                  onClick={() => handleIncrease(target.id)}
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
          onClick={handleReset}
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
          Reset
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
            transition: 'all 0.15s',
          }}
        >
          Confirm Damage
        </button>
      </div>
    </div>
  )
}
