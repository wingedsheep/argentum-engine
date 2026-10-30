/**
 * The Set guide's view model: the playable sets joined with their profiles, and the quick filters,
 * theme filters and sorts the page offers over them. Pure, so it is tested without rendering.
 */
import { SET_PROFILES, type Rating, type SetProfile, type SetTheme } from './setProfiles'

/** A playable set as `GET /api/sets/booster-ready` lists it. */
export interface BoosterReadySet {
  readonly setCode: string
  readonly setName: string
  readonly implementedCount: number
  readonly releaseDate?: string | null
  readonly extensionSet?: boolean
}

export interface GuideSet {
  readonly code: string
  readonly name: string
  readonly releaseDate: string | null
  readonly year: string | null
  readonly cardCount: number
  readonly extension: boolean
  /** Absent for a set finished before anyone wrote its profile; the guide still lists it. */
  readonly profile: SetProfile | undefined
}

export function toGuideSets(sets: readonly BoosterReadySet[]): GuideSet[] {
  return sets.map((s) => ({
    code: s.setCode,
    name: s.setName,
    releaseDate: s.releaseDate ?? null,
    year: s.releaseDate?.slice(0, 4) ?? null,
    cardCount: s.implementedCount,
    extension: s.extensionSet === true,
    profile: SET_PROFILES[s.setCode],
  }))
}

export type RatingKey = 'power' | 'speed' | 'complexity'

export const RATINGS: readonly {
  readonly key: RatingKey
  readonly label: string
  readonly hint: string
  /** Word for each rating, 1 through 5. */
  readonly words: readonly [string, string, string, string, string]
}[] = [
  {
    key: 'power',
    label: 'Power',
    hint: 'How strong the cards are, compared with the other sets here',
    words: ['Gentle', 'Modest', 'Typical', 'Strong', 'Very high'],
  },
  {
    key: 'speed',
    label: 'Speed',
    hint: 'How fast games end — slow board stalls to races over by turn six',
    words: ['Slow', 'Measured', 'Moderate', 'Fast', 'Very fast'],
  },
  {
    key: 'complexity',
    label: 'Complexity',
    hint: 'How much there is to keep track of on each card and turn',
    words: ['Simple', 'Light', 'Moderate', 'Involved', 'Intricate'],
  },
]

export function ratingWord(key: RatingKey, value: Rating): string {
  return RATINGS.find((r) => r.key === key)?.words[value - 1] ?? ''
}

/** One-click answers to "what kind of set do I want tonight?". */
export type QuickFilter = 'beginner' | 'fast' | 'slow' | 'highPower' | 'classic'

export const QUICK_FILTERS: readonly {
  readonly id: QuickFilter
  readonly label: string
  readonly test: (s: GuideSet) => boolean
}[] = [
  { id: 'beginner', label: 'Easy to learn', test: (s) => (s.profile?.complexity ?? 9) <= 2 },
  { id: 'fast', label: 'Fast & aggressive', test: (s) => (s.profile?.speed ?? 0) >= 4 },
  { id: 'slow', label: 'Slow & grindy', test: (s) => (s.profile?.speed ?? 9) <= 2 },
  { id: 'highPower', label: 'High power', test: (s) => (s.profile?.power ?? 0) >= 4 },
  { id: 'classic', label: 'Before 2008', test: (s) => s.year != null && s.year < '2008' },
]

export type GuideSort = 'newest' | 'oldest' | 'power' | 'speed' | 'complexity' | 'name'

export const GUIDE_SORTS: readonly { readonly id: GuideSort; readonly label: string }[] = [
  { id: 'newest', label: 'Newest' },
  { id: 'oldest', label: 'Oldest' },
  { id: 'power', label: 'Power' },
  { id: 'speed', label: 'Speed' },
  { id: 'complexity', label: 'Complexity' },
  { id: 'name', label: 'A–Z' },
]

export interface GuideFilter {
  readonly search: string
  readonly quick: ReadonlySet<QuickFilter>
  readonly themes: ReadonlySet<SetTheme>
}

/** Every active filter must match: quick filters, themes, and a search over name, code and text. */
export function filterGuideSets(sets: readonly GuideSet[], filter: GuideFilter): GuideSet[] {
  const needle = filter.search.trim().toLowerCase()
  const quick = QUICK_FILTERS.filter((q) => filter.quick.has(q.id))
  return sets.filter((s) => {
    if (!quick.every((q) => q.test(s))) return false
    for (const theme of filter.themes) if (!s.profile?.themes.includes(theme)) return false
    if (!needle) return true
    const p = s.profile
    const haystack = [s.name, s.code, p?.tagline, p?.about, p?.playstyle, ...(p?.mechanics ?? [])]
      .join(' ')
      .toLowerCase()
    return haystack.includes(needle)
  })
}

const byNewest = (a: GuideSet, b: GuideSet) => (b.releaseDate ?? '').localeCompare(a.releaseDate ?? '')

/** Rating sorts put the highest first and break ties newest-first; profileless sets sink. */
export function sortGuideSets(sets: readonly GuideSet[], sort: GuideSort): GuideSet[] {
  const out = [...sets]
  switch (sort) {
    case 'newest':
      return out.sort(byNewest)
    case 'oldest':
      return out.sort((a, b) => byNewest(b, a))
    case 'name':
      return out.sort((a, b) => a.name.localeCompare(b.name))
    default:
      return out.sort((a, b) => (b.profile?.[sort] ?? 0) - (a.profile?.[sort] ?? 0) || byNewest(a, b))
  }
}

export const MAX_COMPARE = 4
