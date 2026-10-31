package com.wingedsheep.mtg.sets.definitions.iko

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Ikoria: Lair of Behemoths
 *
 * Set Code: IKO
 */
object IkoriaSet : MtgSet {

    override val code = "IKO"
    override val displayName = "Ikoria: Lair of Behemoths"
    override val releaseDate = "2020-04-24"

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
     * Draft Booster: 10 commons, 3 uncommons, 1 rare or mythic, and a land slot holding one of the ten
     * common gain lands or a basic land, half the time each (each gain land 1/20, each of the 15 basic
     * arts 1/30). The rare slot weights rares twice to mythics' once (53 rares, 15 mythics -> 15/121).
     * Showcase variants and the foil replacing a common in a third of packs are cosmetic, not modelled.
     * Source: the IKO draft booster sheets as kept in taw/magic-search-engine (mtg.wtf/pack/iko-draft).
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        LandSlotBooster(
            base = StandardBooster(commons = 10, mythicChance = 15.0 / 121),
            nonbasicLandNames = GAIN_LANDS,
            basicLands = basicLands.filter { it.metadata.inBooster },
            nonbasicChance = 0.5,
        )
    }

    private val GAIN_LANDS = setOf(
        "Bloodfell Caves", "Blossoming Sands", "Dismal Backwater", "Jungle Hollow", "Rugged Highlands",
        "Scoured Barrens", "Swiftwater Cliffs", "Thornwood Falls", "Tranquil Cove", "Wind-Scarred Crag",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.iko.cards"
}
