import { describe, expect, it, vi } from 'vitest'
import type { LegalActionInfo } from '../types'

// selectors.ts transitively imports gameStore.ts, whose gameplay slice reads
// localStorage.getItem(...) at module-init time to seed autoTapEnabled — a browser global
// the plain Node vitest environment (this project's only one; no jsdom/happy-dom installed)
// doesn't provide. cardIdForAction/isHighlightable are pure and don't need a real store; stub
// just enough of the Storage API before the import pulls the store in.
vi.stubGlobal('localStorage', {
  getItem: () => null,
  setItem: () => {},
  removeItem: () => {},
})

const { cardIdForAction, isHighlightable } = await import('./selectors')
const { entityId } = await import('../types')

// --- Fixture builders -------------------------------------------------------
// Minimal LegalActionInfo objects — cardIdForAction/isHighlightable only read
// `action` and a handful of top-level flags, so these stay as small as the type allows.

const PLAYER = entityId('p1')
const CARD = entityId('c1')
const SOURCE = entityId('src1')
const VEHICLE = entityId('veh1')
const MOUNT = entityId('mnt1')
const ROOM = entityId('room1')

function actionInfo(action: LegalActionInfo['action'], opts: Partial<LegalActionInfo> = {}): LegalActionInfo {
  return {
    actionType: action.type,
    description: action.type,
    action,
    ...opts,
  }
}

describe('cardIdForAction', () => {
  // These four are anchored to `cardId` and always have been.
  it.each([
    ['PlayLand', { type: 'PlayLand', playerId: PLAYER, cardId: CARD }],
    ['CastSpell', { type: 'CastSpell', playerId: PLAYER, cardId: CARD }],
    ['CycleCard', { type: 'CycleCard', playerId: PLAYER, cardId: CARD }],
    ['TypecycleCard', { type: 'TypecycleCard', playerId: PLAYER, cardId: CARD }],
  ] as const)('resolves %s to its cardId', (_label, action) => {
    expect(cardIdForAction(actionInfo(action))).toBe(CARD)
  })

  // PlotCard and SuspendCardFromHand were both missing here — the exact bug that left a
  // suspendable/plottable card in hand with no "you can play this" highlight glow, unlike
  // every other playable card. Regression coverage for both.
  it('resolves PlotCard to its cardId', () => {
    expect(cardIdForAction(actionInfo({ type: 'PlotCard', playerId: PLAYER, cardId: CARD }))).toBe(CARD)
  })

  it('resolves SuspendCardFromHand to its cardId', () => {
    expect(cardIdForAction(actionInfo({ type: 'SuspendCardFromHand', playerId: PLAYER, cardId: CARD }))).toBe(CARD)
  })

  it('resolves ActivateAbility to its sourceId, not a cardId', () => {
    expect(cardIdForAction(actionInfo({
      type: 'ActivateAbility', playerId: PLAYER, sourceId: SOURCE, abilityId: 'a1', targets: [],
    }))).toBe(SOURCE)
  })

  it('resolves TurnFaceUp to its sourceId', () => {
    expect(cardIdForAction(actionInfo({ type: 'TurnFaceUp', playerId: PLAYER, sourceId: SOURCE }))).toBe(SOURCE)
  })

  it('resolves CrewVehicle to its vehicleId', () => {
    expect(cardIdForAction(actionInfo({
      type: 'CrewVehicle', playerId: PLAYER, vehicleId: VEHICLE, crewCreatures: [],
    }))).toBe(VEHICLE)
  })

  it('resolves SaddleMount to its mountId', () => {
    expect(cardIdForAction(actionInfo({
      type: 'SaddleMount', playerId: PLAYER, mountId: MOUNT, saddleCreatures: [],
    }))).toBe(MOUNT)
  })

  it('resolves UnlockRoomDoor to its roomId', () => {
    expect(cardIdForAction(actionInfo({
      type: 'UnlockRoomDoor', playerId: PLAYER, roomId: ROOM, faceId: 'Ritual Chamber',
    }))).toBe(ROOM)
  })

  it('returns undefined for an action with no card anchor (PassPriority)', () => {
    expect(cardIdForAction(actionInfo({ type: 'PassPriority', playerId: PLAYER }))).toBeUndefined()
  })
})

describe('isHighlightable', () => {
  const castSpell = { type: 'CastSpell' as const, playerId: PLAYER, cardId: CARD }

  it('is highlightable when affordable and not a mana ability', () => {
    expect(isHighlightable(actionInfo(castSpell, { isAffordable: true }))).toBe(true)
  })

  it('is not highlightable when explicitly unaffordable', () => {
    expect(isHighlightable(actionInfo(castSpell, { isAffordable: false }))).toBe(false)
  })

  it('defaults to highlightable when isAffordable is unset', () => {
    expect(isHighlightable(actionInfo(castSpell))).toBe(true)
  })

  it('is not highlightable for a plain mana ability (no cost info)', () => {
    expect(isHighlightable(actionInfo(
      { type: 'ActivateAbility', playerId: PLAYER, sourceId: SOURCE, abilityId: 'tap', targets: [] },
      { isManaAbility: true },
    ))).toBe(false)
  })

  it('is highlightable for a mana ability with an X the player must choose (storage lands)', () => {
    expect(isHighlightable(actionInfo(
      { type: 'ActivateAbility', playerId: PLAYER, sourceId: SOURCE, abilityId: 'tap', targets: [] },
      { isManaAbility: true, hasXCost: true, maxAffordableX: 3 },
    ))).toBe(true)
  })

  it('is highlightable for a mana ability that carries a mana cost (e.g. a filter land)', () => {
    expect(isHighlightable(actionInfo(
      { type: 'ActivateAbility', playerId: PLAYER, sourceId: SOURCE, abilityId: 'tap', targets: [] },
      { isManaAbility: true, manaCostString: '{U}' },
    ))).toBe(true)
  })
})
