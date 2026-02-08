import { useState, useCallback } from 'react'
import { Phase, Step, StepShortNames } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import type { PriorityMode } from '../../store/selectors'

interface StepStripProps {
  phase: Phase
  step: Step
  turnNumber: number
  isActivePlayer: boolean
  hasPriority: boolean
  priorityMode: PriorityMode
  activePlayerName?: string | undefined
  stopOverrides: { myTurnStops: Step[]; opponentTurnStops: Step[] }
  onToggleStop: (step: Step, isMyTurn: boolean) => void
  isSpectator?: boolean
}

const STEP_ORDER: Step[] = [
  Step.UNTAP, Step.UPKEEP, Step.DRAW,
  Step.PRECOMBAT_MAIN,
  Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS,
  Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT,
  Step.POSTCOMBAT_MAIN,
  Step.END, Step.CLEANUP,
]

interface PhaseGroupDef {
  id: string
  steps: Step[]
}

const PHASE_GROUPS: PhaseGroupDef[] = [
  { id: 'beginning', steps: [Step.UNTAP, Step.UPKEEP, Step.DRAW] },
  { id: 'main1', steps: [Step.PRECOMBAT_MAIN] },
  { id: 'combat', steps: [Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT] },
  { id: 'main2', steps: [Step.POSTCOMBAT_MAIN] },
  { id: 'end', steps: [Step.END, Step.CLEANUP] },
]

/** Steps where players never receive priority (no stop dots) */
const NO_PRIORITY_STEPS = new Set<Step>([Step.UNTAP, Step.CLEANUP])

/** Short labels shown under the active phase group */
const STEP_PIP_LABELS: Record<Step, string> = {
  [Step.UNTAP]: 'Un',
  [Step.UPKEEP]: 'Up',
  [Step.DRAW]: 'Dr',
  [Step.PRECOMBAT_MAIN]: '',
  [Step.BEGIN_COMBAT]: 'BC',
  [Step.DECLARE_ATTACKERS]: 'Atk',
  [Step.DECLARE_BLOCKERS]: 'Blk',
  [Step.FIRST_STRIKE_COMBAT_DAMAGE]: '1st',
  [Step.COMBAT_DAMAGE]: 'Dmg',
  [Step.END_COMBAT]: 'EC',
  [Step.POSTCOMBAT_MAIN]: '',
  [Step.END]: 'End',
  [Step.CLEANUP]: 'Cl',
}

type ColorSet = { border: string; glow: string; highlight: string; text: string }

const modeColors: Record<PriorityMode, ColorSet> = {
  ownTurn: {
    border: '#4fc3f7',
    glow: '0 0 8px rgba(79, 195, 247, 0.4)',
    highlight: '#4fc3f7',
    text: '#4fc3f7',
  },
  responding: {
    border: '#ffc107',
    glow: '0 0 8px rgba(255, 193, 7, 0.4)',
    highlight: '#ffc107',
    text: '#ffc107',
  },
  waiting: {
    border: 'rgba(255,255,255,0.12)',
    glow: 'none',
    highlight: '#d08040',
    text: '#666',
  },
}

/** When waiting on your own turn (opponent responding), keep blue but dimmed */
const waitingMyTurn: ColorSet = {
  border: 'rgba(79, 195, 247, 0.3)',
  glow: 'none',
  highlight: 'rgba(79, 195, 247, 0.5)',
  text: '#666',
}

