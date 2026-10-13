import { describe, it, expect } from 'vitest'
import type { EntityId } from '@/types'
import { compatibleSelectionPool } from './selectionGroups'
const ids = (s: string[]) => s as EntityId[]
describe('server-approved selection groups', () => {
  const pool = ids(['a', 'b', 'c'])
  const groups = [ids(['a', 'b']), ids(['c'])]
  it('narrows and restores the highlighted pool without reading characteristics', () => {
    expect(compatibleSelectionPool(pool, groups, ids(['a']))).toEqual(ids(['a', 'b']))
    expect(compatibleSelectionPool(pool, groups, ids(['c']))).toEqual(ids(['c']))
    expect(compatibleSelectionPool(pool, groups, [])).toEqual(pool)
    expect(compatibleSelectionPool(pool, groups, ids(['a', 'c']))).toEqual([])
  })
})

  it('excludes the other cards in every selected owner group, and restores them after deselection', () => {
    const a = 'a' as EntityId, b = 'b' as EntityId, c = 'c' as EntityId
    expect(compatibleSelectionPool([a, b, c], null, [a], [[a, b], [c]])).toEqual([a, c])
    expect(compatibleSelectionPool([a, b, c], null, [], [[a, b], [c]])).toEqual([a, b, c])
  })
