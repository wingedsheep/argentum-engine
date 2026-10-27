package com.wingedsheep.mtg.sets.definitions.khm

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Kaldheim Set (2021)
 *
 * Kaldheim is a Norse mythology-inspired plane themed around its ten
 * realms, with sagas, modal double-faced cards, and the Boast mechanic.
 *
 * Set Code: KHM
 * Release Date: February 5, 2021
 * Card Count: 285
 */
object KaldheimSet : MtgSet {

    override val code = "KHM"
    override val displayName = "Kaldheim"
    override val releaseDate = "2021-02-05"
    override val block = "Kaldheim"

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
     * Draft Booster: 10 commons, 3 uncommons, a rare or mythic and one snow land — a snow-covered
     * basic or one of the ten common snow duals (WotC's "Kaldheim Product Overview"). The duals open
     * only in that slot. Its sheet holds each dual 5 times and each of the ten snow-basic arts 7
     * times, so a dual opens 50 times in 120; each mythic opens half as often as each rare (64 rares,
     * 20 mythics), a mythic chance of 20/148 (MTGJSON `booster.draft`). The snow basics are Ice Age
     * reprints that reach the slot through the pool, so [LandSlotBooster.basicLands] stays empty —
     * nonsnow basics never open in a Kaldheim pack. The foil that replaces a common in one pack in
     * three and the showcase / borderless treatments are not modelled.
     */
    override val boosterStrategy: BoosterStrategy by lazy {
        LandSlotBooster(
            base = StandardBooster(commons = 10, mythicChance = 20.0 / 148),
            nonbasicLandNames = setOf(
                "Alpine Meadow", "Arctic Treeline", "Glacial Floodplain", "Highland Forest", "Ice Tunnel",
                "Rimewood Falls", "Snowfield Sinkhole", "Sulfurous Mire", "Volatile Fjord", "Woodland Chasm",
            ),
            basicLands = emptyList(),
            nonbasicChance = 50.0 / 120,
        )
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.khm.cards"
}
