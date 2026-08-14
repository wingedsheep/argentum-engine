package com.wingedsheep.mtg.sets.definitions.leb

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Limited Edition Beta (1993)
 *
 * The second Magic: The Gathering print run — a near-identical reprint of Limited Edition Alpha
 * with corrected templating and the two cards accidentally omitted from Alpha (Circle of
 * Protection: Black and Volcanic Island). Almost every card has its canonical
 * [CardDefinition] in LEA's package (the earliest real printing), so this set contributes mostly
 * `Printing` rows; only the two Alpha-omitted cards debut canonically here.
 *
 * Basic lands are the exception: Beta's own three-art-variant Plains/Island/Swamp/Mountain/Forest
 * (cards 288-302) are registered here directly rather than through the canonical/Printing model —
 * every set with its own basic-land art is equally "canonical" for that art, so there's no
 * fallback to Portal's basics needed once all five colors are covered.
 *
 * Set Code: LEB
 * Release Date: October 4, 1993
 * Card Count: 302
 */
object BetaSet : MtgSet {

    override val code = "LEB"
    override val displayName = "Limited Edition Beta"
    override val releaseDate = "1993-10-04"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.leb.cards"
}
