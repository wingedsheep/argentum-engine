import type { EntityId } from '@/types'

/** Intersect the original pool with the server-approved group containing every current pick. */
export function compatibleSelectionPool(
  pool: readonly EntityId[],
  groups: readonly (readonly EntityId[])[] | null | undefined,
  selected: readonly EntityId[],
  exclusiveGroups?: readonly (readonly EntityId[])[] | null,
): readonly EntityId[] {
  const eligible = exclusiveGroups
    ? pool.filter((id) => selected.includes(id) || !exclusiveGroups.some((group) => group.includes(id) && selected.some((pick) => group.includes(pick))))
    : pool
  if (!groups || selected.length === 0) return eligible
  const group = groups.find((ids) => selected.every((id) => ids.includes(id)))
  return eligible.filter((id) => group?.includes(id))
}
