/**
 * The wizard's URL is now its state, so an encoding that loses or mangles an answer is a broken
 * screen rather than a cosmetic bug. This walks the same space `modeMatrix.test.ts` does — every
 * roster × Cards × shape the wizard can offer — and asserts the round trip.
 */
import { describe, it, expect } from 'vitest'
import {
  ROSTERS,
  SHAPE_IDS,
  cardsChoices,
  defaultCardsAxis,
  shapeChoices,
  type Roster,
  type ShapeId,
} from '../lobby/modeMatrix'
import type { CardsAxis } from '../lobby/axes'
import { EMPTY_DRAFT, draftToPath, pathToDraft, type WizardDraft } from './wizardUrl'

/**
 * Every (roster, cards) the wizard actually offers.
 *
 * A Cards answer is a *kind* at its default sub-shape — the sealed/draft shape is a lobby sub-option,
 * so it is deliberately not in the URL and not in this walk.
 */
function offeredCards(roster: Roster): CardsAxis[] {
  return cardsChoices(roster)
    .filter((c) => !c.disabledReason)
    .map((c) => defaultCardsAxis(c.value))
}

/** Every complete selection reachable through the wizard. */
function everySelection(): WizardDraft[] {
  const out: WizardDraft[] = []
  for (const roster of ROSTERS) {
    for (const cards of offeredCards(roster)) {
      for (const shape of shapeChoices(roster, cards).filter((s) => !s.disabledReason)) {
        out.push({ roster, cards, shape: shape.value })
      }
    }
  }
  return out
}

function roundTrip(draft: WizardDraft): WizardDraft {
  return pathToDraft(draftToPath(draft), true)
}

describe('wizard URL round trip', () => {
  const selections = everySelection()

  it('covers a non-trivial space', () => {
    expect(selections.length).toBeGreaterThan(20)
  })

  it('survives every complete selection', () => {
    const broken = selections.filter((s) => {
      const back = roundTrip(s)
      return JSON.stringify(back) !== JSON.stringify(s)
    })
    expect(broken.map((s) => draftToPath(s))).toEqual([])
  })

  it('survives every partial selection', () => {
    const partials: WizardDraft[] = [
      EMPTY_DRAFT,
      ...ROSTERS.map((roster) => ({ ...EMPTY_DRAFT, roster })),
    ]
    for (const roster of ROSTERS) {
      for (const cards of offeredCards(roster)) {
        // Only meaningful where step 3 is a real question — with one open shape, decoding fills it
        // in on purpose, which is what makes `/play/solo/bring-a-deck` a complete selection.
        if (shapeChoices(roster, cards).filter((s) => !s.disabledReason).length > 1) {
          partials.push({ ...EMPTY_DRAFT, roster, cards })
        }
      }
    }
    const broken = partials.filter((p) => JSON.stringify(roundTrip(p)) !== JSON.stringify(p))
    expect(broken.map((p) => draftToPath(p))).toEqual([])
  })

  it('gives every selection a distinct path', () => {
    const paths = selections.map(draftToPath)
    expect(new Set(paths).size).toBe(paths.length)
  })

  it('writes a path and nothing else — every answer is a segment', () => {
    for (const path of selections.map(draftToPath)) {
      expect(path).not.toContain('?')
      expect(path).toMatch(/^\/play(\/[a-z0-9-]+)+$/)
    }
  })
})

describe('wizard URL decoding is defensive', () => {
  it('ignores paths it does not own', () => {
    for (const path of ['/', '/help', '/deckbuilder', '/tournament/abc', '/playground']) {
      expect(pathToDraft(path, true), path).toEqual(EMPTY_DRAFT)
    }
  })

  it('drops an unknown roster entirely', () => {
    expect(pathToDraft('/play/nobody', true)).toEqual(EMPTY_DRAFT)
    expect(pathToDraft('/play', true)).toEqual(EMPTY_DRAFT)
  })

  it('drops a solo selection when the server has no AI', () => {
    expect(pathToDraft('/play/solo/bring-a-deck', false)).toEqual(EMPTY_DRAFT)
    expect(pathToDraft('/play/solo/bring-a-deck', true).roster).toBe('SOLO')
  })

  it('truncates to the roster when the Cards value is unreachable for it', () => {
    // Momir and Random pool exist only on the two-seat lobby that plays one game.
    for (const slug of ['momir', 'random']) {
      const back = pathToDraft(`/play/group/${slug}`, true)
      expect(back, slug).toEqual({ ...EMPTY_DRAFT, roster: 'GROUP' })
    }
  })

  it('does not encode the sealed or draft sub-shape', () => {
    // It is a lobby sub-option, like deck legality — the wizard commits to the kind at its default.
    expect(draftToPath({ roster: 'GROUP', cards: { kind: 'DRAFT', shape: 'WINSTON' }, shape: 'BRACKET' }))
      .toBe('/play/group/draft/bracket')
    expect(pathToDraft('/play/group/draft', true).cards).toEqual({ kind: 'DRAFT', shape: 'BOOSTER' })
    expect(pathToDraft('/play/friend/sealed', true).cards).toEqual({ kind: 'SEALED', shape: 'STANDARD' })
    // A slug that spells one out is simply not a Cards value, so it truncates to the roster.
    expect(pathToDraft('/play/group/draft-winston', true))
      .toEqual({ ...EMPTY_DRAFT, roster: 'GROUP' })
  })

  it('drops an unknown or unreachable shape but keeps the answers before it', () => {
    const back = pathToDraft('/play/group/draft/not-a-shape', true)
    expect(back.roster).toBe('GROUP')
    expect(back.cards).toEqual({ kind: 'DRAFT', shape: 'BOOSTER' })
    expect(back.shape).toBeNull()
  })

  it('auto-resolves a one-answer shape step', () => {
    const back = pathToDraft('/play/friend/random', true)
    expect(back.shape).toBe<ShapeId>('ONE_GAME')
  })

  it('tolerates a trailing slash', () => {
    expect(pathToDraft('/play/group/', true)).toEqual({ ...EMPTY_DRAFT, roster: 'GROUP' })
  })

  it('has a slug for every shape id', () => {
    // A missing case would make the path collide or read `undefined`; the regex catches that here
    // rather than in the address bar.
    for (const shape of SHAPE_IDS) {
      const path = draftToPath({ roster: 'GROUP', cards: { kind: 'BRING_A_DECK', legality: null }, shape })
      expect(path, shape).toMatch(/^\/play\/group\/bring-a-deck\/[a-z-]+$/)
    }
  })
})
