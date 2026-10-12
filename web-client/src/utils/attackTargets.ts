import type { EntityId } from '@/types'

/**
 * What one attacker may legally attack, as the server sent it: its entry in the sparse
 * `attackTargetsByAttacker` narrowing when present (a per-defender restriction such as "can't
 * attack unless defending player controls a Mountain" or "creatures can't attack you" rules the
 * rest out), otherwise every one of `validAttackTargets`. Pure lookup — no legality is computed
 * here.
 */
export function attackTargetsFor(
  combat: {
    readonly validAttackTargets: readonly EntityId[]
    readonly attackTargetsByAttacker: Readonly<Record<EntityId, readonly EntityId[]>>
  },
  attackerId: EntityId,
): readonly EntityId[] {
  return combat.attackTargetsByAttacker[attackerId] ?? combat.validAttackTargets
}

/** Whether the server lists [targetId] as a legal attack target for [attackerId]. */
export function canAttackTarget(
  combat: Parameters<typeof attackTargetsFor>[0],
  attackerId: EntityId,
  targetId: EntityId,
): boolean {
  return attackTargetsFor(combat, attackerId).includes(targetId)
}
