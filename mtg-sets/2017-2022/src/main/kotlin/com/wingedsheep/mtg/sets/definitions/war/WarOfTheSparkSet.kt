package com.wingedsheep.mtg.sets.definitions.war

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
 * War of the Spark (2019)
 *
 * The conclusion of the Bolas arc, themed around planeswalkers, amass, and proliferate.
 *
 * Set Code: WAR
 * Release Date: May 3, 2019
 */
object WarOfTheSparkSet : MtgSet {

    override val code = "WAR"
    override val displayName = "War of the Spark"
    override val releaseDate = "2019-05-03"

    /**
     * Draft Booster: ten commons, three uncommons, a rare or mythic, and exactly one planeswalker
     * (the set's advertised planeswalker in every pack). The
     * planeswalker is the pack's rare or mythic in 29 of 121 packs (beside three nonplaneswalker
     * uncommons), otherwise an uncommon planeswalker beside two nonplaneswalker uncommons and a
     * nonplaneswalker rare — taw/magic-search-engine `war/draft.yaml`, which bars two-planeswalker
     * packs. Rares print at twice a mythic's rate on both sheets, so the mythic upgrade is 3/29 for
     * the 13 + 3 planeswalker rares/mythics and 12/92 for the other 40 + 12. The basic land and the
     * foil replacing a common are omitted (basics are supplied at deck building); the Planeswalker
     * Deck exclusives (#265-275) carry `inBooster = false`.
     */
    override val boosterStrategy: BoosterStrategy = DedicatedSlotBooster(
        base = StandardBooster(commons = 10, uncommons = 2, mythicChance = 12.0 / 92),
        slots = listOf(
            DedicatedSlot(
                SheetSelector.Planeswalker,
                mapOf(Rarity.UNCOMMON to 1.0),
                rareChance = 29.0 / 121,
                mythicChance = 3.0 / 29,
            ),
        ),
    )

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.war.cards"
}
