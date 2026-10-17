import type { ClientCard, EntityId } from '@/types'
import { Keyword } from '@/types/enums'

/**
 * The band that linking [sourceId] to [targetId] would produce (CR 702.22): both creatures plus
 * every member of whichever bands they already belong to.
 */
export function mergedBand(
  bands: readonly (readonly EntityId[])[],
  sourceId: EntityId,
  targetId: EntityId,
): EntityId[] {
  const sourceBand = bands.find((b) => b.includes(sourceId)) ?? []
  const targetBand = bands.find((b) => b.includes(targetId)) ?? []
  return Array.from(new Set<EntityId>([sourceId, targetId, ...sourceBand, ...targetBand]))
}

/** The slice of a `ClientCard` the band check reads — all of it projected by the server. */
export type BandingCard = Pick<ClientCard, 'keywords' | 'bandsWithOther' | 'bandQualities'>

/**
 * CR 702.22c: the two forms of band the engine's `validateBands` accepts, read off the server's
 * projected card data so the client only ever refuses a band the server would refuse too.
 *
 * - Banding: at most one member without banding — which also covers the two-creature case where
 *   neither has it.
 * - "Bands with other [quality]" (CR 702.22b): some member's "bands with other" ability is one
 *   whose quality *every* member has. The server computes each creature's qualities
 *   (`ClientCard.bandQualities`), so no type or name is interpreted here.
 */
export function bandIsLegal(
  members: readonly EntityId[],
  cardOf: (id: EntityId) => BandingCard | undefined,
): boolean {
  if (members.filter((id) => !cardOf(id)?.keywords.includes(Keyword.BANDING)).length <= 1) return true
  return members.some((holder) =>
    (cardOf(holder)?.bandsWithOther ?? []).some((ability) =>
      members.every((id) => cardOf(id)?.bandQualities?.includes(ability) ?? false),
    ),
  )
}
