import { describe, expect, it } from 'vitest'
import { attachmentStackLayout, shouldShowCastModal } from './shared'
import type { LegalActionInfo } from '../../../types'
import { entityId } from '../../../types'

// --- Fixture builders -------------------------------------------------------
// Minimal LegalActionInfo objects — `shouldShowCastModal` only reads `actionType`
// and `action.type`, so we keep these as small as the type allows.

const PLAYER = entityId('p1')
const CARD = entityId('c1')

function castSpell(opts: { affordable?: boolean } = {}): LegalActionInfo {
  return {
    actionType: 'CastSpell',
    description: 'Cast',
    action: { type: 'CastSpell', playerId: PLAYER, cardId: CARD },
    ...(opts.affordable !== undefined ? { isAffordable: opts.affordable } : {}),
  }
}

function cycle(opts: { affordable?: boolean } = {}): LegalActionInfo {
  return {
    actionType: 'CycleCard',
    description: 'Cycle',
    action: { type: 'CycleCard', playerId: PLAYER, cardId: CARD },
    ...(opts.affordable !== undefined ? { isAffordable: opts.affordable } : {}),
  }
}

function typecycle(): LegalActionInfo {
  return {
    actionType: 'TypecycleCard',
    description: 'Plainscycling',
    action: { type: 'TypecycleCard', playerId: PLAYER, cardId: CARD },
  }
}

function plot(): LegalActionInfo {
  return {
    actionType: 'PlotCard',
    description: 'Plot',
    action: { type: 'PlotCard', playerId: PLAYER, cardId: CARD },
  }
}

function suspend(): LegalActionInfo {
  return {
    actionType: 'SuspendCardFromHand',
    description: 'Suspend',
    action: { type: 'SuspendCardFromHand', playerId: PLAYER, cardId: CARD },
  }
}

function morph(): LegalActionInfo {
  return {
    actionType: 'CastFaceDown',
    description: 'Cast Face-Down',
    action: { type: 'CastSpell', playerId: PLAYER, cardId: CARD, castFaceDown: true },
  }
}

function playLand(): LegalActionInfo {
  return {
    actionType: 'PlayLand',
    description: 'Play land',
    action: { type: 'PlayLand', playerId: PLAYER, cardId: CARD },
  }
}

describe('shouldShowCastModal', () => {
  it('does not open the menu when there are no legal actions', () => {
    expect(shouldShowCastModal([])).toBe(false)
  })

  it('does not open the menu for a single plain cast', () => {
    expect(shouldShowCastModal([castSpell()])).toBe(false)
  })

  it('opens the menu when both cast and cycle are affordable (two actions)', () => {
    expect(shouldShowCastModal([castSpell(), cycle()])).toBe(true)
  })

  // The core fix: a cycling card the player can only afford to *cycle*. The server omits
  // the unaffordable normal cast, so the only legal action is CycleCard — but dragging it
  // must still open the menu (with a grayed-out "Cast") so the player can choose or cancel
  // rather than silently cycling a card they meant to hard-cast.
  it('opens the menu for a card with only the cycle action affordable', () => {
    expect(shouldShowCastModal([cycle({ affordable: true })])).toBe(true)
  })

  it('opens the menu even when the cycle action itself is unaffordable', () => {
    expect(shouldShowCastModal([cycle({ affordable: false })])).toBe(true)
  })

  it('opens the menu for a card with only typecycling', () => {
    expect(shouldShowCastModal([typecycle()])).toBe(true)
  })

  it('opens the menu for a card with only a plot action', () => {
    expect(shouldShowCastModal([plot()])).toBe(true)
  })

  // Ancestral Vision has no printed mana cost, so its only legal action is ever
  // SuspendCardFromHand — the menu must still open (with a grayed-out "Cast") rather than
  // silently suspending a card the player might have wanted to reconsider.
  it('opens the menu for a card with only a suspend action', () => {
    expect(shouldShowCastModal([suspend()])).toBe(true)
  })

  it('opens the menu for a cycling land that already played a land (cycle only)', () => {
    expect(shouldShowCastModal([cycle()])).toBe(true)
  })

  it('opens the menu for a cycling land with both play-land and cycle', () => {
    expect(shouldShowCastModal([playLand(), cycle()])).toBe(true)
  })

  it('does not open the menu for a plain land with only a play-land action', () => {
    expect(shouldShowCastModal([playLand()])).toBe(false)
  })

  it('opens the menu for multiple casting variants (morph + normal cast)', () => {
    expect(shouldShowCastModal([castSpell(), morph()])).toBe(true)
  })
})

// --- attachmentStackLayout --------------------------------------------------

