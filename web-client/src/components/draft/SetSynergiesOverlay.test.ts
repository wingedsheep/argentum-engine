import { describe, expect, it } from 'vitest'
import { getArchetypesForSets } from './SetSynergiesOverlay'

describe('Wilds of Eldraine set synergies', () => {
  it('exposes all ten limited color-pair archetypes', () => {
    const archetypes = getArchetypesForSets(['WOE'])

    expect(archetypes.map((archetype) => archetype.name)).toEqual([
      'Tap Tempo',
      'Faeries',
      'Rats',
      'Ferocious Stompy',
      'Enchanted Creatures',
      'Bargain',
      'Spells',
      'Food',
      'Celebration Aggro',
      'Big Spells',
    ])
    expect(new Set(archetypes.map((archetype) => [...archetype.colors].sort().join('')))).toEqual(
      new Set(['UW', 'BU', 'BR', 'GR', 'GW', 'BW', 'RU', 'BG', 'RW', 'GU']),
    )
  })
})

describe('Aetherdrift set synergies', () => {
  it('provides every two-color limited archetype', () => {
    const archetypes = getArchetypesForSets(['DFT'])

    expect(archetypes.map(({ name }) => name)).toEqual([
      'Artifact Value',
      'Artifact Bleeder',
      'Max Speed Aggro',
      'Exhaust Midrange',
      'Vehicles and Mounts Midrange',
      'Max Speed Attrition',
      'Discard Aggro',
      'Graveyard',
      'Vehicles and Mounts Aggro',
      'Exhaust Ramp',
    ])
    expect(new Set(archetypes.map(({ colors }) => colors.join('')))).toEqual(
      new Set(['WU', 'UB', 'BR', 'RG', 'GW', 'WB', 'UR', 'BG', 'RW', 'GU']),
    )
    expect(archetypes.every(({ keyCard }) => keyCard != null)).toBe(true)
  })
})
