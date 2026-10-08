import { describe, expect, it } from 'vitest'
import { pageForPath } from './activityPage'

describe('pageForPath', () => {
  it('reports only the first route segment, never ids', () => {
    expect(pageForPath('/')).toBe('home')
    expect(pageForPath('')).toBe('home')
    expect(pageForPath('/deckbuilder')).toBe('deckbuilder')
    expect(pageForPath('/deckbuilder/3f2a-deck')).toBe('deckbuilder')
    expect(pageForPath('/help/keywords')).toBe('help')
    expect(pageForPath('/u/1234')).toBe('player-profile')
  })
})
