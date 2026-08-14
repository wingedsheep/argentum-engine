import { describe, expect, it } from 'vitest'
import {
  chooseTargetsView,
  chooseTargetsWalkReducer,
  initialChooseTargetsWalk,
  type ChooseTargetsWalkEvent,
  type ChooseTargetsWalkState,
} from './chooseTargetsWalk'
import type { ChooseTargetsDecision, ClientCard, EntityId, TargetRequirementInfo } from '@/types'
import { ZoneType } from '@/types'
import { entityId } from '@/types/entities.ts'

const owner = entityId('p2')

const card = (id: string, zoneType: ZoneType): ClientCard =>
  ({
    id: entityId(id),
    name: id,
    ownerId: owner,
    zone: { zoneType, ownerId: owner },
  }) as unknown as ClientCard

const bears = card('bears', ZoneType.BATTLEFIELD)
const elves = card('elves', ZoneType.BATTLEFIELD)
const courser = card('courser', ZoneType.GRAVEYARD)
const titan = card('titan', ZoneType.GRAVEYARD)
const cards = Object.fromEntries([bears, elves, courser, titan].map((c) => [c.id, c])) as Record<
  string,
  ClientCard
>

const req = (overrides: Partial<TargetRequirementInfo> = {}): TargetRequirementInfo => ({
  index: 0,
  minTargets: 0,
  maxTargets: 1,
  description: 'up to one target creature',
  ...overrides,
})

const decision = (
  requirements: TargetRequirementInfo[],
  legalTargets: Record<number, readonly EntityId[]>,
): ChooseTargetsDecision => ({
  type: 'ChooseTargetsDecision',
  id: 'd1',
  playerId: entityId('p1'),
  prompt: 'Choose targets',
  context: { phase: 'TRIGGER', sourceName: 'Taskmaster, Mercenary Mimic' },
  targetRequirements: requirements,
  legalTargets,
})

/**
 * Taskmaster, Mercenary Mimic's first-main-phase trigger: "becomes a copy of up to one target
 * creature on the battlefield **or** creature card in a graveyard". The server sends one target
 * requirement whose legal targets span both zones.
 */
const taskmaster = decision([req()], { 0: [bears.id, elves.id, courser.id, titan.id] })

const run = (
  events: ChooseTargetsWalkEvent[],
  from: ChooseTargetsWalkState = initialChooseTargetsWalk,
): ChooseTargetsWalkState => events.reduce(chooseTargetsWalkReducer, from)

const confirm = (targets: readonly EntityId[], totalRequirements = 1): ChooseTargetsWalkEvent => ({
  type: 'confirm',
  targets,
  totalRequirements,
})

describe('ChooseTargetsUI walk — mixed battlefield ∪ graveyard requirement', () => {
  it('offers both routes for a Taskmaster trigger instead of board-only clicking', () => {
    // Playtest regression: the decision path asked the all-or-nothing question ("are ALL legal
    // targets in a pile?"), got "no" because of the battlefield creatures, and fell through to
    // board clicking — leaving the graveyard cards with nothing to click.
    const view = chooseTargetsView(taskmaster, initialChooseTargetsWalk, cards)

    expect(view.isMixed).toBe(true)
    expect(view.collector).toBe('board')
    expect(view.pileCards).toEqual([courser, titan])
    expect(view.pileZoneLabel).toBe('Graveyard')
  })

  it('opens the pile picker on the graveyard half', () => {
    const walk = run([{ type: 'openPile', carried: [] }])
    const view = chooseTargetsView(taskmaster, walk, cards)

    expect(view.collector).toBe('pile')
    expect(view.pileCards.map((c) => c.id)).toEqual([courser.id, titan.id])
  })

  it('submits a graveyard card picked in the picker', () => {
    const walk = run([{ type: 'openPile', carried: [] }, confirm([courser.id])])

    expect(walk.submission).toEqual({ 0: [courser.id] })
  })

  it('submits a battlefield creature picked on the board', () => {
    const walk = run([confirm([bears.id])])

    expect(walk.submission).toEqual({ 0: [bears.id] })
  })

  it('declining the optional trigger submits nothing for the slot', () => {
    expect(run([confirm([])]).submission).toEqual({ 0: [] })
  })

  it('carries board picks into the picker so both halves fill the same requirement', () => {
    // A union requirement that wants two targets: one clicked on the board, one from the pile.
    const twoTargets = decision([req({ minTargets: 2, maxTargets: 2 })], {
      0: [bears.id, courser.id],
    })
    const walk = run([{ type: 'openPile', carried: [bears.id] }])

    expect(walk.pending).toEqual([bears.id])
    expect(chooseTargetsView(twoTargets, walk, cards).collector).toBe('pile')
    expect(run([confirm([bears.id, courser.id])], walk).submission).toEqual({
      0: [bears.id, courser.id],
    })
  })

  it('carries pile picks back to the board banner on View Battlefield', () => {
    const walk = run([
      { type: 'openPile', carried: [] },
      { type: 'closePile', carried: [courser.id] },
    ])

    expect(walk.pending).toEqual([courser.id])
    expect(chooseTargetsView(taskmaster, walk, cards).collector).toBe('board')
  })

  it('never shows the cross-over button when a requirement lives in one place', () => {
    const boardOnly = decision([req()], { 0: [bears.id, elves.id] })
    const pileOnly = decision([req()], { 0: [courser.id] })

    expect(chooseTargetsView(boardOnly, initialChooseTargetsWalk, cards)).toMatchObject({
      collector: 'board',
      isMixed: false,
      pileCards: [],
    })
    expect(chooseTargetsView(pileOnly, initialChooseTargetsWalk, cards)).toMatchObject({
      collector: 'pile',
      isMixed: false,
    })
  })
})

