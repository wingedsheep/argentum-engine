package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.Serializable

/**
 * Where an instant or sorcery goes when it would leave the stack for its owner's graveyard —
 * on resolution, on being countered, or on fizzling (CR 608.2m / CR 701.5a).
 *
 * This is the vocabulary behind the *cast-this-way rider*: an effect that lets you cast a card
 * from somewhere it normally couldn't be cast from often also redirects where the spell ends up,
 * so the same card can't simply be recast next turn. The two spellings printed on cards are the
 * two members here — "exile it instead" (Jetsam, Daring Waverider) and "put it on the bottom of
 * its owner's library instead" (Kylox's Voltstrider).
 *
 * It is deliberately *not* a general zone-change replacement. `RedirectZoneChange` is that, and it
 * applies to any card heading to a graveyard from anywhere; this rider is scoped to one spell the
 * granting effect is casting, and it is stamped on that card rather than registered as a
 * continuous effect. A card that carries neither goes to its owner's graveyard as usual.
 */
@Serializable
enum class AfterResolveDestination {
    /** Exile the card instead of putting it into its owner's graveyard. */
    EXILE,

    /**
     * Put the card on the bottom of its owner's library instead. No shuffle — the card goes
     * under the rest of the library, which is what "on the bottom of its owner's library" means
     * (contrast the Omen face, which shuffles).
     */
    BOTTOM_OF_LIBRARY,
}
