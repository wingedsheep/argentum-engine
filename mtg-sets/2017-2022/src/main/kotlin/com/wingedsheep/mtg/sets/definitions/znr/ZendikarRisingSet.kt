package com.wingedsheep.mtg.sets.definitions.znr

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
 * Zendikar Rising
 *
 * Set Code: ZNR
 */
object ZendikarRisingSet : MtgSet {

    override val code = "ZNR"
    override val displayName = "Zendikar Rising"
    override val releaseDate = "2020-09-25"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Draft Booster: 10 commons, 3 uncommons, a rare or mythic and a basic land (supplied at deck
     * building), with exactly one modal double-faced card in every pack — an uncommon in place of one
     * of the uncommons, or the pack's rare or mythic (MTGJSON `booster.draft`: the double-faced card
     * holds the rare slot in 81 packs of 444). ZNR prints no common double-faced cards. On both rare
     * sheets each rare appears twice as often as each mythic: 15 of 81 double-faced and 45 of 363
     * single-faced rare-slot draws are mythic. The foil replacing a common in a third of packs and
     * the showcase / borderless treatments are cosmetic and not modelled.
     */
    override val boosterStrategy: BoosterStrategy = DedicatedSlotBooster(
        base = StandardBooster(commons = 10, uncommons = 2, mythicChance = 45.0 / 363),
        slots = listOf(
            DedicatedSlot(
                SheetSelector.DoubleFaced,
                mapOf(Rarity.UNCOMMON to 1.0),
                rareChance = 81.0 / 444,
                mythicChance = 15.0 / 81,
            ),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.znr.cards"
}
