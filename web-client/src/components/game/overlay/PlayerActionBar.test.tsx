import type { ReactElement } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { GameStore } from '@/store/slices/types'
import type { LegalActionInfo } from '@/types'
import { entityId } from '@/types'

type ActionState = Pick<GameStore, 'legalActions' | 'pipelineState' | 'interactionEpoch' | 'submitAction'>
let state: ActionState
const submitAction = vi.fn()

vi.mock('@/store/gameStore.ts', () => ({
  useGameStore: Object.assign((selector: (value: ActionState) => unknown) => selector(state), {
    getState: () => state,
  }),
}))

const { PlayerActionBar } = await import('./PlayerActionBar')
const offered: LegalActionInfo = {
  actionType: 'TakePlayerAction',
  description: 'Channel: Pay 1 life to add {C}',
  action: { type: 'TakePlayerAction', playerId: entityId('me'), permissionId: 'channel-permission' },
  isAffordable: true,
  interactionEpoch: 'original',
}

function renderedClick(): () => void {
  const bar = PlayerActionBar() as ReactElement<{ children: ReactElement<{ onClick: () => void }>[] }>
  return bar.props.children[0]!.props.onClick
}

beforeEach(() => {
  submitAction.mockClear()
  state = { legalActions: [offered], pipelineState: null, interactionEpoch: 'original', submitAction }
})

describe('player action origins', () => {
  it('refuses a retained callback after a replacement timeline keeps the same permission', () => {
    const oldClick = renderedClick()
    state = {
      ...state,
      interactionEpoch: 'replacement',
      legalActions: [{ ...offered, interactionEpoch: 'replacement' }],
    }
    oldClick()
    expect(submitAction).not.toHaveBeenCalled()

    renderedClick()()
    expect(submitAction).toHaveBeenCalledExactlyOnceWith(offered.action, 'replacement')
  })

  it('keeps the rendered origin through a same-timeline update', () => {
    const click = renderedClick()
    state = { ...state, legalActions: [{ ...offered }] }
    click()
    expect(submitAction).toHaveBeenCalledExactlyOnceWith(offered.action, 'original')
  })

  it('rechecks affordability before submitting a retained callback', () => {
    const click = renderedClick()
    state = { ...state, legalActions: [{ ...offered, isAffordable: false }] }
    click()
    expect(submitAction).not.toHaveBeenCalled()
  })
})
