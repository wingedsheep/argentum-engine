import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import { entityId, type ClientCard, type ClientGameState, type DecisionContext } from '@/types'
import { DecisionContextCards, hasDecisionContextCards, resolveDecisionCards } from './DecisionContextCards'

vi.mock('@/utils/cardImages.ts', () => ({
  getCardImageUrl: (name: string) => name === 'Card without art' ? '' : `https://example.test/${encodeURIComponent(name)}.jpg`,
}))

function card(id: string, name: string, extra: Partial<ClientCard> = {}): ClientCard {
  return { id: entityId(id), name, ...extra } as ClientCard
}

function state(cards: ClientCard[]): ClientGameState {
  return {
    cards: Object.fromEntries(cards.map((value) => [value.id, value])),
    players: [{ playerId: entityId('opponent'), name: 'Opponent' }],
  } as unknown as ClientGameState
}

function context(targetIds: string[], subjectEntityId?: string): DecisionContext {
  return {
    phase: 'RESOLUTION',
    targetIds: targetIds.map(entityId),
    ...(subjectEntityId ? { subjectEntityId: entityId(subjectEntityId) } : {}),
  }
}

function markup(value: ReturnType<typeof resolveDecisionCards>): string {
  return renderToStaticMarkup(<DecisionContextCards cards={value} />)
}

describe('resolution decision targets', () => {
  it('shows multiple card and player targets in their announced order', () => {
    const resolved = resolveDecisionCards(context(['second', 'opponent', 'first']), state([
      card('first', 'Grizzly Bears'), card('second', 'Hill Giant'),
    ]))
    expect(resolved.targets?.map((target) => target.name)).toEqual(['Hill Giant', 'Opponent', 'Grizzly Bears'])
    const html = markup(resolved)
    expect(html.indexOf('Hill Giant')).toBeLessThan(html.indexOf('Opponent'))
    expect(html.indexOf('Opponent')).toBeLessThan(html.indexOf('Grizzly Bears'))
    expect(html.match(/>Target</g)).toHaveLength(3)
  })

  it('uses the masked face-down name for target text and art', () => {
    const resolved = resolveDecisionCards(context(['hidden']), state([
      card('hidden', 'Face-down creature', { isFaceDown: true }),
    ]))
    const html = markup(resolved)
    expect(resolved.targets?.[0]?.name).toBe('Face-down creature')
    expect(html).toContain('Face-down creature')
    expect(html).not.toContain('Vesuvan Doppelganger')
  })

  it('keeps an iteration subject independent of the selected targets', () => {
    const resolved = resolveDecisionCards(context(['target'], 'subject'), state([
      card('subject', 'Hill Giant'), card('target', 'Grizzly Bears'),
    ]))
    expect(resolved.subject?.card.id).toBe(entityId('subject'))
    expect(resolved.targets?.map((target) => target.id)).toEqual([entityId('target')])
    const html = markup(resolved)
    expect(html).toContain('Deciding for')
    expect(html).toContain('Hill Giant')
    expect(html).toContain('Grizzly Bears')
  })

  it('shows a target name even when no image can be rendered', () => {
    const resolved = resolveDecisionCards(context(['no-art']), state([card('no-art', 'Card without art')]))
    expect(resolved.targets?.[0]?.entry).toBeUndefined()
    expect(hasDecisionContextCards(resolved)).toBe(true)
    const html = markup(resolved)
    expect(html).toContain('Card without art')
    expect(html).not.toContain('<img')
  })

  it('resolves a stack spell through the visible card map', () => {
    const resolved = resolveDecisionCards(context(['spell']), state([card('spell', 'Counterspell')]))
    expect(resolved.targets?.[0]?.name).toBe('Counterspell')
    expect(markup(resolved)).toContain('Counterspell')
  })
})
