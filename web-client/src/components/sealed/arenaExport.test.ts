import { describe, expect, it } from 'vitest'
import { buildArenaDeckList, type ArenaExportInput } from './arenaExport'
import { parseArenaDeckList } from '../deckbuilder/parseArenaDeck'
import type { SealedCardInfo } from '@/types'

// Minimal pool card — only the fields the exporter reads matter here.
const card = (
  name: string,
  setCode?: string,
  collectorNumber?: string
): SealedCardInfo => ({
  name,
  manaCost: null,
  typeLine: '',
  rarity: 'common',
  imageUri: null,
  ...(setCode !== undefined ? { setCode } : {}),
  ...(collectorNumber !== undefined ? { collectorNumber } : {}),
})

const base: ArenaExportInput = {
  deck: [],
  cardPool: [],
  basicLands: [],
  landCounts: {},
  commander: null,
}

describe('buildArenaDeckList', () => {
  it('renders copies with set + collector number, aggregated and sorted', () => {
    const input: ArenaExportInput = {
      ...base,
      deck: ['Lightning Bolt', 'Lightning Bolt', 'Ancestral Recall'],
      cardPool: [
        card('Lightning Bolt', 'lea', '161'),
        card('Lightning Bolt', 'lea', '161'),
        card('Ancestral Recall', 'lea', '47'),
      ],
    }
    expect(buildArenaDeckList(input)).toBe(
      ['Deck', '1 Ancestral Recall (LEA) 47', '2 Lightning Bolt (LEA) 161'].join('\n')
    )
  })

  it('falls back to bare name when a card lacks printing metadata', () => {
    const input: ArenaExportInput = {
      ...base,
      deck: ['Test Goblin'],
      cardPool: [card('Test Goblin')],
    }
    expect(buildArenaDeckList(input)).toBe(['Deck', '1 Test Goblin'].join('\n'))
  })

  it('emits basic lands from landCounts inside the Deck section', () => {
    const input: ArenaExportInput = {
      ...base,
      deck: ['Lightning Bolt'],
      cardPool: [card('Lightning Bolt', 'lea', '161')],
      basicLands: [card('Mountain', 'lea', '292')],
      landCounts: { Mountain: 17, Island: 0 },
    }
    expect(buildArenaDeckList(input)).toBe(
      ['Deck', '1 Lightning Bolt (LEA) 161', '17 Mountain (LEA) 292'].join('\n')
    )
  })

  it('lists unused pool copies as the Sideboard', () => {
    const input: ArenaExportInput = {
      ...base,
      deck: ['Lightning Bolt'],
      cardPool: [
        card('Lightning Bolt', 'lea', '161'),
        card('Lightning Bolt', 'lea', '161'), // one copy left in the pool
        card('Shivan Dragon', 'lea', '174'),
      ],
    }
    expect(buildArenaDeckList(input)).toBe(
      [
        'Deck',
        '1 Lightning Bolt (LEA) 161',
        '',
        'Sideboard',
        '1 Lightning Bolt (LEA) 161',
        '1 Shivan Dragon (LEA) 174',
      ].join('\n')
    )
  })

  it('puts the commander in its own section, out of the maindeck and sideboard', () => {
    const input: ArenaExportInput = {
      ...base,
      deck: ['Atraxa, Praetors’ Voice', 'Sol Ring'],
      cardPool: [
        card('Atraxa, Praetors’ Voice', 'cmr', '1'),
        card('Sol Ring', 'cmr', '472'),
      ],
      commander: 'Atraxa, Praetors’ Voice',
    }
    expect(buildArenaDeckList(input)).toBe(
      [
        'Commander',
        '1 Atraxa, Praetors’ Voice (CMR) 1',
        '',
        'Deck',
        '1 Sol Ring (CMR) 472',
      ].join('\n')
    )
  })

  it('returns empty string when there is nothing to export', () => {
    expect(buildArenaDeckList(base)).toBe('')
  })

  it('round-trips through the Arena deck-list parser', () => {
    const input: ArenaExportInput = {
      ...base,
      deck: ['Lightning Bolt', 'Lightning Bolt', 'Serra Angel'],
      cardPool: [
        card('Lightning Bolt', 'lea', '161'),
        card('Lightning Bolt', 'lea', '161'),
        card('Lightning Bolt', 'lea', '161'), // a third copy stays in the pool
        card('Serra Angel', 'lea', '46'),
      ],
      basicLands: [card('Plains', 'lea', '286')],
      landCounts: { Plains: 16 },
    }
    const parsed = parseArenaDeckList(buildArenaDeckList(input))
    expect(parsed.errors).toEqual([])
    expect(parsed.entries).toEqual([
      expect.objectContaining({ count: 2, name: 'Lightning Bolt', setCode: 'LEA', collectorNumber: '161' }),
      expect.objectContaining({ count: 1, name: 'Serra Angel', setCode: 'LEA', collectorNumber: '46' }),
      expect.objectContaining({ count: 16, name: 'Plains', setCode: 'LEA', collectorNumber: '286' }),
    ])
    expect(parsed.sideboard).toEqual([
      expect.objectContaining({ count: 1, name: 'Lightning Bolt', setCode: 'LEA', collectorNumber: '161' }),
    ])
  })
})
