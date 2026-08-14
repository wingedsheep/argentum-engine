package com.wingedsheep.mtg.sets.definitions.atq

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Antiquities (1994)
 *
 * Set Code: ATQ
 * Release Date: 1994-03-04
 *
 * Scaffolded as the canonical home for cards reprinted in later sets (e.g. Eighth
 * Edition), and since filled in: all 84 buildable cards are implemented, so the set is
 * draftable (not `incomplete`). The 85th, Bronze Tablet, needs the ante zone the engine
 * doesn't have and is listed in `coverage/card-exclusions.json` as never-to-implement.
 */
object AntiquitiesSet : MtgSet {

    override val code = "ATQ"
    override val displayName = "Antiquities"
    override val releaseDate = "1994-03-04"
    override val basicLandsFallback = PortalSet

    override val cards: List<CardDefinition> by lazy {
        // Self-stamp [code] onto every card, honouring the MtgSet contract that `cards` is already
        // set-stamped. This makes `CardDefinition.setCode` a reliable "originally printed in ATQ"
        // signal for every consumer (engine tests included), not only the game-server load path —
        // which is what Golgothian Sylex's OriginallyPrintedInSet("ATQ") predicate reads.
        CardDiscovery.findIn(CARDS_PACKAGE).map { if (it.setCode == null) it.copy(setCode = code) else it }
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.atq.cards"
}
