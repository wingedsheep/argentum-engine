import type { EntityId } from '@/types'

/**
 * Intersect the original pool with the server-approved group containing every current pick,
 * dropping candidates an exclusive group already has a pick from and — for "up to one target … of
 * each color / card type" — candidates that couldn't claim a category the current picks leave free.
 */
export function compatibleSelectionPool(
  pool: readonly EntityId[],
  groups: readonly (readonly EntityId[])[] | null | undefined,
  selected: readonly EntityId[],
  exclusiveGroups?: readonly (readonly EntityId[])[] | null,
  distinctCategories?: Readonly<Record<EntityId, readonly string[]>> | null,
): readonly EntityId[] {
  const exclusive = exclusiveGroups
    ? pool.filter((id) => selected.includes(id) || !exclusiveGroups.some((group) => group.includes(id) && selected.some((pick) => group.includes(pick))))
    : pool
  const eligible = distinctCategories
    ? exclusive.filter((id) => selected.includes(id) ||
        canClaimDistinct([...selected, id].map((pick) => distinctCategories[pick] ?? [])))
    : exclusive
  if (!groups || selected.length === 0) return eligible
  const group = groups.find((ids) => selected.every((id) => ids.includes(id)))
  return eligible.filter((id) => group?.includes(id))
}

/**
 * Whether every entry can be paired with a different category from its own list — a bipartite
 * matching (augmenting paths), so a multicoloured card fills whichever colour slot is left over.
 */
export function canClaimDistinct(categoryLists: readonly (readonly string[])[]): boolean {
  const owner = new Map<string, number>()
  const augment = (index: number, visited: Set<string>): boolean => {
    for (const category of categoryLists[index] ?? []) {
      if (visited.has(category)) continue
      visited.add(category)
      const holder = owner.get(category)
      if (holder === undefined || augment(holder, visited)) {
        owner.set(category, index)
        return true
      }
    }
    return false
  }
  return categoryLists.every((_, index) => augment(index, new Set()))
}
