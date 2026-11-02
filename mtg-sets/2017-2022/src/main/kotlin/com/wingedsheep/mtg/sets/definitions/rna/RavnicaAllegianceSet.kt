package com.wingedsheep.mtg.sets.definitions.rna

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Ravnica Allegiance (2019)
 *
 * The second half of the Guilds of Ravnica block — Azorius, Gruul, Orzhov, Rakdos, and Simic.
 *
 * Set Code: RNA
 * Release Date: January 25, 2019
 */
object RavnicaAllegianceSet : MtgSet {

    override val code = "RNA"
    override val displayName = "Ravnica Allegiance"
    override val releaseDate = "2019-01-25"

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
     * Draft Booster: 10 commons, 3 uncommons, 1 rare or mythic, and a land slot that always holds one
     * of the five Guildgates (two arts each), which never appear on the common sheet. Gateway Plaza
     * stays an ordinary common. The rare slot weights rares twice to mythics' once (53 rares,
     * 15 mythics -> 15/121). The foil replacing a common in 9 of 40 packs is cosmetic and not
     * modelled. Planeswalker Deck cards (#265-272) and the buy-a-box The Haunt of Hightower (#273)
     * carry `inBooster = false`. Source: the RNA draft booster sheets as kept in
     * taw/magic-search-engine (mtg.wtf/pack/rna).
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        LandSlotBooster(
            base = StandardBooster(commons = 10, mythicChance = 15.0 / 121),
            nonbasicLandNames = LAND_SLOT_GUILDGATES,
            basicLands = basicLands.filter { it.metadata.inBooster },
            nonbasicChance = 1.0,
        )
    }

    private val LAND_SLOT_GUILDGATES = setOf(
        "Azorius Guildgate", "Gruul Guildgate", "Orzhov Guildgate", "Rakdos Guildgate", "Simic Guildgate",
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.rna.cards"
}
