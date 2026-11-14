package com.wingedsheep.mtg.sets.definitions.ori

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic Origins (2015)
 *
 * Every card of the set is implemented: canonicals for the cards first printed here, and
 * `Printing` rows for the reprints.
 *
 * Set Code: ORI
 * Release Date: 2015-07-17
 */
object MagicOriginsSet : MtgSet {

    override val code = "ORI"
    override val displayName = "Magic Origins"
    override val releaseDate = "2015-07-17"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (rares print twice on the sheet,
     * mythics once: 55 rares and 16 mythics make the upgrade 16 in 126), plus a basic land, supplied
     * at deck building. The foil that replaced a common in 9 packs in 40 is cosmetic and not
     * modelled. The 16 Welcome Deck cards (#273-288) never opened in packs, so their `Printing`
     * rows are `inBooster = false`. Figures from mtg.wtf/pack/ori (taw/magic-search-engine).
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 16.0 / 126)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ori.cards"
}
