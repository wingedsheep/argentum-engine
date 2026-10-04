import { useGameStore } from '@/store/gameStore'
import { useAttackNeighbours } from '@/store/selectors'
import type { EntityId } from '@/types'

/** How a seat relates to you under attack left / attack right (CR 803.1). */
export type AttackRelation = 'target' | 'attacker'

/**
 * The seat's relation to you in an attack-left / attack-right game: the one opponent you may
 * attack, or the one who may attack you. Null for every other seat, in a game without the
 * restriction, and while spectating (there is no "you" to relate to).
 */
export function useAttackRelation(playerId: EntityId): AttackRelation | null {
  const neighbours = useAttackNeighbours()
  const spectating = useGameStore((state) => state.spectatingState != null)
  if (!neighbours || spectating) return null
  if (neighbours.attacks === playerId) return 'target'
  if (neighbours.attackedBy === playerId) return 'attacker'
  return null
}

const STYLES: Record<AttackRelation, { label: string; glyph: string; color: string; border: string; title: string }> = {
  target: {
    label: 'TARGET',
    glyph: '⚔️',
    color: '#ff9a8f',
    border: 'rgba(255, 110, 100, 0.6)',
    title: 'The only opponent you can attack',
  },
  attacker: {
    label: 'ATTACKS YOU',
    glyph: '🛡',
    color: '#ffd08a',
    border: 'rgba(255, 190, 100, 0.55)',
    title: 'The only opponent who can attack you',
  },
}

/**
 * The relation as a round badge hanging off a rail chip's left edge — outside the chip's content
 * row, so it costs the (already tight) name no width. The rail header spells the relation out.
 */
export function AttackRelationBadge({ relation }: { relation: AttackRelation }) {
  const s = STYLES[relation]
  return (
    <span
      title={s.title}
      style={{
        position: 'absolute',
        left: -9,
        top: '50%',
        transform: 'translateY(-50%)',
        zIndex: 1,
        width: 18,
        height: 18,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: '50%',
        border: `1.5px solid ${s.border}`,
        background: 'rgba(14, 10, 12, 0.95)',
        boxShadow: `0 0 6px ${s.border}`,
        fontSize: 10,
        lineHeight: 1,
        pointerEvents: 'auto',
      }}
    >
      <span aria-hidden>{s.glyph}</span>
    </span>
  )
}

/**
 * A small tag naming the seat's attack relation to you — "⚔️ TARGET" or "🛡 ATTACKS YOU". Shown all
 * game, not just while declaring, so who threatens whom is never a question.
 */
export function AttackRelationTag({ relation }: { relation: AttackRelation }) {
  const s = STYLES[relation]
  return (
    <span
      title={s.title}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 3,
        flexShrink: 0,
        padding: '0 4px',
        borderRadius: 3,
        border: `1px solid ${s.border}`,
        background: 'rgba(0, 0, 0, 0.35)',
        color: s.color,
        fontSize: 9,
        fontWeight: 800,
        letterSpacing: '0.06em',
        lineHeight: '13px',
        whiteSpace: 'nowrap',
      }}
    >
      <span aria-hidden style={{ fontSize: 10, lineHeight: 1 }}>{s.glyph}</span>
      {s.label}
    </span>
  )
}
