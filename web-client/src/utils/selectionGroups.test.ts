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

describe('one target of each category', () => {
  const [wu, w, u, r, colorless] = ids(['wu', 'w', 'u', 'r', 'colorless'])
  const categories = { wu: ['BLUE', 'WHITE'], w: ['WHITE'], u: ['BLUE'], r: ['RED'] } as Record<EntityId, string[]>
  const pool = [wu!, w!, u!, r!, colorless!]

  it('lets a multicoloured pick claim whichever colour is left free', () => {
    expect(compatibleSelectionPool(pool, null, [w!], null, categories)).toEqual([wu, w, u, r])
    expect(compatibleSelectionPool(pool, null, [w!, wu!], null, categories)).toEqual([wu, w, r])
  })

  it('never offers a candidate with no category to claim', () => {
    expect(compatibleSelectionPool(pool, null, [], null, categories)).toEqual([wu, w, u, r])
  })
})
