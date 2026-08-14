import type { ClientCard } from '@/types/gameState'
import { AbilityFlag } from '@/types/enums'

/**
 * "This permanent won't untap" — the one status the battlefield never showed.
 *
 * Three different server signals mean the same thing to a player looking at the board, and until
 * now only the third had any visual at all:
 *
 *  - `CANT_BECOME_UNTAPPED` (Spider-Woman, Blossombind) — no untap source works on it, not even an
 *    "untap target permanent" effect.
 *  - `DOESNT_UNTAP` (Goblin Sharpshooter, Charmed Sleep, Crippling Chill, Tsabo's Web) — CR 502.3:
 *    it is skipped by its controller's untap step, but an untap *effect* still works.
 *  - Exerted (CR 701.43a) — the same skip, once, at its controller's next untap step.
 *
 * They form a strict ladder, so a permanent carrying several shows only the strongest: a creature
 * that can't become untapped at all is not usefully described as also "exerted". Stun counters are
 * deliberately not in the ladder — they already render as their own counter badge with a count,
 * which says more than a lock would (how many untaps are still owed).
 *
 * This is a pure read of state the server already sends on `ClientCard`; nothing here decides
 * rules. The engine's own gate is `ProjectedState.doesntUntapDuringUntapStep`.
 */
export type UntapRestrictionKind = 'CANT_BECOME_UNTAPPED' | 'DOESNT_UNTAP' | 'EXERTED'

export interface UntapRestriction {
  readonly kind: UntapRestrictionKind
  /** Tooltip / aria text, written about the permanent rather than addressed to its controller. */
  readonly label: string
  /**
   * True for the permanent-until-removed restrictions, false for the one-shot exert marker.
   * Drives the badge's emphasis: a lock that expires next untap step should not shout as loudly
   * as one that never will.
   */
  readonly permanent: boolean
}

const RESTRICTIONS: Record<UntapRestrictionKind, UntapRestriction> = {
  CANT_BECOME_UNTAPPED: {
    kind: 'CANT_BECOME_UNTAPPED',
    label: "Can't become untapped — no untap step or untap effect will untap it",
    permanent: true,
  },
  DOESNT_UNTAP: {
    kind: 'DOESNT_UNTAP',
    label: "Doesn't untap during its controller's untap step",
    permanent: true,
  },
  EXERTED: {
    kind: 'EXERTED',
    label: "Exerted — won't untap during its controller's next untap step",
    permanent: false,
  },
}

/**
 * The strongest untap restriction currently on [card], or null when it untaps normally.
 *
 * Reports the restriction regardless of whether the permanent is tapped right now: on an untapped
 * permanent it is a warning that tapping it is one-way, which is exactly the read a player needs
 * *before* they crew, convoke, or attack with it.
 */
export function untapRestrictionOf(card: ClientCard): UntapRestriction | null {
  const flags = card.abilityFlags ?? []
  if (flags.includes(AbilityFlag.CANT_BECOME_UNTAPPED)) return RESTRICTIONS.CANT_BECOME_UNTAPPED
  if (flags.includes(AbilityFlag.DOESNT_UNTAP)) return RESTRICTIONS.DOESNT_UNTAP
  if (card.isExerted === true) return RESTRICTIONS.EXERTED
  return null
}
