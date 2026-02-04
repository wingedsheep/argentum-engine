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

  // Check if this player is a valid target in current targeting mode
  const isValidTargetingTarget = targetingState?.validTargets.includes(playerId) ?? false
  const isTargetingSelected = targetingState?.selectedTargets.includes(playerId) ?? false

  // Check if this player is a valid target in a pending ChooseTargetsDecision
  const isChooseTargetsDecision = pendingDecision?.type === 'ChooseTargetsDecision'
  const decisionLegalTargets = isChooseTargetsDecision
    ? (pendingDecision.legalTargets[0] ?? [])
    : []
  const isValidDecisionTarget = decisionLegalTargets.includes(playerId)

  // Combine both targeting modes
  const isValidTarget = isValidTargetingTarget || isValidDecisionTarget
  const isSelected = isTargetingSelected

  const handleClick = () => {
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
    }
  }

  const size = responsive.isMobile ? 36 : responsive.isTablet ? 42 : 48

  // Dynamic styling based on targeting state
  const bgColor = isPlayer ? '#1a3a5a' : '#3a1a4a'
  const borderColor = isSelected
    ? '#ffff00' // Yellow if selected as target
    : isValidTarget
      ? '#ff4444' // Red glow if valid target
      : isPlayer ? '#3a7aba' : '#7a3a9a'

  const cursor = isValidTarget ? 'pointer' : 'default'
  const boxShadow = isSelected
    ? '0 0 20px rgba(255, 255, 0, 0.8)'
    : isValidTarget
      ? '0 0 15px rgba(255, 68, 68, 0.6)'
      : 'none'

  return (
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
