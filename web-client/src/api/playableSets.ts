/**
 * The fully playable sets — those the server can build complete sealed and draft pools from — as
 * `GET /api/sets/booster-ready` lists them. Public, like the rest of `/api/sets`.
 */
import type { BoosterReadySet } from '@/components/setGuide/setGuideModel'

export async function listPlayableSets(): Promise<BoosterReadySet[]> {
  const res = await fetch('/api/sets/booster-ready')
  if (!res.ok) throw new Error(`Couldn’t load the sets (${res.status})`)
  return (await res.json()) as BoosterReadySet[]
}
