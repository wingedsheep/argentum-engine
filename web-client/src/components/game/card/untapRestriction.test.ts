import { describe, it, expect } from 'vitest'
import { untapRestrictionOf } from './untapRestriction'
import type { ClientCard } from '@/types'
import { AbilityFlag, entityId } from '@/types'

/** Minimal battlefield permanent; override only the axis under test. */
function permanent(over: Record<string, unknown> = {}): ClientCard {
  return {
    id: entityId('p1'),
    name: 'Goblin Sharpshooter',
    abilityFlags: [],
    keywords: [],
    counters: {},
    isTapped: false,
    isExerted: false,
    ...over,
  } as unknown as ClientCard
}

describe('untapRestrictionOf', () => {
  it('reports nothing for a permanent that untaps normally', () => {
    expect(untapRestrictionOf(permanent())).toBeNull()
  })

  it('tolerates a card whose abilityFlags the server omitted', () => {
    expect(untapRestrictionOf(permanent({ abilityFlags: undefined }))).toBeNull()
  })

  it("reports DOESNT_UNTAP (Goblin Sharpshooter, Charmed Sleep)", () => {
    const restriction = untapRestrictionOf(permanent({ abilityFlags: [AbilityFlag.DOESNT_UNTAP] }))
    expect(restriction?.kind).toBe('DOESNT_UNTAP')
    expect(restriction?.permanent).toBe(true)
  })

  it('reports CANT_BECOME_UNTAPPED (Spider-Woman, Blossombind)', () => {
    const restriction = untapRestrictionOf(
      permanent({ abilityFlags: [AbilityFlag.CANT_BECOME_UNTAPPED] }),
    )
    expect(restriction?.kind).toBe('CANT_BECOME_UNTAPPED')
    expect(restriction?.permanent).toBe(true)
  })

  it('reports exert (CR 701.43a) as the weakest, expiring restriction', () => {
    const restriction = untapRestrictionOf(permanent({ isExerted: true }))
    expect(restriction?.kind).toBe('EXERTED')
    expect(restriction?.permanent).toBe(false)
  })

  it('shows only the strongest restriction when several apply', () => {
    // A Spider-Woman-locked creature that was also exerted this turn is not usefully
    // described as "won't untap next turn" — it never untaps at all.
    expect(
      untapRestrictionOf(
        permanent({
          abilityFlags: [AbilityFlag.DOESNT_UNTAP, AbilityFlag.CANT_BECOME_UNTAPPED],
          isExerted: true,
        }),
      )?.kind,
    ).toBe('CANT_BECOME_UNTAPPED')

    expect(
      untapRestrictionOf(permanent({ abilityFlags: [AbilityFlag.DOESNT_UNTAP], isExerted: true }))
        ?.kind,
    ).toBe('DOESNT_UNTAP')
  })

  it('ignores unrelated ability flags', () => {
    expect(
      untapRestrictionOf(
        permanent({ abilityFlags: [AbilityFlag.CANT_BE_BLOCKED, AbilityFlag.CANT_RECEIVE_COUNTERS] }),
      ),
    ).toBeNull()
  })

  it('does not treat MAY_NOT_UNTAP as a restriction — it is the controller\'s option', () => {
    // Everglove Courier: the player *may* choose not to untap it. That is a decision the
    // server raises at the untap step, not a state that keeps the permanent tapped.
    expect(untapRestrictionOf(permanent({ abilityFlags: [AbilityFlag.MAY_NOT_UNTAP] }))).toBeNull()
  })

  it('reports the restriction on an untapped permanent too — tapping it is one-way', () => {
    const restriction = untapRestrictionOf(
      permanent({ isTapped: false, abilityFlags: [AbilityFlag.DOESNT_UNTAP] }),
    )
    expect(restriction?.kind).toBe('DOESNT_UNTAP')
  })
})
