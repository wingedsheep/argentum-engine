package com.wingedsheep.mtg.sets.definitions.bfz

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Battle for Zendikar (2015)
 *
 * Every card of the set is implemented: canonicals for the cards first printed here, and
 * `Printing` rows for the reprints.
 *
 * Set Code: BFZ
 * Release Date: October 2, 2015
 */
object BattleForZendikarSet : MtgSet {

    override val code = "BFZ"
    override val displayName = "Battle for Zendikar"
    override val releaseDate = "2015-10-02"
    override val block = "Battle for Zendikar"

    /**
     * Draft Booster: 10 commons, 3 uncommons and a rare or mythic (rares print twice on the sheet,
     * mythics once: 53 rares and 15 mythics make the upgrade 15 in 121), plus one of the set's
     * full-art basics, supplied at deck building rather than opened. The premium foil (or Zendikar
     * Expedition) that replaced a common in some packs is cosmetic and not modelled; the
     * Expeditions are their own set (EXP), never part of this pool. Figures from mtg.wtf/pack/bfz
     * (taw/magic-search-engine `data/boosters/bfz`).
     */
    override val boosterStrategy: BoosterStrategy = StandardBooster(commons = 10, mythicChance = 15.0 / 121)

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    /** Battle for Zendikar's own basics: one representative full-art printing per type. */
    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.bfz.cards"
}
