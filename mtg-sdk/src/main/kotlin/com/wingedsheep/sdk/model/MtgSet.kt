package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.PlayBooster
import com.wingedsheep.sdk.limited.StandardBooster

/**
 * A Magic: The Gathering set: a named collection of card definitions that can be
 * registered into the engine and (optionally) used for sealed/draft.
 *
 * Implementations are expected to self-stamp the [setCode] on every card returned
 * by [cards] and [basicLands], so consumers do not have to remember to call
 * `.copy(setCode = ...)`.
 */
interface MtgSet {

    /** Three-letter set code (e.g. "EOE"). Stable identifier across the codebase. */
    val code: String

    /** Display name (e.g. "Edge of Eternities"). */
    val displayName: String

    /**
     * Set release date in ISO `YYYY-MM-DD` form, or null if unknown.
     * Used by the deckbuilder to sort sets chronologically and to display the year.
     */
    val releaseDate: String? get() = null

    /** All non-basic-land card definitions in the set, with [code] already stamped. */
    val cards: List<CardDefinition>

    /** Basic land definitions, with [code] already stamped. Empty if the set has none. */
    val basicLands: List<CardDefinition> get() = emptyList()

    /** Block (e.g. "Onslaught") if the set is part of one, otherwise null. */
    val block: String? get() = null

    /** True when this set's [cards] list is incomplete relative to the official set. */
    val incomplete: Boolean get() = false

    /**
     * Strategy that turns the set's card pool into a single booster pack.
     *
     * The default follows the set's era, derived from [releaseDate]:
     *  - before Shards of Alara: classic 15-card [StandardBooster] (11C / 3U / 1R)
     *  - Shards of Alara (2008-10-03) to Murders at Karlov Manor: [StandardBooster]
     *    with 10 commons — paper packs swapped the 11th common for a basic land
     *    (omitted here; the generator supplies basics at deck building) and
     *    introduced the mythic upgrade on the rare slot
     *  - Murders at Karlov Manor (2024-02-09) onward: the [PlayBooster] division
     *
     * Sets without a release date get the classic booster. Sets override this to
     * express custom slot rules (guaranteed legendary, commander draft, etc.).
     */
    val boosterStrategy: BoosterStrategy
        get() {
            val date = releaseDate ?: ""
            return when {
                date >= PLAY_BOOSTER_ERA_START -> PlayBooster()
                date >= MYTHIC_BOOSTER_ERA_START -> StandardBooster(commons = 10)
                else -> StandardBooster()
            }
        }

    /**
     * If this set has no basic lands of its own, the set whose lands should be
     * registered alongside it (Scourge and Legions reuse Onslaught lands).
     */
    val basicLandsFallback: MtgSet? get() = null

    /**
     * Whether the set is wired into the booster generator for sealed/draft.
     *
     * Defaults to true: a finished set shouldn't have to opt in to being draftable. Sets that
     * genuinely can't carry a limited environment — Commander/duel decks, Jumpstart, planar sets —
     * override this to false. Work-in-progress sets don't need to touch it: [incomplete] already
     * keeps them out of `fullyImplemented` (and so behind the picker's partial-sets toggle).
     */
    val sealedSupported: Boolean get() = true

    /**
     * True for extension sets — bonus sheets and other supplemental releases (e.g. The Big Score
     * alongside Outlaws of Thunder Junction) whose pool is too thin to carry a sealed/draft pool
     * on its own. Extension sets are still fully selectable, but only *in combination with* at
     * least one regular set: lobbies reject a selection consisting solely of extension sets, and
     * random-set rolls never land on one.
     */
    val extensionSet: Boolean get() = false

    /**
     * Probability in `[0.0, 1.0]` that an individual card rolled into a booster is shown with one
     * of its alternate-frame printings (showcase / borderless) instead of its canonical art. The
     * roll is per card and only fires for cards that actually have an alternate-frame [Printing]
     * in [printings]; sets with no such printings can leave this at the default of 0.0.
     */
    val boosterVariantChance: Double get() = 0.0

    /**
     * Per-printing rows this set contributes to [com.wingedsheep.engine.registry.PrintingRegistry].
     *
     * Most printings are synthesised at startup from each registered [CardDefinition]'s
     * `setCode` + `metadata.collectorNumber`, so this list can stay empty for the canonical
     * printing of each card. Use it to register **additional** printings — e.g. when a card
     * is reprinted in a later set, the canonical [CardDefinition] (script, types, P/T) lives
     * in the original set's package, but the new set's package can contribute a [Printing]
     * row carrying the reprint's art, set code, collector number, and Scryfall metadata.
     *
     * Each entry's `name` must match an existing [CardDefinition.name] in some registered set.
     */
    val printings: List<Printing> get() = emptyList()

    /**
     * Token art this set prints, consulted when one of its cards creates a token.
     *
     * Tokens have no [CardDefinition] and no [Printing] row of their own, so this is where a set
     * says "my Cat token looks like *this*". The engine resolves art as: an explicit `imageUri`
     * on the effect → this list, for the set the creating card was printed in → the engine-wide
     * generic fallback by creature type. Declaring art here rather than on the card's
     * `CreateToken` effect is what makes a reprint mint its own set's token instead of the
     * original's.
     *
     * Entries are matched by name plus whatever [TokenPrinting] discriminators are pinned; see
     * [TokenPrinting.matches]. A set that printed one token with several illustrations declares a row
     * per art — a batch of tokens created at once is dealt out of them in order.
     */
    val tokenArt: List<TokenPrinting> get() = emptyList()

    companion object {
        /**
         * Release date of Shards of Alara, the first set with mythic rares and
         * the 10-common booster (a basic land took the 11th common's slot).
         * ISO dates compare correctly as strings.
         */
        const val MYTHIC_BOOSTER_ERA_START = "2008-10-03"

        /**
         * Release date of Murders at Karlov Manor, the first set printed as
         * 14-card Play Boosters.
         */
        const val PLAY_BOOSTER_ERA_START = "2024-02-09"
    }
}
