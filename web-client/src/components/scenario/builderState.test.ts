import { describe, expect, it } from 'vitest'
import { clearSeat, fromSpec, swapSeatBoards, toSpec } from './builderState'
import type { ScenarioSpec } from './types'
import { decodeScenario, encodeScenario } from './shareScenario'

describe('Scenario Builder player counters', () => {
  it('preserves imported duel counters through editing, export and sharing', async () => {
    const spec: ScenarioSpec = { player1: { counters: { RAD: 3 } }, player2: { counters: { ENERGY: 2 } } }
    const state = fromSpec(spec)
    state.seats[0]!.life = 18
    const exported = toSpec(state)
    expect(exported.player1!.counters).toEqual({ RAD: 3 })
    expect(exported.player2!.counters).toEqual({ ENERGY: 2 })
    const shared = await decodeScenario(await encodeScenario(exported))
    expect(shared).not.toBeNull()
    expect(toSpec(fromSpec(shared!)).player1!.counters).toEqual({ RAD: 3 })
    expect(state.seats[0]!.counters).not.toBe(spec.player1!.counters)
    expect(exported.player1!.counters).not.toBe(state.seats[0]!.counters)
  })

  it('keeps counters on every multiplayer seat', () => {
    const state = fromSpec({ players: [
      { name: 'One', config: { counters: { RAD: 2 } } },
      { name: 'Two', config: {} },
      { name: 'Three', config: { counters: { RAD: 5, POISON: 1 } } },
    ] })
    const spec = toSpec(state)
    expect(spec.players?.map((seat) => seat.config?.counters)).toEqual([{ RAD: 2 }, undefined, { RAD: 5, POISON: 1 }])
    expect(fromSpec(spec).seats.map((seat) => seat.counters)).toEqual([{ RAD: 2 }, {}, { RAD: 5, POISON: 1 }])
  })

  it('swaps counters with the board and clears them with the seat', () => {
    const state = fromSpec({ player1: { counters: { RAD: 3 } }, player2: { counters: { ENERGY: 2 } } })
    const [a, b] = state.seats
    const swapped = swapSeatBoards(state, a!.id, b!.id)
    expect(swapped.seats.map((seat) => seat.counters)).toEqual([{ ENERGY: 2 }, { RAD: 3 }])
    expect(toSpec(clearSeat(swapped, a!.id)).player1!.counters).toBeUndefined()
    expect(state.seats[0]!.counters).toEqual({ RAD: 3 })
  })
})
