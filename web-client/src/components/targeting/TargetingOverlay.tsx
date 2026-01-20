import { Text } from '@react-three/drei'
import { useTargeting } from '../../hooks/useTargeting'
import { TargetArrow } from './TargetArrow'
import { useGameStore } from '../../store/gameStore'

/**
 * Overlay displayed during targeting mode.
 *
 * Shows:
 * - Instructions for targeting
 * - Arrows to selected targets
 * - Highlight on valid targets (handled by Card3D)
 */
export function TargetingOverlay() {
  const {
    isTargeting,
    targetingState,
    targetsRemaining,
    hasEnoughTargets,
  } = useTargeting()

  const selectedCardId = useGameStore((state) => state.selectedCardId)

  if (!isTargeting) return null

  return (
    <group>
      {/* Targeting instructions (positioned above the battlefield) */}
      <group position={[0, 2, 0]}>
        <Text
          fontSize={0.15}
          color="#ffffff"
          anchorX="center"
          anchorY="middle"
          outlineWidth={0.02}
          outlineColor="#000000"
        >
          {hasEnoughTargets
            ? 'Click to confirm or select different targets'
            : `Select ${targetsRemaining} target${targetsRemaining > 1 ? 's' : ''}`}
        </Text>

        {/* Cancel hint */}
        <Text
          position={[0, -0.25, 0]}
          fontSize={0.1}
          color="#888888"
          anchorX="center"
          anchorY="middle"
        >
          Press Escape to cancel
        </Text>
      </group>

      {/* Draw arrows from source to selected targets */}
      {selectedCardId && targetingState?.selectedTargets.map((targetId) => (
        <TargetArrow
          key={targetId}
          start={[0, 0.5, 2]} // Would be source card position
          end={[0, 0.5, 0]} // Would be target position
          color="#ff00ff"
        />
      ))}
    </group>
  )
}

/**
 * 2D HTML overlay for targeting (alternative to 3D text).
 * Used in the UI layer outside of R3F canvas.
 */
export function TargetingOverlay2D() {
  const {
    isTargeting,
    targetsRemaining,
    hasEnoughTargets,
    cancelTargeting,
    confirmTargeting,
  } = useTargeting()

  if (!isTargeting) return null

  return (
    <div
      style={{
        position: 'absolute',
        top: 20,
        left: '50%',
        transform: 'translateX(-50%)',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        color: 'white',
        padding: '12px 24px',
        borderRadius: 8,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
        zIndex: 100,
      }}
    >
      <span style={{ fontSize: 16 }}>
        {hasEnoughTargets
          ? 'Targets selected'
          : `Select ${targetsRemaining} target${targetsRemaining > 1 ? 's' : ''}`}
      </span>

      <div style={{ display: 'flex', gap: 12 }}>
        <button
          onClick={cancelTargeting}
          style={{
            padding: '6px 16px',
            backgroundColor: '#444',
            color: 'white',
            border: 'none',
            borderRadius: 4,
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>

        {hasEnoughTargets && (
          <button
            onClick={confirmTargeting}
            style={{
              padding: '6px 16px',
              backgroundColor: '#0a0',
              color: 'white',
              border: 'none',
              borderRadius: 4,
              cursor: 'pointer',
            }}
          >
            Confirm
          </button>
        )}
      </div>
    </div>
  )
}
