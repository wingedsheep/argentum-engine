import { Phase, Step, StepDisplayNames } from '../../types'

interface PhaseIndicatorProps {
  phase: Phase
  step: Step
  turnNumber: number
  isActivePlayer: boolean
  hasPriority: boolean
}

/**
 * Phase and step indicator.
 */
export function PhaseIndicator({
  phase,
  step,
  turnNumber,
  isActivePlayer,
  hasPriority,
}: PhaseIndicatorProps) {
  return (
    <div
      style={{
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderRadius: 8,
        padding: '8px 16px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        pointerEvents: 'auto',
      }}
    >
      {/* Turn number */}
      <span style={{ color: '#888', fontSize: 11 }}>
        Turn {turnNumber}
      </span>

      {/* Phase/Step display */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <PhaseIcon phase={phase} />
        <span style={{ color: '#fff', fontSize: 14, fontWeight: 500 }}>
          {StepDisplayNames[step]}
        </span>
      </div>

      {/* Priority indicator */}
      <div
        style={{
          display: 'flex',
          gap: 8,
          marginTop: 4,
        }}
      >
        <StatusBadge
          label="Active"
          active={isActivePlayer}
          color="#0088ff"
        />
        <StatusBadge
          label="Priority"
          active={hasPriority}
          color="#00ff00"
        />
      </div>
    </div>
  )
}

/**
 * Phase icon based on current phase.
 */
function PhaseIcon({ phase }: { phase: Phase }) {
  const icons: Record<Phase, string> = {
    [Phase.BEGINNING]: '🌅',
    [Phase.PRECOMBAT_MAIN]: '📜',
    [Phase.COMBAT]: '⚔️',
    [Phase.POSTCOMBAT_MAIN]: '📜',
    [Phase.ENDING]: '🌙',
  }

  return <span style={{ fontSize: 18 }}>{icons[phase]}</span>
}

/**
 * Status badge showing active/priority state.
 */
function StatusBadge({
  label,
  active,
  color,
}: {
  label: string
  active: boolean
  color: string
}) {
  return (
    <div
      style={{
        padding: '2px 8px',
        borderRadius: 4,
        backgroundColor: active ? color : '#333',
        opacity: active ? 1 : 0.5,
        fontSize: 11,
        color: active ? '#000' : '#888',
        fontWeight: active ? 600 : 400,
      }}
    >
      {label}
    </div>
  )
}

/**
 * Full phase breakdown showing all phases.
 */
export function PhaseBreakdown({
  currentPhase,
}: {
  currentPhase: Phase
  currentStep: Step
}) {
  const phases = [
    Phase.BEGINNING,
    Phase.PRECOMBAT_MAIN,
    Phase.COMBAT,
    Phase.POSTCOMBAT_MAIN,
    Phase.ENDING,
  ]

  return (
    <div
      style={{
        display: 'flex',
        gap: 4,
      }}
    >
      {phases.map((phase) => (
        <div
          key={phase}
          style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            backgroundColor:
              phase === currentPhase
                ? '#00ff00'
                : phases.indexOf(phase) < phases.indexOf(currentPhase)
                ? '#888'
                : '#333',
          }}
        />
      ))}
    </div>
  )
}
