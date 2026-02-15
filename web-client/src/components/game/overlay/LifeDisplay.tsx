import React from 'react'
import { useGameStore } from '../../../store/gameStore'
import type { EntityId, ClientPlayerEffect } from '../../../types'
import { useResponsiveContext, getEffectIcon } from '../board/shared'
import { styles } from '../board/styles'

/**
 * Life total display - interactive when in targeting mode or when a pending decision requires player targeting.
 */
export function LifeDisplay({
  life,
  isPlayer = false,
  playerId,
  playerName,
  spectatorMode = false,
}: {
  life: number
  isPlayer?: boolean
  playerId: EntityId
  playerName?: string
  spectatorMode?: boolean
}) {
  const responsive = useResponsiveContext()
  const targetingState = useGameStore((state) => state.targetingState)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const submitTargetsDecision = useGameStore((state) => state.submitTargetsDecision)
  const distributeState = useGameStore((state) => state.distributeState)
  const incrementDistribute = useGameStore((state) => state.incrementDistribute)
  const decrementDistribute = useGameStore((state) => state.decrementDistribute)
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)

  // Check if this player is a valid target in current targeting mode
  const isValidTargetingTarget = targetingState?.validTargets.includes(playerId) ?? false
  const isTargetingSelected = targetingState?.selectedTargets.includes(playerId) ?? false

  // Check if this player is a valid target in a pending ChooseTargetsDecision
  const isChooseTargetsDecision = pendingDecision?.type === 'ChooseTargetsDecision'
  const decisionLegalTargets = isChooseTargetsDecision
    ? (pendingDecision.legalTargets[0] ?? [])
    : []
  const isValidDecisionTarget = decisionLegalTargets.includes(playerId)

  // Check if this player is a valid option in decision selection mode (SelectCardsDecision with useTargetingUI)
  const isValidDecisionSelection = decisionSelectionState?.validOptions.includes(playerId) ?? false
  const isSelectedDecisionOption = decisionSelectionState?.selectedOptions.includes(playerId) ?? false

  // Inline damage distribution checks
  const isDistributeTarget = distributeState?.targets.includes(playerId) ?? false
  const distributeAllocated = isDistributeTarget ? (distributeState?.distribution[playerId] ?? 0) : 0
  const distributeTotalAllocated = distributeState
    ? Object.values(distributeState.distribution).reduce((sum, v) => sum + v, 0)
    : 0
  const distributeRemaining = distributeState ? distributeState.totalAmount - distributeTotalAllocated : 0

  // Combine all targeting modes
  const isValidTarget = isValidTargetingTarget || isValidDecisionTarget || isValidDecisionSelection
  const isSelected = isTargetingSelected || isSelectedDecisionOption

  const handleClick = () => {
    // Handle inline distribute mode - click to add damage
    if (isDistributeTarget && distributeRemaining > 0) {
      incrementDistribute(playerId)
      return
    }

    // Handle regular targeting state - click to select, click again to unselect
    if (targetingState) {
      if (isTargetingSelected) {
        removeTarget(playerId)
        return
      }
      if (isValidTargetingTarget) {
        addTarget(playerId)
        return
      }
    }

    // Handle pending decision targeting
    if (isChooseTargetsDecision && isValidDecisionTarget) {
      // Submit the decision with this player as the target
      submitTargetsDecision({ 0: [playerId] })
      return
    }

    // Handle decision selection mode (SelectCardsDecision with useTargetingUI)
    if (isValidDecisionSelection) {
      toggleDecisionSelection(playerId)
      return
    }
  }

  const size = responsive.isMobile ? 36 : responsive.isTablet ? 42 : 48

  // Dynamic styling based on targeting state
  const bgColor = isPlayer ? '#1a3a5a' : '#3a1a4a'
  const borderColor = isDistributeTarget && distributeAllocated > 0
    ? '#ff6b35' // Orange for distribute targets with allocation
    : isDistributeTarget
      ? '#ff8c42' // Dim orange for unallocated distribute targets
      : isSelected
        ? '#ffff00' // Yellow if selected as target
        : isValidTarget
          ? '#ff4444' // Red glow if valid target
          : isPlayer ? '#3a7aba' : '#7a3a9a'

  const cursor = isValidTarget || isDistributeTarget ? 'pointer' : 'default'
  const boxShadow = isDistributeTarget && distributeAllocated > 0
    ? '0 0 16px rgba(255, 107, 53, 0.7), 0 0 32px rgba(255, 107, 53, 0.4)'
    : isDistributeTarget
      ? '0 0 12px rgba(255, 140, 66, 0.5)'
      : isSelected
        ? '0 0 20px rgba(255, 255, 0, 0.8)'
        : isValidTarget
          ? '0 0 15px rgba(255, 68, 68, 0.6)'
          : 'none'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: isDistributeTarget ? 4 : 0 }}>
      <div
        data-player-id={playerId}
        data-life-id={playerId}
        data-life-display={playerId}
        onClick={handleClick}
        style={{
          ...styles.lifeDisplay,
          width: size,
          height: size,
          fontSize: responsive.fontSize.large,
          backgroundColor: bgColor,
          borderColor: borderColor,
          cursor,
          boxShadow,
          transition: 'all 0.2s ease-in-out',
          position: 'relative',
        }}
      >
        <span
          style={{
            position: 'absolute',
            top: -8,
            left: '50%',
            transform: 'translateX(-50%)',
            fontSize: 9,
            fontWeight: 'bold',
            color: isPlayer ? '#4a9aea' : '#aa6aca',
            backgroundColor: '#1a1a2e',
            padding: '1px 4px',
            borderRadius: 3,
            whiteSpace: 'nowrap',
          }}
        >
          {spectatorMode && playerName ? playerName.toUpperCase() : (isPlayer ? 'YOU' : 'OPPONENT')}
        </span>
        <span style={{ color: life <= 5 ? '#ff4444' : '#ffffff' }}>{life}</span>

        {/* Damage allocation badge */}
        {isDistributeTarget && distributeAllocated > 0 && (
          <div style={{
            position: 'absolute',
            top: -4,
            right: -4,
            backgroundColor: '#dc2626',
            color: 'white',
            width: 18,
            height: 18,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 'bold',
            fontSize: 11,
            boxShadow: '0 2px 6px rgba(220, 38, 38, 0.6)',
            zIndex: 5,
          }}>
            {distributeAllocated}
          </div>
        )}
      </div>

      {/* Inline +/- controls for distribute mode */}
      {isDistributeTarget && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 2,
        }}>
          <button
            onClick={(e) => { e.stopPropagation(); decrementDistribute(playerId) }}
            disabled={distributeAllocated <= (distributeState?.minPerTarget ?? 0)}
            style={{
              width: 20,
              height: 20,
              borderRadius: 4,
              border: 'none',
              backgroundColor: distributeAllocated <= (distributeState?.minPerTarget ?? 0) ? '#333' : '#dc2626',
              color: distributeAllocated <= (distributeState?.minPerTarget ?? 0) ? '#666' : 'white',
              fontSize: 13,
              fontWeight: 'bold',
              cursor: distributeAllocated <= (distributeState?.minPerTarget ?? 0) ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 0,
            }}
          >
            -
          </button>
          <span style={{
            color: 'white',
            fontSize: 12,
            fontWeight: 700,
            minWidth: 18,
            textAlign: 'center',
          }}>
            {distributeAllocated}
          </span>
          <button
            onClick={(e) => { e.stopPropagation(); incrementDistribute(playerId) }}
            disabled={distributeRemaining <= 0}
            style={{
              width: 20,
              height: 20,
              borderRadius: 4,
              border: 'none',
              backgroundColor: distributeRemaining <= 0 ? '#333' : '#16a34a',
              color: distributeRemaining <= 0 ? '#666' : 'white',
              fontSize: 13,
              fontWeight: 'bold',
              cursor: distributeRemaining <= 0 ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 0,
            }}
          >
            +
          </button>
        </div>
      )}
    </div>
  )
}

/**
 * Active effects badges - shows status effects on a player.
 */
export function ActiveEffectsBadges({ effects }: { effects: readonly ClientPlayerEffect[] | undefined }) {
  const responsive = useResponsiveContext()
  const [hoveredEffect, setHoveredEffect] = React.useState<string | null>(null)

  if (!effects || effects.length === 0) return null

  return (
    <div style={styles.effectBadgesContainer}>
      {effects.map((effect) => (
        <div
          key={effect.effectId}
          style={{
            ...styles.effectBadge,
            padding: responsive.isMobile ? '2px 6px' : '4px 8px',
            fontSize: responsive.fontSize.small,
          }}
          onMouseEnter={() => setHoveredEffect(effect.effectId)}
          onMouseLeave={() => setHoveredEffect(null)}
        >
          {effect.icon && <span style={styles.effectBadgeIcon}>{getEffectIcon(effect.icon)}</span>}
          <span style={styles.effectBadgeName}>{effect.name}</span>
          {hoveredEffect === effect.effectId && effect.description && (
            <div style={styles.effectTooltip}>
              {effect.description}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
