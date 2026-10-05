import type { AttackMode, ClientPlayer, EntityId } from '@/types'

/**
 * Who a seat may attack, and who may attack it, under a Free-for-All attack-left / attack-right
 * game (CR 803.1). Turn order proceeds to the left (CR 101.4), so "the player to your left" is the
 * next living seat in turn order and "to your right" the previous one — `players` arrives in turn
 * order, and eliminated seats are skipped exactly as the engine's seat helpers skip them.
 */
export interface AttackNeighbours {
  readonly mode: 'LEFT' | 'RIGHT'
  /** The one opponent [seatId] may attack. */
  readonly attacks: EntityId
  /** The one opponent who may attack [seatId]. */
  readonly attackedBy: EntityId
}

/**
 * Null when there is nothing to show: no restriction (any opponent may be attacked), the seat is
 * out of the game, or fewer than three players remain — with two left, left and right are the same
 * opponent and the restriction stops restricting anything.
 *
 * Display only. What a declaration may actually name is the server's `validAttackTargets`.
 */
export function attackNeighbours(
  players: readonly ClientPlayer[],
  mode: AttackMode | null | undefined,
  seatId: EntityId | null | undefined,
): AttackNeighbours | null {
  if ((mode !== 'LEFT' && mode !== 'RIGHT') || seatId == null) return null
  const living = players.filter((p) => !p.hasLost)
  if (living.length < 3) return null
  const index = living.findIndex((p) => p.playerId === seatId)
  if (index === -1) return null
  const next = living[(index + 1) % living.length]!.playerId
  const previous = living[(index - 1 + living.length) % living.length]!.playerId
  return mode === 'LEFT'
    ? { mode, attacks: next, attackedBy: previous }
    : { mode, attacks: previous, attackedBy: next }
}

/** Short label for the rule, e.g. the rail header. */
export function attackModeLabel(mode: 'LEFT' | 'RIGHT'): string {
  return mode === 'LEFT' ? 'Attack left' : 'Attack right'
}
