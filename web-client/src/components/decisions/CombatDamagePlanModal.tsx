import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { CombatDamagePlanDecision, CombatDamagePlanEntry, EntityId } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { bandColorFor } from '../game/board/styles'

interface TargetInfo {
  id: EntityId
  name: string
  imageUri: string | null | undefined
  toughness: number | null | undefined
  isPlayer: boolean
  lifeTotal?: number
}

/**
 * Bundled combat damage planner. Replaces the per-attacker AssignDamageDecision
 * modals with a single screen: one row per attacker, each row containing the
 * attacker's blockers (and defender for trample) as ± allocation cards. A
 * single "Confirm Damage" button at the bottom submits the whole plan at once.
 *
 * Banded attackers get a colored ring + B-label so the band grouping carries
 * through from the battlefield into the planner.
 */
export function CombatDamagePlanModal({ decision }: { decision: CombatDamagePlanDecision }) {
  const submitPlan = useGameStore((s) => s.submitCombatDamagePlanDecision)
  const gameState = useGameStore((s) => s.gameState)
  const hoverCard = useGameStore((s) => s.hoverCard)
  const responsive = useResponsive()
  const [minimized, setMinimized] = useState(false)

  // Per-attacker draft assignments (mutable). Initialise from defaults.
  const [draft, setDraft] = useState<Record<EntityId, Record<EntityId, number>>>(() => {
    const initial: Record<EntityId, Record<EntityId, number>> = {}
    for (const entry of decision.entries) {
      initial[entry.attackerId] = { ...entry.defaultAssignments }
    }
    return initial
  })

  // Index banded entries to a stable color slot (same as battlefield view).
  const bandIndexFor = (entry: CombatDamagePlanEntry): number => {
    if (!entry.bandId) return -1
    const seen: string[] = []
    for (const e of decision.entries) {
      if (e.bandId && !seen.includes(e.bandId)) seen.push(e.bandId)
    }
    return seen.indexOf(entry.bandId)
  }

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
        Return to Combat Damage
      </button>
    )
  }

  const targetsFor = (entry: CombatDamagePlanEntry): TargetInfo[] => {
    const ids = [...entry.orderedTargets, ...(entry.defenderId ? [entry.defenderId] : [])]
    return ids.map((id) => {
      const player = gameState?.players.find((p) => p.playerId === id)
      if (player) {
        return {
          id,
          name: player.name,
          imageUri: null,
          toughness: null,
          isPlayer: true,
          lifeTotal: player.life,
        }
      }
      const card = gameState?.cards[id]
      return {
        id,
        name: card?.name ?? 'Unknown',
        imageUri: card?.imageUri,
        toughness: card?.toughness,
        isPlayer: false,
      }
    })
  }

  const remainingFor = (entry: CombatDamagePlanEntry): number => {
    const assigned = Object.values(draft[entry.attackerId] ?? {}).reduce((a, b) => a + b, 0)
    return entry.availablePower - assigned
  }

  const adjust = (attackerId: EntityId, targetId: EntityId, delta: number) => {
    setDraft((prev) => {
      const row = { ...(prev[attackerId] ?? {}) }
      const next = Math.max(0, (row[targetId] ?? 0) + delta)
      row[targetId] = next
      return { ...prev, [attackerId]: row }
    })
  }

  const reset = () => {
    const initial: Record<EntityId, Record<EntityId, number>> = {}
    for (const entry of decision.entries) {
      initial[entry.attackerId] = { ...entry.defaultAssignments }
    }
    setDraft(initial)
  }

  const allFullyAssigned = decision.entries.every((e) => remainingFor(e) === 0)

  const handleConfirm = () => {
    if (!allFullyAssigned) return
    submitPlan(draft)
  }

  const isCompact = responsive.isMobile || decision.entries.length >= 3
  const thumbWidth = isCompact ? 70 : 100
  const thumbHeight = Math.round(thumbWidth * 1.4)

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
        gap: responsive.isMobile ? 12 : 16,
        padding: responsive.containerPadding,
        paddingTop: responsive.isMobile ? 16 : 28,
        pointerEvents: 'auto',
        zIndex: 1000,
        overflowY: 'auto',
      }}
    >
      <div style={{ textAlign: 'center' }}>
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 20 : 26,
            fontWeight: 600,
          }}
        >
          Assign Combat Damage
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '6px 0 0',
            fontSize: responsive.fontSize.small,
          }}
        >
          {decision.entries.length === 1
            ? '1 attacker'
            : `${decision.entries.length} attackers in this combat damage step`}
        </p>
      </div>

      {/* One row per attacker */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
          width: '100%',
          maxWidth: 1400,
        }}
      >
        {decision.entries.map((entry) => {
          const remaining = remainingFor(entry)
          const targets = targetsFor(entry)
          const attackerCard = gameState?.cards[entry.attackerId]
          const idx = bandIndexFor(entry)
          const band = idx >= 0 ? bandColorFor(idx) : null
          return (
            <div
              key={entry.attackerId}
              style={{
                display: 'flex',
                gap: 12,
                alignItems: 'center',
                padding: 10,
                background: 'rgba(20, 20, 30, 0.7)',
                border: band ? `2px solid ${band.border}` : '1px solid #333',
                borderRadius: 8,
                boxShadow: band ? `0 0 16px ${band.glow}` : 'none',
              }}
            >
              {/* Attacker thumbnail + name + power */}
              <div
                onMouseEnter={(e) => hoverCard(entry.attackerId, { x: e.clientX, y: e.clientY })}
                onMouseLeave={() => hoverCard(null)}
                style={{
                  flexShrink: 0,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 4,
                  minWidth: thumbWidth + 16,
                }}
              >
                <div
                  style={{
                    width: thumbWidth,
                    height: thumbHeight,
                    borderRadius: 6,
                    overflow: 'hidden',
                    border: '2px solid #ff4444',
                    boxShadow: '0 0 12px rgba(255, 68, 68, 0.5)',
                  }}
                >
                  <img
                    src={getCardImageUrl(attackerCard?.name ?? entry.attackerName, attackerCard?.imageUri)}
                    alt={entry.attackerName}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                </div>
                <div style={{ color: 'white', fontSize: 12, fontWeight: 600, textAlign: 'center', maxWidth: thumbWidth + 16 }}>
                  {entry.attackerName}
                </div>
                <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                  {band && (
                    <span
                      style={{
                        background: band.chipBg,
                        color: 'white',
                        fontWeight: 700,
                        fontSize: 10,
                        padding: '1px 5px',
                        borderRadius: 3,
                        border: `1px solid ${band.border}`,
                      }}
                    >
                      B{idx + 1}
                    </span>
                  )}
                  <span style={{ color: '#fbbf24', fontSize: 11 }}>
                    {entry.availablePower} pwr
                  </span>
                  {entry.hasTrample && (
                    <span style={{ color: '#f59e0b', fontSize: 10 }}>trample</span>
                  )}
                  {entry.hasDeathtouch && (
                    <span style={{ color: '#a855f7', fontSize: 10 }}>deathtouch</span>
                  )}
                </div>
                <div
                  style={{
                    color: remaining === 0 ? '#4ade80' : '#f87171',
                    fontSize: 11,
                    fontWeight: 600,
                  }}
                >
                  {remaining === 0 ? '✓ done' : `${remaining} left`}
                </div>
              </div>

              {/* Target cells */}
              <div
                style={{
                  display: 'flex',
                  gap: 8,
                  flexWrap: 'wrap',
                  flex: 1,
                }}
              >
                {targets.map((target) => {
                  const allocated = draft[entry.attackerId]?.[target.id] ?? 0
                  const minimum = entry.minimumAssignments[target.id] ?? 0
                  const toughness = target.toughness ?? 0
                  const isLethal = !target.isPlayer && allocated >= toughness && toughness > 0
                  return (
                    <div
                      key={target.id}
                      onMouseEnter={(e) => !target.isPlayer && hoverCard(target.id, { x: e.clientX, y: e.clientY })}
                      onMouseLeave={() => hoverCard(null)}
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: 4,
                        padding: 6,
                        background: 'rgba(0,0,0,0.4)',
                        border: isLethal ? '2px solid #dc2626' : '1px solid #444',
                        borderRadius: 6,
                        minWidth: thumbWidth + 16,
                      }}
                    >
                      {target.isPlayer ? (
                        <div
                          style={{
                            width: thumbWidth,
                            height: thumbHeight,
                            borderRadius: 4,
                            backgroundColor: '#1a1a2e',
                            border: '1px solid #333',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 4,
                          }}
                        >
                          <span style={{ color: '#888', fontSize: 24 }}>♟</span>
                          <span style={{ color: 'white', fontSize: 11 }}>{target.name}</span>
                          <span style={{ color: '#888', fontSize: 10 }}>
                            Life: {target.lifeTotal}
                          </span>
                        </div>
                      ) : (
                        <div
                          style={{
                            width: thumbWidth,
                            height: thumbHeight,
                            borderRadius: 4,
                            overflow: 'hidden',
                          }}
                        >
                          <img
                            src={getCardImageUrl(target.name, target.imageUri)}
                            alt={target.name}
                            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                          />
                        </div>
                      )}
                      <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                        <button
                          onClick={() => adjust(entry.attackerId, target.id, -1)}
                          disabled={allocated <= minimum}
                          style={{
                            width: 24,
                            height: 24,
                            borderRadius: 4,
                            border: '1px solid #555',
                            background: '#1f2937',
                            color: allocated <= minimum ? '#555' : 'white',
                            fontWeight: 700,
                            cursor: allocated <= minimum ? 'not-allowed' : 'pointer',
                          }}
                        >
                          −
                        </button>
                        <div
                          style={{
                            minWidth: 28,
                            textAlign: 'center',
                            fontWeight: 700,
                            color: allocated > 0 ? '#dc2626' : '#666',
                          }}
                        >
                          {allocated}
                        </div>
                        <button
                          onClick={() => adjust(entry.attackerId, target.id, 1)}
                          disabled={remaining <= 0}
                          style={{
                            width: 24,
                            height: 24,
                            borderRadius: 4,
                            border: '1px solid #555',
                            background: '#1f2937',
                            color: remaining <= 0 ? '#555' : 'white',
                            fontWeight: 700,
                            cursor: remaining <= 0 ? 'not-allowed' : 'pointer',
                          }}
                        >
                          +
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>

      <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 8 }}>
        <button
          onClick={reset}
          style={{
            padding: '10px 20px',
            backgroundColor: '#374151',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: 'pointer',
            fontSize: 14,
          }}
        >
          Reset to default
        </button>
        <button
          onClick={() => setMinimized(true)}
          style={{
            padding: '10px 20px',
            backgroundColor: '#1f2937',
            color: '#aaa',
            border: '1px solid #444',
            borderRadius: 6,
            cursor: 'pointer',
            fontSize: 14,
          }}
        >
          Minimize
        </button>
        <button
          onClick={handleConfirm}
          disabled={!allFullyAssigned}
          style={{
            padding: '12px 28px',
            backgroundColor: allFullyAssigned ? '#dc2626' : '#3d2020',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: allFullyAssigned ? 'pointer' : 'not-allowed',
            fontSize: 15,
            fontWeight: 600,
            opacity: allFullyAssigned ? 1 : 0.55,
          }}
        >
          Confirm Damage
        </button>
      </div>
    </div>
  )
}