describe('ChooseTargetsUI walk — per-requirement walking', () => {
  // The Spot, Living Portal: "exile up to one target nonland permanent AND up to one target
  // nonland permanent card from a graveyard" — two requirements, one per collector.
  const spot = decision(
    [req({ index: 0 }), req({ index: 1, description: 'up to one card from a graveyard' })],
    { 0: [bears.id, elves.id], 1: [courser.id, titan.id] },
  )

  it('advances to the next requirement instead of submitting', () => {
    const walk = run([confirm([bears.id], 2)])

    expect(walk.requirementIndex).toBe(1)
    expect(walk.submission).toBeNull()
    expect(chooseTargetsView(spot, walk, cards).collector).toBe('pile')
  })

  it('submits every slot together once the last requirement is answered', () => {
    const walk = run([confirm([bears.id], 2), confirm([courser.id], 2)])

    expect(walk.submission).toEqual({ 0: [bears.id], 1: [courser.id] })
  })

  it('drops a target already confirmed for another requirement from the pool', () => {
    const bothBoard = decision([req({ index: 0 }), req({ index: 1 })], {
      0: [bears.id, elves.id],
      1: [bears.id, elves.id],
    })
    const walk = run([confirm([bears.id], 2)])

    expect(chooseTargetsView(bothBoard, walk, cards).legalTargets).toEqual([elves.id])
  })

  it('steps Back with the previous requirement pre-selected and its pool restored', () => {
    const bothBoard = decision([req({ index: 0 }), req({ index: 1 })], {
      0: [bears.id, elves.id],
      1: [bears.id, elves.id],
    })
    const walk = run([confirm([bears.id], 2), { type: 'back' }])

    expect(walk.requirementIndex).toBe(0)
    expect(walk.pending).toEqual([bears.id])
    expect(walk.collected).toEqual({})
    expect(chooseTargetsView(bothBoard, walk, cards).legalTargets).toEqual([bears.id, elves.id])
  })

  it('builds a fresh submission object on every confirm', () => {
    // ChooseTargetsUI dedupes its SubmitDecision on the identity of `submission`: StrictMode
    // re-runs the effect with the same object (suppress), while a re-confirm after a rejected
    // submission must produce a different one (send). That only works if the reducer never hands
    // back the same object twice.
    const first = run([confirm([bears.id])])
    const second = chooseTargetsWalkReducer(first, confirm([elves.id]))

    expect(second.submission).not.toBe(first.submission)
    expect(second.submission).toEqual({ 0: [elves.id] })
  })

  it('is a no-op stepping Back from the first requirement', () => {
    expect(chooseTargetsWalkReducer(initialChooseTargetsWalk, { type: 'back' })).toBe(
      initialChooseTargetsWalk,
    )
  })

  it('closes a picker left open when the requirement changes', () => {
    // Otherwise the picker would reopen over the next requirement, which may have no pile at all.
    const advanced = run([{ type: 'openPile', carried: [] }, confirm([courser.id], 2)])
    expect(advanced.pilePickerOpen).toBe(false)

    const backwards = run([confirm([bears.id], 2), { type: 'openPile', carried: [] }, { type: 'back' }])
    expect(backwards.pilePickerOpen).toBe(false)
  })
})
