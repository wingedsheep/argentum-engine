package com.wingedsheep.mtg.sets.definitions.soi

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.DedicatedSlot
import com.wingedsheep.sdk.limited.DedicatedSlotBooster
import com.wingedsheep.sdk.limited.SheetSelector
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shadows over Innistrad (2016)
 *
 * Set Code: SOI
 * Release Date: April 8, 2016
 */
object ShadowsOverInnistradSet : MtgSet {

    override val code = "SOI"
    override val displayName = "Shadows over Innistrad"
    override val releaseDate = "2016-04-08"
    override val block = "Shadows over Innistrad"

    /**
     * Draft Booster: 8 single-faced commons, 3 single-faced uncommons and a single-faced rare or mythic, a
     * double-faced common or uncommon, and one more slot that is a single-faced common in seven packs in eight
     * and a double-faced rare or mythic in the eighth. The double-faced common/uncommon sheet prints each of
     * its 4 commons 5 times and each of its 20 uncommons twice (so a pack's double-faced card is a common a
     * third of the time); the rare sheet prints each of its 6 rares twice and each of its 3 mythics once. The
     * basic land is supplied at deck building, and the traditional foil that replaced a common in 9 packs in 40
     * is not modelled. Figures from taw/magic-search-engine `data/boosters/soi/draft.yaml`, after lethe.xyz's
     * SOI collation notes — the same shape as Eldritch Moon's, whose sheet adds the meld cards.
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        DedicatedSlotBooster(
            base = StandardBooster(commons = 8, uncommons = 3),
            slots = listOf(
                DedicatedSlot(SheetSelector.DoubleFaced, mapOf(Rarity.COMMON to 4.0 * 5, Rarity.UNCOMMON to 20.0 * 2)),
                DedicatedSlot(
                    SheetSelector.DoubleFaced,
                    mapOf(Rarity.COMMON to 7.0, Rarity.RARE to 12.0 / 15, Rarity.MYTHIC to 3.0 / 15),
                    ordinaryRarities = setOf(Rarity.COMMON),
                ),
            ),
        )
    }

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.soi.cards"
}