export function StepStrip({
  step,
  turnNumber,
  isActivePlayer,
  hasPriority,
  priorityMode,
  activePlayerName,
  stopOverrides,
  onToggleStop,
  isSpectator = false,
}: StepStripProps) {
  const responsive = useResponsive()
  const colors = priorityMode === 'waiting' && isActivePlayer
    ? waitingMyTurn
    : modeColors[priorityMode]
  const isMobile = responsive.isMobile
  const currentStepIndex = STEP_ORDER.indexOf(step)

  const statusText = activePlayerName
    ? `${activePlayerName}'s Turn`
    : priorityMode === 'ownTurn'
    ? 'Your Turn'
    : priorityMode === 'responding'
    ? 'Responding'
    : isActivePlayer
    ? 'Your Turn'
    : "Opponent's Turn"

  return (
    <div
      style={{
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        borderRadius: isMobile ? 6 : 8,
        padding: isMobile ? '5px 8px' : '6px 12px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: isMobile ? 3 : 4,
        pointerEvents: 'auto',
        border: `1px solid ${colors.border}`,
        boxShadow: colors.glow,
        transition: 'border-color 0.2s, box-shadow 0.2s',
        minWidth: 0,
      }}
    >
      {/* Status row */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
      }}>
        <span style={{
          color: hasPriority ? colors.text : '#666',
          fontSize: isMobile ? 8 : 10,
          fontWeight: hasPriority ? 600 : 400,
          textTransform: 'uppercase',
          letterSpacing: '0.6px',
        }}>
          {statusText}
        </span>
        <span style={{
          color: '#555',
          fontSize: isMobile ? 7 : 9,
        }}>
          T{turnNumber}
        </span>
      </div>

      {/* Phase groups with icons and pips */}
      <div style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: isMobile ? 6 : 10,
      }}>
        {PHASE_GROUPS.map((group) => {
          const isActiveGroup = group.steps.includes(step)
          const isPastGroup = group.steps.every(s => STEP_ORDER.indexOf(s) < currentStepIndex)
          const iconColor = isActiveGroup
            ? colors.highlight
            : isPastGroup
            ? 'rgba(255,255,255,0.35)'
            : 'rgba(255,255,255,0.15)'
          return (
            <div key={group.id} style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: isMobile ? 1 : 2,
            }}>
              {/* Phase icon */}
              <PhaseIcon
                phase={group.id}
                color={iconColor}
                size={isMobile ? 11 : 14}
                glow={isActiveGroup ? colors.highlight : undefined}
              />

              {/* Step pips */}
              <div style={{ display: 'flex', gap: 0, alignItems: 'flex-start' }}>
                {group.steps.map(s => {
                  const i = STEP_ORDER.indexOf(s)
                  return (
                    <StepPip
                      key={s}
                      step={s}
                      isCurrent={s === step}
                      isPast={i < currentStepIndex}
                      colors={colors}
                      isMobile={isMobile}
                      canHaveStops={!NO_PRIORITY_STEPS.has(s) && !isSpectator}
                      hasMyTurnStop={stopOverrides.myTurnStops.includes(s)}
                      hasOpponentTurnStop={stopOverrides.opponentTurnStops.includes(s)}
                      onToggleStop={onToggleStop}
                    />
                  )
                })}
              </div>

              {/* Active step label */}
              {isActiveGroup && (
                <span style={{
                  fontSize: isMobile ? 6 : 7,
                  color: colors.highlight,
                  fontWeight: 600,
                  letterSpacing: '0.3px',
                  lineHeight: 1,
                  opacity: 0.8,
                }}>
                  {STEP_PIP_LABELS[step] || StepShortNames[step]}
                </span>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}

/* ── Phase icons ───────────────────────────────────────── */

function PhaseIcon({ phase, color, size, glow }: {
  phase: string
  color: string
  size: number
  glow?: string | undefined
}) {
  const glowFilter = glow ? `drop-shadow(0 0 3px ${glow})` : 'none'

  // Roman numerals for main phases
  if (phase === 'main1' || phase === 'main2') {
    return (
      <span style={{
        color,
        fontSize: size,
        fontWeight: 800,
        fontFamily: '"Georgia", "Times New Roman", serif',
        lineHeight: 1,
        display: 'block',
        textAlign: 'center',
        filter: glowFilter,
        transition: 'color 0.2s, filter 0.2s',
      }}>
        {phase === 'main1' ? 'I' : 'II'}
      </span>
    )
  }

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 16 16"
      fill="none"
      style={{ display: 'block', filter: glowFilter, transition: 'filter 0.2s' }}
    >
      {phase === 'beginning' && (
        /* Sun: circle with 4 rays */
        <>
          <circle cx="8" cy="8" r="2.5" fill={color} />
          <line x1="8" y1="1.5" x2="8" y2="4" stroke={color} strokeWidth="1.4" strokeLinecap="round" />
          <line x1="8" y1="12" x2="8" y2="14.5" stroke={color} strokeWidth="1.4" strokeLinecap="round" />
          <line x1="1.5" y1="8" x2="4" y2="8" stroke={color} strokeWidth="1.4" strokeLinecap="round" />
          <line x1="12" y1="8" x2="14.5" y2="8" stroke={color} strokeWidth="1.4" strokeLinecap="round" />
        </>
      )}
      {phase === 'combat' && (
        /* Shield */
        <path
          d="M8 1.5 L13.5 4.5 L13.5 9 C13.5 12 8 15 8 15 C8 15 2.5 12 2.5 9 L2.5 4.5 Z"
          fill={color}
        />
      )}
      {phase === 'end' && (
        /* Crescent moon */
        <path
          d="M10 2.5 C7 2.5 4.5 5 4.5 8 C4.5 11 7 13.5 10 13.5 C8 12 6.8 10.2 6.8 8 C6.8 5.8 8 4 10 2.5 Z"
          fill={color}
        />
      )}
    </svg>
  )
}

/* ── Step pip ──────────────────────────────────────────── */

function StepPip({
  step,
  isCurrent,
  isPast,
  colors,
  isMobile,
  canHaveStops,
  hasMyTurnStop,
  hasOpponentTurnStop,
  onToggleStop,
}: {
  step: Step
  isCurrent: boolean
  isPast: boolean
  colors: { highlight: string }
  isMobile: boolean
  canHaveStops: boolean
  hasMyTurnStop: boolean
  hasOpponentTurnStop: boolean
  onToggleStop: (step: Step, isMyTurn: boolean) => void
}) {
  const [hovered, setHovered] = useState(false)
  const handleMouseEnter = useCallback(() => setHovered(true), [])
  const handleMouseLeave = useCallback(() => setHovered(false), [])

  const pipSize = isMobile ? 5 : 7
  const dotSize = isMobile ? 4 : 5
  const columnWidth = isMobile ? 10 : 14
  const hasAnyStop = hasMyTurnStop || hasOpponentTurnStop
  const showDots = canHaveStops && (hovered || hasAnyStop)

  const pipColor = isCurrent
    ? colors.highlight
    : isPast
    ? 'rgba(255,255,255,0.3)'
    : 'rgba(255,255,255,0.12)'

  return (
    <div
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        width: columnWidth,
        gap: 2,
        paddingTop: 1,
        paddingBottom: 1,
        position: 'relative',
      }}
    >
      {/* Hover tooltip */}
      {hovered && !isMobile && (
        <div style={{
          position: 'absolute',
          bottom: '100%',
          left: '50%',
          transform: 'translateX(-50%)',
          marginBottom: 4,
          padding: '2px 5px',
          backgroundColor: 'rgba(0,0,0,0.9)',
          border: '1px solid rgba(255,255,255,0.15)',
          borderRadius: 3,
          whiteSpace: 'nowrap',
          pointerEvents: 'none',
          zIndex: 10,
        }}>
          <span style={{
            fontSize: 9,
            color: isCurrent ? colors.highlight : '#ccc',
            fontWeight: isCurrent ? 600 : 400,
          }}>
            {StepShortNames[step]}
          </span>
        </div>
      )}

      {/* Pip */}
      <div
        onClick={canHaveStops ? (e) => {
          e.stopPropagation()
          // Both on → turn both off; otherwise turn both on
          const bothOn = hasMyTurnStop && hasOpponentTurnStop
          if (bothOn) {
            onToggleStop(step, true)
            onToggleStop(step, false)
          } else {
            if (!hasMyTurnStop) onToggleStop(step, true)
            if (!hasOpponentTurnStop) onToggleStop(step, false)
          }
        } : undefined}
        style={{
          width: pipSize,
          height: pipSize,
          borderRadius: '50%',
          backgroundColor: pipColor,
          boxShadow: isCurrent ? `0 0 6px ${colors.highlight}` : 'none',
          transition: 'background-color 0.2s, box-shadow 0.2s',
          flexShrink: 0,
          cursor: canHaveStops ? 'pointer' : undefined,
        }}
      />

      {/* Stop dots (stacked vertically to avoid overlap with adjacent pips) */}
      {canHaveStops && (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          gap: 0,
          alignItems: 'center',
          justifyContent: 'center',
          height: showDots ? ((dotSize + 4) * 2) : 0,
          overflow: 'hidden',
          transition: 'height 0.15s ease',
        }}>
          <StopDot
            active={hasMyTurnStop}
            visible={showDots}
            size={dotSize}
            title="My turn stop"
            color="#4fc3f7"
            onClick={() => onToggleStop(step, true)}
          />
          <StopDot
            active={hasOpponentTurnStop}
            visible={showDots}
            size={dotSize}
            title="Opponent turn stop"
            color="#ffc107"
            onClick={() => onToggleStop(step, false)}
          />
        </div>
      )}
    </div>
  )
}

/* ── Stop dot ──────────────────────────────────────────── */

function StopDot({
  active,
  visible,
  size,
  title,
  color,
  onClick,
}: {
  active: boolean
  visible: boolean
  size: number
  title: string
  color: string
  onClick: () => void
}) {
  const [dotHovered, setDotHovered] = useState(false)

  return (
    <div
      title={title}
      onClick={(e) => {
        e.stopPropagation()
        onClick()
      }}
      onMouseEnter={() => setDotHovered(true)}
      onMouseLeave={() => setDotHovered(false)}
      style={{
        width: size + 4,
        height: size + 4,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
      }}
    >
      <div style={{
        width: size,
        height: size,
        borderRadius: '50%',
        backgroundColor: active ? color : 'transparent',
        border: `1px solid ${active ? color : dotHovered ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.2)'}`,
        opacity: visible ? 1 : 0,
        transition: 'opacity 0.15s, background-color 0.1s, border-color 0.1s',
        flexShrink: 0,
      }} />
    </div>
  )
}