describe('attachmentStackLayout', () => {
  // Realistic desktop numbers: a 100x140 card, 16px peek, 28px sideways gutter.
  const CARD_WIDTH = 100
  const CARD_HEIGHT = 140
  const PEEK = 16
  const GUTTER = 28
  // GameCard reserves `height + 8` for a card lying sideways.
  const UPRIGHT_BOX = CARD_WIDTH
  const SIDEWAYS_BOX = CARD_HEIGHT + 8

  function layout(hostTapped: boolean, attachmentsTapped: readonly boolean[]) {
    return attachmentStackLayout({
      cardWidth: CARD_WIDTH,
      cardHeight: CARD_HEIGHT,
      peek: PEEK,
      hostTapped,
      attachmentsTapped,
      gutter: GUTTER,
    })
  }

  // The reported bug: with the whole stack sharing one rotated wrapper, tapping the
  // equipped creature rotated its Equipment too, so an untapped Equipment looked
  // tapped and players couldn't tell it was still available to tap. An Equipment and
  // its host are independent permanents (CR 301.5d) — orientation is per-card.
  it('leaves an untapped attachment upright when the host is tapped', () => {
    const { attachments, host } = layout(true, [false])

    expect(attachments[0]?.width).toBe(UPRIGHT_BOX)
    expect(host.width).toBe(SIDEWAYS_BOX)
  })

  it('lays a tapped attachment sideways even while the host is untapped', () => {
    const { attachments, host } = layout(false, [true])

    expect(attachments[0]?.width).toBe(SIDEWAYS_BOX)
    expect(host.width).toBe(UPRIGHT_BOX)
  })

  it('keeps every card upright when nothing is tapped, reserving no sideways gutter', () => {
    const { attachments, host, containerWidth } = layout(false, [false])

    expect(attachments[0]?.width).toBe(UPRIGHT_BOX)
    expect(host.width).toBe(UPRIGHT_BOX)
    expect(containerWidth).toBe(CARD_WIDTH)
  })

  it('orients each card independently in a mixed stack', () => {
    const { attachments, host } = layout(true, [false, true])

    expect(attachments.map((a) => a.width)).toEqual([UPRIGHT_BOX, SIDEWAYS_BOX])
    expect(host.width).toBe(SIDEWAYS_BOX)
  })

  // Each upright attachment shows one more `peek` of itself than the card in front of
  // it, and that ladder must not shift when the *host* taps — otherwise a peeking
  // Equipment would jump around as its host taps and untaps.
  it('stacks the upright peek ladder by index regardless of host tap state', () => {
    const untappedHost = layout(false, [false, false])
    const tappedHost = layout(true, [false, false])

    expect(untappedHost.attachments.map((a) => a.top)).toEqual([0, PEEK])
    expect(tappedHost.attachments.map((a) => a.top)).toEqual([0, PEEK])
    expect(tappedHost.host.top).toBe(2 * PEEK)
  })

  // A sideways attachment is already wider than its host, so it shows down both flanks
  // without needing height above the card. Riding above the host just pushed it away
  // from the permanent it belongs to; tuck it under instead.
  // GameCard bottom-aligns a sideways card's visible band inside its own box, so a box
  // ending at the container bottom puts the band on the host's lower edge.
  it('bottom-aligns a sideways attachment level with the host', () => {
    const { attachments, host, containerHeight } = layout(false, [true])
    const box = attachments[0]!

    expect(box.top + CARD_HEIGHT).toBe(containerHeight)
    expect(box.top).toBe(host.top)
  })

  it('gives a sideways attachment no peek height of its own', () => {
    // One upright attachment earns a peek; adding a sideways one must not add another.
    expect(layout(false, [true]).containerHeight).toBe(CARD_HEIGHT)
    expect(layout(false, [false, true]).containerHeight).toBe(CARD_HEIGHT + PEEK)
  })

  // The upright ladder counts rungs, not raw indices, so a sideways attachment earlier in
  // the list doesn't leave a gap in the peek ladder above the host.
  it('does not let a sideways attachment consume an upright ladder rung', () => {
    const { attachments } = layout(false, [true, false, false])

    expect(attachments[1]?.top).toBe(0)
    expect(attachments[2]?.top).toBe(PEEK)
  })

  // Rows are bottom-aligned (`alignItems: flex-end`), so the host's box must end flush
  // with the container bottom. That keeps an attached permanent on the same baseline as
  // an unattached one whether or not either is tapped.
  it('sits the host box flush with the container bottom in every orientation', () => {
    for (const hostTapped of [false, true]) {
      for (const attachmentTapped of [false, true]) {
        const { host, containerHeight } = layout(hostTapped, [attachmentTapped])
        expect(host.top + CARD_HEIGHT).toBe(containerHeight)
      }
    }
  })

  // A card's visual center must not move when it rotates — otherwise tapping a
  // permanent would slide it sideways within its slot.
  it('centers every card box on a shared vertical axis', () => {
    const { attachments, host, containerWidth } = layout(true, [false, true])
    const center = containerWidth / 2

    for (const box of [...attachments, host]) {
      expect(box.left + box.width / 2).toBe(center)
    }
  })

  it('reserves the sideways footprint when only an attachment is tapped', () => {
    // The host stays upright, but the stack still has to make room for the sideways
    // Equipment behind it or it would overlap its neighbours in the row.
    expect(layout(false, [true]).containerWidth).toBe(SIDEWAYS_BOX + GUTTER)
  })

  it('grows the container by one peek per attachment', () => {
    expect(layout(false, []).containerHeight).toBe(CARD_HEIGHT)
    expect(layout(false, [false]).containerHeight).toBe(CARD_HEIGHT + PEEK)
    expect(layout(false, [false, false]).containerHeight).toBe(CARD_HEIGHT + 2 * PEEK)
  })
})
