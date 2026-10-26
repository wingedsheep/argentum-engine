import { describe, expect, it } from 'vitest'
import { isActionOnCard } from './useLegalActions'
import type { LegalActionInfo } from '../types'
import { entityId } from '../types'

const PLAYER = entityId('p1')
const CARD = entityId('c1')
const OTHER = entityId('c2')

const handAction = (type: 'ForetellCard' | 'PlotCard' | 'SuspendCardFromHand', cardId = CARD): LegalActionInfo =>
  ({ actionType: type, description: type, action: { type, playerId: PLAYER, cardId } }) as LegalActionInfo

describe('isActionOnCard', () => {
  // A card whose only action is a special action from hand (a foretell card with no legal cast)
  // must still open its menu when clicked.
  it.each(['ForetellCard', 'PlotCard', 'SuspendCardFromHand'] as const)('maps %s to its card', (type) => {
    expect(isActionOnCard(handAction(type), CARD)).toBe(true)
    expect(isActionOnCard(handAction(type, OTHER), CARD)).toBe(false)
  })

  it('ignores actions not taken through a card', () => {
    const pass: LegalActionInfo = {
      actionType: 'PassPriority',
      description: 'Pass priority',
      action: { type: 'PassPriority', playerId: PLAYER },
    }
    expect(isActionOnCard(pass, CARD)).toBe(false)
  })
})
