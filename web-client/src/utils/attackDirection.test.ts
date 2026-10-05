import { describe, it, expect } from 'vitest'
import type { ClientPlayer } from '@/types'
import { entityId } from '@/types'
import { attackNeighbours } from './attackDirection'

function seat(id: string, hasLost = false): ClientPlayer {
  return { playerId: entityId(id), hasLost } as unknown as ClientPlayer
}

// Turn order A → B → C → D.
const table = [seat('a'), seat('b'), seat('c'), seat('d')]

describe('attackNeighbours', () => {
  it('attack left: you attack the next seat and are attacked by the previous one', () => {
    expect(attackNeighbours(table, 'LEFT', entityId('a'))).toEqual({
      mode: 'LEFT',
      attacks: entityId('b'),
      attackedBy: entityId('d'),
    })
  })

  it('attack right: you attack the previous seat and are attacked by the next one', () => {
    expect(attackNeighbours(table, 'RIGHT', entityId('a'))).toEqual({
      mode: 'RIGHT',
      attacks: entityId('d'),
      attackedBy: entityId('b'),
    })
  })

  it('skips eliminated seats, as the engine does', () => {
    const withLoss = [seat('a'), seat('b', true), seat('c'), seat('d')]
    expect(attackNeighbours(withLoss, 'LEFT', entityId('a'))).toMatchObject({
      attacks: entityId('c'),
      attackedBy: entityId('d'),
    })
  })

  it('is null once only two players remain — left and right are the same opponent', () => {
    const headsUp = [seat('a'), seat('b', true), seat('c'), seat('d', true)]
    expect(attackNeighbours(headsUp, 'LEFT', entityId('a'))).toBeNull()
  })

  it('is null when any opponent may be attacked', () => {
    expect(attackNeighbours(table, 'MULTIPLE', entityId('a'))).toBeNull()
    expect(attackNeighbours(table, null, entityId('a'))).toBeNull()
  })

  it('is null for an eliminated or unknown seat', () => {
    const withLoss = [seat('a', true), seat('b'), seat('c'), seat('d')]
    expect(attackNeighbours(withLoss, 'LEFT', entityId('a'))).toBeNull()
    expect(attackNeighbours(table, 'LEFT', entityId('zz'))).toBeNull()
  })
})
